package cl.duoc.activaciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.activaciones.data.repository.Repositorio
import cl.duoc.activaciones.model.Organizacion
import cl.duoc.activaciones.model.OrganizacionErrores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrganizacionesViewModel(private val repo: Repositorio) : ViewModel() {

    val organizaciones: StateFlow<List<Organizacion>> =
        repo.organizaciones()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _errores = MutableStateFlow(OrganizacionErrores())
    val errores: StateFlow<OrganizacionErrores> = _errores.asStateFlow()

    fun limpiarErrores() { _errores.value = OrganizacionErrores() }

    // Cada campo limpia su propio error al escribir
    fun onNombreChange() = _errores.update { it.copy(nombre = null) }
    fun onTipoChange() = _errores.update { it.copy(tipo = null) }
    fun onRubroChange() = _errores.update { it.copy(rubro = null) }
    fun onRegionChange() = _errores.update { it.copy(region = null, comuna = null) }
    fun onComunaChange() = _errores.update { it.copy(comuna = null) }
    fun onSitioWebChange() = _errores.update { it.copy(sitioWeb = null) }
    fun onObservacionesChange() = _errores.update { it.copy(observaciones = null) }

    // ---------- Validaciones ----------

    private fun errorNombre(nombre: String): String? = when {
        nombre.isBlank() -> "El nombre es obligatorio"
        nombre.trim().length < 3 -> "Debe tener al menos 3 caracteres"
        nombre.trim().length > 60 -> "Máximo 60 caracteres"
        else -> null
    }

    private fun errorTipo(tipo: String): String? =
        if (tipo.isBlank()) "Selecciona el tipo de organización" else null

    private fun errorRubro(rubro: String): String? = when {
        rubro.isBlank() -> "El rubro es obligatorio"
        rubro.trim().length < 3 -> "Debe tener al menos 3 caracteres"
        rubro.trim().length > 40 -> "Máximo 40 caracteres"
        else -> null
    }

    private fun errorRegion(region: String): String? =
        if (region.isBlank()) "Selecciona la región" else null

    private fun errorComuna(comuna: String): String? =
        if (comuna.isBlank()) "Selecciona la comuna" else null

    private fun errorSitioWeb(sitioWeb: String): String? = when {
        sitioWeb.isBlank() -> "El sitio web es obligatorio"
        !REGEX_WEB.matches(sitioWeb.trim()) -> "Sitio web inválido (ej: www.empresa.cl)"
        else -> null
    }

    private fun errorObservaciones(obs: String): String? = when {
        obs.isBlank() -> "Las observaciones son obligatorias"
        obs.trim().length < 5 -> "Debe tener al menos 5 caracteres"
        obs.trim().length > 300 -> "Máximo 300 caracteres"
        else -> null
    }

    fun crear(
        nombre: String, tipo: String, rubro: String, region: String, comuna: String,
        sitioWeb: String, observaciones: String, alExito: () -> Unit
    ) {
        val err = OrganizacionErrores(
            nombre = errorNombre(nombre),
            tipo = errorTipo(tipo),
            rubro = errorRubro(rubro),
            region = errorRegion(region),
            comuna = errorComuna(comuna),
            sitioWeb = errorSitioWeb(sitioWeb),
            observaciones = errorObservaciones(observaciones)
        )
        _errores.value = err
        if (err.hayErrores) return

        viewModelScope.launch {
            repo.guardarOrganizacion(
                Organizacion(
                    nombre = nombre.trim(), tipo = tipo.trim(), rubro = rubro.trim(),
                    comuna = comuna, region = region,
                    sitioWeb = sitioWeb.trim(), observaciones = observaciones.trim()
                )
            )
            alExito()
        }
    }

    fun eliminar(o: Organizacion) {
        viewModelScope.launch { repo.eliminarOrganizacion(o.id) }
    }

    private companion object {
        val REGEX_WEB = Regex("^(https?://)?([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}(/\\S*)?$")
    }
}