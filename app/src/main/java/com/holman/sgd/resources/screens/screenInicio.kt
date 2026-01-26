package com.holman.sgd.resources.screens

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.holman.sgd.R
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.resources.DefaultCard
import com.holman.sgd.resources.FondoInicio
import com.holman.sgd.ui.theme.*
import com.holman.sgd.resources.components.*

@SuppressLint("ContextCastToActivity", "UnusedBoxWithConstraintsScope")
@Composable
fun Inicio(navController: NavHostController) {

    // Dialog global de salida (misma funcionalidad)
    SalirDialog()

    // Datos del menú
    val cards = getCardsInicio()
    val colors = getColorsCardsInicio()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cardsPerRow = calculateCardsPerRow(maxWidth)

        // Fondo
        FondoInicio()

        // Grid
        InicioGrid(
            cards = cards,
            colors = colors,
            cardsPerRow = cardsPerRow,
            onCardClick = { route ->
                navigateSingleTop(navController, route)
            }
        )
    }
}

@Composable
private fun InicioGrid(
    cards: List<Triple<String, Int, Pair<String, String>>>,
    colors: List<Color>,
    cardsPerRow: Int,
    onCardClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in cards.indices step cardsPerRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (j in 0 until cardsPerRow) {
                    if (i + j < cards.size) {
                        val (title, icon, routeDesc) = cards[i + j]
                        val (route, description) = routeDesc

                        DefaultCard(
                            title = title,
                            iconResId = icon,
                            backgroundColor = colors[(i + j) % colors.size],
                            backgroundAlpha = Transparencia.STRONG,
                            descriptionCard = description,
                            modifier = Modifier.weight(1f),
                            onClick = { onCardClick(route) }
                        )
                    } else {
                        // Mantiene el grid alineado cuando faltan cards en la última fila
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ============================================================================
// Helpers
// ============================================================================

/** Define cuántas cards por fila según el ancho máximo disponible */
private fun calculateCardsPerRow(maxWidthDp: androidx.compose.ui.unit.Dp): Int {
    return if (maxWidthDp < 700.dp) 2 else 3
}

/** Navegación: mantiene tu popUpTo + launchSingleTop exactamente igual */
private fun navigateSingleTop(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) {
            inclusive = false
        }
        launchSingleTop = true
    }
}

/** Helper: detectar tablet por smallestScreenWidthDp (misma lógica) */
@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 600
}

// ============================================================================
// Dialog de salida (BackHandler) - misma funcionalidad
// ============================================================================

@SuppressLint("ContextCastToActivity")
@Composable
fun SalirDialog() {
    val context = LocalContext.current
    val activity = context as? Activity
    var showExitDialog by remember { mutableStateOf(false) }

    // Back -> mostrar dialog
    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        Dialog(onDismissRequest = { showExitDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BackgroundDefault,
                tonalElevation = 0.dp,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .wrapContentWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Salir de la aplicación",
                        color = TextDefaultBlack,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "¿Seguro que quieres salir de la aplicación?",
                        color = TextDefaultBlack,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomButton(
                                text = "Sí, Salir",
                                borderColor = ButtonDarkError,
                                onClick = {
                                    showExitDialog = false
                                    activity?.finish()
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomButton(
                                text = "Cancelar",
                                borderColor = ButtonDarkGray,
                                onClick = { showExitDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
