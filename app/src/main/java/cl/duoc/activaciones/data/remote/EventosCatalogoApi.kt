package cl.duoc.activaciones.data.remote

import cl.duoc.activaciones.model.EventoCatalogo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/** Endpoints de la API PHP de XAMPP sobre la tabla eventos_catalogo. */
interface EventosCatalogoApi {

    @GET("eventos.php")
    suspend fun listar(): List<EventoCatalogo>

    @GET("eventos.php")
    suspend fun obtener(@Query("id") id: Int): EventoCatalogo

    @POST("eventos.php")
    suspend fun crear(@Body evento: EventoCatalogo): EventoCatalogo

    @PUT("eventos.php")
    suspend fun actualizar(@Query("id") id: Int, @Body evento: EventoCatalogo): EventoCatalogo

    @DELETE("eventos.php")
    suspend fun eliminar(@Query("id") id: Int): Response<Unit>
}
