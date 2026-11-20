package cl.duoc.activaciones.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.activaciones.model.EventoCatalogo
import cl.duoc.activaciones.model.TIPOS_EVENTO_CATALOGO
import cl.duoc.activaciones.viewmodel.EventoCatalogoViewModel

/** Catálogo de eventos almacenado en MySQL (XAMPP) y consumido con Retrofit. */
@Composable
fun EventoCatalogoScreen(alVolver: () -> Unit, vm: EventoCatalogoViewModel = viewModel()) {
    val s by vm.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var porEliminar by remember { mutableStateOf<EventoCatalogo?>(null) }

    LaunchedEffect(s.mensaje) {
        s.mensaje?.let { snackbar.showSnackbar(it); vm.mensajeMostrado() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de eventos (${s.eventos.size})") },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                actions = { IconButton(onClick = vm::cargar) { Icon(Icons.Default.Refresh, "Recargar") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::nuevo) { Icon(Icons.Default.Add, "Registrar evento") }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                s.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                s.error != null -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = vm::cargar) { Text("Reintentar") }
                }
                s.eventos.isEmpty() -> Text(
                    "No hay eventos registrados. Toca + para agregar uno.",
                    Modifier.align(Alignment.Center).padding(24.dp)
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(s.eventos, key = { it.id }) { e ->
                        EventoCard(e, alEditar = { vm.editar(e) }, alEliminar = { porEliminar = e })
                    }
                }
            }
        }
    }

    if (s.formularioAbierto) FormularioEvento(vm)

    porEliminar?.let { e ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar evento") },
            text = { Text("Se eliminará \"${e.nombre}\" de la base de datos.") },
            confirmButton = { TextButton(onClick = { vm.eliminar(e); porEliminar = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EventoCard(e: EventoCatalogo, alEditar: () -> Unit, alEliminar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.nombre, style = MaterialTheme.typography.titleMedium)
                Text("${e.tipo} · ${e.lugar}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${EventoCatalogoViewModel.aPantalla(e.fecha)} · ${e.cupos} cupos",
                    style = MaterialTheme.typography.bodySmall
                )
                if (e.descripcion.isNotBlank()) Text(e.descripcion, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = alEditar) { Icon(Icons.Default.Edit, "Editar") }
            IconButton(onClick = alEliminar) {
                Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun FormularioEvento(vm: EventoCatalogoViewModel) {
    val s by vm.uiState.collectAsState()
    val e = s.errores
    AlertDialog(
        onDismissRequest = vm::cerrarFormulario,
        title = { Text(if (s.idEdicion == null) "Registrar evento" else "Editar evento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CampoTexto("Nombre", s.nombre, vm::onNombre, e.nombre)
                SelectorTipo(s.tipo, vm::onTipo, e.tipo)
                CampoTexto("Fecha (dd/MM/aaaa)", s.fecha, vm::onFecha, e.fecha)
                CampoTexto("Lugar o comuna", s.lugar, vm::onLugar, e.lugar)
                CampoTexto("Cupos", s.cupos, vm::onCupos, e.cupos, numerico = true)
                CampoTexto("Descripción (opcional)", s.descripcion, vm::onDescripcion, null)
            }
        },
        confirmButton = {
            TextButton(onClick = vm::guardar, enabled = !s.guardando) {
                Text(if (s.guardando) "Guardando..." else "Guardar")
            }
        },
        dismissButton = { TextButton(onClick = vm::cerrarFormulario) { Text("Cancelar") } }
    )
}

@Composable
private fun CampoTexto(
    etiqueta: String, valor: String, alCambiar: (String) -> Unit, error: String?, numerico: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        singleLine = true,
        keyboardOptions = if (numerico) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SelectorTipo(valor: String, alElegir: (String) -> Unit, error: String?) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo") },
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(abierto) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            TIPOS_EVENTO_CATALOGO.forEach { t ->
                DropdownMenuItem(text = { Text(t) }, onClick = { alElegir(t); abierto = false })
            }
        }
    }
}
