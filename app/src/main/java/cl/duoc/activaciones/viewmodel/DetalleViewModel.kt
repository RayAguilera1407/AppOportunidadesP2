package cl.duoc.activaciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.activaciones.data.local.OportunidadEntity
import cl.duoc.activaciones.data.repository.OportunidadRepository
import cl.duoc.activaciones.data.repository.Repositorio
import cl.duoc.activaciones.model.Contacto
import cl.duoc.activaciones.model.DIA_MS
import cl.duoc.activaciones.model.EstadoOportunidad
import cl.duoc.activaciones.model.Organizacion
import cl.duoc.activaciones.model.Perfil
import cl.duoc.activaciones.model.Prioridad
import cl.duoc.activaciones.model.ResultadoActivacion
import cl.duoc.activaciones.model.ResultadoErrores
import cl.duoc.activaciones.model.Seguimiento
import cl.duoc.activaciones.model.SeguimientoErrores
import cl.duoc.activaciones.model.TipoAccion
import cl.duoc.activaciones.model.Usuario
import cl.duoc.activaciones.model.visibles
import cl.duoc.activaciones.util.Sesion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleViewModel(
    private val repo: Repositorio,
    private val oportunidadRepo: OportunidadRepository,
    sesion: Sesion,
    private val id: Long
) : ViewModel() {

    private fun <T> Flow<T>.estado(inicial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), inicial)

    private val usuario: StateFlow<Usuario?> = sesion.usuarioId
        .flatMapLatest { uid -> if (uid == null) flowOf<Usuario?>(null) else repo.usuario(uid) }
        .estado(null)

    val esSupervisor: StateFlow<Boolean> = usuario.map { it?.perfil == Perfil.SUPERVISOR }.estado(false)

    val ejecutivos: StateFlow<List<Usuario>> =
        repo.usuarios().map { l -> l.filter { it.perfil == Perfil.EJECUTIVO } }.estado(emptyList())

    /** Si la oportunidad no es del usuario (y no es supervisor), llega null. */
    val oportunidad: StateFlow<OportunidadEntity?> =
        combine(oportunidadRepo.oportunidad(id), usuario) { o, u ->
            o?.takeIf { visibles(listOf(it), u).isNotEmpty() }
        }.estado(null)

    val organizacion: StateFlow<Organizacion?> =
        oportunidad.flatMapLatest { o ->
            val orgId = o?.organizacionId
            if (orgId == null) flowOf<Organizacion?>(null) else repo.organizacion(orgId)
        }.estado(null)

    val contactos: StateFlow<List<Contacto>> =
        oportunidad.flatMapLatest { o ->
            val orgId = o?.organizacionId
            if (orgId == null) flowOf(emptyList<Contacto>()) else repo.contactos(orgId)
        }.estado(emptyList())

    val historial: StateFlow<List<Seguimiento>> =
        oportunidadRepo.historial(id).estado(emptyList())

    val resultado: StateFlow<ResultadoActivacion?> =
        oportunidadRepo.resultado(id).estado(null)

    /** Regla de negocio: se cierra si está descartada, o si confirmó participación y ya tiene resultado. */
    val puedeCerrar: StateFlow<Boolean> = combine(oportunidad, resultado) { o, r ->
        o != null && (o.estado == EstadoOportunidad.DESCARTADA ||
                (o.estado == EstadoOportunidad.PARTICIPACION_CONFIRMADA && r != null))
    }.estado(false)

    // ---------- Errores de formularios ----------
    private val _erroresSeguimiento = MutableStateFlow(SeguimientoErrores())
    val erroresSeguimiento: StateFlow<SeguimientoErrores> = _erroresSeguimiento.asStateFlow()

    private val _erroresResultado = MutableStateFlow(ResultadoErrores())
    val erroresResultado: StateFlow<ResultadoErrores> = _erroresResultado.asStateFlow()

    fun limpiarErrores() {
        _erroresSeguimiento.value = SeguimientoErrores()
        _erroresResultado.value = ResultadoErrores()
    }

    private fun errorEntero(texto: String, obligatorio: Boolean = false): String? = when {
        texto.isBlank() -> if (obligatorio) "Campo obligatorio" else null
        texto.toIntOrNull()?.let { it >= 0 } != true -> "Debe ser un número entero positivo"
        else -> null
    }

    // ---------- Acciones ----------
    fun cambiarEstado(nuevo: EstadoOportunidad) {
        val actual = oportunidad.value ?: return
        viewModelScope.launch { oportunidadRepo.cambiarEstado(actual, nuevo) }
    }

    /** Solo el supervisor ajusta la prioridad desde el detalle. */
    fun cambiarPrioridad(nueva: Prioridad) {
        val actual = oportunidad.value ?: return
        if (!esSupervisor.value || actual.prioridad == nueva) return
        viewModelScope.launch { oportunidadRepo.guardar(actual.copy(prioridad = nueva)) }
    }

    /** Solo el supervisor reasigna el responsable. */
    fun reasignar(nuevoResponsableId: Long) {
        val actual = oportunidad.value ?: return
        if (!esSupervisor.value || actual.responsableId == nuevoResponsableId) return
        viewModelScope.launch { oportunidadRepo.guardar(actual.copy(responsableId = nuevoResponsableId)) }
    }

    fun agregarSeguimiento(
        tipo: TipoAccion, resultado: String, proxima: String, dias: String, alExito: () -> Unit
    ) {
        val o = oportunidad.value ?: return
        if (o.fechaCierre != null) return
        val err = SeguimientoErrores(
            resultado = when {
                resultado.isBlank() -> "Describe el resultado de la gestión"
                resultado.trim().length < 3 -> "Debe tener al menos 3 caracteres"
                else -> null
            },
            dias = errorEntero(dias)
        )
        _erroresSeguimiento.value = err
        if (err.hayErrores) return
        val ahora = System.currentTimeMillis()
        viewModelScope.launch {
            oportunidadRepo.agregarSeguimiento(
                Seguimiento(
                    oportunidadId = id, tipoAccion = tipo, fechaHora = ahora,
                    resultado = resultado.trim(), proximaAccion = proxima.trim(),
                    fechaProximoSeguimiento = dias.toIntOrNull()?.let { ahora + it * DIA_MS }
                )
            )
            alExito()
        }
    }

    fun eliminarSeguimiento(s: Seguimiento) {
        if (oportunidad.value == null) return
        viewModelScope.launch { oportunidadRepo.eliminarSeguimiento(s.id) }
    }

    fun guardarResultado(
        asistentes: String, contactos: String, inscripciones: String,
        evaluacion: String, alExito: () -> Unit
    ) {
        val o = oportunidad.value ?: return
        if (o.fechaCierre != null || o.estado != EstadoOportunidad.PARTICIPACION_CONFIRMADA) return
        val err = ResultadoErrores(
            asistentes = errorEntero(asistentes, obligatorio = true),
            contactos = errorEntero(contactos),
            inscripciones = errorEntero(inscripciones),
            evaluacion = if (evaluacion.isBlank()) "Indica el impacto o la evaluación" else null
        )
        _erroresResultado.value = err
        if (err.hayErrores) return
        viewModelScope.launch {
            oportunidadRepo.guardarResultado(
                ResultadoActivacion(
                    oportunidadId = id,
                    asistentes = asistentes.toInt(),
                    contactosObtenidos = contactos.toIntOrNull() ?: 0,
                    inscripciones = inscripciones.toIntOrNull() ?: 0,
                    evaluacion = evaluacion.trim()
                )
            )
            alExito()
        }
    }

    fun eliminar(alTerminar: () -> Unit) {
        val actual = oportunidad.value ?: return
        viewModelScope.launch {
            oportunidadRepo.eliminar(actual)
            alTerminar()
        }
    }

    fun cerrar() {
        if (!puedeCerrar.value) return
        val actual = oportunidad.value ?: return
        viewModelScope.launch { oportunidadRepo.cerrar(actual) }
    }

    fun reabrir() {
        val actual = oportunidad.value ?: return
        viewModelScope.launch { oportunidadRepo.reabrir(actual) }
    }
}