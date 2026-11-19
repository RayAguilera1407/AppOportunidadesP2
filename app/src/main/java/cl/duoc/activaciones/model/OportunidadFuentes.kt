package cl.duoc.activaciones.model

import cl.duoc.activaciones.data.repository.OportunidadRepository
import cl.duoc.activaciones.data.repository.Repositorio
import cl.duoc.activaciones.util.Sesion

/** Dependencias del módulo de oportunidades: repositorios y sesión. */
data class OportunidadFuentes(
    val oportunidadRepo: OportunidadRepository,
    val repo: Repositorio,
    val sesion: Sesion
)