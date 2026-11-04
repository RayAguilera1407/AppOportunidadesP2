package cl.duoc.activaciones.model

/** Estado del formulario de oportunidad: valores, errores y estado de guardado. */
data class OportunidadUiState(
    val nombre: String = "",
    val tipo: TipoEvento? = null,
    val descripcion: String = "",
    val fuente: String = "",
    val prioridad: Prioridad? = null,
    val potencial: Potencial? = null,
    val estado: EstadoOportunidad = EstadoOportunidad.EVENTO_IDENTIFICADO,
    val observaciones: String = "",
    val organizacionId: Long? = null,
    val responsableId: Long? = null, // solo el supervisor puede reasignarlo
    // Evento (opcional)
    val fechaInicio: String = "",
    val fechaTermino: String = "",
    val horario: String = "",
    val comuna: String = "",
    val region: String = "",
    val publico: String = "",
    val condiciones: String = "",
    val costo: String = "",
    // Recursos nativos
    val referenciaGeografica: String = "",
    val fotoPath: String? = null,
    val codigoQr: String = "",
    val mensajeRecurso: String? = null,
    // Errores (objeto aparte, igual que UsuarioUiState)
    val errores: OportunidadErrores = OportunidadErrores(),
    // Guardado
    val esEdicion: Boolean = false,
    val guardando: Boolean = false,
    val guardadoExitosamente: Boolean = false
) {
    val tieneErrores: Boolean get() = errores.hayErrores
}