package com.holman.sgd.resources.config

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.ui.theme.BackgroundDefault
import com.holman.sgd.ui.theme.ButtonDarkGray

@Composable
fun SeguridadScreen(onNavigateBack: () -> Unit) {
    BackHandler(enabled = true) { onNavigateBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDefault)
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Seguridad", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(16.dp))

        // TODO: Cambio de contraseña, bloqueo con huella/biometría, PIN, etc.
        Text("Configura bloqueo con huella, PIN o cambio de contraseña.")

        Spacer(Modifier.weight(1f))

        CustomButton(
            text = "Volver",
            borderColor = ButtonDarkGray,
            onClick = onNavigateBack
        )
    }
}
