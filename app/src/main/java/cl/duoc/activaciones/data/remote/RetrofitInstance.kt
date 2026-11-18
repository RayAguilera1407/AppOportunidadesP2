package cl.duoc.activaciones.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Singleton que contiene la configuración de Retrofit
object RetrofitInstance {

    // Se instancia el servicio de la API una sola vez
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com") // URL base de la API
            .addConverterFactory(GsonConverterFactory.create()) // Conversor JSON
            .build()
            .create(ApiService::class.java) // Implementa la interfaz ApiService
    }

    // Catálogo de eventos externos (archivo eventos.json en tu repo de GitHub)
    val eventosApi: EventosApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/RayAguilera1407/AppOportunidadesP2/main/") // debe terminar en "/"
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EventosApi::class.java)
    }
}