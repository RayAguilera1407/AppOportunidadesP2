package cl.duoc.activaciones.model

import cl.duoc.activaciones.data.local.OportunidadEntity

/** El ejecutivo ve solo sus oportunidades; el supervisor ve todas. */
fun visibles(ops: List<OportunidadEntity>, usuario: Usuario?): List<OportunidadEntity> = when {
    usuario == null -> emptyList()
    usuario.perfil == Perfil.SUPERVISOR -> ops
    else -> ops.filter { it.responsableId == usuario.id }
}

fun cumplePeriodo(fecha: Long?, periodo: PeriodoFecha, ahora: Long): Boolean = when (periodo) {
    PeriodoFecha.TODAS -> true
    PeriodoFecha.SIN_FECHA -> fecha == null
    PeriodoFecha.PROXIMOS_7 -> fecha != null && fecha in ahora..(ahora + 7 * DIA_MS)
    PeriodoFecha.PROXIMOS_30 -> fecha != null && fecha in ahora..(ahora + 30 * DIA_MS)
    PeriodoFecha.PASADOS -> fecha != null && fecha < ahora
}

fun filtrar(
    ops: List<OportunidadEntity>,
    f: Filtro,
    ahora: Long = System.currentTimeMillis()
): List<OportunidadEntity> = ops.filter { o ->
    (f.texto.isBlank() || o.nombre.contains(f.texto, true) || o.comuna.contains(f.texto, true) ||
            o.descripcion.contains(f.texto, true)) &&
            (f.estado == null || o.estado == f.estado) &&
            (f.prioridad == null || o.prioridad == f.prioridad) &&
            (f.tipo == null || o.tipo == f.tipo) &&
            (f.region == null || o.region == f.region) &&
            (f.responsableId == null || o.responsableId == f.responsableId) &&
            (f.organizacionId == null || o.organizacionId == f.organizacionId) &&
            cumplePeriodo(o.fechaInicio, f.periodo, ahora)
}

fun calcularAlertas(ops: List<OportunidadEntity>, segs: List<Seguimiento>, ahora: Long): Alertas {
    val porOp = segs.groupBy { it.oportunidadId }
    val cerradas = setOf(EstadoOportunidad.DESCARTADA, EstadoOportunidad.PARTICIPACION_CONFIRMADA)

    val proximos = ops.filter {
        it.estado != EstadoOportunidad.DESCARTADA &&
                it.fechaInicio?.let { f -> f in ahora..(ahora + DIAS_EVENTO_PROXIMO * DIA_MS) } == true
    }
    val atrasados = ops.filter { o ->
        val ultimo = porOp[o.id].orEmpty()
            .filter { it.fechaProximoSeguimiento != null }
            .maxByOrNull { it.fechaHora }
        o.estado !in cerradas && ultimo != null && ultimo.fechaProximoSeguimiento!! < ahora
    }
    val sinContacto = ops.filter { o ->
        o.estado in setOf(EstadoOportunidad.EVENTO_IDENTIFICADO, EstadoOportunidad.CONTACTO_PENDIENTE) &&
                porOp[o.id].orEmpty().none { it.tipoAccion != TipoAccion.CAMBIO_ESTADO }
    }
    val sinRespuesta = ops.filter { o ->
        o.estado in setOf(EstadoOportunidad.PROPUESTA_ENVIADA, EstadoOportunidad.EN_ESPERA_RESPUESTA) &&
                ahora - o.fechaCambioEstado > DIAS_SIN_RESPUESTA * DIA_MS
    }
    return Alertas(proximos, atrasados, sinContacto, sinRespuesta)
}