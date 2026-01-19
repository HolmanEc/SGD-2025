package com.holman.sgd.resources

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holman.sgd.resources.components.ContenedorPrincipal
import com.holman.sgd.resources.config.*
import com.holman.sgd.ui.theme.BackgroundDefault

@Composable
fun Configuracion() {
    var showGestionScreen by rememberSaveable { mutableStateOf(false) }
    var showAboutScreen by rememberSaveable { mutableStateOf(false) }
    var showAparienciaScreen by rememberSaveable { mutableStateOf(false) }
    var showCalendarioScreen by rememberSaveable { mutableStateOf(false) }
    var showNotificacionesScreen by rememberSaveable { mutableStateOf(false) }
    var showRespaldoScreen by rememberSaveable { mutableStateOf(false) }
    var showSeguridadScreen by rememberSaveable { mutableStateOf(false) }

    val anySubscreen = showGestionScreen || showAboutScreen || showAparienciaScreen ||
            showCalendarioScreen || showNotificacionesScreen || showRespaldoScreen || showSeguridadScreen

    BackHandler(enabled = anySubscreen) {
        when {
            showGestionScreen        -> showGestionScreen = false
            showAboutScreen          -> showAboutScreen = false
            showAparienciaScreen     -> showAparienciaScreen = false
            showCalendarioScreen     -> showCalendarioScreen = false
            showNotificacionesScreen -> showNotificacionesScreen = false
            showRespaldoScreen       -> showRespaldoScreen = false
            showSeguridadScreen      -> showSeguridadScreen = false
        }
    }

    when {
        showGestionScreen        -> GestionAcademicaScreen(onNavigateBack = { showGestionScreen = false })
        showAboutScreen          -> About(onNavigateBack = { showAboutScreen = false })
        showAparienciaScreen     -> AparienciaScreen(onNavigateBack = { showAparienciaScreen = false })
        showCalendarioScreen     -> CalendarioScreen(onNavigateBack = { showCalendarioScreen = false })
        showNotificacionesScreen -> NotificacionesScreen(onNavigateBack = { showNotificacionesScreen = false })
        showRespaldoScreen       -> RespaldoRestauracionScreen(onNavigateBack = { showRespaldoScreen = false })
        showSeguridadScreen      -> SeguridadScreen(onNavigateBack = { showSeguridadScreen = false })
        else -> {
            Box(modifier = Modifier
                .fillMaxSize()
            ) {
                FondoScreenDefault()

                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(ContenedorPrincipal),
                ) {
                    TituloGeneralScreens(texto = "Configuraciones")
                    Spacer(modifier = Modifier.width(8.dp))

                    // 🔹 Contenido principal encima del fondo
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        //contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            ConfigCard(
                                icon = Icons.Default.Brush,
                                titulo = "Apariencia",
                                descripcion = "Cambia el tema de la app entre claro y oscuro",
                                onClick = { showAparienciaScreen = true }
                            )
                        }

                        item {
                            ConfigCard(
                                icon = Icons.Default.School,
                                titulo = "Gestión Académica",
                                descripcion = "Administra docentes, cursos, paralelos, asignaturas, especialidades y periodos",
                                onClick = { showGestionScreen = true }
                            )
                        }

                        item {
                            ConfigCard(
                                icon = Icons.Default.Event,
                                titulo = "Calendario",
                                descripcion = "Define días de clase y periodos académicos",
                                onClick = { showCalendarioScreen = true }
                            )
                        }

                        item {
                            ConfigCard(
                                icon = Icons.Default.Notifications,
                                titulo = "Notificaciones",
                                descripcion = "Configura recordatorios y alertas",
                                onClick = { showNotificacionesScreen = true }
                            )
                        }

                        item {
                            ConfigCard(
                                icon = Icons.Default.Backup,
                                titulo = "Respaldo y Restauración",
                                descripcion = "Exporta o importa los datos de la app",
                                onClick = { showRespaldoScreen = true }
                            )
                        }

                        item {
                            ConfigCard(
                                icon = Icons.Default.Lock,
                                titulo = "Seguridad",
                                descripcion = "Cambiar contraseña o activar bloqueo con huella",
                                onClick = { showSeguridadScreen = true }
                            )
                        }

                        item {
                            ConfigCard(
                                icon = Icons.Default.Info,
                                titulo = "Acerca de la app",
                                descripcion = "Información y versión de la aplicación",
                                onClick = { showAboutScreen = true }
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun ConfigCard(
    icon: ImageVector,
    titulo: String,
    descripcion: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // 👈 aire extra alrededor de cada card
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundDefault)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = titulo,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = titulo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = descripcion,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
