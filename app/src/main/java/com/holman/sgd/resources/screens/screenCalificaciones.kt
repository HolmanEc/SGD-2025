package com.holman.sgd.resources

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.holman.sgd.resources.calificaciones.*
import com.holman.sgd.resources.calificaciones.TablaConfig.INSUMOS_COUNT
import com.holman.sgd.ui.theme.*
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Calificaciones(navController: NavHostController) {
    val context = LocalContext.current

    var nominas by remember { mutableStateOf<List<NominaResumen>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedNomina by remember { mutableStateOf<NominaResumen?>(null) }
    var selectedNominaColor by remember { mutableStateOf<Color?>(null) }

    // 🔹 El hijo nos registrará aquí su "onBackRequest" (guardar si cambió y luego volver)
    var detalleOnBackRequest by remember { mutableStateOf<(() -> Unit)?>(null) }

    // 🔹 Prefetch antes de abrir el detalle
    var isOpeningDetail by remember { mutableStateOf(false) }
    var prefetchEstudiantes by remember { mutableStateOf<List<TablaConfig.EstudianteCalificacion>?>(null) }

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

    // 🔙 Back del sistema
    androidx.activity.compose.BackHandler(enabled = true) {
        if (selectedNomina == null) {
            navController.popBackStack()
        } else {
            detalleOnBackRequest?.invoke() ?: run {
                selectedNomina = null
                selectedNominaColor = null
                prefetchEstudiantes = null
            }
        }
    }

    if (selectedNomina != null) {
        ScreenNominaDetalleCalificaciones(
            nomina = selectedNomina!!,
            headerColor = selectedNominaColor ?: EncabezadoEnDetalleNominas,
            // El hijo llama esto CUANDO YA guardó (si había cambios) y está listo para salir:
            onBack = {
                selectedNomina = null
                selectedNominaColor = null
                detalleOnBackRequest = null
                prefetchEstudiantes = null
            },
            // El hijo nos REGISTRA su "onBackRequest": guardar-si-dirty y luego onBack()
            onRegisterBackRequest = { handler -> detalleOnBackRequest = handler },
            // ✅ datos precargados para entrar “ya listo”
            initialEstudiantes = prefetchEstudiantes,
            skipInitialLoad = prefetchEstudiantes != null
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            FondoScreenDefault()

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
                        isLoading -> Spacer(Modifier.size(1.dp)) // overlay cubre
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
                                                // 🔹 PREFETCH: mantenemos la lista visible hasta cargar todo
                                                isOpeningDetail = true
                                                cargarCalificacionesDesdeFirestore(
                                                    nominaId = nomina.id,
                                                    onSuccess = { lista ->
                                                        prefetchEstudiantes = lista
                                                        selectedNomina = nomina
                                                        selectedNominaColor = colorSeleccionado
                                                        isOpeningDetail = false
                                                    },
                                                    onError = { msg ->
                                                        isOpeningDetail = false
                                                        mensajealert(context, "❌ $msg")
                                                    }
                                                )
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

            // 🔸 Overlay combinado:
            // - isLoading: carga inicial de la lista (lo que ya tenías)
            // - isOpeningDetail: prefetch al abrir detalle
            LoadingDotsOverlay(isLoading = isLoading || isOpeningDetail)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ScreenNominaDetalleCalificaciones(
    nomina: NominaResumen,
    headerColor: Color,
    onBack: () -> Unit,
    onRegisterBackRequest: ((() -> Unit) -> Unit),
    initialEstudiantes: List<TablaConfig.EstudianteCalificacion>? = null,
    skipInitialLoad: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var isLoading by remember { mutableStateOf(true) }
    var estudiantes by remember { mutableStateOf<List<TablaConfig.EstudianteCalificacion>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) } // overlay durante guardado
    val isBusy by remember { derivedStateOf { isLoading || isSaving } }

    // Baseline: snapshot inmutable de notas al cargar/recargar
    var baseline by remember { mutableStateOf<Map<String, List<Double?>>>(emptyMap()) }

    fun eqNota(a: Double?, b: Double?, eps: Double = 1e-6) =
        if (a == null && b == null) true else if (a == null || b == null) false else kotlin.math.abs(a - b) < eps

    fun notasCambiaron(actual: List<Double?>, anterior: List<Double?>): Boolean {
        val editableCols = TablaConfig.INSUMOS_COUNT + 4 // INSUMOS + PROYECTO + EVAL + REFUERZO + MEJORA
        val base = anterior.take(editableCols)
        val now  = actual.take(editableCols)
        if (base.size != now.size) return true
        return base.indices.any { i -> !eqNota(base[i], now[i]) }
    }

    fun estudiantesModificados(): List<TablaConfig.EstudianteCalificacion> =
        estudiantes.filter { est ->
            val before = baseline[est.idUnico]
            before == null || notasCambiaron(est.notas, before)
        }

    fun tieneCambiosPendientes(): Boolean = estudiantesModificados().isNotEmpty()

    fun saveIfDirty(showToast: Boolean, onDone: () -> Unit = {}) {
        val cambios = estudiantesModificados()
        if (cambios.isEmpty()) {
            if (showToast) mensajealert(context, "ℹ️ No hay cambios para guardar.")
            onDone(); return
        }
        isSaving = true
        guardarCalificacionesEnFirestore(
            nominaId = nomina.id,
            estudiantes = cambios
        ) {
            baseline = estudiantes.associate { e -> e.idUnico to e.notas.map { v -> v } }
            isSaving = false
            if (showToast) mensajealert(context, "✅  Calificaciones guardadas.")
            onDone()
        }
    }

    fun refresh(fromSave: Boolean) {
        if (!fromSave) isLoading = true
        cargarCalificacionesDesdeFirestore(
            nominaId = nomina.id,
            onSuccess = { lista ->
                estudiantes = lista
                baseline = lista.associate { e -> e.idUnico to e.notas.map { v -> v } }
                if (fromSave) {
                    isSaving = false
                    mensajealert(context, "✅  Calificaciones guardadas.")
                } else {
                    isLoading = false
                }
            },
            onError = { err ->
                estudiantes = emptyList()
                if (fromSave) {
                    isSaving = false
                    mensajealert(context, "❌  Error al recargar: $err")
                } else {
                    isLoading = false
                    mensajealert(context, "❌  Error al cargar: $err")
                }
            }
        )
    }

    // ✅ Si llega data precargada, úsala y evita el loading inicial
    LaunchedEffect(Unit) {
        if (initialEstudiantes != null) {
            estudiantes = initialEstudiantes
            baseline = initialEstudiantes.associate { e -> e.idUnico to e.notas.map { v -> v } }
            isLoading = false
        }
    }

    // Carga inicial SOLO si no hubo prefetch
    LaunchedEffect(nomina.id) {
        if (skipInitialLoad) return@LaunchedEffect
        refresh(fromSave = false)
    }

    // Back del sistema registrado en el padre
    LaunchedEffect(nomina.id, estudiantes, baseline, isBusy) {
        onRegisterBackRequest {
            if (!isBusy && tieneCambiosPendientes()) {
                saveIfDirty(showToast = false) { onBack() }
            } else {
                onBack()
            }
        }
    }

    // Autosave best-effort al ir a background/cierre
    DisposableEffect(lifecycleOwner, estudiantes, baseline) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                val cambios = estudiantesModificados()
                if (cambios.isNotEmpty()) {
                    guardarCalificacionesEnFirestore(nominaId = nomina.id, estudiantes = cambios) {
                        // silencioso
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = BackgroundDefault,
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.offset(x = (-8).dp, y = 8.dp)
            ) {
                FloatingExportButton(
                    visible = !isBusy,
                    onClick = {}
                )
                FloatingSaveButton(
                    visible = !isBusy,
                    onClick = { saveIfDirty(showToast = true) }
                )
            }
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
                    text = "Volver a nóminas",
                    borderColor = ButtonDarkGray,
                    onClick = {
                        if (!isBusy && tieneCambiosPendientes()) {
                            saveIfDirty(showToast = false) { onBack() }
                        } else {
                            onBack()
                        }
                    }
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
                        isLoading -> Spacer(Modifier.size(1.dp))
                        else -> {
                            val colores = TablaColors.fromNomina(headerColor)
                            TablaCalificaciones(
                                estudiantes = estudiantes,
                                nominaId = nomina.id,
                                onRefresh = { refresh(fromSave = false) },
                                colores = colores
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

fun exportCalificacionesXlsxToDownloads(
    context: Context,
    nominaId: String,
    estudiantes: List<TablaConfig.EstudianteCalificacion>,
    insumosCount: Int
) {
    if (estudiantes.isEmpty()) {
        mensajealert(context, "ℹ️ No hay datos para exportar.")
        return
    }

    try {
        // ---------- 1) Construir el workbook .xlsx en memoria ----------
        val wb = XSSFWorkbook()
        val sheet = wb.createSheet("Calificaciones")

        // Estilo de encabezado
        val headerStyle = wb.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        // Headers
        val headersGrupo1 = listOf("ID", "ESTUDIANTE")
        val headersFormativa = (1..insumosCount).map { "ACTIVIDAD $it" } + "FORMATIVA"
        val headersSumativa = listOf("PROYECTO", "EXAMEN", "REFUERZO", "MEJORA", "EVALUACION")
        val headersFinales  = listOf("FORMATIVA", "SUMATIVA", "PROMEDIO", "CUALIT. A", "CUALIT. B")
        val headers = headersGrupo1 + headersFormativa + headersSumativa + headersFinales

        var rowIdx = 0
        var row = sheet.createRow(rowIdx++)
        headers.forEachIndexed { i, h ->
            val cell = row.createCell(i)
            cell.setCellValue(h)
            cell.cellStyle = headerStyle
        }

        // Filas de datos (orden por número/ID)
        estudiantes.sortedBy { it.numero }.forEach { est ->
            row = sheet.createRow(rowIdx++)
            var col = 0

            val notas = est.notas

            // Formativa
            val actividades = (0 until insumosCount).map { idx -> notas.getOrNull(idx) }
            val promForm = TablaCalculos.promedioActividades(notas, insumosCount)

            // Sumativa editables
            val proyecto = notas.getOrNull(insumosCount + 0)
            val examen   = notas.getOrNull(insumosCount + 1)
            val refuerzo = notas.getOrNull(insumosCount + 2)
            val mejora   = notas.getOrNull(insumosCount + 3)

            // Sumativa calculada
            val evaMejorada = TablaCalculos.evaluacionMejorada(notas, insumosCount)

            // Derivados finales (incluye cualitativos)
            val der = TablaCalculos.calcularDerivados(notas, insumosCount)

            fun putStr(v: String?) { row.createCell(col++).setCellValue(v ?: "") }
            fun putNumAsString(n: Double?) { row.createCell(col++).setCellValue(TablaCalculos.formatNota(n)) }

            // Datos personales
            putStr(est.numero.toString())
            putStr(est.nombre)

            // Formativa: actividades + promedio formativa
            actividades.forEach { putNumAsString(it) }
            putNumAsString(promForm)

            // Sumativa: 4 editables + evaluación mejorada
            putNumAsString(proyecto)
            putNumAsString(examen)
            putNumAsString(refuerzo)
            putNumAsString(mejora)
            putNumAsString(evaMejorada)

            // Finales: form 70, sum 30, promedio, cualitativos
            putNumAsString(der.evFormativa)
            putNumAsString(der.evSumativa)
            putNumAsString(der.promedio)
            putStr(der.cualitativoA)
            putStr(der.cualitativoB)
        }

        // Auto-size columnas
        headers.indices.forEach { i -> sheet.autoSizeColumn(i) }

        // Serializar a bytes
        val bytes = wb.use { workbook ->
            val bos = ByteArrayOutputStream()
            workbook.write(bos)
            bos.toByteArray()
        }

        // ---------- 2) Guardar en "Descargas" y confirmar ----------
        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        val stamp = sdf.format(Date())
        val fileName = "Calificaciones_${nominaId}_$stamp.xlsx"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ : MediaStore (no requiere permisos especiales)
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("No se pudo crear el archivo en Descargas.")

            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)

            mensajealert(context, "📊 Excel guardado en Descargas: $fileName")
        } else {
            // Android 5–9: escribir en carpeta pública Descargas (puede requerir WRITE_EXTERNAL_STORAGE)
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { it.write(bytes) }
            mensajealert(context, "📊 Excel guardado en Descargas: ${outFile.name}")
        }
    } catch (e: Exception) {
        mensajealert(context, "❌ Error exportando Excel: ${e.localizedMessage}")
    }
}

fun guardarCalificacionesEnFirestore(
    nominaId: String,
    estudiantes: List<TablaConfig.EstudianteCalificacion>,
    onComplete: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val refNomina = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)
    val colCalificaciones = refNomina.collection("calificaciones")

    // 1) Leer _config para respetar el insumosCount real de la nómina
    colCalificaciones.document("_config").get()
        .addOnSuccessListener { cfg ->
            val insumosCount = (cfg.getLong("insumosCount") ?: INSUMOS_COUNT.toLong()).toInt()

            // 2) Guardar por idUnico (col1)
            val batch = db.batch()
            estudiantes.forEach { est ->
                val docRef = colCalificaciones.document(est.idUnico)  // ✅ idUnico inmutable
                val data = mapOf(
                    "nombre" to est.nombre,
                    "actividades" to est.notas.take(insumosCount),
                    "proyecto" to est.notas.getOrNull(insumosCount),
                    "evaluacion" to est.notas.getOrNull(insumosCount + 1),
                    "refuerzo" to est.notas.getOrNull(insumosCount + 2),
                    "mejora" to est.notas.getOrNull(insumosCount + 3),
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
            }
            batch.commit().addOnSuccessListener { onComplete() }
                .addOnFailureListener { onComplete() } // evitamos colgar la UI
        }
        .addOnFailureListener {
            // Si no hay _config, usamos el valor por defecto y guardamos igual
            val insumosCount = INSUMOS_COUNT
            val batch = db.batch()
            estudiantes.forEach { est ->
                val docRef = colCalificaciones.document(est.idUnico)
                val data = mapOf(
                    "nombre" to est.nombre,
                    "actividades" to est.notas.take(insumosCount),
                    "proyecto" to est.notas.getOrNull(insumosCount),
                    "evaluacion" to est.notas.getOrNull(insumosCount + 1),
                    "refuerzo" to est.notas.getOrNull(insumosCount + 2),
                    "mejora" to est.notas.getOrNull(insumosCount + 3),
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
            }
            batch.commit().addOnCompleteListener { onComplete() }
        }
}

fun cargarCalificacionesDesdeFirestore(
    nominaId: String,
    onSuccess: (List<TablaConfig.EstudianteCalificacion>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val refNomina = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)
    val colCalificaciones = refNomina.collection("calificaciones")

    // 1) Leer la TABLA para obtener el roster (col1..col4)
    refNomina.get()
        .addOnSuccessListener { nominaDoc ->
            if (!nominaDoc.exists()) {
                onError("Nómina no encontrada"); return@addOnSuccessListener
            }

            val tabla = nominaDoc.get("tabla") as? List<Map<String, Any?>> ?: emptyList()
            val filas = tabla.drop(1) // saltar encabezado
            // Roster: idUnico (col1), nro (col2), cedula (col3), nombre (col4)
            data class Roster(val id: String, val nro: Int, val ced: String, val nom: String)
            val roster = filas.mapIndexed { idx, fila ->
                val id    = (fila["col1"] as? String).orEmpty().trim()
                val nroS  = (fila["col2"] as? String).orEmpty().trim()
                val nro   = nroS.toIntOrNull() ?: (idx + 1)
                val ced   = (fila["col3"] as? String).orEmpty().trim()
                val nom   = (fila["col4"] as? String).orEmpty().trim().ifEmpty { "Alumno" }
                Roster(id = id, nro = nro, ced = ced, nom = nom)
            }

            // Si la tabla está vacía devolvemos lista vacía
            if (roster.isEmpty()) { onSuccess(emptyList()); return@addOnSuccessListener }

            // 2) Leer _config para conocer insumosCount
            colCalificaciones.document("_config").get()
                .addOnSuccessListener { cfg ->
                    val insumosCount = (cfg.getLong("insumosCount") ?: INSUMOS_COUNT.toLong()).toInt()

                    // 3) Leer calificaciones existentes
                    colCalificaciones.get()
                        .addOnSuccessListener { snap ->
                            // Índices para fallback (docs antiguos)
                            val porId = mutableMapOf<String, Map<String, Any?>>()
                            val porCed = mutableMapOf<String, Map<String, Any?>>()
                            val porNom = mutableMapOf<String, Map<String, Any?>>()

                            snap.documents.forEach { doc ->
                                if (doc.id == "_config") return@forEach
                                val data = doc.data ?: return@forEach
                                porId[doc.id] = data
                                (data["cedula"] as? String)?.trim()?.uppercase()?.let { if (it.isNotEmpty()) porCed[it] = data }
                                (data["nombre"] as? String)?.trim()?.uppercase()?.let { if (it.isNotEmpty()) porNom[it] = data }
                            }

                            // 4) Construir lista final en orden por Nro (col2)
                            val lista = roster.sortedBy { it.nro }.map { r ->
                                // Buscar primero por idUnico (col1), luego por cédula, luego por nombre
                                val data = porId[r.id]
                                    ?: (if (r.ced.isNotBlank()) porCed[r.ced.uppercase()] else null)
                                    ?: porNom[r.nom.uppercase()]

                                // Actividades (formativas)
                                val actividades: List<Double?> =
                                    (data?.get("actividades") as? List<*>)
                                        ?.map { (it as? Number)?.toDouble() }
                                        ?.let { list ->
                                            // Normalizar a insumosCount
                                            if (list.size >= insumosCount) list.take(insumosCount)
                                            else list + List(insumosCount - list.size) { null }
                                        }
                                        ?: List(insumosCount) { null }

                                // Sumativas (4)
                                val proyecto   = (data?.get("proyecto") as? Number)?.toDouble()
                                val evaluacion = (data?.get("evaluacion") as? Number)?.toDouble()
                                val refuerzo   = (data?.get("refuerzo") as? Number)?.toDouble()
                                val mejora     = (data?.get("mejora") as? Number)?.toDouble()

                                // Cálculos derivados (misma lógica que tenías)
                                val actValidas = actividades.filterNotNull()
                                val evFormativa = if (actValidas.isNotEmpty()) actValidas.average() * 0.7 else null
                                val sumativas = listOfNotNull(proyecto, evaluacion, refuerzo, mejora)
                                val evSumativa = if (sumativas.isNotEmpty()) sumativas.average() * 0.3 else null
                                val evTrimestral = if (evFormativa != null && evSumativa != null) evFormativa + evSumativa else null
                                val promedio = evTrimestral
                                val cualitativoA: Double? = null
                                val cualitativoB: Double? = null

                                val notas = actividades +
                                        listOf(proyecto, evaluacion, refuerzo, mejora) +
                                        listOf(evTrimestral, evFormativa, evSumativa, promedio, cualitativoA, cualitativoB)

                                TablaConfig.EstudianteCalificacion(
                                    idUnico = r.id,     // ✅ idUnico (col1)
                                    numero  = r.nro,
                                    nombre  = r.nom,
                                    notas   = notas.toMutableList()
                                )
                            }

                            onSuccess(lista)
                        }
                        .addOnFailureListener { e ->
                            onError(e.localizedMessage ?: "Error leyendo calificaciones")
                        }
                }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Error leyendo _config")
                }
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error leyendo nómina")
        }
}
