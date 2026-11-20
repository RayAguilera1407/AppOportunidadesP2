package cl.duoc.activaciones.viewmodel

import cl.duoc.activaciones.data.local.OportunidadEntity
import cl.duoc.activaciones.data.repository.OportunidadRepository
import cl.duoc.activaciones.data.repository.Repositorio
import cl.duoc.activaciones.model.EstadoOportunidad
import cl.duoc.activaciones.model.Filtro
import cl.duoc.activaciones.model.Perfil
import cl.duoc.activaciones.model.Potencial
import cl.duoc.activaciones.model.Prioridad
import cl.duoc.activaciones.model.TipoEvento
import cl.duoc.activaciones.model.Usuario
import cl.duoc.activaciones.util.Sesion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OportunidadViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val oportunidadRepo = mockk<OportunidadRepository>(relaxed = true)
    private val repo = mockk<Repositorio>(relaxed = true)
    private val sesion = mockk<Sesion>(relaxed = true)

    private fun usuario(id: Long, perfil: Perfil) =
        Usuario(id, "Usuario $id", "u$id@demo.cl", "x", perfil, "RM")

    private fun op(id: Long, responsable: Long) = OportunidadEntity(
        id = id, nombre = "Op $id", tipo = TipoEvento.FERIA_EMPRESARIAL, descripcion = "desc", fuente = "web",
        fechaDeteccion = 0, prioridad = Prioridad.MEDIA, potencial = Potencial.MEDIO,
        estado = EstadoOportunidad.EVENTO_IDENTIFICADO, fechaCambioEstado = 0, organizacionId = 1,
        responsableId = responsable, fechaInicio = null, fechaTermino = null, horario = "", comuna = "Peñalolén",
        region = "Metropolitana", publicoEsperado = 0, condiciones = "", costo = 0
    )

    /** Crea el ViewModel con la sesión iniciada por [actual] y las oportunidades [ops]. */
    private fun crearVm(actual: Usuario, ops: List<OportunidadEntity> = emptyList()): OportunidadViewModel {
        every { sesion.usuarioId } returns MutableStateFlow<Long?>(actual.id)
        every { repo.usuario(any()) } returns flowOf(actual)
        coEvery { repo.usuarioUnaVez(any()) } returns actual
        every { repo.usuarios() } returns flowOf(listOf(actual))
        every { repo.organizaciones() } returns flowOf(emptyList())
        every { repo.contactos(any()) } returns flowOf(emptyList())
        every { oportunidadRepo.oportunidades() } returns flowOf(ops)
        return OportunidadViewModel(oportunidadRepo, repo, sesion)
    }

    /** Llena todos los campos obligatorios con datos válidos. */
    private fun OportunidadViewModel.llenarValido() {
        onNombre("Feria laboral comunal")
        onTipo(TipoEvento.FERIA_EMPRESARIAL)
        onDescripcion("Stand informativo")
        onFuente("Redes sociales")
        onPrioridad(Prioridad.ALTA)
        onPotencial(Potencial.ALTO)
        onObservaciones("Dato de prueba")
        onOrganizacion(1L)
        onFechaInicio("10/12/2026")
        onFechaTermino("11/12/2026")
        onHorario("09:00 - 18:00")
        onRegion("Metropolitana")
        onComuna("Peñalolén")
        onPublico("150")
        onEspacio("Stand 3x3")
        onCondiciones("Requiere acreditación")
        onCosto("0")
    }

    @Test
    fun `formulario vacio marca errores y no guarda`() = runTest {
        val vm = crearVm(usuario(1, Perfil.EJECUTIVO))

        vm.guardar()
        advanceUntilIdle()

        val e = vm.uiState.value.errores
        assertTrue(e.hayErrores)
        assertNotNull(e.nombre)
        assertNotNull(e.tipo)
        assertNotNull(e.fechaInicio)
        assertNotNull(e.organizacion)
        coVerify(exactly = 0) { oportunidadRepo.guardar(any()) }
    }

    @Test
    fun `cambiar un campo limpia solo su error`() = runTest {
        val vm = crearVm(usuario(1, Perfil.EJECUTIVO))
        vm.guardar()

        vm.onNombre("Feria laboral")

        assertNull(vm.uiState.value.errores.nombre)
        assertNotNull(vm.uiState.value.errores.descripcion)
    }

    @Test
    fun `nombre corto y termino anterior al inicio son errores`() = runTest {
        val vm = crearVm(usuario(1, Perfil.EJECUTIVO))
        vm.llenarValido()
        vm.onNombre("Fe")
        vm.onFechaTermino("01/12/2026")

        vm.guardar()

        assertEquals("Debe tener al menos 3 caracteres", vm.uiState.value.errores.nombre)
        assertEquals("El término no puede ser anterior al inicio", vm.uiState.value.errores.fechaTermino)
    }

    @Test
    fun `costo con letras no es valido`() = runTest {
        val vm = crearVm(usuario(1, Perfil.EJECUTIVO))
        vm.llenarValido()
        vm.onCosto("abc")

        vm.guardar()

        assertEquals("Debe ser un número entero positivo", vm.uiState.value.errores.costo)
    }

    @Test
    fun `cambiar la region borra la comuna elegida`() = runTest {
        val vm = crearVm(usuario(1, Perfil.EJECUTIVO))
        vm.onRegion("Metropolitana")
        vm.onComuna("Peñalolén")

        vm.onRegion("Valparaíso")

        assertEquals("", vm.uiState.value.comuna)
    }

    @Test
    fun `formulario valido guarda con el ejecutivo como responsable`() = runTest {
        val vm = crearVm(usuario(1, Perfil.EJECUTIVO))
        vm.llenarValido()

        vm.guardar()
        advanceUntilIdle()

        val guardada = slot<OportunidadEntity>()
        coVerify { oportunidadRepo.guardar(capture(guardada)) }
        assertEquals("Feria laboral comunal", guardada.captured.nombre)
        assertEquals(1L, guardada.captured.responsableId)
        assertEquals(EstadoOportunidad.EVENTO_IDENTIFICADO, guardada.captured.estado)
        assertTrue(vm.uiState.value.guardadoExitosamente)
        assertFalse(vm.uiState.value.guardando)
    }

    @Test
    fun `supervisor debe elegir responsable`() = runTest {
        val vm = crearVm(usuario(9, Perfil.SUPERVISOR))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.usuario.collect {} }
        advanceUntilIdle()
        vm.llenarValido()

        vm.guardar()

        assertEquals("Selecciona el responsable", vm.uiState.value.errores.responsable)
    }

    @Test
    fun `ejecutivo solo ve sus oportunidades en la lista`() = runTest {
        val vm = crearVm(usuario(1, Perfil.EJECUTIVO), listOf(op(1, responsable = 1), op(2, responsable = 2)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.lista.collect {} }
        advanceUntilIdle()

        assertEquals(listOf(1L), vm.lista.value.map { it.id })
    }

    @Test
    fun `el filtro se aplica sobre la lista`() = runTest {
        val vm = crearVm(usuario(9, Perfil.SUPERVISOR), listOf(op(1, 1), op(2, 2).copy(prioridad = Prioridad.ALTA)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.lista.collect {} }

        vm.actualizarFiltro(Filtro(prioridad = Prioridad.ALTA))
        advanceUntilIdle()

        assertEquals(listOf(2L), vm.lista.value.map { it.id })
    }

    @Test
    fun `ubicacion nula muestra mensaje de error`() = runTest {
        val vm = crearVm(usuario(1, Perfil.EJECUTIVO))

        vm.onUbicacion(null, null)

        assertEquals("No se pudo obtener la ubicación. Activa el GPS e inténtalo de nuevo.", vm.uiState.value.mensajeRecurso)
    }
}
