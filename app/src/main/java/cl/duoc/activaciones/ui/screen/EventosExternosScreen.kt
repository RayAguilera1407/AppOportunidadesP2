package cl.duoc.activaciones.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.activaciones.ui.theme.barraColores
import cl.duoc.activaciones.viewmodel.EventosExternosViewModel

@Composable
fun EventosExternosScreen(alVolver: () -> Unit) {
    val vm: EventosExternosViewModel = viewModel()
    val estado by vm.estado.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = barraColores(),
                title = { Text("Catálogo de eventos externos") },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") } },
                actions = { IconButton(onClick = vm::cargar) { Icon(Icons.Default.Refresh, contentDescription = "Actualizar") } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(
                value = estado.texto, onValueChange = vm::onTexto,
                label = { Text("Buscar por nombre, comuna u organización") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            )
            if (!estado.cargando && !estado.desdeServidor) {
                Text(
                    "Sin conexión al servicio: se muestran datos ficticios de prueba.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (estado.cargando) {
                CircularProgressIndicator(Modifier.padding(16.dp))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(estado.filtrados, key = { it.id }) { e ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(e.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("ID ${e.id} · ${e.fecha}")
                                Text("${e.comuna} · ${e.organizacion}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}