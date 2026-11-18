package cl.duoc.activaciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.activaciones.data.repository.EventoExternoRepository
import cl.duoc.activaciones.model.EventoExterno
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventosUiState(
    val cargando: Boolean = true,
    val eventos: List<EventoExterno> = emptyList(),
    val desdeServidor: Boolean = false,
    val texto: String = ""
) {
    val filtrados: List<EventoExterno>
        get() = if (texto.isBlank()) eventos else eventos.filter {
            it.nombre.contains(texto, true) || it.comuna.contains(texto, true) ||
                    it.organizacion.contains(texto, true)
        }
}

class EventosExternosViewModel(
    private val repository: EventoExternoRepository = EventoExternoRepository()
) : ViewModel() {

    private val _estado = MutableStateFlow(EventosUiState())
    val estado: StateFlow<EventosUiState> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            val r = repository.obtenerEventos()
            _estado.update { it.copy(cargando = false, eventos = r.eventos, desdeServidor = r.desdeServidor) }
        }
    }

    fun onTexto(v: String) = _estado.update { it.copy(texto = v) }
}