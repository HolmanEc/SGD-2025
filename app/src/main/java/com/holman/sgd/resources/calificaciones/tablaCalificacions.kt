package com.holman.sgd.resources.calificaciones

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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holman.sgd.ui.theme.BordeTablaGray
import com.holman.sgd.ui.theme.FondoFilaImpar
import com.holman.sgd.ui.theme.FondoFilaPar
import com.holman.sgd.ui.theme.TextDefaultBlack
import com.holman.sgd.ui.theme.TextoReprobado

/* ---------------------- 1) Config ---------------------- */
object TablaConfig {
    const val INSUMOS_COUNT = 10

    data class ConfigTabla(
        val colWidthNotas: Int = 90,
        val colWidthId: Int = 35,
        val colWidthNombre: Int = 200,
        val rowHeight: Int = 42
    )

    data class EstudianteCalificacion(
        val idUnico: String,
        val numero: Int,
        val nombre: String,
        val notas: MutableList<Double?> // editables
    )
}

/* ---------------------- 2) Colores ---------------------- */
object TablaColors {
    data class ConfigTablaColores(
        val borde: Color,                 // = color de la nómina
        val encabezadoPrincipal: Color,   // = color de la nómina
        val encabezadoSecundario: Color,  // = aclarado del color de la nómina
        val encabezadoSumativa: Color,    // = color de la nómina
        val encabezadoFinales: Color,     // = color de la nómina
        val notasPar: Color,              // = aclarado suave
        val notasImpar: Color,             // = aclarado medio
        val fondoContenedor: Color        // = aclarado para fondo
    )

    /** Punto ÚNICO de control de colores desde la nómina */
    fun fromNomina(
        base: Color,
        lightenSecondary: Float = 0.78f,
        lightenPar: Float = 0.93f,
        lightenImpar: Float = 0.87f,
        lightenContainer: Float = 0.78f
    ): ConfigTablaColores {
        val encabezadoPrincipal = base
        val encabezadoSumativa = base
        val encabezadoFinales = base
        val encabezadoSecundario = lighten(base, lightenSecondary)

        val notasPar = lighten(base, lightenPar)
        val notasImpar = lighten(base, lightenImpar)

        val borde = base
        // 👇 NUEVO
        val fondoContenedor = lighten(base, lightenContainer)

        return ConfigTablaColores(
            borde = borde,
            encabezadoPrincipal = encabezadoPrincipal,
            encabezadoSecundario = encabezadoSecundario,
            encabezadoSumativa = encabezadoSumativa,
            encabezadoFinales = encabezadoFinales,
            notasPar = notasPar,
            notasImpar = notasImpar,
            // 👇 NUEVO
            fondoContenedor = fondoContenedor
        )
    }


    /* Helpers internos */
    private fun blend(a: Color, b: Color, t: Float): Color {
        val clamped = t.coerceIn(0f, 1f)
        return Color(
            red = a.red * (1 - clamped) + b.red * clamped,
            green = a.green * (1 - clamped) + b.green * clamped,
            blue = a.blue * (1 - clamped) + b.blue * clamped,
            alpha = a.alpha
        )
    }
    private fun lighten(base: Color, amount: Float): Color = blend(base, Color.White, amount)
    @Suppress("unused")
    private fun darken(base: Color, amount: Float): Color = blend(base, Color.Black, amount)
}

/* ---------------------- 3) Bordes ---------------------- */
object TablaBorders {
    fun Modifier.cellBorder(
        indexRow: Int,
        indexCol: Int,
        totalRows: Int,
        totalCols: Int,
        drawOuterTop: Boolean = true,
        drawOuterLeft: Boolean = true,
        drawOuterRight: Boolean = true,
        drawOuterBottom: Boolean = true,
        borde: Color = BordeTablaGray
    ): Modifier = this.then(
        Modifier.drawBehind {
            val strokeWidth = 1.dp.toPx()
            if (indexRow < totalRows - 1)
                drawLine(borde, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
            if (indexCol < totalCols - 1)
                drawLine(borde, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth)
            if (indexRow == 0 && drawOuterTop)
                drawLine(borde, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth)
            if (indexCol == 0 && drawOuterLeft)
                drawLine(borde, Offset(0f, 0f), Offset(0f, size.height), strokeWidth)
            if (indexCol == totalCols - 1 && drawOuterRight)
                drawLine(borde, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth)
            if (indexRow == totalRows - 1 && drawOuterBottom)
                drawLine(borde, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
        }
    )
}

/* ---------------------- 4) Cálculos ---------------------- */
object TablaCalculos {
    data class Derivados(
        val evTrimestral: Double?,
        val evFormativa: Double?,
        val evSumativa: Double?,
        val promedio: Double?,
        val cualitativoA: Double?,
        val cualitativoB: Double?
    )

    fun safeParseNota(raw: String): Double? {
        val norm = raw.replace(',', '.').trim()
        val v = norm.toDoubleOrNull() ?: return null
        return v.coerceIn(0.0, 10.0)
    }

    fun formatNota(n: Double?): String = n?.let { String.format("%.2f", it) } ?: "—"

    fun calcularDerivados(notas: List<Double?>, insumosCount: Int): Derivados {
        val actividades = notas.take(insumosCount)
        val proyecto   = notas.getOrNull(insumosCount)
        val evaluacion = notas.getOrNull(insumosCount + 1)
        val refuerzo   = notas.getOrNull(insumosCount + 2)
        val mejora     = notas.getOrNull(insumosCount + 3)

        val actValidas = actividades.filterNotNull()
        val evFormativa = if (actValidas.isNotEmpty()) actValidas.average() * 0.70 else null

        val sumativas = listOfNotNull(proyecto, evaluacion, refuerzo, mejora)
        val evSumativa = if (sumativas.isNotEmpty()) sumativas.average() * 0.30 else null

        val evTrimestral = if (evFormativa != null && evSumativa != null) evFormativa + evSumativa else null
        val promedio = evTrimestral

        return Derivados(evTrimestral, evFormativa, evSumativa, promedio, null, null)
    }
}

/* --- 4.1) Texto global + excepción reprobados --- */
@Composable
private fun TablaProvideTextColor(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(color = TextDefaultBlack))
    ) { content() }
}

private fun colorTextoNota(nota: Double?): Color =
    if (nota != null && nota < 7.0) TextoReprobado else TextDefaultBlack

/* ---------------------- 5) UI ---------------------- */
object TablaUI {

    @Composable
    fun TablaCalificaciones(
        estudiantes: List<TablaConfig.EstudianteCalificacion>,
        nominaId: String,
        onRefresh: () -> Unit,
        config: TablaConfig.ConfigTabla = TablaConfig.ConfigTabla(),
        colores: TablaColors.ConfigTablaColores
    ) {
        val scrollStateX = rememberScrollState()

        var editingCell by remember { mutableStateOf<Pair<String, Int>?>(null) }
        var editingValue by remember { mutableStateOf(TextFieldValue("")) }

        val headersGrupo1 = listOf("ID", "ESTUDIANTE")
        val headersFormativa = (1..TablaConfig.INSUMOS_COUNT).map { "ACTIVIDAD $it" }
        val headersSumativa = listOf("PROYECTO", "EVALUACION", "REFUERZO", "MEJORA")
        val headersFinales = listOf(
            "EV TRIMESTRAL", "EV FORMATIVA", "EV SUMATIVA", "PROMEDIO", "CUALITATIVO A", "CUALITATIVO B"
        )

        val totalCols = headersGrupo1.size + headersFormativa.size + headersSumativa.size + headersFinales.size
        val editableCols = headersFormativa.size + headersSumativa.size
        val firstEditableIndex = headersGrupo1.size
        val firstFinalIndex = headersGrupo1.size + editableCols

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, colores.borde, RoundedCornerShape(16.dp))
                .background(colores.fondoContenedor)
        ){
            TablaProvideTextColor {
                Column {
                    Encabezados(
                        headersGrupo1, headersFormativa, headersSumativa, headersFinales,
                        totalCols, config, colores, scrollStateX
                    )
                    CuerpoTabla(
                        estudiantes, totalCols, editableCols, firstEditableIndex, firstFinalIndex,
                        config, colores, scrollStateX, editingCell, { editingCell = it }, editingValue, { editingValue = it }
                    )
                }
            }
        }
    }

    @Composable
    private fun Encabezados(
        headersGrupo1: List<String>,
        headersFormativa: List<String>,
        headersSumativa: List<String>,
        headersFinales: List<String>,
        totalCols: Int,
        config: TablaConfig.ConfigTabla,
        colores: TablaColors.ConfigTablaColores,
        scrollStateX: androidx.compose.foundation.ScrollState
    ) {
        Row {
            Column {
                // Encabezado principal (DATOS PERSONALES) con color de nómina
                Box(
                    modifier = Modifier
                        .width((config.colWidthId + config.colWidthNombre).dp)
                        .height(config.rowHeight.dp)
                        .background(colores.encabezadoPrincipal)
                        .let { TablaBorders.run { it.cellBorder(0, 0, 2, totalCols, drawOuterBottom = false, borde = colores.borde) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text("DATOS PERSONALES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Sub-encabezados (ID / ESTUDIANTE) con color aclarado
                Row {
                    headersGrupo1.forEachIndexed { i, h ->
                        Box(
                            modifier = Modifier
                                .width(if (h == "ID") config.colWidthId.dp else config.colWidthNombre.dp)
                                .height(config.rowHeight.dp)
                                .background(colores.encabezadoSecundario)
                                .let { TablaBorders.run { it.cellBorder(1, i, 2, totalCols, drawOuterBottom = true, borde = colores.borde) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(h, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Row(modifier = Modifier.horizontalScroll(scrollStateX)) {
                Column {
                    Row {
                        // Título Formativa (principal)
                        Box(
                            modifier = Modifier
                                .width((config.colWidthNotas * headersFormativa.size).dp)
                                .height(config.rowHeight.dp)
                                .background(colores.encabezadoPrincipal)
                                .let { TablaBorders.run { it.cellBorder(0, headersGrupo1.size, 2, totalCols, drawOuterBottom = false, borde = colores.borde) } },
                            contentAlignment = Alignment.Center
                        ) { Text("EVALUACIÓN FORMATIVA (70%)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                        // Título Sumativa (principal)
                        Box(
                            modifier = Modifier
                                .width((config.colWidthNotas * headersSumativa.size).dp)
                                .height(config.rowHeight.dp)
                                .background(colores.encabezadoSumativa)
                                .let { TablaBorders.run { it.cellBorder(0, headersGrupo1.size + headersFormativa.size, 2, totalCols, drawOuterBottom = false, borde = colores.borde) } },
                            contentAlignment = Alignment.Center
                        ) { Text("EVALUACIÓN SUMATIVA (30%)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                        // Título Finales (principal)
                        Box(
                            modifier = Modifier
                                .width((config.colWidthNotas * headersFinales.size).dp)
                                .height(config.rowHeight.dp)
                                .background(colores.encabezadoFinales)
                                .let { TablaBorders.run { it.cellBorder(0, headersGrupo1.size + headersFormativa.size + headersSumativa.size, 2, totalCols, drawOuterBottom = false, borde = colores.borde) } },
                            contentAlignment = Alignment.Center
                        ) { Text("PROMEDIOS FINALES", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    }

                    // Sub-encabezados de columnas de notas con color aclarado
                    Row {
                        (headersFormativa + headersSumativa + headersFinales).forEachIndexed { j, h ->
                            Box(
                                modifier = Modifier
                                    .width(config.colWidthNotas.dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.encabezadoSecundario)
                                    .let { TablaBorders.run { it.cellBorder(1, headersGrupo1.size + j, 2, totalCols, drawOuterBottom = true, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(h, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CuerpoTabla(
        estudiantes: List<TablaConfig.EstudianteCalificacion>,
        totalCols: Int,
        editableCols: Int,
        firstEditableIndex: Int,
        firstFinalIndex: Int,
        config: TablaConfig.ConfigTabla,
        colores: TablaColors.ConfigTablaColores,
        scrollStateX: androidx.compose.foundation.ScrollState,
        editingCell: Pair<String, Int>?,
        onEditingCellChange: (Pair<String, Int>?) -> Unit,
        editingValue: TextFieldValue,
        onEditingValueChange: (TextFieldValue) -> Unit
    ) {
        val keyboard = LocalSoftwareKeyboardController.current

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ){
            itemsIndexed(estudiantes) { index, est ->
                val totalRows = estudiantes.size

                // 👉 ¿Esta fila es la que se está editando?
                val editingThisRow = editingCell?.first == est.idUnico
                // 👉 Suscríbete a los cambios del texto SOLO en la fila que edita
                //    Esto fuerza recomposición de TODA la fila en cada tecla.
                val _recomposeTrigger = if (editingThisRow) editingValue.text else null

                // ✅ Recalcula SIEMPRE a partir de las notas actuales
                val derivados = TablaCalculos.calcularDerivados(est.notas, TablaConfig.INSUMOS_COUNT)

                Row {
                    // ID (col 1)
                    Box(
                        modifier = Modifier
                            .width(config.colWidthId.dp)
                            .height(config.rowHeight.dp)
                            .background(colores.encabezadoSecundario)
                            .let { TablaBorders.run { it.cellBorder(index, 0, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(est.numero.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Estudiante (col 2)
                    Box(
                        modifier = Modifier
                            .width(config.colWidthNombre.dp)
                            .height(config.rowHeight.dp)
                            .background(colores.encabezadoSecundario)
                            .let { TablaBorders.run { it.cellBorder(index, 1, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            est.nombre,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Notas
                    Row(modifier = Modifier.horizontalScroll(scrollStateX)) {
                        // Editables
                        (0 until editableCols).forEach { j ->
                            val colIndex = firstEditableIndex + j
                            val isEditing = (editingCell == est.idUnico to colIndex)
                            val fondo = if (index % 2 == 0) colores.notasPar else colores.notasImpar
                            val focusRequester = remember { FocusRequester() }

                            Box(
                                modifier = Modifier
                                    .width(config.colWidthNotas.dp)
                                    .height(config.rowHeight.dp)
                                    .background(fondo)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isEditing) {
                                    // Foco y teclado al entrar en edición
                                    LaunchedEffect(est.idUnico, colIndex, isEditing) {
                                        if (isEditing) {
                                            focusRequester.requestFocus()
                                            keyboard?.show()
                                        }
                                    }

                                    BasicTextField(
                                        value = editingValue,
                                        onValueChange = { newValue ->
                                            onEditingValueChange(newValue)
                                            // Actualiza el modelo (0..10, soporta coma)
                                            est.notas[j] = TablaCalculos.safeParseNota(newValue.text)
                                        },
                                        modifier = Modifier
                                            .width(config.colWidthNotas.dp)
                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                            .focusRequester(focusRequester),
                                        textStyle = LocalTextStyle.current.copy(
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        ),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            imeAction = ImeAction.Next,
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = {
                                                val currentIndex = estudiantes.indexOfFirst { it.idUnico == est.idUnico }
                                                val nextIndex = currentIndex + 1
                                                if (nextIndex < estudiantes.size) {
                                                    val siguienteEst = estudiantes[nextIndex]
                                                    onEditingCellChange(siguienteEst.idUnico to colIndex)
                                                    val nextTxt = siguienteEst.notas[j]?.let { TablaCalculos.formatNota(it) } ?: ""
                                                    onEditingValueChange(
                                                        TextFieldValue(
                                                            text = nextTxt,
                                                            selection = TextRange(0, nextTxt.length)
                                                        )
                                                    )
                                                } else onEditingCellChange(null)
                                            },
                                            onDone = { onEditingCellChange(null) }
                                        )
                                    )
                                } else {
                                    val nota = est.notas[j]
                                    Text(
                                        text = TablaCalculos.formatNota(nota),
                                        color = colorTextoNota(nota),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.clickable {
                                            // Un tap: activar edición + precargar y seleccionar todo
                                            onEditingCellChange(est.idUnico to colIndex)
                                            val txt = nota?.let { TablaCalculos.formatNota(it) } ?: ""
                                            onEditingValueChange(
                                                TextFieldValue(
                                                    text = txt,
                                                    selection = TextRange(0, txt.length)
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // Finales (readonly: 6) — ahora sí cambian en tiempo real en esta fila
                        val finales = listOf(
                            derivados.evTrimestral,
                            derivados.evFormativa,
                            derivados.evSumativa,
                            derivados.promedio,
                            derivados.cualitativoA,
                            derivados.cualitativoB
                        )

                        finales.forEachIndexed { k, valor ->
                            val colIndex = firstFinalIndex + k
                            val fondo = if (index % 2 == 0) colores.notasPar else colores.notasImpar

                            Box(
                                modifier = Modifier
                                    .width(config.colWidthNotas.dp)
                                    .height(config.rowHeight.dp)
                                    .background(fondo.copy(alpha = if (k <= 3) 1f else 0.9f))
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = TablaCalculos.formatNota(valor),
                                    color = colorTextoNota(valor),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

/* ---------------------- 6) API pública ---------------------- */
@Composable
fun TablaCalificaciones(
    estudiantes: List<TablaConfig.EstudianteCalificacion>,
    nominaId: String,
    onRefresh: () -> Unit,
    config: TablaConfig.ConfigTabla = TablaConfig.ConfigTabla(),
    colores: TablaColors.ConfigTablaColores
) {
    TablaUI.TablaCalificaciones(estudiantes, nominaId, onRefresh, config, colores)
}
