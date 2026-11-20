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

    /**
     * Base de datos externa: API PHP en XAMPP sobre MySQL (tabla eventos_catalogo).
     * 10.0.2.2 es el "localhost" del PC visto desde el emulador.
     * En un celular físico, cambia por la IP del PC en la red Wi-Fi (por ejemplo 192.168.1.10).
     */
    const val URL_XAMPP = "http://10.0.2.2/activaciones_api/"

    val catalogoApi: EventosCatalogoApi by lazy {
        Retrofit.Builder()
            .baseUrl(URL_XAMPP)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EventosCatalogoApi::class.java)
    }
}
