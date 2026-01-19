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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.resources.FondoScreenDefault
import com.holman.sgd.resources.TituloGeneralScreens
import com.holman.sgd.resources.components.ContenedorPrincipal
import com.holman.sgd.resources.screens.isTablet
import kotlin.String


// Enum para controlar qué formulario se muestra
enum class NominaFormulario {
    MENU, CREAR, REVISAR
}

@Composable
fun Nominas(navController: NavHostController) {
    // ⬇️ antes: remember { mutableStateOf(...) }
    var formularioActual by rememberSaveable { mutableStateOf(NominaFormulario.MENU) }

    val context = LocalContext.current
    val datosExcel = remember { mutableStateListOf<List<String>>() }

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

///
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedBoxWithConstraintsScope")
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
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0) // evita el padding automático del Scaffold
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
                    // 🔹 Contenido desplazable
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                                        else -> ({})
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

                        Spacer(Modifier.height(8.dp))
                    }

                    // 🔹 Botón dentro del contenedor principal (parte inferior)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                            //.navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        CustomButton(
                            text = "Volver",
                            borderColor = ButtonDarkGray,
                            onClick = { navController.popBackStack() },
                        )
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
            .aspectRatio(1f)
            .shadow(
                elevation = 8.dp,                      // sombra visible libre
                shape = RoundedCornerShape(8.dp),
                clip = false
            )
            .clip(RoundedCornerShape(8.dp))           // recorta el contenido para que no se desborde
            .background(
                color = backgroundColor.copy(alpha = 0.95f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
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