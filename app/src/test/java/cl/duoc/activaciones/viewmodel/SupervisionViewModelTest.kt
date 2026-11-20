package cl.duoc.activaciones.viewmodel

import cl.duoc.activaciones.data.local.OportunidadEntity
import cl.duoc.activaciones.data.repository.OportunidadRepository
import cl.duoc.activaciones.data.repository.Repositorio
import cl.duoc.activaciones.model.EstadoOportunidad
import cl.duoc.activaciones.model.Perfil
import cl.duoc.activaciones.model.Potencial
import cl.duoc.activaciones.model.Prioridad
import cl.duoc.activaciones.model.TipoEvento
import cl.duoc.activaciones.model.Usuario
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SupervisionViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repo = mockk<Repositorio>(relaxed = true)
    private val oportunidadRepo = mockk<OportunidadRepository>(relaxed = true)

    private fun usuario(id: Long, nombre: String) =
        Usuario(id, nombre, "u$id@demo.cl", "x", Perfil.EJECUTIVO, "RM")

    private fun op(
        id: Long, estado: EstadoOportunidad, prioridad: Prioridad = Prioridad.MEDIA,
        responsable: Long = 1, publico: Int = 0, costo: Int = 0
    ) = OportunidadEntity(
        id = id, nombre = "Op $id", tipo = TipoEvento.FERIA_EMPRESARIAL, descripcion = "desc", fuente = "web",
        fechaDeteccion = 0, prioridad = prioridad, potencial = Potencial.MEDIO, estado = estado,
        fechaCambioEstado = 0, organizacionId = 1, responsableId = responsable, fechaInicio = null,
        fechaTermino = null, horario = "", comuna = "Peñalolén", region = "Metropolitana",
        publicoEsperado = publico, condiciones = "", costo = costo
    )

    /** Crea el ViewModel, se suscribe al resumen (WhileSubscribed) y deja correr las corrutinas. */
    private fun TestScope.resumenDe(
        ops: List<OportunidadEntity>, usuarios: List<Usuario>
    ): SupervisionViewModel {
        every { oportunidadRepo.oportunidades() } returns flowOf(ops)
        every { repo.usuarios() } returns flowOf(usuarios)
        val vm = SupervisionViewModel(repo, oportunidadRepo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.resumen.collect {} }
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `sin oportunidades el resumen queda en cero`() = runTest {
        val r = resumenDe(emptyList(), emptyList()).resumen.value

        assertEquals(0, r.total)
        assertEquals(0, r.cerradas)
        assertEquals(0, r.participacionesConfirmadas)
    }

    @Test
    fun `cuenta total, cerradas, por estado y por prioridad`() = runTest {
        val ops = listOf(
            op(1, EstadoOportunidad.EVENTO_IDENTIFICADO, Prioridad.ALTA),
            op(2, EstadoOportunidad.EVENTO_IDENTIFICADO, Prioridad.BAJA).copy(fechaCierre = 100L),
            op(3, EstadoOportunidad.DESCARTADA, Prioridad.ALTA)
        )
        val r = resumenDe(ops, emptyList()).resumen.value

        assertEquals(3, r.total)
        assertEquals(1, r.cerradas)
        assertEquals(EstadoOportunidad.values().size, r.porEstado.size)
        assertEquals(2, r.porEstado.toMap()[EstadoOportunidad.EVENTO_IDENTIFICADO.etiqueta])
        assertEquals(1, r.porEstado.toMap()[EstadoOportunidad.DESCARTADA.etiqueta])
        assertEquals(2, r.porPrioridad.toMap()[Prioridad.ALTA.etiqueta])
        assertEquals(1, r.porPrioridad.toMap()[Prioridad.BAJA.etiqueta])
    }

    @Test
    fun `por responsable usa el nombre, ordena de mayor a menor y marca sin asignar`() = runTest {
        val ops = listOf(
            op(1, EstadoOportunidad.CONTACTO_PENDIENTE, responsable = 1),
            op(2, EstadoOportunidad.CONTACTO_PENDIENTE, responsable = 2),
            op(3, EstadoOportunidad.CONTACTO_PENDIENTE, responsable = 2),
            op(4, EstadoOportunidad.CONTACTO_PENDIENTE, responsable = 99)
        )
        val r = resumenDe(ops, listOf(usuario(1, "Camila"), usuario(2, "Diego"))).resumen.value

        assertEquals("Diego" to 2, r.porResponsable.first())
        assertEquals(1, r.porResponsable.toMap()["Camila"])
        assertEquals(1, r.porResponsable.toMap()["Sin asignar"])
    }

    @Test
    fun `suma publico y costo solo de las participaciones confirmadas`() = runTest {
        val ops = listOf(
            op(1, EstadoOportunidad.PARTICIPACION_CONFIRMADA, publico = 100, costo = 50_000),
            op(2, EstadoOportunidad.PARTICIPACION_CONFIRMADA, publico = 300, costo = 0),
            op(3, EstadoOportunidad.PROPUESTA_ENVIADA, publico = 1000, costo = 90_000)
        )
        val r = resumenDe(ops, emptyList()).resumen.value

        assertEquals(2, r.participacionesConfirmadas)
        assertEquals(400, r.publicoConfirmado)
        assertEquals(50_000, r.costoConfirmado)
    }
}
