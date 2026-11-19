package cl.duoc.activaciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.activaciones.data.local.OportunidadEntity
import cl.duoc.activaciones.data.repository.OportunidadRepository
import cl.duoc.activaciones.data.repository.Repositorio
import cl.duoc.activaciones.model.Contacto
import cl.duoc.activaciones.model.EstadoOportunidad
import cl.duoc.activaciones.model.Filtro
import cl.duoc.activaciones.model.OportunidadErrores
import cl.duoc.activaciones.model.OportunidadFuentes
import cl.duoc.activaciones.model.OportunidadUiState
import cl.duoc.activaciones.model.Organizacion
import cl.duoc.activaciones.model.Perfil
import cl.duoc.activaciones.model.Potencial
import cl.duoc.activaciones.model.Prioridad
import cl.duoc.activaciones.model.TipoEvento
import cl.duoc.activaciones.model.Usuario
import cl.duoc.activaciones.model.filtrar
import cl.duoc.activaciones.model.visibles
import cl.duoc.activaciones.util.Sesion
import cl.duoc.activaciones.util.aFecha
import cl.duoc.activaciones.util.parsearFecha
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/** ViewModel del módulo de oportunidades: lista (con filtros) y formulario crear/editar. */
@OptIn(ExperimentalCoroutinesApi::class)
class OportunidadViewModel(
    fuentes: OportunidadFuentes
) : ViewModel() {

    private val oportunidadRepo: OportunidadRepository = fuentes.oportunidadRepo
    private val repo: Repositorio = fuentes.repo
    private val sesion: Sesion = fuentes.sesion

    // Mantiene la llamada anterior: OportunidadViewModel(app.oportunidadRepo, app.repo, app.sesion)
    constructor(
        oportunidadRepo: OportunidadRepository,
        repo: Repositorio,
        sesion: Sesion
    ) : this(OportunidadFuentes(oportunidadRepo, repo, sesion))

    private fun <T> Flow<T>.estado(scope: CoroutineScope, inicial: T): StateFlow<T> =
        stateIn(scope, SharingStarted.WhileSubscribed(5000), inicial)

    // ---------- Lista ----------
    val usuario: StateFlow<Usuario?> = sesion.usuarioId
        .flatMapLatest { id -> if (id == null) flowOf<Usuario?>(null) else repo.usuario(id) }
        .estado(viewModelScope, null)

    val usuarios: StateFlow<List<Usuario>> = repo.usuarios().estado(viewModelScope, emptyList())
    val organizaciones: StateFlow<List<Organizacion>> = repo.organizaciones().estado(viewModelScope, emptyList())

    private val _filtro = MutableStateFlow(Filtro())
    val filtro: StateFlow<Filtro> = _filtro.asStateFlow()

    private val propias: Flow<List<OportunidadEntity>> =
        combine(oportunidadRepo.oportunidades(), usuario) { ops, u -> visibles(ops, u) }

    /** Se recalcula sola cuando Room emite cambios: la lista se actualiza al registrar o editar. */
    val lista: StateFlow<List<OportunidadEntity>> =
        combine(propias, _filtro) { ops, f -> filtrar(ops, f) }.estado(viewModelScope, emptyList())

    val regiones: StateFlow<List<String>> = propias
        .map { ops -> ops.map { it.region }.filter { it.isNotBlank() }.distinct().sorted() }
        .estado(viewModelScope, emptyList())

    fun actualizarFiltro(f: Filtro) {
        _filtro.value = f
    }

    fun eliminar(o: OportunidadEntity) {
        viewModelScope.launch { oportunidadRepo.eliminar(o) }
    }

    // ---------- Formulario ----------
    private val _uiState = MutableStateFlow(OportunidadUiState())
    val uiState: StateFlow<OportunidadUiState> = _uiState.asStateFlow()

    /** Contactos de la organización seleccionada en el formulario. */
    val contactos: StateFlow<List<Contacto>> = _uiState
        .map { it.organizacionId }
        .distinctUntilChanged()
        .flatMapLatest { orgId ->
            if (orgId == null) flowOf(emptyList<Contacto>()) else repo.contactos(orgId)
        }
        .estado(viewModelScope, emptyList())

    private var original: OportunidadEntity? = null
    private var idCargado: Long? = null

    fun cargar(id: Long) {
        if (idCargado == id) return
        idCargado = id
        if (id <= 0) {
            original = null
            _uiState.value = OportunidadUiState()
            return
        }
        viewModelScope.launch {
            val o = oportunidadRepo.oportunidadUnaVez(id) ?: return@launch
            // Mínimo privilegio: un ejecutivo no puede editar oportunidades ajenas
            val usuarioActual = sesion.usuarioId.value?.let { repo.usuarioUnaVez(it) }
            if (visibles(listOf(o), usuarioActual).isEmpty()) return@launch

            original = o
            _uiState.value = OportunidadUiState(
                nombre = o.nombre, tipo = o.tipo, descripcion = o.descripcion, fuente = o.fuente,
                prioridad = o.prioridad, potencial = o.potencial, estado = o.estado,
                observaciones = o.observaciones, organizacionId = o.organizacionId,
                responsableId = o.responsableId,
                fechaInicio = o.fechaInicio?.aFecha() ?: "", fechaTermino = o.fechaTermino?.aFecha() ?: "",
                horario = o.horario, comuna = o.comuna, region = o.region,
                publico = if (o.publicoEsperado > 0) o.publicoEsperado.toString() else "",
                condiciones = o.condiciones, costo = if (o.costo > 0) o.costo.toString() else "",
                referenciaGeografica = o.referenciaGeografica, fotoPath = o.fotoPath,
                codigoQr = o.codigoQr,
                documentoPath = o.documentoPath, documentoNombre = o.documentoNombre,
                esEdicion = true,
                contactoId = o.contactoId,
                espacio = o.espacio
            )
        }
    }

    // Cada cambio limpia el error de su propio campo
    fun onNombre(v: String) = _uiState.update { it.copy(nombre = v, errores = it.errores.copy(nombre = null)) }
    fun onTipo(v: TipoEvento) = _uiState.update { it.copy(tipo = v, errores = it.errores.copy(tipo = null)) }
    fun onDescripcion(v: String) = _uiState.update { it.copy(descripcion = v, errores = it.errores.copy(descripcion = null)) }
    fun onFuente(v: String) = _uiState.update { it.copy(fuente = v, errores = it.errores.copy(fuente = null)) }
    fun onPrioridad(v: Prioridad) = _uiState.update { it.copy(prioridad = v, errores = it.errores.copy(prioridad = null)) }
    fun onPotencial(v: Potencial) = _uiState.update { it.copy(potencial = v, errores = it.errores.copy(potencial = null)) }
    fun onEstado(v: EstadoOportunidad) = _uiState.update { it.copy(estado = v) }
    fun onObservaciones(v: String) = _uiState.update { it.copy(observaciones = v, errores = it.errores.copy(observaciones = null)) }

    // Si cambia la organización, el contacto anterior ya no es válido
    fun onOrganizacion(v: Long?) = _uiState.update {
        if (it.organizacionId == v) it
        else it.copy(
            organizacionId = v, contactoId = null,
            errores = it.errores.copy(organizacion = null, contacto = null)
        )
    }
    fun onContacto(v: Long?) = _uiState.update { it.copy(contactoId = v, errores = it.errores.copy(contacto = null)) }
    fun onEspacio(v: String) = _uiState.update { it.copy(espacio = v, errores = it.errores.copy(espacio = null)) }
    fun onResponsable(v: Long) = _uiState.update { it.copy(responsableId = v, errores = it.errores.copy(responsable = null)) }
    fun onFechaInicio(v: String) = _uiState.update {
        it.copy(fechaInicio = v, errores = it.errores.copy(fechaInicio = null, fechaTermino = null))
    }
    fun onFechaTermino(v: String) = _uiState.update { it.copy(fechaTermino = v, errores = it.errores.copy(fechaTermino = null)) }
    fun onHorario(v: String) = _uiState.update { it.copy(horario = v, errores = it.errores.copy(horario = null)) }

    // Si cambia la región, la comuna anterior ya no es válida
    fun onRegion(v: String) = _uiState.update {
        if (it.region == v) it
        else it.copy(region = v, comuna = "", errores = it.errores.copy(region = null, comuna = null))
    }
    fun onComuna(v: String) = _uiState.update { it.copy(comuna = v, errores = it.errores.copy(comuna = null)) }

    fun onPublico(v: String) = _uiState.update { it.copy(publico = v, errores = it.errores.copy(publico = null)) }
    fun onCondiciones(v: String) = _uiState.update { it.copy(condiciones = v, errores = it.errores.copy(condiciones = null)) }
    fun onCosto(v: String) = _uiState.update { it.copy(costo = v, errores = it.errores.copy(costo = null)) }

    fun onUbicacion(latitud: Double?, longitud: Double?) = _uiState.update {
        if (latitud == null || longitud == null) {
            it.copy(mensajeRecurso = "No se pudo obtener la ubicación. Activa el GPS e inténtalo de nuevo.")
        } else {
            it.copy(
                referenciaGeografica = String.format(Locale.US, "Lat %.5f, Lng %.5f", latitud, longitud),
                mensajeRecurso = "Ubicación registrada"
            )
        }
    }

    fun onFoto(path: String) = _uiState.update { it.copy(fotoPath = path, mensajeRecurso = "Foto de evidencia guardada") }

    // Se llama cuando vuelve el resultado del escáner QR
    fun onQr(contenido: String) = _uiState.update { it.copy(codigoQr = contenido, mensajeRecurso = "Código QR registrado") }

    // Se llama cuando el usuario elige un documento para adjuntar
    fun onDocumento(path: String, nombre: String) = _uiState.update {
        it.copy(documentoPath = path, documentoNombre = nombre, mensajeRecurso = "Documento adjuntado")
    }

    fun onMensajeRecurso(mensaje: String) = _uiState.update { it.copy(mensajeRecurso = mensaje) }

    /** Campo numérico obligatorio: debe ser un entero >= 0. */
    private fun errorEntero(texto: String, campo: String): String? = when {
        texto.isBlank() -> "$campo es obligatorio"
        texto.toIntOrNull()?.let { it >= 0 } != true -> "Debe ser un número entero positivo"
        else -> null
    }

    /** Valida en el ViewModel; la vista solo muestra los errores que quedan en el UiState. */
    fun guardar() {
        val s = _uiState.value
        if (s.guardando) return

        val inicio = if (s.fechaInicio.isBlank()) null else parsearFecha(s.fechaInicio)
        val termino = if (s.fechaTermino.isBlank()) null else parsearFecha(s.fechaTermino)

        val esSupervisorActual = usuario.value?.perfil == Perfil.SUPERVISOR
        // Solo se exige contacto si la organización elegida tiene contactos
        val requiereContacto = s.organizacionId != null && contactos.value.isNotEmpty()

        val errores = OportunidadErrores(
            nombre = when {
                s.nombre.isBlank() -> "El nombre es obligatorio"
                s.nombre.trim().length < 3 -> "Debe tener al menos 3 caracteres"
                else -> null
            },
            tipo = if (s.tipo == null) "Selecciona el tipo de oportunidad" else null,
            descripcion = if (s.descripcion.isBlank()) "La descripción es obligatoria" else null,
            fuente = if (s.fuente.isBlank()) "Indica la fuente de información" else null,
            prioridad = if (s.prioridad == null) "Selecciona la prioridad" else null,
            potencial = if (s.potencial == null) "Selecciona el potencial comercial" else null,
            observaciones = if (s.observaciones.isBlank()) "Las observaciones son obligatorias" else null,
            organizacion = if (s.organizacionId == null) "Selecciona la organización" else null,
            contacto = if (requiereContacto && s.contactoId == null) "Selecciona el contacto" else null,
            responsable = if (esSupervisorActual && s.responsableId == null) "Selecciona el responsable" else null,
            fechaInicio = when {
                s.fechaInicio.isBlank() -> "La fecha de inicio es obligatoria"
                inicio == null -> "Fecha inválida (dd/MM/aaaa)"
                else -> null
            },
            fechaTermino = when {
                s.fechaTermino.isBlank() -> "La fecha de término es obligatoria"
                termino == null -> "Fecha inválida (dd/MM/aaaa)"
                inicio != null && termino < inicio -> "El término no puede ser anterior al inicio"
                else -> null
            },
            horario = if (s.horario.isBlank()) "El horario es obligatorio" else null,
            region = if (s.region.isBlank()) "Selecciona la región" else null,
            comuna = if (s.region.isNotBlank() && s.comuna.isBlank()) "Selecciona la comuna" else null,
            publico = errorEntero(s.publico, "El público esperado"),
            espacio = if (s.espacio.isBlank()) "El espacio o stand es obligatorio" else null,
            condiciones = if (s.condiciones.isBlank()) "Las condiciones son obligatorias" else null,
            costo = errorEntero(s.costo, "El costo")
        )
        _uiState.update { it.copy(errores = errores) }
        if (errores.hayErrores) return

        val usuarioId = sesion.usuarioId.value ?: return
        val tipo = s.tipo ?: return
        val prioridad = s.prioridad ?: return
        val potencial = s.potencial ?: return
        _uiState.update { it.copy(guardando = true) }

        viewModelScope.launch {
            val ahora = System.currentTimeMillis()
            val base = original
            // Asignar responsable: solo el supervisor; el ejecutivo conserva el actual o queda él mismo
            val esSupervisor = repo.usuarioUnaVez(usuarioId)?.perfil == Perfil.SUPERVISOR
            val responsable = if (esSupervisor) {
                s.responsableId ?: base?.responsableId ?: usuarioId
            } else {
                base?.responsableId ?: usuarioId
            }
            val entidad = OportunidadEntity(
                id = base?.id ?: 0,
                nombre = s.nombre.trim(),
                tipo = tipo,
                descripcion = s.descripcion.trim(),
                fuente = s.fuente.trim(),
                fechaDeteccion = base?.fechaDeteccion ?: ahora,
                prioridad = prioridad,
                potencial = potencial,
                estado = s.estado,
                fechaCambioEstado = if (base == null || base.estado != s.estado) ahora else base.fechaCambioEstado,
                observaciones = s.observaciones.trim(),
                organizacionId = s.organizacionId,
                contactoId = if (s.organizacionId != null) s.contactoId else null,
                responsableId = responsable,
                fechaInicio = inicio,
                fechaTermino = termino,
                horario = s.horario.trim(),
                comuna = s.comuna.trim(),
                region = s.region.trim(),
                publicoEsperado = s.publico.toIntOrNull() ?: 0,
                condiciones = s.condiciones.trim(),
                espacio = s.espacio.trim(),
                costo = s.costo.toIntOrNull() ?: 0,
                referenciaGeografica = s.referenciaGeografica,
                fotoPath = s.fotoPath,
                codigoQr = s.codigoQr,
                documentoPath = s.documentoPath,
                documentoNombre = s.documentoNombre,
                fechaCierre = base?.fechaCierre // conserva el cierre al editar
            )
            oportunidadRepo.guardar(entidad)
            _uiState.update { it.copy(guardando = false, guardadoExitosamente = true) }
        }
    }
}