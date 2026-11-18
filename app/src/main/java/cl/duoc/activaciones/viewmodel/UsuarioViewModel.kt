package cl.duoc.activaciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.activaciones.data.repository.Repositorio
import cl.duoc.activaciones.model.Perfil
import cl.duoc.activaciones.model.Usuario
import cl.duoc.activaciones.model.UsuarioErrores
import cl.duoc.activaciones.model.UsuarioUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ViewModel compartido entre RegistroScreen y ResumenScreen (Guía 11). */
class UsuarioViewModel(private val repo: Repositorio) : ViewModel() {

    private val _estado = MutableStateFlow(UsuarioUiState())
    val estado: StateFlow<UsuarioUiState> = _estado.asStateFlow()

    /** Último usuario creado: lo lee ResumenScreen (el formulario se limpia al registrar). */
    private val _ultimoRegistro = MutableStateFlow<Usuario?>(null)
    val ultimoRegistro: StateFlow<Usuario?> = _ultimoRegistro.asStateFlow()

    // Cada cambio limpia el error de su campo
    fun onNombreChange(v: String) = _estado.update { it.copy(nombre = v, errores = it.errores.copy(nombre = null)) }
    fun onCorreoChange(v: String) = _estado.update { it.copy(correo = v, errores = it.errores.copy(correo = null)) }
    fun onClaveChange(v: String) = _estado.update {
        it.copy(clave = v, errores = it.errores.copy(clave = null, confirmarClave = null))
    }
    fun onConfirmarClaveChange(v: String) = _estado.update {
        it.copy(confirmarClave = v, errores = it.errores.copy(confirmarClave = null))
    }
    fun onDireccionChange(v: String) = _estado.update {
        it.copy(direccion = v, errores = it.errores.copy(direccion = null))
    }
    fun onAreaChange(v: String) = _estado.update { it.copy(area = v, errores = it.errores.copy(area = null)) }
    fun onZonaChange(v: String) = _estado.update { it.copy(zona = v, errores = it.errores.copy(zona = null)) }
    fun onAceptarTerminosChange(v: Boolean) = _estado.update {
        it.copy(aceptaTerminos = v, errores = it.errores.copy(terminos = null))
    }

    fun limpiar() {
        _estado.value = UsuarioUiState()
    }

    // ---------- Validaciones ----------

    private fun errorNombre(nombre: String): String? = when {
        nombre.isBlank() -> "El nombre es obligatorio"
        nombre.trim().length < 3 -> "Debe tener al menos 3 caracteres"
        nombre.any { it.isDigit() } -> "El nombre no puede contener números"
        else -> null
    }

    private fun errorCorreo(correo: String): String? = when {
        correo.isBlank() -> "El correo es obligatorio"
        !REGEX_CORREO.matches(correo.trim()) -> "Correo inválido (ej: nombre@dominio.cl)"
        else -> null
    }

    private fun errorClave(clave: String): String? = when {
        clave.length < 6 -> "Debe tener al menos 6 caracteres"
        clave.none { it.isLetter() } || clave.none { it.isDigit() } -> "Debe incluir letras y números"
        else -> null
    }

    private fun errorDireccion(direccion: String): String? = when {
        direccion.isBlank() -> "La dirección es obligatoria"
        direccion.trim().length < 5 -> "Debe tener al menos 5 caracteres"
        else -> null
    }

    /** Valida todo el formulario, deja los errores en el estado y devuelve true si es válido. */
    fun validarFormulario(): Boolean {
        val s = _estado.value
        val errores = UsuarioErrores(
            nombre = errorNombre(s.nombre),
            correo = errorCorreo(s.correo),
            clave = errorClave(s.clave),
            confirmarClave = if (s.confirmarClave != s.clave) "Las contraseñas no coinciden" else null,
            direccion = errorDireccion(s.direccion),
            area = if (s.area == null) "Selecciona tu área" else null,
            zona = if (s.zona == null) "Selecciona tu zona o sucursal" else null,
            terminos = if (!s.aceptaTerminos) "Debes aceptar los términos para continuar" else null
        )
        _estado.update { it.copy(errores = errores) }
        return !errores.hayErrores
    }

    fun registrar(alExito: () -> Unit) {
        val s = _estado.value
        if (s.registrando) return
        if (!validarFormulario()) return

        val area = s.area ?: return
        val zona = s.zona ?: return
        _estado.update { it.copy(registrando = true) }

        viewModelScope.launch {
            val ahora = System.currentTimeMillis()
            val nuevo = Usuario(
                nombre = s.nombre.trim(),
                correo = s.correo.trim().lowercase(),
                clave = s.clave, // el repositorio la guarda con hash
                perfil = Perfil.EJECUTIVO, // el registro nunca crea supervisores
                zona = zona,
                area = area,
                direccion = s.direccion.trim(),
                fechaCreacion = ahora,
                fechaActualizacion = ahora
            )
            if (repo.registrar(nuevo)) {
                _ultimoRegistro.value = nuevo
                _estado.value = UsuarioUiState()
                alExito()
            } else {
                _estado.update {
                    it.copy(
                        registrando = false,
                        errores = it.errores.copy(correo = "Este correo ya está registrado")
                    )
                }
            }
        }
    }

    private companion object {
        val REGEX_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}