package cl.duoc.activaciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.activaciones.data.repository.EventoExternoRepository
import cl.duoc.activaciones.model.EventosUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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