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
import com.holman.sgd.ui.theme.BackgroundDefault

@Composable
fun AparienciaScreen(onNavigateBack: () -> Unit) {
    BackHandler(enabled = true) { onNavigateBack() } // Por qué: mantener UX de back

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDefault)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Apariencia",
            style = MaterialTheme.typography.titleLarge
        )
    }
}
