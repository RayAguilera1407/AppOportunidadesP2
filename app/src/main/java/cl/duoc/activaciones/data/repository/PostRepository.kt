package cl.duoc.activaciones.data.repository

import cl.duoc.activaciones.model.Post

class PostRepository {

    private val eventoRepository = EventoExternoRepository()

    suspend fun getPosts(): List<Post> {
        val resultado = eventoRepository.obtenerEventos()
        return resultado.eventos.map { evento ->
            Post(
                userId = 1,
                id = evento.id,
                title = evento.nombre,
                body = """
                    Fecha: ${evento.fecha}
                    Comuna: ${evento.comuna}
                    Organizador: ${evento.organizacion}
                """.trimIndent()
            )
        }
    }
}