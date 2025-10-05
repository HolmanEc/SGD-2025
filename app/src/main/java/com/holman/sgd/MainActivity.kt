package com.holman.sgd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.holman.sgd.ui.theme.SGDTheme
import kotlinx.coroutines.launch
import com.holman.sgd.resources.*
import com.holman.sgd.resources.screens.*
import com.holman.sgd.resources.nominas.*
import com.holman.sgd.ui.theme.*

// MAIN ACTIVITY
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SGDTheme {
                App()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Estado centralizado de la app
    val appState = remember(navController, drawerState, scope) {
        AppState(navController, drawerState, scope)
    }

    // Detectar ruta actual
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentRoute = currentRoute,
                onDestinationClick = { screen ->
                    appState.navigateSingleTop(screen.route, Screen.inicio.route)
                }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Sistema de Gestión Docente", color = TextDefaultWhite) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú", tint = TextDefaultWhite)
                        }
                    },
                    actions = {
                        // 🏠 Botón de Inicio - Siempre lleva a la pantalla principal
                        IconButton(onClick = { appState.navigateSingleTop(Screen.inicio.route, Screen.inicio.route) }) {
                            Icon(Icons.Default.Home, contentDescription = "Inicio", tint = TextDefaultWhite)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // ⚙️ Botón de Configuración
                        IconButton(onClick = { appState.navigateSingleTop(Screen.config.route, Screen.inicio.route) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Configuración", tint = TextDefaultWhite)
                        }

                        // ℹ️ Botón de Acerca de
                        IconButton(onClick = { appState.navigateSingleTop(Screen.about.route, Screen.inicio.route) }) {
                            Icon(Icons.Default.Info, contentDescription = "Acerca de", tint = TextDefaultWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundBar)
                )

            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavigationHost(navController)
            }
        }
    }
}

@Composable
fun DrawerContent(
    currentRoute: String?,
    onDestinationClick: (Screen) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = BackgroundBar,
        drawerContentColor = TextDefaultWhite
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(16.dp))
                Image(
                    painter = painterResource(id = R.drawable.barra),
                    contentDescription = "Logo de la app",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "SISTEMA DE GESTIÓN DOCENTE",
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
            }

            // Items
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Screen.items.forEach { screen ->
                    val isSelected = screen.route == currentRoute
                    val backgroundColor =
                        if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent
                    val textColor = Color.White
                    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .clickable { onDestinationClick(screen) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = textColor
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = screen.title,
                            color = textColor,
                            fontWeight = fontWeight
                        )
                    }
                }
            }
        }
    }

}


// Definición de pantallas con íconos
sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object inicio : Screen("inicio", "Inicio", Icons.Default.Home)
    object nominas : Screen("nominas", "Nóminas", Icons.Default.List)
    object calificaciones : Screen("calificaciones", "Calificaciones", Icons.Default.Grade)
    object asistencias : Screen("asistencias", "Asistencias", Icons.Default.Event)
    object tutoria : Screen("tutoria", "Tutoría", Icons.Default.People)
    object documentos : Screen("documentos", "Documentos", Icons.Default.Description)
    object varios : Screen("varios", "Varios", Icons.Default.MoreHoriz)
    object config : Screen("config", "Configuración", Icons.Default.Settings)
    object about : Screen("about", "Acerca de", Icons.Default.Info)

    companion object {
        val items = listOf(inicio, nominas, calificaciones, asistencias, tutoria, documentos, varios)
    }
}

// NAV HOST
@Composable
fun NavigationHost(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.inicio.route) {
        composable(Screen.inicio.route) { Inicio(navController) }
        composable(Screen.nominas.route) { Nominas(navController = navController) }
        composable(Screen.calificaciones.route) { Calificaciones(navController = navController) }
        composable(Screen.asistencias.route) { Asistencias(navController = navController) }
        composable(Screen.tutoria.route) { Tutoria() }
        composable(Screen.documentos.route) { Documentos() }
        composable(Screen.varios.route) { Varios() }
        composable(Screen.config.route) { ConfiguracionScreen() }
        composable(Screen.about.route) { AboutScreen() }
    }
}

// APP STATE CENTRALIZADO
class AppState(
    private val navController: NavHostController,
    private val drawerState: DrawerState,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    fun navigateSingleTop(route: String, root: String) {
        scope.launch { drawerState.close() }
        navController.navigate(route) {
            popUpTo(root) { inclusive = false }
            launchSingleTop = true
        }
    }
}
