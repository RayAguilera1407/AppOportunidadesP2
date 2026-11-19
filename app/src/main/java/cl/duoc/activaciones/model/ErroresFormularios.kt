package cl.duoc.activaciones.model

data class OrganizacionErrores(
    val nombre: String? = null,
    val tipo: String? = null,
    val rubro: String? = null,
    val region: String? = null,
    val comuna: String? = null,
    val sitioWeb: String? = null,
    val observaciones: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(nombre, tipo, rubro, region, comuna, sitioWeb, observaciones)
            .any { it != null }
}

data class ContactoErrores(
    val nombre: String? = null, val area: String? = null,
    val correo: String? = null, val telefono: String? = null
) {
    val hayErrores get() = listOf(nombre, area, correo, telefono).any { it != null }
}

data class SeguimientoErrores(val resultado: String? = null, val dias: String? = null) {
    val hayErrores get() = resultado != null || dias != null
}

data class ResultadoErrores(
    val asistentes: String? = null, val contactos: String? = null,
    val inscripciones: String? = null, val evaluacion: String? = null
) {
    val hayErrores get() = listOf(asistentes, contactos, inscripciones, evaluacion).any { it != null }
}