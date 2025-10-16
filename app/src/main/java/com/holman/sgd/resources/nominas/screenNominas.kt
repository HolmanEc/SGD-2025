package com.holman.sgd.resources.nominas

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holman.sgd.R
import com.holman.sgd.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.resources.FondoScreenDefault
import com.holman.sgd.resources.screens.isTablet
import kotlin.String


// Enum para controlar qué formulario se muestra
enum class NominaFormulario {
    MENU, CREAR, REVISAR
}

@Composable
fun Nominas(navController: NavHostController) {
    var formularioActual by remember { mutableStateOf(NominaFormulario.MENU) }
    val context = LocalContext.current

    val datosExcel = remember { mutableStateListOf<List<String>>() }

    // Intercepta “Atrás” a nivel del módulo
    BackHandler(enabled = true) {
        when (formularioActual) {
            NominaFormulario.MENU -> navController.popBackStack()
            else -> {
                formularioActual = NominaFormulario.MENU
                datosExcel.clear()
            }
        }
    }

    when (formularioActual) {
        NominaFormulario.MENU -> MenuNominas(
            navController = navController,
            onFormularioSeleccionado = { formularioActual = it }
        )
        NominaFormulario.CREAR -> {
            CrearNomina(
                onBack = {
                    formularioActual = NominaFormulario.MENU
                    datosExcel.clear()
                },
                onCargarArchivo = { uri ->
                    datosExcel.clear()
                    datosExcel.addAll(procesarArchivoExcel(context, uri))
                },
                onSuccessGuardar = { datosExcel.clear() },
                datos = datosExcel
            )
        }
        NominaFormulario.REVISAR -> revisarNomina {
            formularioActual = NominaFormulario.MENU
        }
    }
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun MenuNominas(
    navController: NavHostController,
    onFormularioSeleccionado: (NominaFormulario) -> Unit
) {
    val cards = listOf(
        Triple(
            "Crear nómina",
            R.drawable.ic_crear,
            "Genera una nueva nómina desde cero.\nAgrega y organiza estudiantes de un período."
        ),
        Triple(
            "Revisar nómina",
            R.drawable.ic_revisar,
            "Accede a nóminas registradas.\nPermite editar, borrar y actualizar estudiantes."
        ),
    )

    Scaffold(
        containerColor = Color.Transparent, // 🔹 transparente para dejar ver el fondo
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CustomButton(
                    text = "Volver",
                    borderColor = ButtonDarkGray,
                    onClick = { navController.popBackStack() },
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            FondoScreenDefault()

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val maxWidthDp = maxWidth
                val cardsPerRow = if (maxWidthDp < 700.dp) 2 else 4

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Gestión de nóminas",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Text(
                        text = "Aquí puedes crear o revisar las nóminas guardadas en el sistema.\n" +
                                "Utiliza las opciones para administrar la información de nóminas de manera rápida y sencilla.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextoOscuro,
                        modifier = Modifier.padding(bottom = 30.dp),
                        textAlign = TextAlign.Center
                    )

                    // 🔹 Filas dinámicas de cards
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
                                val onClick = when (title) {
                                    "Crear nómina" -> { { onFormularioSeleccionado(NominaFormulario.CREAR) } }
                                    "Revisar nómina" -> { { onFormularioSeleccionado(NominaFormulario.REVISAR) } }
                                    else -> { {} }
                                }

                                MenuCardNominas(
                                    title = title,
                                    iconResId = icon,
                                    backgroundColor = Card1,
                                    descriptionCard = description,
                                    modifier = Modifier.weight(1f),
                                    onClick = onClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/////////////

@Composable
fun MenuCardNominas(
    title: String,
    iconResId: Int,
    backgroundColor: Color,
    descriptionCard: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isTablet = isTablet()

    Box(
        modifier = modifier
            .aspectRatio(1f) // cuadradas
            .background(
                color = backgroundColor.copy(alpha = 0.95f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(16.dp) // menor padding para dejar espacio real
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

            // Título siempre visible
            Text(
                text = title.uppercase(),
                color = TextDefaultBlack,
                fontWeight = FontWeight.Bold,
                fontSize = if (isTablet) 18.sp else 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Solo se muestra la descripción en TABLET
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