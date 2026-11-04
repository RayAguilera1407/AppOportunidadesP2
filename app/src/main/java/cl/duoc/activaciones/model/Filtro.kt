package cl.duoc.activaciones.model

data class Filtro(
    val texto: String = "",
    val estado: EstadoOportunidad? = null,
    val prioridad: Prioridad? = null,
    val tipo: TipoEvento? = null,
    val region: String? = null,
    val responsableId: Long? = null,
    val organizacionId: Long? = null,
    val periodo: PeriodoFecha = PeriodoFecha.TODAS
)