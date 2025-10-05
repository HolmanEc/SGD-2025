package com.holman.sgd.resources

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.holman.sgd.resources.components.getColorsCardsInicio
import com.holman.sgd.ui.theme.*

/* ---------------- VARIABLE GLOBAL ---------------- */
const val INSUMOS_COUNT = 5

/* ---------------- PANTALLA PRINCIPAL ---------------- */
@Composable
fun Calificaciones(navController: NavHostController) {
    var nominas by remember { mutableStateOf<List<NominaResumen>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedNomina by remember { mutableStateOf<NominaResumen?>(null) }
    var selectedNominaColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(Unit) {
        cargarNominasDesdeFirestore(
            onSuccess = { lista ->
                nominas = lista
                isLoading = false
            },
            onError = { msg ->
                error = msg
                isLoading = false
            }
        )
    }

    if (selectedNomina != null) {
        ScreenNominaDetalleCalificaciones(
            nomina = selectedNomina!!,
            headerColor = selectedNominaColor ?: EncabezadoEnDetalleNominas,
            onBack = {
                selectedNomina = null
                selectedNominaColor = null
            }
        )
    } else {
        Box( // 👈 Para superponer el overlay
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDefault)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            // Antes: CircularProgressIndicator()
                            // Ahora dejamos el espacio, el overlay cubre la pantalla
                            Spacer(Modifier.size(1.dp))
                        }
                        error != null -> Text(
                            "❌ Error: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                        nominas.isEmpty() -> Text(
                            "📋 No hay nóminas guardadas.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📊 Nóminas para Calificaciones",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    textAlign = TextAlign.Center
                                )

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    itemsIndexed(nominas) { index, nomina ->
                                        NominaCard(
                                            nomina = nomina,
                                            index = index,
                                            onClick = { colorSeleccionado: Color ->
                                                selectedNomina = nomina
                                                selectedNominaColor = colorSeleccionado
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                CustomButton(
                    text = "Volver",
                    borderColor = ButtonDarkGray,
                    onClick = { navController.popBackStack() }
                )
            }

            // 👇 Tu overlay de puntos durante carga de nóminas
            LoadingDotsOverlay(isLoading = isLoading)
        }
    }
}


////
///
/* ---------------- DATOS ---------------- */
data class EstudianteCalificacion(
    val idUnico: String,
    val numero: Int,
    val nombre: String,
    val notas: MutableList<Double?> // editable
)

/* ---------------- EXTENSIÓN PARA BORDES ---------------- */
fun Modifier.cellBorder(
    indexRow: Int,
    indexCol: Int,
    totalRows: Int,
    totalCols: Int,
    drawOuterTop: Boolean = true,
    drawOuterLeft: Boolean = true,
    drawOuterRight: Boolean = true,
    drawOuterBottom: Boolean = true
): Modifier {
    return this.then(
        Modifier.drawBehind {
            val strokeWidth = 1.dp.toPx()
            val color = Color.Black
            if (indexRow < totalRows - 1) {
                drawLine(
                    color,
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    strokeWidth
                )
            }
            if (indexCol < totalCols - 1) {
                drawLine(
                    color,
                    Offset(size.width, 0f),
                    Offset(size.width, size.height),
                    strokeWidth
                )
            }
            if (indexRow == 0 && drawOuterTop) {
                drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth)
            }
            if (indexCol == 0 && drawOuterLeft) {
                drawLine(color, Offset(0f, 0f), Offset(0f, size.height), strokeWidth)
            }
            if (indexCol == totalCols - 1 && drawOuterRight) {
                drawLine(
                    color,
                    Offset(size.width, 0f),
                    Offset(size.width, size.height),
                    strokeWidth
                )
            }
            if (indexRow == totalRows - 1 && drawOuterBottom) {
                drawLine(
                    color,
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    strokeWidth
                )
            }
        }
    )
}

/* ---------------- CONFIGURACIÓN TABLA ---------------- */
data class ConfigTabla(
    val colWidthNotas: Int = 90,
    val colWidthId: Int = 35,
    val colWidthNombre: Int = 200,
    val rowHeight: Int = 42
)

data class ConfigTablaColores(
    val borde: Color = BordeTablaGray,
    val encabezadoPrincipal: Color = FondoEncabezadoPrincipal,
    val encabezadoSecundario: Color = FondoEncabezadoSecundario,
    val encabezadoSumativa: Color = FondoEncabezadoSumativa,
    val encabezadoFinales: Color = FondoEncabezadoFinales,
    val textoEncabezado: Color = TextoEncabezado,
    val textoEncabezadoSecundario: Color = TextoEncabezado,
    val notasPar: Color = FondoFilaPar,
    val notasImpar: Color = FondoFilaImpar,
    val aprobado: Color = TextoAprobado,
    val reprobado: Color = TextoReprobado
)

/* ---------------- TABLA ---------------- */
@Composable
fun TablaCalificaciones(
    estudiantes: List<EstudianteCalificacion>,
    nominaId: String,
    onRefresh: () -> Unit,
    config: ConfigTabla = ConfigTabla(),
    colores: ConfigTablaColores = ConfigTablaColores()
) {
    val scrollStateX = rememberScrollState()
    var editingCell by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var editingValue by remember { mutableStateOf(TextFieldValue("")) }

    val headersGrupo1 = listOf("ID", "ESTUDIANTE")
    val headersFormativa = (1..INSUMOS_COUNT).map { "ACTIVIDAD $it" }
    val headersSumativa = listOf("PROYECTO", "EVALUACION", "REFUERZO", "MEJORA")
    val headersFinales = listOf(
        "EV TRIMESTRAL",
        "EV FORMATIVA",
        "EV SUMATIVA",
        "PROMEDIO",
        "CUALITATIVO A",
        "CUALITATIVO B"
    )
    val totalCols =
        headersGrupo1.size + headersFormativa.size + headersSumativa.size + headersFinales.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = BordeTablaGray,
                shape = RoundedCornerShape(16.dp)
            )
    )
    {
        Column {
            /* -------- ENCABEZADOS MULTINIVEL -------- */
            Row {
                Column {
                    Box(
                        modifier = Modifier
                            .width((config.colWidthId + config.colWidthNombre).dp)
                            .height(config.rowHeight.dp)
                            .background(colores.encabezadoPrincipal)
                            .cellBorder(0, 0, 2, totalCols, drawOuterBottom = false),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "DATOS PERSONALES",
                            color = colores.textoEncabezado,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Row {
                        headersGrupo1.forEachIndexed { i, h ->
                            Box(
                                modifier = Modifier
                                    .width(if (h == "ID") config.colWidthId.dp else config.colWidthNombre.dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.encabezadoSecundario)
                                    .cellBorder(1, i, 2, totalCols, drawOuterBottom = true),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    h,
                                    color = colores.textoEncabezadoSecundario,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.horizontalScroll(scrollStateX)) {
                    Column {
                        Row {
                            Box(
                                modifier = Modifier
                                    .width((config.colWidthNotas * headersFormativa.size).dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.encabezadoPrincipal)
                                    .cellBorder(
                                        0,
                                        headersGrupo1.size,
                                        2,
                                        totalCols,
                                        drawOuterBottom = false
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "EVALUACIÓN FORMATIVA (70%)",
                                    color = colores.textoEncabezado,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width((config.colWidthNotas * headersSumativa.size).dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.encabezadoSumativa)
                                    .cellBorder(
                                        0,
                                        headersGrupo1.size + headersFormativa.size,
                                        2,
                                        totalCols,
                                        drawOuterBottom = false
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "EVALUACIÓN SUMATIVA (30%)",
                                    color = colores.textoEncabezado,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width((config.colWidthNotas * headersFinales.size).dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.encabezadoFinales)
                                    .cellBorder(
                                        0,
                                        headersGrupo1.size + headersFormativa.size + headersSumativa.size,
                                        2,
                                        totalCols,
                                        drawOuterBottom = false
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "PROMEDIOS FINALES",
                                    color = colores.textoEncabezado,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Row {
                            (headersFormativa + headersSumativa + headersFinales).forEachIndexed { j, h ->
                                Box(
                                    modifier = Modifier
                                        .width(config.colWidthNotas.dp)
                                        .height(config.rowHeight.dp)
                                        .background(colores.encabezadoSecundario)
                                        .cellBorder(
                                            1,
                                            headersGrupo1.size + j,
                                            2,
                                            totalCols,
                                            drawOuterBottom = true
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        h,
                                        color = colores.textoEncabezadoSecundario,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            /* -------- CUERPO TABLA -------- */
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(estudiantes) { index, est ->
                    val totalRows = estudiantes.size
                    Row {
                        // ID
                        Box(
                            modifier = Modifier
                                .width(config.colWidthId.dp)
                                .height(config.rowHeight.dp)
                                .background(colores.encabezadoSecundario)
                                .cellBorder(
                                    index,
                                    0,
                                    totalRows,
                                    totalCols,
                                    drawOuterTop = index != 0
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                est.numero.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colores.textoEncabezadoSecundario
                            )
                        }
                        // Nombre
                        Box(
                            modifier = Modifier
                                .width(config.colWidthNombre.dp)
                                .height(config.rowHeight.dp)
                                .background(colores.encabezadoSecundario)
                                .cellBorder(
                                    index,
                                    1,
                                    totalRows,
                                    totalCols,
                                    drawOuterTop = index != 0
                                ),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                est.nombre,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 4.dp),
                                color = colores.textoEncabezadoSecundario
                            )
                        }
                        // Notas
                        Row(modifier = Modifier.horizontalScroll(scrollStateX)) {
                            est.notas.forEachIndexed { j, nota ->
                                val colIndex = headersGrupo1.size + j
                                Box(
                                    modifier = Modifier
                                        .width(config.colWidthNotas.dp)
                                        .height(config.rowHeight.dp)
                                        .background(if (index % 2 == 0) colores.notasPar else colores.notasImpar)
                                        .cellBorder(
                                            index,
                                            colIndex,
                                            totalRows,
                                            totalCols,
                                            drawOuterTop = index != 0
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    /////////////
                                    if (editingCell == est.idUnico to j) {
                                        BasicTextField(
                                            value = editingValue.text,
                                            onValueChange = { newValue ->
                                                editingValue = TextFieldValue(newValue)
                                                val num = newValue.toDoubleOrNull()
                                                if (num != null && num in 1.0..10.0) {
                                                    est.notas[j] = num
                                                } else if (newValue.isBlank()) {
                                                    est.notas[j] = null
                                                }
                                            },
                                            modifier = Modifier
                                                .width(config.colWidthNotas.dp)
                                                .wrapContentHeight(align = Alignment.CenterVertically),
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                color = Color.Black
                                            ),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                                            keyboardActions = KeyboardActions(
                                                onNext = {
                                                    // 👉 Busca la siguiente fila en la misma columna
                                                    val currentIndex =
                                                        estudiantes.indexOfFirst { it.idUnico == est.idUnico }
                                                    val nextIndex = currentIndex + 1
                                                    if (nextIndex < estudiantes.size) {
                                                        val siguienteEst = estudiantes[nextIndex]
                                                        editingCell = siguienteEst.idUnico to j
                                                        editingValue = TextFieldValue(
                                                            siguienteEst.notas[j]?.toString() ?: ""
                                                        )
                                                    } else {
                                                        // 👉 Si no hay más filas, salir de edición
                                                        editingCell = null
                                                    }
                                                }
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = nota?.let { String.format("%.2f", it) } ?: "-",
                                            color = if (nota != null && nota < 7.0) colores.reprobado else colores.aprobado,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.clickable {
                                                editingCell = est.idUnico to j
                                                editingValue =
                                                    TextFieldValue(nota?.toString() ?: "")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

//////////////
/* ---------------- DETALLE ---------------- */




//////
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ScreenNominaDetalleCalificaciones(
    nomina: NominaResumen,
    headerColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var estudiantes by remember { mutableStateOf<List<EstudianteCalificacion>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) } // controla overlay durante guardado
    val isBusy by remember { derivedStateOf { isLoading || isSaving } }

    fun refresh(fromSave: Boolean) {
        if (!fromSave) isLoading = true
        cargarCalificacionesDesdeFirestore(
            nominaId = nomina.id,
            onSuccess = {
                estudiantes = it
                if (fromSave) {
                    isSaving = false
                    mensajealert(context, "✅ Calificaciones guardadas.")
                } else {
                    isLoading = false
                }
            },
            onError = { err ->
                estudiantes = emptyList()
                if (fromSave) {
                    isSaving = false
                    mensajealert(context, "❌ Error al recargar: $err")
                } else {
                    isLoading = false
                    mensajealert(context, "❌ Error al cargar: $err")
                }
            }
        )
    }

    LaunchedEffect(nomina.id) { refresh(fromSave = false) }

    Scaffold(
        containerColor = BackgroundDefault,
        floatingActionButton = {
            FloatingSaveButton(
                visible = !isBusy,
                onClick = {
                    isSaving = true
                    guardarCalificacionesEnFirestore(
                        nominaId = nomina.id,
                        estudiantes = estudiantes
                    ) {
                        // recarga usando el mismo overlay
                        refresh(fromSave = true)
                    }
                },
                modifier = Modifier.offset(x = (-8).dp, y = 8.dp)
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                CustomButton(
                    text = "<<  Volver a nóminas",
                    borderColor = ButtonDarkGray,
                    onClick = onBack
                )
                Spacer(Modifier.height(8.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NominaHeaderCard(
                        nomina = nomina,
                        backgroundColor = headerColor,
                        onClick = null
                    )

                    Spacer(Modifier.height(16.dp))

                    when {
                        isLoading -> {
                            Spacer(Modifier.size(1.dp))
                        }
                        else -> {
                            TablaCalificaciones(
                                estudiantes = estudiantes.toMutableList(),
                                nominaId = nomina.id,
                                onRefresh = { refresh(fromSave = false) }
                            )
                        }
                    }
                }
            }

            if (isBusy) {
                LoadingDotsOverlay(isLoading = true)
            }
        }
    }
}


//////
/* ---------------- GUARDAR FIRESTORE ---------------- */
fun guardarCalificacionesEnFirestore(
    nominaId: String,
    estudiantes: List<EstudianteCalificacion>,
    onComplete: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val batch = db.batch()
    val refNomina = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)

    estudiantes.forEach { est ->
        val docRef = refNomina.collection("calificaciones").document(est.idUnico)
        val data = mapOf(
            "nombre" to est.nombre,
            "actividades" to est.notas.take(INSUMOS_COUNT),
            "proyecto" to est.notas.getOrNull(INSUMOS_COUNT),
            "evaluacion" to est.notas.getOrNull(INSUMOS_COUNT + 1),
            "refuerzo" to est.notas.getOrNull(INSUMOS_COUNT + 2),
            "mejora" to est.notas.getOrNull(INSUMOS_COUNT + 3),
            "updatedAt" to System.currentTimeMillis()
        )
        batch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
    }
    batch.commit().addOnSuccessListener { onComplete() }
}

/* ---------------- CARGAR FIRESTORE ---------------- */
fun cargarCalificacionesDesdeFirestore(
    nominaId: String,
    onSuccess: (List<EstudianteCalificacion>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val ref = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)
        .collection("calificaciones")

    ref.get()
        .addOnSuccessListener { snap ->
            val estudiantes = snap.documents
                .filter { it.id != "_config" }
                .map { doc ->
                    val idUnico = doc.id
                    val nombre = doc.getString("nombre") ?: "Alumno"

                    val actividades = (doc.get("actividades") as? List<Number?>)
                        ?.map { it?.toDouble() }
                        ?: List(INSUMOS_COUNT) { null }

                    val proyecto = doc.getDouble("proyecto")
                    val evaluacion = doc.getDouble("evaluacion")
                    val refuerzo = doc.getDouble("refuerzo")
                    val mejora = doc.getDouble("mejora")

                    val actividadesValidas = actividades.filterNotNull()
                    val evFormativa = if (actividadesValidas.isNotEmpty())
                        actividadesValidas.average() * 0.7 else null

                    val sumativas = listOfNotNull(proyecto, evaluacion, refuerzo, mejora)
                    val evSumativa = if (sumativas.isNotEmpty())
                        sumativas.average() * 0.3 else null

                    val evTrimestral = if (evFormativa != null && evSumativa != null)
                        evFormativa + evSumativa else null
                    val promedio = evTrimestral

                    val cualitativoA: Double? = null
                    val cualitativoB: Double? = null

                    val notas = actividades +
                            listOf(proyecto, evaluacion, refuerzo, mejora) +
                            listOf(
                                evTrimestral,
                                evFormativa,
                                evSumativa,
                                promedio,
                                cualitativoA,
                                cualitativoB
                            )

                    EstudianteCalificacion(
                        idUnico = idUnico,
                        numero = 0,
                        nombre = nombre,
                        notas = notas.toMutableList()
                    )
                }
                .sortedBy { it.nombre.lowercase() }
                .mapIndexed { idx, est -> est.copy(numero = idx + 1) }

            onSuccess(estudiantes)
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error cargando calificaciones")
        }
}
