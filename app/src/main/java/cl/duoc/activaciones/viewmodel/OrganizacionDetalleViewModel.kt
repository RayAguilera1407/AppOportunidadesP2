package cl.duoc.activaciones.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.activaciones.data.local.OportunidadEntity
import cl.duoc.activaciones.data.repository.OportunidadRepository
import cl.duoc.activaciones.data.repository.Repositorio
import cl.duoc.activaciones.model.Contacto
import cl.duoc.activaciones.model.ContactoErrores
import cl.duoc.activaciones.model.ESTADOS_CONTACTO
import cl.duoc.activaciones.model.Organizacion
import cl.duoc.activaciones.model.Usuario
import cl.duoc.activaciones.model.visibles
import cl.duoc.activaciones.util.Sesion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizacionDetalleViewModel(
    private val repo: Repositorio,
    oportunidadRepo: OportunidadRepository,
    sesion: Sesion,
    private val id: Long
) : ViewModel() {

    private val usuario: StateFlow<Usuario?> = sesion.usuarioId
        .flatMapLatest { uid -> if (uid == null) flowOf<Usuario?>(null) else repo.usuario(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val organizacion: StateFlow<Organizacion?> =
        repo.organizacion(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val contactos: StateFlow<List<Contacto>> =
        repo.contactos(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** El ejecutivo solo ve sus oportunidades; el supervisor ve todas. */
    val eventos: StateFlow<List<OportunidadEntity>> =
        combine(oportunidadRepo.porOrganizacion(id), usuario) { ops, u -> visibles(ops, u) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _errores = MutableStateFlow(ContactoErrores())
    val errores: StateFlow<ContactoErrores> = _errores.asStateFlow()

    fun limpiarErrores() { _errores.value = ContactoErrores() }

    fun agregarContacto(
        nombre: String, cargo: String, area: String, correo: String, telefono: String,
        medio: String, estado: String, observaciones: String, alExito: () -> Unit
    ) {
        val err = ContactoErrores(
            nombre = when {
                nombre.isBlank() -> "El nombre es obligatorio"
                nombre.trim().length < 3 -> "Debe tener al menos 3 caracteres"
                else -> null
            },
            area = if (area.isBlank()) "Selecciona el área" else null,
            correo = if (correo.isNotBlank() && !REGEX_CORREO.matches(correo.trim()))
                "Correo inválido (ej: nombre@dominio.cl)" else null,
            telefono = if (telefono.isNotBlank() && !REGEX_TELEFONO.matches(telefono.trim()))
                "Teléfono inválido (ej: +56 9 1234 5678)" else null
        )
        _errores.value = err
        if (err.hayErrores) return
        viewModelScope.launch {
            repo.guardarContacto(
                Contacto(
                    organizacionId = id, nombre = nombre.trim(), cargo = cargo.trim(), area = area,
                    correo = correo.trim(), telefono = telefono.trim(), medioPreferente = medio,
                    estado = estado,
                    fechaUltimoContacto = if (estado == ESTADOS_CONTACTO.first()) null else System.currentTimeMillis(),
                    observaciones = observaciones.trim()
                )
            )
            alExito()
        }
    }

    fun eliminarContacto(c: Contacto) {
        viewModelScope.launch { repo.eliminarContacto(c.id) }
    }

    private companion object {
        val REGEX_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        val REGEX_TELEFONO = Regex("^\\+?[0-9 ]{8,15}$")
    }
}