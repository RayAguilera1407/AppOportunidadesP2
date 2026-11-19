package cl.duoc.activaciones.model

/** Estado de la pantalla del catálogo de eventos externos. */
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