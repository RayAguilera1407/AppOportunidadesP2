package cl.duoc.activaciones.data.repository

import androidx.room.withTransaction
import cl.duoc.activaciones.data.local.AppDatabase
import cl.duoc.activaciones.model.EstadoOportunidad
import cl.duoc.activaciones.data.local.OportunidadEntity
import cl.duoc.activaciones.model.ResultadoActivacion
import cl.duoc.activaciones.model.Seguimiento
import cl.duoc.activaciones.model.TipoAccion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Acceso a datos del módulo de oportunidades. El ViewModel nunca toca el DAO directamente. */
class OportunidadRepository(private val db: AppDatabase) {
    private val dao = db.oportunidadDao()
    private val seguimientos = db.seguimientoDao()
    private val resultados = db.resultadoDao()

    fun oportunidades() = dao.todas()
    fun oportunidad(id: Long) = dao.porId(id)
    suspend fun oportunidadUnaVez(id: Long) = dao.porIdUnaVez(id)
    fun porOrganizacion(orgId: Long) = dao.porOrganizacion(orgId)

    fun todosLosSeguimientos() = seguimientos.todos()
    fun historial(opId: Long) = seguimientos.historial(opId)
    suspend fun agregarSeguimiento(s: Seguimiento) = seguimientos.insertar(s)
    suspend fun eliminarSeguimiento(id: Long) = seguimientos.eliminar(id)

    fun resultado(opId: Long) = resultados.porOportunidad(opId)
    suspend fun guardarResultado(r: ResultadoActivacion) = resultados.guardar(r)

    /** Inserta o actualiza. Si el estado cambió, deja el registro en el historial. */
    /** Inserta o actualiza. Deja en el historial los cambios de estado, prioridad y responsable. */
    suspend fun guardar(o: OportunidadEntity): Long = db.withTransaction {
        val ahora = System.currentTimeMillis()
        if (o.id == 0L) {
            val id = dao.insertar(o)
            seguimientos.insertar(
                Seguimiento(
                    oportunidadId = id, tipoAccion = TipoAccion.CAMBIO_ESTADO, fechaHora = ahora,
                    resultado = "Oportunidad registrada (${o.estado.etiqueta})"
                )
            )
            id
        } else {
            val anterior = dao.porIdUnaVez(o.id)
            dao.actualizar(o)
            if (anterior != null) {
                suspend fun registrar(texto: String) = seguimientos.insertar(
                    Seguimiento(
                        oportunidadId = o.id, tipoAccion = TipoAccion.CAMBIO_ESTADO,
                        fechaHora = ahora, resultado = texto
                    )
                )
                if (anterior.estado != o.estado) {
                    registrar("${anterior.estado.etiqueta} → ${o.estado.etiqueta}")
                }
                if (anterior.prioridad != o.prioridad) {
                    registrar("Prioridad: ${anterior.prioridad.etiqueta} → ${o.prioridad.etiqueta}")
                }
                if (anterior.responsableId != o.responsableId) {
                    val usuarios = db.usuarioDao()
                    val de = usuarios.porIdUnaVez(anterior.responsableId)?.nombre ?: "Sin asignar"
                    val a = usuarios.porIdUnaVez(o.responsableId)?.nombre ?: "Sin asignar"
                    registrar("Responsable: $de → $a")
                }
            }
            o.id
        }
    }

    /** Cierra la oportunidad y deja el cierre en el historial. */
    suspend fun cerrar(o: OportunidadEntity) = db.withTransaction {
        val ahora = System.currentTimeMillis()
        dao.actualizar(o.copy(fechaCierre = ahora))
        seguimientos.insertar(
            Seguimiento(
                oportunidadId = o.id, tipoAccion = TipoAccion.CIERRE, fechaHora = ahora,
                resultado = "Oportunidad cerrada (${o.estado.etiqueta})"
            )
        )
    }

    suspend fun reabrir(o: OportunidadEntity) = db.withTransaction {
        dao.actualizar(o.copy(fechaCierre = null))
        seguimientos.insertar(
            Seguimiento(
                oportunidadId = o.id, tipoAccion = TipoAccion.SEGUIMIENTO,
                fechaHora = System.currentTimeMillis(), resultado = "Oportunidad reabierta"
            )
        )
    }

    /** Elimina la oportunidad junto con su historial, resultado, foto y documento. */
    suspend fun eliminar(o: OportunidadEntity) {
        db.withTransaction {
            seguimientos.eliminarDeOportunidad(o.id)
            resultados.eliminarDeOportunidad(o.id)
            dao.eliminar(o.id)
        }
        withContext(Dispatchers.IO) {
            o.fotoPath?.let { File(it).delete() }
            o.documentoPath?.let { File(it).delete() }
        }
    }

    suspend fun cambiarEstado(o: OportunidadEntity, nuevo: EstadoOportunidad) {
        if (o.estado == nuevo) return
        guardar(o.copy(estado = nuevo, fechaCambioEstado = System.currentTimeMillis()))
    }
}
