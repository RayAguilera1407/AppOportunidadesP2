package cl.duoc.activaciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.activaciones.data.repository.OportunidadRepository
import cl.duoc.activaciones.data.repository.Repositorio
import cl.duoc.activaciones.model.EstadoOportunidad
import cl.duoc.activaciones.model.Prioridad
import cl.duoc.activaciones.model.ResumenSupervision
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Seguimiento general del supervisor: conteos por estado, prioridad y responsable. */
class SupervisionViewModel(
    repo: Repositorio,
    oportunidadRepo: OportunidadRepository
) : ViewModel() {

    val resumen: StateFlow<ResumenSupervision> =
        combine(oportunidadRepo.oportunidades(), repo.usuarios()) { ops, usuarios ->
            val nombres = usuarios.associate { it.id to it.nombre }
            val confirmadas = ops.filter { it.estado == EstadoOportunidad.PARTICIPACION_CONFIRMADA }
            ResumenSupervision(
                total = ops.size,
                cerradas = ops.count { it.fechaCierre != null },
                porEstado = EstadoOportunidad.values().map { e -> e.etiqueta to ops.count { it.estado == e } },
                porPrioridad = Prioridad.values().map { p -> p.etiqueta to ops.count { it.prioridad == p } },
                porResponsable = ops.groupBy { it.responsableId }
                    .map { (id, lista) -> (nombres[id] ?: "Sin asignar") to lista.size }
                    .sortedByDescending { it.second },
                participacionesConfirmadas = confirmadas.size,
                publicoConfirmado = confirmadas.sumOf { it.publicoEsperado },
                costoConfirmado = confirmadas.sumOf { it.costo }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResumenSupervision())
}