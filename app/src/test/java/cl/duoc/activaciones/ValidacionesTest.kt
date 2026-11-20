package cl.duoc.activaciones

import cl.duoc.activaciones.data.repository.Repositorio
import cl.duoc.activaciones.viewmodel.UsuarioViewModel
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validaciones del formulario de registro (Guía 11).
 * Las reglas viven en UsuarioViewModel.validarFormulario(): devuelve true si no hay errores
 * y deja los mensajes en estado.value.errores.
 */
class ValidacionesTest {

    // El repositorio no se usa al validar; se simula para no depender de Room
    private fun vmValido() = UsuarioViewModel(mockk<Repositorio>(relaxed = true)).apply {
        onNombreChange("Camila Rojas")
        onCorreoChange("camila@demo.cl")
        onClaveChange("clave123")
        onConfirmarClaveChange("clave123")
        onDireccionChange("Av. Ficticia 123")
        onAreaChange("Comercial")
        onZonaChange("Metropolitana")
        onAceptarTerminosChange(true)
    }

    @Test
    fun `formulario completo no tiene errores`() {
        val vm = vmValido()
        assertTrue(vm.validarFormulario())
        assertFalse(vm.estado.value.errores.hayErrores)
    }

    @Test
    fun `formulario vacio marca todos los campos`() {
        val vm = UsuarioViewModel(mockk<Repositorio>(relaxed = true))
        assertFalse(vm.validarFormulario())
        val e = vm.estado.value.errores
        assertTrue(e.hayErrores)
        assertNotNull(e.nombre)
        assertNotNull(e.correo)
        assertNotNull(e.clave)
        assertNotNull(e.direccion)
        assertNotNull(e.area)
        assertNotNull(e.zona)
        assertNotNull(e.terminos)
    }

    @Test
    fun `correo sin dominio es invalido`() {
        val vm = vmValido().apply { onCorreoChange("camila@") }
        assertFalse(vm.validarFormulario())
        assertNotNull(vm.estado.value.errores.correo)
    }

    @Test
    fun `claves distintas no coinciden`() {
        val vm = vmValido().apply { onConfirmarClaveChange("otra123") }
        assertFalse(vm.validarFormulario())
        assertNotNull(vm.estado.value.errores.confirmarClave)
    }

    @Test
    fun `clave sin numeros es rechazada`() {
        val vm = vmValido().apply {
            onClaveChange("abcdefg")
            onConfirmarClaveChange("abcdefg")
        }
        assertFalse(vm.validarFormulario())
        assertNotNull(vm.estado.value.errores.clave)
    }

    @Test
    fun `nombre con numeros es rechazado`() {
        val vm = vmValido().apply { onNombreChange("Camila 2") }
        assertFalse(vm.validarFormulario())
        assertNotNull(vm.estado.value.errores.nombre)
    }

    @Test
    fun `corregir un campo limpia su error`() {
        val vm = vmValido().apply { onCorreoChange("camila@") }
        vm.validarFormulario()
        vm.onCorreoChange("camila@demo.cl")
        assertNull(vm.estado.value.errores.correo)
    }
}
