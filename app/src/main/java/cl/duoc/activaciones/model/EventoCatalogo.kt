package cl.duoc.activaciones.model

/**
 * Fila de la tabla eventos_catalogo (MySQL en XAMPP), tal como la devuelve la API.
 * fecha viaja en formato AAAA-MM-DD.
 */
data class EventoCatalogo(
    val id: Int = 0,
    val nombre: String,
    val tipo: String,
    val fecha: String,
    val lugar: String,
    val cupos: Int,
    val descripcion: String = ""
)

/** Errores del formulario de registro de evento (null = sin error). */
data class EventoCatalogoErrores(
    val nombre: String? = null,
    val tipo: String? = null,
    val fecha: String? = null,
    val lugar: String? = null,
    val cupos: String? = null
) {
    val hayErrores: Boolean get() = listOf(nombre, tipo, fecha, lugar, cupos).any { it != null }
}

/** Estado de la pantalla del catálogo: lista remota + formulario de registro. */
data class EventoCatalogoUiState(
    val eventos: List<EventoCatalogo> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
    // Formulario
    val formularioAbierto: Boolean = false,
    val idEdicion: Int? = null,
    val nombre: String = "",
    val tipo: String = "",
    val fecha: String = "",      // dd/MM/aaaa en pantalla
    val lugar: String = "",
    val cupos: String = "",
    val descripcion: String = "",
    val errores: EventoCatalogoErrores = EventoCatalogoErrores(),
    val guardando: Boolean = false
)

/** Tipos sugeridos para el selector del formulario. */
val TIPOS_EVENTO_CATALOGO = listOf(
    "Feria empresarial", "Feria institucional", "Actividad de trabajadores",
    "Congreso o seminario", "Actividad comunitaria", "Otro"
)
