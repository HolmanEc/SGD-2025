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
import androidx.compose.material3.AlertDialogDefaults.shape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.holman.sgd.ui.theme.TextDefaultBlack
import com.holman.sgd.ui.theme.TextoReprobado
import kotlin.math.roundToInt

/* ---------------------- 1) Config ---------------------- */
object TablaConfig {
    const val INSUMOS_COUNT = 10

    // --- Evita errores si la lista de notas es más corta de lo esperado ---
    fun padNotas(notas: MutableList<Double?>, size: Int) {
        while (notas.size < size) notas.add(null)
    }

    data class ConfigTabla(
        val colWidthId: Int = 35,
        val colWidthNombre: Int = 200,
        val colWidthEvForm: Int = 90,
        val colWidthEvSum: Int = 80,
        val colWidthProm: Int = 80,
        val rowHeight: Int = 42
    )


    data class EstudianteCalificacion(
        val idUnico: String,
        val numero: Int,
        val nombre: String,
        val notas: MutableList<Double?>
    )
}

/* ---------------------- 2) Colores ---------------------- */
object TablaColors {
    @Composable
    fun TablaProvideTextColor(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(color = TextDefaultBlack))
        ) { content() }
    }

    fun colorTextoNota(nota: Double?): Color =
        if (nota != null && nota < TablaCalculos.APROBACION_MIN) TextoReprobado else TextDefaultBlack

    data class ConfigTablaColores(
        val borde: Color,                 // = color de la nómina
        val encabezadoPrincipal: Color,   // = color de la nómina (Formativa header)
        val encabezadoSecundario: Color,  // = aclarado del color de la nómina (subheaders editables)
        val encabezadoSumativa: Color,    // = color de la nómina
        val encabezadoFinales: Color,     // = color de la nómina
        val notasPar: Color,              // = aclarado suave
        val notasImpar: Color,            // = aclarado medio
        val fondoContenedor: Color,       // = aclarado para fondo

        val noEditableFormativa: Color,   // sub-encabezado + celdas de "P. FORMATIVA"
        val noEditableSumativa: Color,    // sub-encabezado + celdas de "P. SUMATIVA"
        val noEditableFinales: Color,     // sub-encabezado + celdas de "PROMEDIOS FINALES"
        val editableActivo: Color         // celda en edicion"
    )

    /** Punto ÚNICO de control de colores desde la nómina */
    fun fromNomina(
        base: Color,
        lightenSecondary: Float = 0.78f,
        lightenPar: Float = 0.93f,
        lightenImpar: Float = 0.87f,
        lightenContainer: Float = 0.78f,
        darkenBorder: Float = 0.15f,
        softenFormFactor: Float = 0.60f,
        softenSumFactor: Float = 0.60f,
        softenFinalFactor: Float = 0.40f,
        darkenActiveCell: Float = 0.00f
    ): ConfigTablaColores {
        val encabezadoPrincipal = base
        val encabezadoSumativa = base
        val encabezadoFinales = base
        val encabezadoSecundario = lighten(base, lightenSecondary)

        val notasPar = lighten(base, lightenPar)
        val notasImpar = lighten(base, lightenImpar)

        val borde = darken(base, darkenBorder)
        val fondoContenedor = lighten(base, lightenContainer)

        // 🔹 Colores “soft” para NO editables (diferentes del secundario)
        val noEditableFormativa = soften(encabezadoPrincipal, softenFormFactor)
        val noEditableSumativa  = soften(encabezadoSumativa,  softenSumFactor)
        val noEditableFinales   = soften(encabezadoFinales, softenFinalFactor)

        val editableActivo = darken(base, darkenActiveCell)

        return ConfigTablaColores(
            borde = borde,
            encabezadoPrincipal = encabezadoPrincipal,
            encabezadoSecundario = encabezadoSecundario,
            encabezadoSumativa = encabezadoSumativa,
            encabezadoFinales = encabezadoFinales,
            notasPar = notasPar,
            notasImpar = notasImpar,
            fondoContenedor = fondoContenedor,
            noEditableFormativa = noEditableFormativa,
            noEditableSumativa = noEditableSumativa,
            noEditableFinales = noEditableFinales,
            editableActivo = editableActivo
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
    private fun soften(base: Color, factor: Float): Color = lighten(base, factor) // “aclarado suave”
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

    /////////////////////////////////////////////
    // DATOS TRIMESTRALES
    const val APROBACION_MIN = 7.0
    const val PESO_FORMATIVA = 0.70
    const val PESO_PROYECTO = 0.15
    const val PESO_EVALUACION = 0.15

    data class Derivados(
        val evTrimestral: Double?,
        val evFormativa: Double?,
        val evSumativa: Double?,
        val promedio: Double?,
        val cualitativoA: String?,
        val cualitativoB: String?
    )

    fun safeParseNota(raw: String): Double? {
        val norm = raw.replace(',', '.').trim()
        val v = norm.toDoubleOrNull() ?: return null
        return v.coerceIn(0.0, 10.0)
    }

    const val PLACEHOLDER_VACIO = ""
    fun formatNota(n: Double?): String = n?.let { String.format("%.2f", it) } ?: PLACEHOLDER_VACIO

    fun promedioActividades(notas: List<Double?>, insumosCount: Int): Double? {
        val vals = notas.take(insumosCount).filterNotNull()
        return if (vals.isNotEmpty()) vals.average().coerceIn(0.0, 10.0) else null
    }

    fun evaluacionMejorada(notas: List<Double?>, insumosCount: Int): Double? {
        // Columnas relativas dentro del bloque SUMATIVA
        val examenRaw   = notas.getOrNull(insumosCount + 1)
        val refuerzoRaw = notas.getOrNull(insumosCount + 2)
        val mejoraRaw   = notas.getOrNull(insumosCount + 3)

        // 🔴 Regla: si NO hay EXAMEN, no se calcula nada
        val examen = examenRaw?.coerceIn(0.0, 10.0) ?: return null

        // Si el examen ya es ≥ 9, se toma tal cual
        if (examen >= 9.0) return examen

        // Promedio con los valores disponibles (incluye EXAMEN siempre)
        val valores = buildList {
            add(examen)
            refuerzoRaw?.let { add(it.coerceIn(0.0, 10.0)) }
            mejoraRaw?.let { add(it.coerceIn(0.0, 10.0)) }
        }

        val promedioERM = valores.average().coerceIn(0.0, 10.0)

        // Regla: resultado = min(9, max(EXAMEN, promedio(Examen,Refuerzo,Mejora)))
        val mejorado = maxOf(examen, promedioERM)
        return minOf(9.0, mejorado)
    }

    fun cualitativoA(n: Double?): String? {
        if (n == null) return null
        val key = n.roundToInt().coerceIn(1, 10)
        return when (key) {
            10 -> "A+"
            9  -> "A-"
            8  -> "B+"
            7  -> "B-"
            6  -> "C+"
            5  -> "C-"
            4  -> "D+"
            3  -> "D-"
            2  -> "E+"
            else -> "E-"
        }
    }

    fun cualitativoB(nFinal: Double?): String? {
        // nFinal puede ser null si aún no hay EV Trimestral
        val q = nFinal?.roundToInt()?.coerceIn(1, 10) ?: return null
        return when (q) {
            10, 9 -> "A"
            8, 7  -> "B"
            6, 5  -> "C"
            4, 3  -> "D"
            2, 1  -> "E"
            else  -> null
        }
    }

    fun calcularDerivados(notas: List<Double?>, insumosCount: Int): Derivados {

        val actividades = notas.take(insumosCount).filterNotNull()
        val evFormativa = if (actividades.isNotEmpty())
            actividades.average().coerceIn(0.0, 10.0) * PESO_FORMATIVA
        else null

        val proyecto = notas.getOrNull(insumosCount + 0)?.coerceIn(0.0, 10.0)
        val compProyecto = proyecto?.times(PESO_PROYECTO)

        val evMejorada = evaluacionMejorada(notas, insumosCount)
        val compEvMejorada = evMejorada?.times(PESO_EVALUACION)

        val partesSum = listOfNotNull(compProyecto, compEvMejorada)
        val evSumativa = if (partesSum.isNotEmpty()) partesSum.sum().coerceIn(0.0, 10.0) else null

        val evTrimestral = when {
            evFormativa != null && evSumativa != null ->
                (evFormativa + evSumativa).coerceIn(0.0, 10.0)
            evFormativa != null -> evFormativa
            evSumativa  != null -> evSumativa
            else -> null
        }

        val promedio = evTrimestral
        val cualitA = cualitativoA(promedio)
        val cualitB = cualitativoB(promedio)

        return Derivados(
            evTrimestral = evTrimestral,
            evFormativa  = evFormativa,
            evSumativa   = evSumativa,
            promedio     = promedio,
            cualitativoA = cualitA,
            cualitativoB = cualitB
        )
    }


    /////////////////////////////////////
    // INFORME

    // --- INFORME ---
    data class DerivadosInforme(
        val promFinal: Double?,     // promedio(T1,T2,T3)
        val promAnual: Double?,     // max(promFinal, supletorio?) o promFinal
        val cualitativoA: String?,
        val cualitativoB: String?
    )

    /**
     * Espera una lista de 4 posiciones (editables):
     * 0 -> PromT1, 1 -> PromT2, 2 -> PromT3, 3 -> Supletorio
     */
    fun calcularDerivadosInforme(notas: List<Double?>): DerivadosInforme {
        val t1 = notas.getOrNull(0)?.coerceIn(0.0, 10.0)
        val t2 = notas.getOrNull(1)?.coerceIn(0.0, 10.0)
        val t3 = notas.getOrNull(2)?.coerceIn(0.0, 10.0)
        val suple = notas.getOrNull(3)?.coerceIn(0.0, 10.0)

        val trimestres = listOfNotNull(t1, t2, t3)
        val promFinal = if (trimestres.isNotEmpty()) trimestres.average().coerceIn(0.0, 10.0) else null

        val promAnual = when {
            promFinal == null && suple == null -> null
            promFinal == null -> suple
            suple == null -> promFinal
            else -> maxOf(promFinal, suple)
        }?.coerceIn(0.0, 10.0)

        val cualitA = cualitativoA(promAnual)
        val cualitB = cualitativoB(promAnual)

        return DerivadosInforme(
            promFinal = promFinal,
            promAnual = promAnual,
            cualitativoA = cualitA,
            cualitativoB = cualitB
        )
    }




}

/* ---------------------- 5) UI Tabla --------------------- */
object TablaUI {

    //////////////////////////////////////////////////
    // TRIMESTRALES
   @Composable
   fun TablaTrimestreUI(
       estudiantes: List<TablaConfig.EstudianteCalificacion>,
       nominaId: String,
       onRefresh: () -> Unit,
       config: TablaConfig.ConfigTabla = TablaConfig.ConfigTabla(),
       colores: TablaColors.ConfigTablaColores
   ) {
       val scrollStateX = rememberScrollState()

       var editingCell by remember { mutableStateOf<Pair<String, Int>?>(null) }
       LaunchedEffect(estudiantes) {
           estudiantes.forEach { TablaConfig.padNotas(it.notas, TablaConfig.INSUMOS_COUNT + 4) }
       }


       val headersGrupo1 = listOf("ID", "ESTUDIANTE")

       val headersFormativa = (1..TablaConfig.INSUMOS_COUNT).map { "ACTIVIDAD $it" } + "FORMATIVA"

       val headersSumativa = listOf("PROYECTO", "EXAMEN", "REFUERZO", "MEJORA", "EVALUACION")

       val headersFinales = listOf("FORMATIVA", "SUMATIVA", "PROMEDIO", "CUALIT. A", "CUALIT. B")

          val formativaEditableCount = TablaConfig.INSUMOS_COUNT
       val computedFormCols = 1
       val sumativaEditableCount = 4
       val computedSumCols = 1

       val totalCols = headersGrupo1.size + headersFormativa.size + headersSumativa.size + headersFinales.size

       val editableCols = formativaEditableCount + sumativaEditableCount

       val firstEditableIndex = headersGrupo1.size
       val firstComputedFormIndex = firstEditableIndex + formativaEditableCount
       val firstSumativaEditableIndex = firstComputedFormIndex + computedFormCols
       val firstComputedSumIndex = firstSumativaEditableIndex + sumativaEditableCount
       val firstFinalIndex = firstComputedSumIndex + computedSumCols

       Box(
           modifier = Modifier
               .fillMaxWidth()
               .wrapContentHeight()
               .shadow(elevation = 4.dp, shape = shape, clip = false)
               .clip(RoundedCornerShape(8.dp))
               .border(0.dp, colores.borde, RoundedCornerShape(8.dp))
               .background(colores.fondoContenedor)
       ){
           TablaColors.TablaProvideTextColor {
               Column {
                   Encabezados(
                       headersGrupo1, headersFormativa, headersSumativa, headersFinales,
                       totalCols, config, colores, scrollStateX
                   )
                   CuerpoTabla(
                       estudiantes = estudiantes,
                       totalCols = totalCols,
                       editableCols = editableCols,
                       firstEditableIndex = firstEditableIndex,
                       firstComputedFormIndex = firstComputedFormIndex,
                       firstSumativaEditableIndex = firstSumativaEditableIndex,
                       firstComputedSumIndex = firstComputedSumIndex,
                       firstFinalIndex = firstFinalIndex,
                       config = config,
                       colores = colores,
                       scrollStateX = scrollStateX,
                       editingCell = editingCell,
                       onEditingCellChange = { editingCell = it }
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
                Box(
                    modifier = Modifier
                        .width((config.colWidthId + config.colWidthNombre).dp)
                        .height(config.rowHeight.dp)
                        .background(colores.encabezadoPrincipal)
                        .let { TablaBorders.run { it.cellBorder(0, 0, 2, totalCols, drawOuterBottom = false, borde = colores.borde) } },
                    contentAlignment = Alignment.Center
                ) { Text("DATOS PERSONALES", fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                Row {
                    headersGrupo1.forEachIndexed { i, h ->
                        val w = if (h == "ID") config.colWidthId else config.colWidthNombre
                        Box(
                            modifier = Modifier
                                .width(w.dp)
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

                        val ajustePxForm = (headersFormativa.size * 0.3f).dp
                        Box(
                            modifier = Modifier
                                .width((config.colWidthEvForm * headersFormativa.size).dp + ajustePxForm)
                                .height(config.rowHeight.dp)
                                .background(colores.encabezadoPrincipal)
                                .let { TablaBorders.run { it.cellBorder(0, headersGrupo1.size, 2, totalCols, drawOuterBottom = false, borde = colores.borde) } },
                            contentAlignment = Alignment.Center
                        ) { Text("EVALUACIÓN FORMATIVA (70%)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                        Box(
                            modifier = Modifier
                                .width((config.colWidthEvSum * headersSumativa.size).dp)
                                .height(config.rowHeight.dp)
                                .background(colores.encabezadoSumativa)
                                .let { TablaBorders.run { it.cellBorder(0, headersGrupo1.size + headersFormativa.size, 2, totalCols, drawOuterBottom = false, borde = colores.borde) } },
                            contentAlignment = Alignment.Center
                        ) { Text("EVALUACIÓN SUMATIVA (30%)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                        Box(
                            modifier = Modifier
                                .width((config.colWidthProm * headersFinales.size).dp + 1.dp)
                                .height(config.rowHeight.dp)
                                .background(colores.encabezadoFinales)
                                .let { TablaBorders.run { it.cellBorder(0, headersGrupo1.size + headersFormativa.size + headersSumativa.size, 2, totalCols, drawOuterBottom = false, borde = colores.borde) } },
                            contentAlignment = Alignment.Center
                        ) { Text("PROMEDIOS FINALES", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    }

                    Row {
                        val totalForm = headersFormativa.size
                        val totalSum  = headersSumativa.size

                        (headersFormativa + headersSumativa + headersFinales).forEachIndexed { j, h ->
                            val width = when {
                                j < totalForm -> config.colWidthEvForm
                                j < totalForm + totalSum -> config.colWidthEvSum
                                else -> config.colWidthProm
                            }

                            val bgColor =
                                if (j < totalForm) {
                                    val esPFormativa = (j == totalForm - 1)
                                    if (esPFormativa) colores.noEditableFormativa else colores.encabezadoSecundario
                                } else if (j < totalForm + totalSum) {
                                    val jSum = j - totalForm
                                    val esEvalNoEditable = (jSum == totalSum - 1)
                                    if (esEvalNoEditable) colores.noEditableSumativa else colores.encabezadoSecundario
                                } else {
                                    colores.noEditableFinales
                                }

                            Box(
                                modifier = Modifier
                                    .width(width.dp)
                                    .height(config.rowHeight.dp)
                                    .background(bgColor)
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
        firstComputedFormIndex: Int,
        firstSumativaEditableIndex: Int,
        firstComputedSumIndex: Int,
        firstFinalIndex: Int,
        config: TablaConfig.ConfigTabla,
        colores: TablaColors.ConfigTablaColores,
        scrollStateX: androidx.compose.foundation.ScrollState,
        editingCell: Pair<String, Int>?,
        onEditingCellChange: (Pair<String, Int>?) -> Unit
    ) {
        val keyboard = LocalSoftwareKeyboardController.current

        // Conteos (en sincronía con encabezados)
        val formativaEditableCount = TablaConfig.INSUMOS_COUNT        // 10 actividades
        val sumativaEditableCount = 4                                 // PROYECTO, EXAMEN, REFUERZO, MEJORA

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            itemsIndexed(estudiantes) { index, est ->
                val totalRows = estudiantes.size

                // Derivados globales (70/30 + cualitativo A)
                val derivados = TablaCalculos.calcularDerivados(est.notas, TablaConfig.INSUMOS_COUNT)

                Row {
                    /* ---- ID ---- */
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

                    /* ---- ESTUDIANTE ---- */
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

                    /* ---- NOTAS (scroll horizontal) ---- */
                    Row(modifier = Modifier.horizontalScroll(scrollStateX)) {

                        /* 1) FORMATIVA – ACTIVIDADES (editables) */
                        for (j in 0 until formativaEditableCount) {
                            val colIndex = firstEditableIndex + j
                            val isEditing = editingCell == (est.idUnico to colIndex)
                            val fondoBase = if (index % 2 == 0) colores.notasPar else colores.notasImpar
                            val fondo = if (isEditing) colores.editableActivo else fondoBase

                            val focusRequester = remember { FocusRequester() }

                            Box(
                                modifier = Modifier
                                    .width(config.colWidthEvForm.dp)
                                    .height(config.rowHeight.dp)
                                    .background(fondo)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isEditing) {
                                    key(est.idUnico to colIndex) {
                                        val initialTxt = est.notas[j]?.let { TablaCalculos.formatNota(it) } ?: ""
                                        var localValue by remember { mutableStateOf(TextFieldValue(initialTxt, TextRange(0, initialTxt.length))) }

                                        LaunchedEffect(Unit) {
                                            focusRequester.requestFocus()
                                            keyboard?.show()
                                        }

                                        BasicTextField(
                                            value = localValue,
                                            onValueChange = { newValue ->
                                                localValue = newValue
                                                est.notas[j] = TablaCalculos.safeParseNota(newValue.text)
                                            },
                                            modifier = Modifier
                                                .width(config.colWidthEvForm.dp)
                                                .wrapContentHeight(align = Alignment.CenterVertically)
                                                .focusRequester(focusRequester),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Decimal),
                                            keyboardActions = KeyboardActions(
                                                onNext = {
                                                    val currentIndex = estudiantes.indexOfFirst { it.idUnico == est.idUnico }
                                                    val nextIndex = currentIndex + 1
                                                    if (nextIndex < estudiantes.size) {
                                                        val siguienteEst = estudiantes[nextIndex]
                                                        onEditingCellChange(siguienteEst.idUnico to colIndex)
                                                    } else onEditingCellChange(null)
                                                },
                                                onDone = { onEditingCellChange(null) }
                                            )
                                        )
                                    }
                                } else {
                                    val nota = est.notas[j]
                                    Text(
                                        text = TablaCalculos.formatNota(nota),
                                        color = TablaColors.colorTextoNota(nota),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .width(config.colWidthEvForm.dp)
                                            .clickable { onEditingCellChange(est.idUnico to colIndex) }
                                    )
                                }
                            }
                        }

                        /* 2) FORMATIVA – "FORMATIVA" (no editable, promedio actividades) */
                        run {
                            val promedioForm = TablaCalculos.promedioActividades(est.notas, TablaConfig.INSUMOS_COUNT)
                            Box(
                                modifier = Modifier
                                    .width(config.colWidthEvForm.dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.noEditableFormativa)
                                    .let { TablaBorders.run { it.cellBorder(index, firstComputedFormIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    TablaCalculos.formatNota(promedioForm),
                                    color = TablaColors.colorTextoNota(promedioForm),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        /* 3) SUMATIVA – 4 columnas editables */
                        for (j in 0 until sumativaEditableCount) {
                            val colIndex = firstSumativaEditableIndex + j     // índice visual
                            val notaIndex = formativaEditableCount + j        // índice en arreglo

                            val isEditing = editingCell == (est.idUnico to colIndex)
                            val fondoBase = if (index % 2 == 0) colores.notasPar else colores.notasImpar
                            val fondo = if (isEditing) colores.editableActivo else fondoBase

                            val focusRequester = remember { FocusRequester() }

                            Box(
                                modifier = Modifier
                                    .width(config.colWidthEvSum.dp)
                                    .height(config.rowHeight.dp)
                                    .background(fondo)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isEditing) {
                                    key(est.idUnico to colIndex) {
                                        val initialTxt = est.notas.getOrNull(notaIndex)?.let { TablaCalculos.formatNota(it) } ?: ""
                                        var localValue by remember { mutableStateOf(TextFieldValue(initialTxt, TextRange(0, initialTxt.length))) }

                                        LaunchedEffect(Unit) {
                                            focusRequester.requestFocus()
                                            keyboard?.show()
                                        }

                                        BasicTextField(
                                            value = localValue,
                                            onValueChange = { newValue ->
                                                localValue = newValue
                                                est.notas[notaIndex] = TablaCalculos.safeParseNota(newValue.text)
                                            },
                                            modifier = Modifier
                                                .width(config.colWidthEvSum.dp)
                                                .wrapContentHeight(align = Alignment.CenterVertically)
                                                .focusRequester(focusRequester),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Decimal),
                                            keyboardActions = KeyboardActions(
                                                onNext = {
                                                    val currentIndex = estudiantes.indexOfFirst { it.idUnico == est.idUnico }
                                                    val nextIndex = currentIndex + 1
                                                    if (nextIndex < estudiantes.size) {
                                                        val siguienteEst = estudiantes[nextIndex]
                                                        onEditingCellChange(siguienteEst.idUnico to colIndex)
                                                    } else onEditingCellChange(null)
                                                },
                                                onDone = { onEditingCellChange(null) }
                                            )
                                        )
                                    }
                                } else {
                                    val nota = est.notas.getOrNull(notaIndex)
                                    Text(
                                        text = TablaCalculos.formatNota(nota),
                                        color = TablaColors.colorTextoNota(nota),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .width(config.colWidthEvSum.dp)
                                            .clickable { onEditingCellChange(est.idUnico to colIndex) }
                                    )
                                }
                            }
                        }

                        /* 4) SUMATIVA – "SUMATIVA" (no editable, evaluación mejorada) */
                        run {
                            val evaMejorada = TablaCalculos.evaluacionMejorada(est.notas, TablaConfig.INSUMOS_COUNT)
                            Box(
                                modifier = Modifier
                                    .width(config.colWidthEvSum.dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.noEditableSumativa)
                                    .let { TablaBorders.run { it.cellBorder(index, firstComputedSumIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    TablaCalculos.formatNota(evaMejorada),
                                    color = TablaColors.colorTextoNota(evaMejorada),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        /* 5) FINALES – 3 numéricos + Cualitativo A + Cualitativo B */
                        // 5.1) Numéricos: FORM 70, SUM 30, PROMEDIO/FINAL
                        val finalesNumericos = listOf(
                            derivados.evFormativa,
                            derivados.evSumativa,
                            derivados.promedio
                        )
                        finalesNumericos.forEachIndexed { k, valor ->
                            val colIndex = firstFinalIndex + k
                            Box(
                                modifier = Modifier
                                    .width(config.colWidthProm.dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.noEditableFinales)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    TablaCalculos.formatNota(valor),
                                    color = TablaColors.colorTextoNota(valor),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 5.2) Cualitativo A (texto)
                        run {
                            val colIndex = firstFinalIndex + 3
                            val texto = derivados.cualitativoA ?: TablaCalculos.PLACEHOLDER_VACIO
                            Box(
                                modifier = Modifier
                                    .width(config.colWidthProm.dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.noEditableFinales)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(texto, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }

                        // 5.3) Cualitativo B (reservado a futuro)
                        run {
                            val colIndex = firstFinalIndex + 4
                            val texto = derivados.cualitativoB ?: TablaCalculos.PLACEHOLDER_VACIO
                            Box(
                                modifier = Modifier
                                    .width(config.colWidthProm.dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.noEditableFinales)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(texto, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }


    /////////////////////////////////////////////////
    // INFORME
    @Composable
    fun TablaInformeUI(
        estudiantes: List<TablaConfig.EstudianteCalificacion>,
        nominaId: String,
        onRefresh: () -> Unit,
        config: TablaConfig.ConfigTabla = TablaConfig.ConfigTabla(),
        colores: TablaColors.ConfigTablaColores
    ) {
        val scrollStateX = rememberScrollState()
        var editingCell by remember { mutableStateOf<Pair<String, Int>?>(null) }

        // Para informe necesitamos 4 posiciones editables: T1, T2, T3, Suple
        LaunchedEffect(estudiantes) {
            estudiantes.forEach { TablaConfig.padNotas(it.notas, 4) }
        }

        // Bloques/encabezados
        val headersGrupo1 = listOf("ID", "ESTUDIANTE")
        val headersTrimestrales = listOf("PROM. T1", "PROM. T2", "PROM. T3") // editables
        val headersFinales = listOf(
            "PROM. FINAL",    // calculado
            "SUPLETORIO",     // editable
            "PROM. ANUAL",    // calculado
            "CUALIT. A",      // calculado
            "CUALIT. B"       // calculado
        )

        val trimestralesEditableCount = headersTrimestrales.size // 3
        val supleEditableCount = 1                                // solo supletorio
        val finalesComputedCount = 4                              // final, anual, cualitA, cualitB

        val totalCols = headersGrupo1.size + headersTrimestrales.size + headersFinales.size

        // Índices visuales
        val firstEditableIndex = headersGrupo1.size // empieza con PROM T1
        val firstFinalIndex = firstEditableIndex + trimestralesEditableCount
        val supleColIndex = firstFinalIndex + 1 // dentro de finales, el 2do es Supletorio (editable)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(elevation = 4.dp, shape = shape, clip = false)
                .clip(RoundedCornerShape(8.dp))
                .border(0.dp, colores.borde, RoundedCornerShape(8.dp))
                .background(colores.fondoContenedor)
        ) {
            TablaColors.TablaProvideTextColor {
                Column {
                    // Encabezados
                    EncabezadosInforme(
                        headersGrupo1 = headersGrupo1,
                        headersTrimestrales = headersTrimestrales,
                        headersFinales = headersFinales,
                        totalCols = totalCols,
                        config = config,
                        colores = colores,
                        scrollStateX = scrollStateX
                    )
                    // Cuerpo
                    CuerpoInforme(
                        estudiantes = estudiantes,
                        totalCols = totalCols,
                        firstEditableIndex = firstEditableIndex,
                        firstFinalIndex = firstFinalIndex,
                        supleColIndex = supleColIndex,
                        config = config,
                        colores = colores,
                        scrollStateX = scrollStateX,
                        editingCell = editingCell,
                        onEditingCellChange = { editingCell = it }
                    )
                }
            }
        }
    }
    @Composable
    private fun EncabezadosInforme(
        headersGrupo1: List<String>,
        headersTrimestrales: List<String>,
        headersFinales: List<String>,
        totalCols: Int,
        config: TablaConfig.ConfigTabla,
        colores: TablaColors.ConfigTablaColores,
        scrollStateX: androidx.compose.foundation.ScrollState
    ) {
        Row {
            Column {
                // Banda: DATOS PERSONALES
                Box(
                    modifier = Modifier
                        .width((config.colWidthId + config.colWidthNombre).dp)
                        .height(config.rowHeight.dp)
                        .background(colores.encabezadoPrincipal)
                        .let { TablaBorders.run { it.cellBorder(0, 0, 2, totalCols, drawOuterBottom = false, borde = colores.borde) } },
                    contentAlignment = Alignment.Center
                ) { Text("DATOS PERSONALES", fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                Row {
                    headersGrupo1.forEachIndexed { i, h ->
                        val w = if (h == "ID") config.colWidthId else config.colWidthNombre
                        Box(
                            modifier = Modifier
                                .width(w.dp)
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
                        // PROMEDIOS TRIMESTRALES
                        Box(
                            modifier = Modifier
                                .width((config.colWidthEvForm * headersTrimestrales.size).dp)
                                .height(config.rowHeight.dp)
                                .background(colores.encabezadoPrincipal)
                                .let { TablaBorders.run { it.cellBorder(0, headersGrupo1.size, 2, totalCols, drawOuterBottom = false, borde = colores.borde) } },
                            contentAlignment = Alignment.Center
                        ) { Text("PROMEDIOS TRIMESTRALES", fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                        // RESULTADOS FINALES
                        Box(
                            modifier = Modifier
                                .width((config.colWidthProm * headersFinales.size).dp + 1.dp)
                                .height(config.rowHeight.dp)
                                .background(colores.encabezadoFinales)
                                .let { TablaBorders.run { it.cellBorder(0, headersGrupo1.size + headersTrimestrales.size, 2, totalCols, drawOuterBottom = false, borde = colores.borde) } },
                            contentAlignment = Alignment.Center
                        ) { Text("RESULTADOS FINALES", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    }

                    Row {
                        (headersTrimestrales + headersFinales).forEachIndexed { j, h ->
                            val width = if (j < headersTrimestrales.size) config.colWidthEvForm else config.colWidthProm

                            val bgColor =
                                if (j < headersTrimestrales.size) {
                                    // T1-T3 ahora NO editables
                                    colores.noEditableFinales
                                } else {
                                    val jFin = j - headersTrimestrales.size
                                    // Dentro de finales, SOLO Supletorio (índice 1) es editable
                                    if (jFin == 1) colores.encabezadoSecundario else colores.noEditableFinales
                                }

                            Box(
                                modifier = Modifier
                                    .width(width.dp)
                                    .height(config.rowHeight.dp)
                                    .background(bgColor)
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
    private fun CuerpoInforme(
        estudiantes: List<TablaConfig.EstudianteCalificacion>,
        totalCols: Int,
        firstEditableIndex: Int,   // primera col editable = PROM T1 (pero ya NO editamos)
        firstFinalIndex: Int,      // primera col del bloque final = PROM FINAL (calc)
        supleColIndex: Int,        // índice visual de SUPLETORIO (editable)
        config: TablaConfig.ConfigTabla,
        colores: TablaColors.ConfigTablaColores,
        scrollStateX: androidx.compose.foundation.ScrollState,
        editingCell: Pair<String, Int>?,
        onEditingCellChange: (Pair<String, Int>?) -> Unit
    ) {
        val keyboard = LocalSoftwareKeyboardController.current
        val trimestralesCount = 3 // T1-T3

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            itemsIndexed(estudiantes) { index, est ->
                val totalRows = estudiantes.size

                // Aseguro 4 posiciones: T1,T2,T3,Suple
                val notas = remember(est.idUnico) { est.notas }
                if (notas.size < 4) TablaConfig.padNotas(notas, 4)

                val derivados = TablaCalculos.calcularDerivadosInforme(notas)

                Row {
                    // ID
                    Box(
                        modifier = Modifier
                            .width(config.colWidthId.dp)
                            .height(config.rowHeight.dp)
                            .background(colores.encabezadoSecundario)
                            .let { TablaBorders.run { it.cellBorder(index, 0, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                        contentAlignment = Alignment.Center
                    ) { Text(est.numero.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold) }

                    // ESTUDIANTE
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

                    // Resto columnas (scroll X)
                    Row(modifier = Modifier.horizontalScroll(scrollStateX)) {

                        // 1) PROMEDIOS TRIMESTRALES (T1-T3) → NO editables
                        for (j in 0 until trimestralesCount) {
                            val colIndex = firstEditableIndex + j
                            val fondoBase = if (index % 2 == 0) colores.notasPar else colores.notasImpar

                            Box(
                                modifier = Modifier
                                    .width(config.colWidthEvForm.dp)
                                    .height(config.rowHeight.dp)
                                    .background(fondoBase)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                val nota = notas.getOrNull(j)
                                Text(
                                    text = TablaCalculos.formatNota(nota),
                                    color = TablaColors.colorTextoNota(nota),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(config.colWidthEvForm.dp)
                                    // ❌ sin clickable: no editable
                                )
                            }
                        }

                        // 2) PROM. FINAL (calculado)
                        run {
                            val colIndex = firstFinalIndex + 0
                            val valor = derivados.promFinal
                            Box(
                                modifier = Modifier
                                    .width(config.colWidthProm.dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.noEditableFinales)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    TablaCalculos.formatNota(valor),
                                    color = TablaColors.colorTextoNota(valor),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 3) SUPLETORIO (única editable)
                        run {
                            val j = 3 // índice en arreglo de notas (Supletorio)
                            val colIndex = supleColIndex
                            val isEditing = editingCell == (est.idUnico to colIndex)
                            val fondoBase = if (index % 2 == 0) colores.notasPar else colores.notasImpar
                            val fondo = if (isEditing) colores.editableActivo else fondoBase
                            val focusRequester = remember { FocusRequester() }

                            Box(
                                modifier = Modifier
                                    .width(config.colWidthProm.dp)
                                    .height(config.rowHeight.dp)
                                    .background(fondo)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isEditing) {
                                    key(est.idUnico to colIndex) {
                                        val initialTxt = notas.getOrNull(j)?.let { TablaCalculos.formatNota(it) } ?: ""
                                        var localValue by remember { mutableStateOf(TextFieldValue(initialTxt, TextRange(0, initialTxt.length))) }

                                        LaunchedEffect(Unit) {
                                            focusRequester.requestFocus()
                                            keyboard?.show()
                                        }

                                        BasicTextField(
                                            value = localValue,
                                            onValueChange = { nv ->
                                                localValue = nv
                                                notas[j] = TablaCalculos.safeParseNota(nv.text)
                                            },
                                            modifier = Modifier
                                                .width(config.colWidthProm.dp)
                                                .wrapContentHeight(align = Alignment.CenterVertically)
                                                .focusRequester(focusRequester),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
                                            singleLine = true,
                                            // ✅ Cambiamos a NEXT para permitir navegación vertical
                                            keyboardOptions = KeyboardOptions(
                                                imeAction = ImeAction.Next,
                                                keyboardType = KeyboardType.Decimal
                                            ),
                                            keyboardActions = KeyboardActions(
                                                // Avanza a la misma columna (Suple) de la siguiente fila
                                                onNext = {
                                                    val currentIndex = estudiantes.indexOfFirst { it.idUnico == est.idUnico }
                                                    val nextIndex = currentIndex + 1
                                                    if (nextIndex < estudiantes.size) {
                                                        val siguienteEst = estudiantes[nextIndex]
                                                        onEditingCellChange(siguienteEst.idUnico to colIndex)
                                                    } else {
                                                        onEditingCellChange(null)
                                                    }
                                                },
                                                // Por si algún teclado envía Done con la tecla Enter física
                                                onDone = {
                                                    val currentIndex = estudiantes.indexOfFirst { it.idUnico == est.idUnico }
                                                    val nextIndex = currentIndex + 1
                                                    if (nextIndex < estudiantes.size) {
                                                        val siguienteEst = estudiantes[nextIndex]
                                                        onEditingCellChange(siguienteEst.idUnico to colIndex)
                                                    } else {
                                                        onEditingCellChange(null)
                                                    }
                                                }
                                            )
                                        )
                                    }
                                } else {
                                    val nota = notas.getOrNull(j)
                                    Text(
                                        text = TablaCalculos.formatNota(nota),
                                        color = TablaColors.colorTextoNota(nota),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .width(config.colWidthProm.dp)
                                            .clickable { onEditingCellChange(est.idUnico to colIndex) } // ✅ único editable
                                    )
                                }
                            }
                        }


                        // 4) PROM. ANUAL (calculado)
                        run {
                            val colIndex = firstFinalIndex + 2
                            val valor = derivados.promAnual
                            Box(
                                modifier = Modifier
                                    .width(config.colWidthProm.dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.noEditableFinales)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    TablaCalculos.formatNota(valor),
                                    color = TablaColors.colorTextoNota(valor),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 5) CUALIT. A (texto)
                        run {
                            val colIndex = firstFinalIndex + 3
                            val texto = derivados.cualitativoA ?: TablaCalculos.PLACEHOLDER_VACIO
                            Box(
                                modifier = Modifier
                                    .width(config.colWidthProm.dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.noEditableFinales)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(texto, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }

                        // 6) CUALIT. B (texto)
                        run {
                            val colIndex = firstFinalIndex + 4
                            val texto = derivados.cualitativoB ?: TablaCalculos.PLACEHOLDER_VACIO
                            Box(
                                modifier = Modifier
                                    .width(config.colWidthProm.dp)
                                    .height(config.rowHeight.dp)
                                    .background(colores.noEditableFinales)
                                    .let { TablaBorders.run { it.cellBorder(index, colIndex, totalRows, totalCols, drawOuterTop = index != 0, borde = colores.borde) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(texto, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* -------------------- 6) API pública -------------------- */
@Composable
fun TablaTrimetre(
    estudiantes: List<TablaConfig.EstudianteCalificacion>,
    nominaId: String,
    onRefresh: () -> Unit,
    config: TablaConfig.ConfigTabla = TablaConfig.ConfigTabla(),
    colores: TablaColors.ConfigTablaColores
) {
    TablaUI.TablaTrimestreUI(estudiantes, nominaId, onRefresh, config, colores)
}

@Composable
fun TablaInforme(
    estudiantes: List<TablaConfig.EstudianteCalificacion>,
    nominaId: String,
    onRefresh: () -> Unit,
    config: TablaConfig.ConfigTabla = TablaConfig.ConfigTabla(),
    colores: TablaColors.ConfigTablaColores
) {
    TablaUI.TablaInformeUI(estudiantes, nominaId, onRefresh, config, colores)
}

///////////////////////////////////////////////////////

