package cl.duoc.activaciones.model

// Errores individuales del formulario de oportunidad
data class OportunidadErrores(
    val nombre: String? = null,
    val tipo: String? = null,
    val descripcion: String? = null,
    val fuente: String? = null,
    val prioridad: String? = null,
    val potencial: String? = null,
    val fecha: String? = null,
    val publico: String? = null,
    val costo: String? = null,
    val comuna: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(
            nombre, tipo, descripcion, fuente, prioridad,
            potencial, fecha, publico, costo, comuna
        ).any { it != null }
}