package cl.duoc.activaciones.data.repository

import cl.duoc.activaciones.data.remote.EventosApi
import cl.duoc.activaciones.data.remote.RetrofitInstance
import cl.duoc.activaciones.model.EventoExterno

data class ResultadoEventos(val eventos: List<EventoExterno>, val desdeServidor: Boolean)

/** Consulta el catálogo con Retrofit; si falla, usa datos ficticios locales. */
class EventoExternoRepository(private val api: EventosApi = RetrofitInstance.eventosApi) {

    suspend fun obtenerEventos(): ResultadoEventos =
        try {
            val remotos = api.getEventos()
            if (remotos.isEmpty()) ResultadoEventos(EVENTOS_DE_PRUEBA, false)
            else ResultadoEventos(remotos, true)
        } catch (e: Exception) {
            ResultadoEventos(EVENTOS_DE_PRUEBA, false)
        }

    private companion object {
        val EVENTOS_DE_PRUEBA = listOf(
            EventoExterno(1, "Feria de Empleo Santiago Centro", "12/12/2026", "Santiago", "Municipalidad de Ejemplo"),
            EventoExterno(2, "Expo Retail Pacífico", "20/01/2027", "Viña del Mar", "Retail Pacífico Demo Ltda."),
            EventoExterno(3, "Jornada de Bienestar Minero", "05/02/2027", "Las Condes", "Minera Andes Ficticia S.A."),
            EventoExterno(4, "Feria Técnica Biobío", "18/03/2027", "Concepción", "Instituto Técnico Prueba")
        )
    }
}