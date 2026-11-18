package cl.duoc.activaciones.data.remote

import cl.duoc.activaciones.model.EventoExterno
import retrofit2.http.GET

interface EventosApi {
    @GET("eventos.json")
    suspend fun getEventos(): List<EventoExterno>
}