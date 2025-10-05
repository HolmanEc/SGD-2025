package com.holman.sgd.resources

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holman.sgd.R
import com.holman.sgd.ui.theme.BackgroundDefault

@Composable
fun AboutScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDefault)
            .padding(32.dp)
    ) {
        // 🔹 Contenido con scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 60.dp), // espacio para el pie
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo_launcher),
                contentDescription = "Logo App",
                modifier = Modifier
                    .size(120.dp)
                    .padding(top = 32.dp, bottom = 32.dp)
            )

            // Título
            Text(
                text = "Sobre Nosotros",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 32.dp, bottom = 32.dp)
            )

            // Descripción
            Text(
                text = "Nuestra aplicación está diseñada para facilitar la gestión académica de docentes, estudiantes y padres de familia.\n\n" +
                        "🔹 Registro y revisión de nóminas.\n" +
                        "🔹 Control de asistencias.\n" +
                        "🔹 Consulta de calificaciones.\n" +
                        "🔹 Reportes y comunicación con padres.\n\n" +
                        "El objetivo es brindar una herramienta moderna, eficiente y fácil de usar para la administración escolar.",
                fontSize = 16.sp,
                textAlign = TextAlign.Justify
            )
        }

        // 🔹 Pie con créditos siempre abajo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "© 2025 - Holman Dev\nTodos los derechos reservados.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
