package cl.duoc.activaciones.model

// Errores individuales del formulario de oportunidad (uno por campo)
data class OportunidadErrores(
    val nombre: String? = null,
    val tipo: String? = null,
    val descripcion: String? = null,
    val fuente: String? = null,
    val prioridad: String? = null,
    val potencial: String? = null,
    val observaciones: String? = null,
    val organizacion: String? = null,
    val contacto: String? = null,
    val responsable: String? = null,
    val fechaInicio: String? = null,
    val fechaTermino: String? = null,
    val horario: String? = null,
    val region: String? = null,
    val comuna: String? = null,
    val publico: String? = null,
    val espacio: String? = null,
    val condiciones: String? = null,
    val costo: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(
            nombre, tipo, descripcion, fuente, prioridad, potencial,
            observaciones, organizacion, contacto, responsable,
            fechaInicio, fechaTermino, horario, region, comuna,
            publico, espacio, condiciones, costo
        ).any { it != null }
}