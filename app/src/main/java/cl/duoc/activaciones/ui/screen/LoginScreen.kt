package cl.duoc.activaciones.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.activaciones.ui.components.app
import cl.duoc.activaciones.ui.components.vmFactory
import cl.duoc.activaciones.ui.theme.FormaBoton
import cl.duoc.activaciones.ui.theme.coloresCampo
import cl.duoc.activaciones.viewmodel.LoginViewModel

@Composable
fun LoginScreen(alEntrar: () -> Unit, alRegistrar: () -> Unit) {
    val app = app()
    val vm: LoginViewModel = viewModel(factory = vmFactory { LoginViewModel(app.repo, app.sesion) })
    val error by vm.error.collectAsState()
    var correo by rememberSaveable { mutableStateOf("ejecutivo@demo.cl") }
    var clave by rememberSaveable { mutableStateOf("1234") }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Activaciones", style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
        )
        Text("Gestión de oportunidades corporativas", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            correo, { correo = it }, label = { Text("Correo") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            colors = coloresCampo(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            clave, { clave = it }, label = { Text("Contraseña") }, singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            colors = coloresCampo(), modifier = Modifier.fillMaxWidth()
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { vm.ingresar(correo, clave, alEntrar) },
            shape = FormaBoton,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("INGRESAR") }
        Spacer(Modifier.height(16.dp))
        Row {
            Text("¿No tienes cuenta? ")
            Text(
                "Crear cuenta", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = alRegistrar)
            )
        }
    }
}