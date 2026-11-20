package cl.duoc.activaciones.data.repository

import cl.duoc.activaciones.data.remote.EventosCatalogoApi
import cl.duoc.activaciones.data.remote.RetrofitInstance
import cl.duoc.activaciones.model.EventoCatalogo

/** Acceso a la base externa (MySQL en XAMPP) a través de Retrofit. */
class EventoCatalogoRepository(
    private val api: EventosCatalogoApi = RetrofitInstance.catalogoApi
) {
    suspend fun listar(): List<EventoCatalogo> = api.listar()

    suspend fun guardar(evento: EventoCatalogo): EventoCatalogo =
        if (evento.id == 0) api.crear(evento) else api.actualizar(evento.id, evento)

    suspend fun eliminar(id: Int) {
        val r = api.eliminar(id)
        if (!r.isSuccessful) error("El servidor respondió ${r.code()}")
    }
}
