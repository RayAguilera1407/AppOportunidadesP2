package cl.duoc.activaciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.activaciones.data.repository.EventoCatalogoRepository
import cl.duoc.activaciones.model.EventoCatalogo
import cl.duoc.activaciones.model.EventoCatalogoErrores
import cl.duoc.activaciones.model.EventoCatalogoUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/** Catálogo de eventos guardado en MySQL (XAMPP): listar, registrar, editar y eliminar. */
class EventoCatalogoViewModel(
    private val repository: EventoCatalogoRepository = EventoCatalogoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventoCatalogoUiState())
    val uiState: StateFlow<EventoCatalogoUiState> = _uiState.asStateFlow()

    init { cargar() }

    // ---------- Lista ----------
    fun cargar() {
        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val eventos = repository.listar()
                _uiState.update { it.copy(cargando = false, eventos = eventos) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(cargando = false, error = "No se pudo conectar con el servidor. Revisa que Apache y MySQL estén iniciados en XAMPP.")
                }
            }
        }
    }

    fun eliminar(evento: EventoCatalogo) {
        viewModelScope.launch {
            try {
                repository.eliminar(evento.id)
                _uiState.update { s -> s.copy(eventos = s.eventos.filterNot { it.id == evento.id }, mensaje = "Evento eliminado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(mensaje = "No se pudo eliminar: ${e.localizedMessage}") }
            }
        }
    }

    fun mensajeMostrado() = _uiState.update { it.copy(mensaje = null) }

    // ---------- Formulario ----------
    fun nuevo() = _uiState.update {
        it.copy(
            formularioAbierto = true, idEdicion = null, nombre = "", tipo = "", fecha = "",
            lugar = "", cupos = "", descripcion = "", errores = EventoCatalogoErrores()
        )
    }

    fun editar(e: EventoCatalogo) = _uiState.update {
        it.copy(
            formularioAbierto = true, idEdicion = e.id, nombre = e.nombre, tipo = e.tipo,
            fecha = aPantalla(e.fecha), lugar = e.lugar, cupos = e.cupos.toString(),
            descripcion = e.descripcion, errores = EventoCatalogoErrores()
        )
    }

    fun cerrarFormulario() = _uiState.update { it.copy(formularioAbierto = false) }

    // Cada cambio limpia el error de su campo
    fun onNombre(v: String) = _uiState.update { it.copy(nombre = v, errores = it.errores.copy(nombre = null)) }
    fun onTipo(v: String) = _uiState.update { it.copy(tipo = v, errores = it.errores.copy(tipo = null)) }
    fun onFecha(v: String) = _uiState.update { it.copy(fecha = v, errores = it.errores.copy(fecha = null)) }
    fun onLugar(v: String) = _uiState.update { it.copy(lugar = v, errores = it.errores.copy(lugar = null)) }
    fun onCupos(v: String) = _uiState.update { it.copy(cupos = v, errores = it.errores.copy(cupos = null)) }
    fun onDescripcion(v: String) = _uiState.update { it.copy(descripcion = v) }

    /** Valida en el ViewModel; si no hay errores, envía POST (nuevo) o PUT (edición). */
    fun guardar() {
        val s = _uiState.value
        if (s.guardando) return
        val fechaApi = aApi(s.fecha)
        val errores = EventoCatalogoErrores(
            nombre = when {
                s.nombre.isBlank() -> "El nombre es obligatorio"
                s.nombre.trim().length < 3 -> "Debe tener al menos 3 caracteres"
                else -> null
            },
            tipo = if (s.tipo.isBlank()) "Selecciona el tipo" else null,
            fecha = when {
                s.fecha.isBlank() -> "La fecha es obligatoria"
                fechaApi == null -> "Fecha inválida (dd/MM/aaaa)"
                else -> null
            },
            lugar = if (s.lugar.isBlank()) "El lugar es obligatorio" else null,
            cupos = when {
                s.cupos.isBlank() -> "Los cupos son obligatorios"
                s.cupos.toIntOrNull()?.let { it >= 0 } != true -> "Debe ser un número entero positivo"
                else -> null
            }
        )
        _uiState.update { it.copy(errores = errores) }
        if (errores.hayErrores || fechaApi == null) return

        val evento = EventoCatalogo(
            id = s.idEdicion ?: 0, nombre = s.nombre.trim(), tipo = s.tipo, fecha = fechaApi,
            lugar = s.lugar.trim(), cupos = s.cupos.toInt(), descripcion = s.descripcion.trim()
        )
        _uiState.update { it.copy(guardando = true) }
        viewModelScope.launch {
            try {
                val guardado = repository.guardar(evento)
                _uiState.update { st ->
                    val lista = if (st.idEdicion == null) st.eventos + guardado
                    else st.eventos.map { if (it.id == guardado.id) guardado else it }
                    st.copy(
                        eventos = lista.sortedBy { it.fecha }, guardando = false, formularioAbierto = false,
                        mensaje = if (st.idEdicion == null) "Evento registrado" else "Evento actualizado"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(guardando = false, mensaje = "No se pudo guardar: ${e.localizedMessage}") }
            }
        }
    }

    companion object {
        private fun formato(p: String) = SimpleDateFormat(p, Locale.US).apply { isLenient = false }

        /** dd/MM/aaaa -> AAAA-MM-DD (null si la fecha no es válida). */
        fun aApi(fecha: String): String? = runCatching {
            if (!Regex("""\d{2}/\d{2}/\d{4}""").matches(fecha.trim())) return null
            formato("yyyy-MM-dd").format(formato("dd/MM/yyyy").parse(fecha.trim())!!)
        }.getOrNull()

        /** AAAA-MM-DD -> dd/MM/aaaa para mostrar. */
        fun aPantalla(fecha: String): String = runCatching {
            formato("dd/MM/yyyy").format(formato("yyyy-MM-dd").parse(fecha)!!)
        }.getOrDefault(fecha)
    }
}
