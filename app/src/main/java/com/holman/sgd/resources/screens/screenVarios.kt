package com.holman.sgd.resources.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.navigation.NavHostController
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.resources.DefaultCard
import com.holman.sgd.resources.FondoScreenDefault
import com.holman.sgd.resources.TituloGeneralScreens
import com.holman.sgd.resources.components.ContenedorPrincipal
import com.holman.sgd.resources.components.Transparencia
import com.holman.sgd.resources.components.getCardsVariosMenu
import com.holman.sgd.resources.varios.GestionContactosScreen
import com.holman.sgd.ui.theme.*

private enum class VariosScreen {
    MENU,
    GESTION_CONTACTOS,
    DATOS_GENERALES
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedBoxWithConstraintsScope")
@Composable
fun Varios(
    navController: NavHostController
) {
    var screen by remember { mutableStateOf(VariosScreen.MENU) }

    BackHandler(enabled = screen != VariosScreen.MENU) {
        screen = VariosScreen.MENU
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            FondoScreenDefault()

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // ✅ CAPTURA AQUÍ para evitar el error de receiver implícito
                val maxWidthDp: Dp = this.maxWidth

                when (screen) {
                    VariosScreen.MENU -> Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        VariosMenuScreen(
                            maxWidthDp = maxWidthDp,
                            onOpenGestionContactos = { screen = VariosScreen.GESTION_CONTACTOS },
                            onOpenDatosGenerales = { screen = VariosScreen.DATOS_GENERALES },
                            onBackApp = { navController.popBackStack() }
                        )
                    }

                    VariosScreen.GESTION_CONTACTOS -> Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        GestionContactosScreen(
                            onBack = { screen = VariosScreen.MENU }
                        )
                    }

                    VariosScreen.DATOS_GENERALES -> Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        DatosGeneralesScreen(
                            onBack = { screen = VariosScreen.MENU }
                        )
                    }
                }
            }
        }
    }
}

/**
 * ======================
 * ✅ SCREEN 2: MENÚ VARIOS
 * ======================
 */
@Composable
private fun VariosMenuScreen(
    maxWidthDp: Dp,
    onOpenGestionContactos: () -> Unit,
    onOpenDatosGenerales: () -> Unit,
    onBackApp: () -> Unit
) {
    val cards = getCardsVariosMenu()
    val cardsPerRow = if (maxWidthDp < 700.dp) 2 else 4

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(ContenedorPrincipal),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TituloGeneralScreens(texto = "Varios")
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Elige una opción para administrar secciones generales.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextDefaultBlack,
                modifier = Modifier.padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            for (i in cards.indices step cardsPerRow) {
                val rowItems = cards.drop(i).take(cardsPerRow)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        16.dp,
                        Alignment.CenterHorizontally
                    )
                ) {
                    rowItems.forEach { (title, icon, desc) ->

                        val onClick: () -> Unit = when (title) {
                            "Gestionar contactos" -> onOpenGestionContactos
                            "Datos generales" -> onOpenDatosGenerales
                            else -> ({})
                        }

                        DefaultCard(
                            title = title,
                            iconResId = icon,
                            backgroundColor = Card6,
                            backgroundAlpha = Transparencia.DEFAULT,
                            descriptionCard = desc,
                            modifier = Modifier.weight(1f),
                            onClick = onClick
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CustomButton(
                text = "Volver",
                borderColor = ButtonDarkGray,
                onClick = onBackApp
            )
        }
    }
}

/**
 * ======================
 * ✅ SCREEN 3: DATOS GENERALES
 * ======================
 */
@Composable
private fun DatosGeneralesScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(ContenedorPrincipal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            TituloGeneralScreens(texto = "Datos generales")
            Spacer(Modifier.height(16.dp))

            Text(
                text = "¡Bienvenidos!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDefaultBlack,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Aquí podrás administrar los datos generales.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextDefaultBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CustomButton(
                text = "Volver",
                borderColor = ButtonDarkGray,
                onClick = onBack
            )
        }
    }
}
