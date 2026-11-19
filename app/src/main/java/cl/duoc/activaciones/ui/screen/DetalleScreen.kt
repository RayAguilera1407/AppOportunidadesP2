package cl.duoc.activaciones.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.activaciones.model.EstadoOportunidad
import cl.duoc.activaciones.model.Prioridad
import cl.duoc.activaciones.model.ResultadoActivacion
import cl.duoc.activaciones.model.ResultadoErrores
import cl.duoc.activaciones.model.Seguimiento
import cl.duoc.activaciones.model.SeguimientoErrores
import cl.duoc.activaciones.model.TipoAccion
import cl.duoc.activaciones.ui.components.Campo
import cl.duoc.activaciones.ui.components.ConfirmarEliminacion
import cl.duoc.activaciones.ui.components.EstadoChip
import cl.duoc.activaciones.ui.components.Selector
import cl.duoc.activaciones.ui.components.app
import cl.duoc.activaciones.ui.components.vmFactory
import cl.duoc.activaciones.ui.theme.barraColores
import cl.duoc.activaciones.util.aFecha
import cl.duoc.activaciones.util.aFechaHora
import cl.duoc.activaciones.viewmodel.DetalleViewModel

@Composable
fun DetalleScreen(id: Long, alVolver: () -> Unit, alEditar: (Long) -> Unit) {
    val app = app()
    val vm: DetalleViewModel = viewModel(factory = vmFactory { DetalleViewModel(app.repo, app.oportunidadRepo, app.sesion, id) })
    val oportunidad by vm.oportunidad.collectAsState()
    val organizacion by vm.organizacion.collectAsState()
    val contactos by vm.contactos.collectAsState()
    val historial by vm.historial.collectAsState()
    val resultado by vm.resultado.collectAsState()
    val puedeCerrar by vm.puedeCerrar.collectAsState()
    val esSupervisor by vm.esSupervisor.collectAsState()
    val ejecutivos by vm.ejecutivos.collectAsState()
    val errSeguimiento by vm.erroresSeguimiento.collectAsState()
    val errResultado by vm.erroresResultado.collectAsState()
    var dialogoSeguimiento by remember { mutableStateOf(false) }
    var dialogoResultado by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var confirmarCierre by remember { mutableStateOf(false) }
    var seguimientoPorEliminar by remember { mutableStateOf<Seguimiento?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = barraColores(),
                title = { Text("Detalle de oportunidad") },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") } },
                actions = {
                    IconButton(onClick = { alEditar(id) }) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                    IconButton(onClick = { confirmarEliminar = true }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
                }
            )
        }
    ) { pad ->
        val o = oportunidad
        if (o == null) {
            Text("Cargando…", Modifier.padding(pad).padding(16.dp))
        } else LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(o.nombre, style = MaterialTheme.typography.headlineSmall)
                EstadoChip(o.estado)
            }
            item {
                Seccion("Oportunidad") {
                    Dato("Tipo", o.tipo.etiqueta)
                    Dato("Descripción", o.descripcion)
                    Dato("Fuente", o.fuente)
                    Dato("Detectada", o.fechaDeteccion.aFecha())
                    Dato("Prioridad", o.prioridad.etiqueta)
                    Dato("Potencial comercial", o.potencial.etiqueta)
                    if (o.observaciones.isNotBlank()) Dato("Observaciones", o.observaciones)
                    o.fechaCierre?.let { Dato("Cerrada el", it.aFecha()) }
                }
            }
            item {
                Seccion("Evento") {
                    Dato("Fechas", o.fechaInicio?.let { "${it.aFecha()} al ${(o.fechaTermino ?: it).aFecha()}" } ?: "Por definir")
                    if (o.horario.isNotBlank()) Dato("Horario", o.horario)
                    Dato("Lugar", listOf(o.comuna, o.region).filter { it.isNotBlank() }.joinToString(", ").ifEmpty { "Por definir" })
                    Dato("Público esperado", o.publicoEsperado.toString())
                    Dato("Condiciones", o.condiciones)
                    if (o.espacio.isNotBlank()) Dato("Espacio", o.espacio)
                    Dato("Costo", "$${o.costo}")
                }
            }
            if (o.referenciaGeografica.isNotBlank() || o.fotoPath != null ||
                o.codigoQr.isNotBlank() || o.documentoPath != null
            ) {
                item {
                    Seccion("Evidencias") {
                        if (o.referenciaGeografica.isNotBlank()) Dato("Referencia geográfica", o.referenciaGeografica)
                        if (o.codigoQr.isNotBlank()) Dato("Código QR", o.codigoQr)
                        if (o.documentoPath != null) Dato("Documento", o.documentoNombre)
                        FotoEvidencia(o.fotoPath)
                    }
                }
            }
            item {
                Seccion("Organización y contactos") {
                    if (organizacion == null) Dato("Organización", "Sin asignar")
                    organizacion?.let { Dato(it.nombre, "${it.tipo} · ${it.rubro}") }
                    contactos.firstOrNull { it.id == o.contactoId }?.let {
                        Dato("Contacto de la oportunidad", "${it.nombre} · ${it.cargo}")
                    }
                    contactos.forEach { c -> Dato(c.nombre, "${c.cargo} (${c.area}) · ${c.correo} · ${c.telefono}") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Selector("Estado", EstadoOportunidad.values().toList(), o.estado, { it.etiqueta }) { vm.cambiarEstado(it) }
                }
            }
            if (esSupervisor) {
                item {
                    Seccion("Supervisión") {
                        Selector("Prioridad", Prioridad.values().toList(), o.prioridad, { it.etiqueta }) { vm.cambiarPrioridad(it) }
                        Selector(
                            "Responsable",
                            ejecutivos.map { it.id },
                            o.responsableId ?: -1L,
                            { execId -> ejecutivos.firstOrNull { it.id == execId }?.nombre ?: "Sin asignar" }
                        ) { vm.reasignar(it) }
                    }
                }
            }
            item {
                val cerrada = o.fechaCierre != null
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                vm.limpiarErrores()
                                dialogoSeguimiento = true
                            },
                            enabled = !cerrada
                        ) {
                            Text("Registrar seguimiento")
                        }
                        if (o.estado == EstadoOportunidad.PARTICIPACION_CONFIRMADA) {
                            OutlinedButton(
                                onClick = {
                                    vm.limpiarErrores()
                                    dialogoResultado = true
                                },
                                enabled = !cerrada
                            ) {
                                Text("Resultado")
                            }
                        }
                    }
                    if (cerrada) {
                        Text("Oportunidad cerrada", color = MaterialTheme.colorScheme.secondary)
                        OutlinedButton(onClick = vm::reabrir) { Text("Reabrir oportunidad") }
                    } else {
                        OutlinedButton(onClick = { confirmarCierre = true }, enabled = puedeCerrar) {
                            Text("Cerrar oportunidad")
                        }
                        if (!puedeCerrar) {
                            Text(
                                "Para cerrar: descártala, o confirma la participación y registra el resultado.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            resultado?.let { r ->
                item {
                    Seccion("Resultado de la activación") {
                        Dato("Asistentes", r.asistentes.toString())
                        Dato("Contactos obtenidos", r.contactosObtenidos.toString())
                        Dato("Inscripciones", r.inscripciones.toString())
                        Dato("Evaluación", r.evaluacion)
                        if (r.observaciones.isNotBlank()) Dato("Observaciones", r.observaciones)
                    }
                }
            }
            item { Text("Historial", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            items(historial, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${s.tipoAccion.etiqueta} · ${s.fechaHora.aFechaHora()}", fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (s.tipoAccion != TipoAccion.CAMBIO_ESTADO && s.tipoAccion != TipoAccion.CIERRE) {
                                IconButton(onClick = { seguimientoPorEliminar = s }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar seguimiento", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Text(s.resultado)
                        if (s.proximaAccion.isNotBlank()) {
                            Text(
                                "Próxima acción: ${s.proximaAccion}" + (s.fechaProximoSeguimiento?.let { " (${it.aFecha()})" } ?: ""),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            item { Text("") }
        }
    }

    if (confirmarEliminar) {
        ConfirmarEliminacion(
            titulo = "Eliminar oportunidad",
            mensaje = "Se eliminará la oportunidad con su historial, resultado y evidencias. Esta acción no se puede deshacer.",
            alCancelar = { confirmarEliminar = false },
            alConfirmar = {
                confirmarEliminar = false
                vm.eliminar(alTerminar = alVolver)
            }
        )
    }
    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text("Cerrar oportunidad") },
            text = { Text("Se registrará el cierre en el historial y dejará de generar alertas. Puedes reabrirla después.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.cerrar()
                    confirmarCierre = false
                }) { Text("Cerrar") }
            },
            dismissButton = { TextButton(onClick = { confirmarCierre = false }) { Text("Cancelar") } }
        )
    }
    seguimientoPorEliminar?.let { s ->
        ConfirmarEliminacion(
            titulo = "Eliminar seguimiento",
            mensaje = "¿Eliminar el registro \"${s.resultado}\" del historial?",
            alCancelar = { seguimientoPorEliminar = null },
            alConfirmar = {
                vm.eliminarSeguimiento(s)
                seguimientoPorEliminar = null
            }
        )
    }
    if (dialogoSeguimiento) {
        DialogoSeguimiento(
            errores = errSeguimiento,
            alCerrar = { dialogoSeguimiento = false }
        ) { tipo, resultadoSeg, proxima, dias ->
            vm.agregarSeguimiento(tipo, resultadoSeg, proxima, dias) {
                dialogoSeguimiento = false
            }
        }
    }
    if (dialogoResultado) {
        DialogoResultado(
            actual = resultado,
            errores = errResultado,
            alCerrar = { dialogoResultado = false }
        ) { asistentes, contactosObt, inscripciones, evaluacion, observaciones ->
            vm.guardarResultado(asistentes, contactosObt, inscripciones, evaluacion, observaciones) {
                dialogoResultado = false
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            contenido()
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Text("$etiqueta: $valor", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun DialogoSeguimiento(
    errores: SeguimientoErrores,
    alCerrar: () -> Unit,
    alGuardar: (TipoAccion, String, String, String) -> Unit
) {
    val tipos = TipoAccion.values().filter { it != TipoAccion.CAMBIO_ESTADO && it != TipoAccion.CIERRE }
    var tipo by remember { mutableStateOf(TipoAccion.LLAMADA) }
    var resultado by remember { mutableStateOf("") }
    var proxima by remember { mutableStateOf("") }
    var dias by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Nuevo seguimiento") },
        text = {
            Column {
                Selector("Acción", tipos, tipo, { it.etiqueta }) { tipo = it }
                Campo("Resultado", resultado, error = errores.resultado, unaLinea = false) { resultado = it }
                Campo("Próxima acción", proxima) { proxima = it }
                Campo("Próximo seguimiento en (días)", dias, error = errores.dias, numerico = true) { dias = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { alGuardar(tipo, resultado, proxima, dias) }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoResultado(
    actual: ResultadoActivacion?,
    errores: ResultadoErrores,
    alCerrar: () -> Unit,
    alGuardar: (String, String, String, String, String) -> Unit
) {
    var asistentes by remember { mutableStateOf(actual?.asistentes?.toString() ?: "") }
    var contactos by remember { mutableStateOf(actual?.contactosObtenidos?.toString() ?: "") }
    var inscripciones by remember { mutableStateOf(actual?.inscripciones?.toString() ?: "") }
    var evaluacion by remember { mutableStateOf(actual?.evaluacion ?: "") }
    var observaciones by remember { mutableStateOf(actual?.observaciones ?: "") }

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Resultado de la activación") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Campo("Asistentes", asistentes, error = errores.asistentes, numerico = true) { asistentes = it }
                Campo("Contactos obtenidos", contactos, error = errores.contactos, numerico = true) { contactos = it }
                Campo("Inscripciones", inscripciones, error = errores.inscripciones, numerico = true) { inscripciones = it }
                Campo("Impacto o evaluación", evaluacion, error = errores.evaluacion, unaLinea = false) { evaluacion = it }
                Campo("Observaciones", observaciones, unaLinea = false) { observaciones = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { alGuardar(asistentes, contactos, inscripciones, evaluacion, observaciones) }) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )
}