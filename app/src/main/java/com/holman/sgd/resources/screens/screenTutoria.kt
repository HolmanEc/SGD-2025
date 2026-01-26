package com.holman.sgd.resources

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.holman.sgd.R
import com.holman.sgd.resources.components.ContenedorPrincipal
import com.holman.sgd.ui.theme.*
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.BoxWithConstraints
import com.holman.sgd.resources.components.Transparencia

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedBoxWithConstraintsScope")
@Composable
fun Tutoria(
    navController: NavHostController,
    onGestionAsignatura: () -> Unit = {},
    onGestionTutoria: () -> Unit = {},
    iconAsignatura: Int = R.drawable.ic_crear,
    iconTutoria: Int = R.drawable.ic_revisar
) {
    // Lista de cards (título, icono, descripción)
    val cards = listOf(
        Triple(
            "Gestión de Asignatura",
            iconAsignatura,
            "Configura parámetros por asignatura,\nactividades y criterios de evaluación."
        ),
        Triple(
            "Gestión de Tutoría",
            iconTutoria,
            "Registra y revisa procesos de tutoría,\nacuerdos y seguimientos."
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            FondoScreenDefault()

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ContenedorPrincipal)
            ) {
                val availableWidth = maxWidth
                val cardsPerRow = if (availableWidth < 700.dp) 2 else 4

                Column(modifier = Modifier.fillMaxSize()) {

                    // Contenido desplazable
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TituloGeneralScreens(texto = "Gestión de asignatura y tutorías")
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Elige una opción para administrar los procesos de tutoría.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextDefaultBlack,
                            modifier = Modifier.padding(bottom = 24.dp),
                            textAlign = TextAlign.Center
                        )

                        // Grilla dinámica (igual a Nóminas)
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
                                    val onClick = when (title) {
                                        "Gestión de Asignatura" -> onGestionAsignatura
                                        "Gestión de Tutoría" -> onGestionTutoria
                                        else -> ({})
                                    }

                                    DefaultCard(
                                        title = title,
                                        iconResId = icon,
                                        backgroundColor = Card4,
                                        backgroundAlpha = Transparencia.DEFAULT,
                                        descriptionCard = desc,
                                        modifier = Modifier.weight(1f),
                                        onClick = onClick
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    // Botón inferior
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomButton(
                            text = "Volver",
                            borderColor = ButtonDarkGray,
                            onClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
