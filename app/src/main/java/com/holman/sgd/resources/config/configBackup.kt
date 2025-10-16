package com.holman.sgd.resources.config

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.ui.theme.BackgroundDefault
import com.holman.sgd.ui.theme.ButtonDarkGray

@Composable
fun RespaldoRestauracionScreen(onNavigateBack: () -> Unit) {
    BackHandler(enabled = true) { onNavigateBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDefault)
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Respaldo y Restauración", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(16.dp))

        // TODO: Exportar/Importar datos locales o de Firestore/Drive
        Text("Exporta o importa los datos de la aplicación.")

        Spacer(Modifier.weight(1f))

        CustomButton(
            text = "Volver",
            borderColor = ButtonDarkGray,
            onClick = onNavigateBack
        )
    }
}
