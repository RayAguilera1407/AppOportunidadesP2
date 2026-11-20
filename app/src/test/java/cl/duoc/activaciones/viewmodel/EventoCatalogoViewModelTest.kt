package cl.duoc.activaciones.viewmodel

import cl.duoc.activaciones.data.repository.EventoCatalogoRepository
import cl.duoc.activaciones.model.EventoCatalogo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EventoCatalogoViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repo = mockk<EventoCatalogoRepository>(relaxed = true)
    private val feria = EventoCatalogo(1, "Feria laboral", "Feria institucional", "2026-12-10", "Peñalolén", 40)

    private fun crearVm(lista: List<EventoCatalogo> = listOf(feria)): EventoCatalogoViewModel {
        coEvery { repo.listar() } returns lista
        return EventoCatalogoViewModel(repo)
    }

    @Test
    fun `al iniciar carga los eventos del servidor`() = runTest {
        val vm = crearVm()
        advanceUntilIdle()
        assertEquals(listOf(feria), vm.uiState.value.eventos)
        assertFalse(vm.uiState.value.cargando)
    }

    @Test
    fun `si el servidor falla muestra error`() = runTest {
        coEvery { repo.listar() } throws RuntimeException("sin conexión")
        val vm = EventoCatalogoViewModel(repo)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `formulario vacio marca errores y no envia`() = runTest {
        val vm = crearVm(); advanceUntilIdle()
        vm.nuevo()
        vm.guardar()
        advanceUntilIdle()
        val e = vm.uiState.value.errores
        assertTrue(e.hayErrores)
        assertNotNull(e.nombre); assertNotNull(e.tipo); assertNotNull(e.fecha); assertNotNull(e.lugar); assertNotNull(e.cupos)
        coVerify(exactly = 0) { repo.guardar(any()) }
    }

    @Test
    fun `fecha invalida y cupos negativos son errores`() = runTest {
        val vm = crearVm(); advanceUntilIdle()
        vm.nuevo()
        vm.onNombre("Expo"); vm.onTipo("Otro"); vm.onLugar("Santiago")
        vm.onFecha("31/02/2026"); vm.onCupos("-5")
        vm.guardar()
        assertEquals("Fecha inválida (dd/MM/aaaa)", vm.uiState.value.errores.fecha)
        assertEquals("Debe ser un número entero positivo", vm.uiState.value.errores.cupos)
    }

    @Test
    fun `registrar envia la fecha en formato de la API y agrega el evento`() = runTest {
        val vm = crearVm(emptyList()); advanceUntilIdle()
        val enviado = slot<EventoCatalogo>()
        coEvery { repo.guardar(capture(enviado)) } answers { enviado.captured.copy(id = 7) }

        vm.nuevo()
        vm.onNombre("Expo Emprende"); vm.onTipo("Feria empresarial"); vm.onFecha("20/01/2027")
        vm.onLugar("Santiago"); vm.onCupos("60")
        vm.guardar()
        advanceUntilIdle()

        assertEquals("2027-01-20", enviado.captured.fecha)
        assertEquals(0, enviado.captured.id)
        assertEquals(listOf(7), vm.uiState.value.eventos.map { it.id })
        assertFalse(vm.uiState.value.formularioAbierto)
        assertEquals("Evento registrado", vm.uiState.value.mensaje)
    }

    @Test
    fun `editar carga los datos y guarda con el mismo id`() = runTest {
        val vm = crearVm(); advanceUntilIdle()
        coEvery { repo.guardar(any()) } answers { firstArg() }

        vm.editar(feria)
        assertEquals("10/12/2026", vm.uiState.value.fecha)
        vm.onCupos("50")
        vm.guardar()
        advanceUntilIdle()

        assertEquals(50, vm.uiState.value.eventos.single().cupos)
        coVerify { repo.guardar(match { it.id == 1 }) }
    }

    @Test
    fun `eliminar quita el evento de la lista`() = runTest {
        val vm = crearVm(); advanceUntilIdle()
        vm.eliminar(feria)
        advanceUntilIdle()
        coVerify { repo.eliminar(1) }
        assertTrue(vm.uiState.value.eventos.isEmpty())
    }

    @Test
    fun `cambiar un campo limpia su error`() = runTest {
        val vm = crearVm(); advanceUntilIdle()
        vm.nuevo(); vm.guardar()
        vm.onNombre("Feria")
        assertNull(vm.uiState.value.errores.nombre)
    }
}
