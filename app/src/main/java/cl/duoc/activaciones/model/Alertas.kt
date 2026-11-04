package cl.duoc.activaciones.model

import cl.duoc.activaciones.data.local.OportunidadEntity

data class Alertas(
    val eventosProximos: List<OportunidadEntity> = emptyList(),
    val seguimientosAtrasados: List<OportunidadEntity> = emptyList(),
    val sinContacto: List<OportunidadEntity> = emptyList(),
    val propuestasSinRespuesta: List<OportunidadEntity> = emptyList()
) {
    val total get() = eventosProximos.size + seguimientosAtrasados.size +
            sinContacto.size + propuestasSinRespuesta.size
}