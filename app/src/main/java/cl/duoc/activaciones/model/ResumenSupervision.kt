package cl.duoc.activaciones.model

/** Conteos para la pantalla de supervisión general. */
data class ResumenSupervision(
    val total: Int = 0,
    val cerradas: Int = 0,
    val porEstado: List<Pair<String, Int>> = emptyList(),
    val porPrioridad: List<Pair<String, Int>> = emptyList(),
    val porResponsable: List<Pair<String, Int>> = emptyList(),
    val participacionesConfirmadas: Int = 0,
    val publicoConfirmado: Int = 0,
    val costoConfirmado: Int = 0
)