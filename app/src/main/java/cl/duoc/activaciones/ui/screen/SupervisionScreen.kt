package cl.duoc.activaciones.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import cl.duoc.activaciones.ui.components.app
import cl.duoc.activaciones.ui.components.vmFactory
import cl.duoc.activaciones.ui.theme.barraColores
import cl.duoc.activaciones.viewmodel.SupervisionViewModel

@Composable
fun SupervisionScreen(alVolver: () -> Unit) {
    val app = app()
    val vm: SupervisionViewModel = viewModel(factory = vmFactory { SupervisionViewModel(app.repo, app.oportunidadRepo) })
    val r by vm.resumen.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = barraColores(),
                title = { Text("Supervisión general") },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") } }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Bloque("Resumen", listOf(
                    "Oportunidades totales" to "${r.total}",
                    "Cerradas" to "${r.cerradas}",
                    "Participaciones confirmadas" to "${r.participacionesConfirmadas}",
                    "Público esperado (confirmadas)" to "${r.publicoConfirmado}",
                    "Costo total (confirmadas)" to "$${r.costoConfirmado}"
                ))
            }
            item { Bloque("Por estado", r.porEstado.map { it.first to "${it.second}" }) }
            item { Bloque("Por prioridad", r.porPrioridad.map { it.first to "${it.second}" }) }
            item { Bloque("Por responsable", r.porResponsable.map { it.first to "${it.second}" }) }
        }
    }
}

@Composable
private fun Bloque(titulo: String, filas: List<Pair<String, String>>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            filas.forEach { (nombre, valor) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(nombre)
                    Text(valor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}