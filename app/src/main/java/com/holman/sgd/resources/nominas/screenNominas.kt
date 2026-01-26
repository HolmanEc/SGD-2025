package com.holman.sgd.resources.nominas

import android.annotation.SuppressLint
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.holman.sgd.R
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.resources.DefaultCard
import com.holman.sgd.resources.FondoScreenDefault
import com.holman.sgd.resources.TituloGeneralScreens
import com.holman.sgd.resources.components.ContenedorPrincipal
import com.holman.sgd.resources.components.Transparencia
import com.holman.sgd.resources.components.getCardsNominasMenu
import com.holman.sgd.resources.screens.isTablet
import com.holman.sgd.ui.theme.*

// ============================================================================
// Enum para controlar qué formulario se muestra
// ============================================================================
enum class NominaFormulario {
    MENU, CREAR, REVISAR
}

// ============================================================================
// Screen principal: Nominas
// - Maneja estado de qué vista mostrar (MENU / CREAR / REVISAR)
// - BackHandler: vuelve al menú o sale del módulo
// - Gestiona lista datosExcel (se mantiene la lógica)
// ============================================================================
@Composable
fun Nominas(navController: NavHostController) {
    var formularioActual by rememberSaveable { mutableStateOf(NominaFormulario.MENU) }

    val context = LocalContext.current
    val datosExcel = remember { mutableStateListOf<List<String>>() }

    // Back del sistema: misma lógica original
    NominasBackHandler(
        navController = navController,
        formularioActual = formularioActual,
        onGoMenu = {
            formularioActual = NominaFormulario.MENU
            datosExcel.clear()
        }
    )

    // Router de vistas: misma lógica
    NominasRouter(
        formularioActual = formularioActual,
        navController = navController,
        datosExcel = datosExcel,
        onFormularioSeleccionado = { formularioActual = it },
        onVolverMenu = {
            formularioActual = NominaFormulario.MENU
            datosExcel.clear()
        },
        onCargarArchivo = { uri ->
            datosExcel.clear()
            datosExcel.addAll(procesarArchivoExcel(context, uri))
        }
    )
}

// ============================================================================
// Router: decide qué composable mostrar según el formularioActual
// ============================================================================
@Composable
private fun NominasRouter(
    formularioActual: NominaFormulario,
    navController: NavHostController,
    datosExcel: MutableList<List<String>>,
    onFormularioSeleccionado: (NominaFormulario) -> Unit,
    onVolverMenu: () -> Unit,
    onCargarArchivo: (android.net.Uri) -> Unit
) {
    when (formularioActual) {
        NominaFormulario.MENU -> MenuNominas(
            navController = navController,
            onFormularioSeleccionado = onFormularioSeleccionado
        )

        NominaFormulario.CREAR -> {
            CrearNomina(
                onBack = { onVolverMenu() },
                onCargarArchivo = { uri -> onCargarArchivo(uri) },
                onSuccessGuardar = { datosExcel.clear() },
                datos = datosExcel
            )
        }

        NominaFormulario.REVISAR -> revisarNomina {
            onVolverMenu()
        }
    }
}

// ============================================================================
// BackHandler centralizado (misma lógica exacta)
// ============================================================================
@Composable
private fun NominasBackHandler(
    navController: NavHostController,
    formularioActual: NominaFormulario,
    onGoMenu: () -> Unit
) {
    BackHandler(enabled = true) {
        when (formularioActual) {
            NominaFormulario.MENU -> navController.popBackStack()
            else -> onGoMenu()
        }
    }
}

// ============================================================================
// MENU NOMINAS
// - Muestra intro + cards (Crear / Revisar)
// - Cards por fila según ancho
// - Botón Volver abajo
// ============================================================================
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedBoxWithConstraintsScope")
@Composable
fun MenuNominas(
    navController: NavHostController,
    onFormularioSeleccionado: (NominaFormulario) -> Unit
) {
    val cards = remember { getCardsNominasMenu() }

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
                val cardsPerRow = calculateCardsPerRowNominas(maxWidth)

                Column(modifier = Modifier.fillMaxSize()) {
                    // Contenido scroll
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NominasHeader()

                        // Grid de cards
                        NominasMenuGrid(
                            cards = cards,
                            cardsPerRow = cardsPerRow,
                            onFormularioSeleccionado = onFormularioSeleccionado
                        )

                        Spacer(Modifier.height(8.dp))
                    }

                    // Botón inferior
                    NominasFooterBackButton(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// ============================================================================
// Header del menú (título + descripción)
// ============================================================================
@Composable
private fun NominasHeader() {
    TituloGeneralScreens(texto = "Gestión de nóminas")
    Spacer(modifier = Modifier.width(8.dp))

    Text(
        text = "Aquí puedes crear o revisar las nóminas guardadas en el sistema.\n" +
                "Utiliza las opciones para administrar la información de nóminas de manera rápida y sencilla.",
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = TextDefaultBlack,
        modifier = Modifier.padding(bottom = 30.dp),
        textAlign = TextAlign.Center
    )
}

// ============================================================================
// Grid dinámico del menú (misma lógica de filas)
// ============================================================================
@Composable
private fun NominasMenuGrid(
    cards: List<Triple<String, Int, String>>,
    cardsPerRow: Int,
    onFormularioSeleccionado: (NominaFormulario) -> Unit
) {
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
            rowItems.forEach { (title, icon, description) ->
                DefaultCard(
                    title = title,
                    iconResId = icon,
                    backgroundColor = Card1,
                    backgroundAlpha = Transparencia.DEFAULT,
                    descriptionCard = description,
                    modifier = Modifier.weight(1f),
                    onClick = { onFormularioSeleccionado(routeToFormulario(title)) }
                )
            }
        }
    }
}

// ============================================================================
// Footer: Botón volver (misma ubicación / estilo)
// ============================================================================
@Composable
private fun NominasFooterBackButton(onBack: () -> Unit) {
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

// ============================================================================
// Helpers: cálculo de cards por fila (mismo criterio que usabas)
// ============================================================================
private fun calculateCardsPerRowNominas(availableWidth: androidx.compose.ui.unit.Dp): Int {
    return if (availableWidth < 700.dp) 2 else 4
}

// ============================================================================
// Helper: mapea el título a qué formulario abre (misma lógica que tu when)
// ============================================================================
private fun routeToFormulario(title: String): NominaFormulario {
    return when (title) {
        "Crear nómina" -> NominaFormulario.CREAR
        "Revisar nómina" -> NominaFormulario.REVISAR
        else -> NominaFormulario.MENU
    }
}

// ============================================================================
// CARD (misma UI / sombras / tamaños / lógica tablet)
// ============================================================================
