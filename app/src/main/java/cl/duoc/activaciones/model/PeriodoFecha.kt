package cl.duoc.activaciones.model

/** Filtro por fecha del evento (criterio 21 del caso). */
enum class PeriodoFecha(val etiqueta: String) {
    TODAS("Todas"),
    PROXIMOS_7("Próximos 7 días"),
    PROXIMOS_30("Próximos 30 días"),
    PASADOS("Ya realizados"),
    SIN_FECHA("Sin fecha")
}