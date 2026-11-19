package cl.duoc.activaciones.model

/** Resultado de consultar el catálogo externo: los eventos y si vinieron del servidor. */
data class ResultadoEventos(
    val eventos: List<EventoExterno>,
    val desdeServidor: Boolean
)