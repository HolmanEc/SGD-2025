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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.holman.sgd.ui.theme.*
import com.holman.sgd.resources.components.*

@SuppressLint("ContextCastToActivity", "UnusedBoxWithConstraintsScope")

@Composable
fun Inicio(navController: NavHostController) {
    SalirDialog()

    val cards = getCardsInicio()
    val colors = getColorsCardsInicio()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidthDp = maxWidth

        // Define cuántas cards mostrar por fila según el ancho
        val cardsPerRow = if (maxWidthDp < 700.dp) 2 else 3

        Image(
            painter = painterResource(id = R.drawable.fondo3),
            contentDescription = "Fondo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.50f))
        )

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

                            MenuCard(
                                title = title,
                                iconResId = icon,
                                backgroundColor = colors[(i + j) % colors.size],
                                descriptionCard = description,
                                modifier = Modifier.weight(1f)
                            ) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MenuCard(
    title: String,
    iconResId: Int,
    backgroundColor: Color,
    descriptionCard: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isTablet = isTablet() // usamos Configuración del sistema 📱💻

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // todas cuadradas, como al inicio
            .background(
                color = backgroundColor.copy(alpha = 0.95f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícono
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = title,
                modifier = Modifier.size(if (isTablet) 72.dp else 56.dp)
            )

            // Título
            Text(
                text = title.uppercase(),
                color = TextDefaultBlack,
                fontWeight = FontWeight.Bold,
                fontSize = if (isTablet) 16.sp else 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // ✅ Solo en TABLET aparece descripción
            if (isTablet) {
                Text(
                    text = descriptionCard,
                    color = TextDefaultBlack,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
// Funcion auxiliar

@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 600
}
@SuppressLint("ContextCastToActivity")
@Composable
fun SalirDialog() {
    val context = LocalContext.current
    val activity = context as? Activity
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        Dialog(
            onDismissRequest = { showExitDialog = false }
        ) {
            // Contenedor del diálogo (tú controlas color, bordes y sombra)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BackgroundDefault,         // color de fondo del diálogo
                tonalElevation = 0.dp,             // sin elevación tonal
                shadowElevation = 16.dp,           // << sombra real visible
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)   // margen respecto a los bordes de pantalla
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
