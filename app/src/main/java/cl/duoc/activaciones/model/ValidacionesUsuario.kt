package cl.duoc.activaciones.model

fun validarRegistro(state: UsuarioUiState): UsuarioErrores {
    val errorNombre = if (state.nombre.isBlank()) "El nombre es obligatorio" else null

    val errorCorreo = when {
        state.correo.isBlank() -> "El correo es obligatorio"
        !state.correo.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) -> "Correo inválido"
        else -> null
    }

    val errorClave = when {
        state.clave.isBlank() -> "La contraseña es obligatoria"
        state.clave.length < 6 -> "Mínimo 6 caracteres"
        !state.clave.any { it.isDigit() } -> "Debe contener al menos un número"
        else -> null
    }

    val errorConfirmar = if (state.clave != state.confirmarClave) {
        "Las contraseñas no coinciden"
    } else null

    val errorDireccion = if (state.direccion.isBlank()) "La dirección es obligatoria" else null

    val errorTerminos = if (!state.aceptaTerminos) "Debes aceptar los términos" else null

    return UsuarioErrores(
        nombre = errorNombre,
        correo = errorCorreo,
        clave = errorClave,
        confirmarClave = errorConfirmar,
        direccion = errorDireccion,
        terminos = errorTerminos
    )
}