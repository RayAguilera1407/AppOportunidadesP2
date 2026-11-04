package cl.duoc.activaciones.data.repository

import cl.duoc.activaciones.data.remote.RetrofitInstance
import cl.duoc.activaciones.model.Post

// Este repositorio se encarga de acceder a los datos usando Retrofit
class PostRepository {

    // Función que obtiene los posts desde la API
    suspend fun getPosts(): List<Post> {
        return RetrofitInstance.api.getPosts()
    }
}