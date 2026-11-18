package cl.duoc.activaciones.model

/** Registro del catálogo externo de eventos (datos ficticios / públicos). */
data class EventoExterno(
    val id: Int,
    val nombre: String,
    val fecha: String,
    val comuna: String,
    val organizacion: String
)