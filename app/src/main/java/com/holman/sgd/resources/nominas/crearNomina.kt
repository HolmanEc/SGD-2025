package com.holman.sgd.resources.nominas

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.resources.mensajealert
import com.holman.sgd.ui.theme.BordeGris
import com.holman.sgd.ui.theme.ButtonDarkGray
import com.holman.sgd.ui.theme.FondoGris
import com.holman.sgd.ui.theme.TextoClaroLight
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.holman.sgd.ui.theme.*
import com.google.firebase.firestore.DocumentReference
import com.holman.sgd.resources.FondoScreenDefault
import com.holman.sgd.resources.LoadingDotsOverlay
import com.holman.sgd.resources.TituloGeneralScreens
import com.holman.sgd.resources.VistaPreviaTablaExcel
import com.holman.sgd.resources.components.ContenedorPrincipal
import org.apache.poi.ss.usermodel.DateUtil
import java.text.SimpleDateFormat
import java.util.UUID
import org.apache.poi.ss.usermodel.*

@Composable
fun CrearNomina(
    onBack: () -> Unit,
    onCargarArchivo: (Uri) -> Unit,
    onSuccessGuardar: () -> Unit,
    datos: List<List<String>>
) {
    val context = LocalContext.current

    var institucion by rememberSaveable { mutableStateOf("") }
    var periodo by rememberSaveable { mutableStateOf("") }
    var docente by rememberSaveable { mutableStateOf("") }
    var curso by rememberSaveable { mutableStateOf("") }
    var paralelo by rememberSaveable { mutableStateOf("") }
    var asignatura by rememberSaveable { mutableStateOf("") }
    var especialidad by rememberSaveable { mutableStateOf("") }

    var isSaving by rememberSaveable { mutableStateOf(false) }
    val isBusy by remember { derivedStateOf { isSaving } }

    var datosTabla by rememberSaveable(stateSaver = TablaExcelSaver) {
        mutableStateOf(if (datos.isNotEmpty()) datos else emptyList())
    }

    LaunchedEffect(datos.size, datos.firstOrNull()) {
        if (datos.isNotEmpty()) datosTabla = datos
    }

    BackHandler(enabled = true) { if (!isBusy) onBack() }

    val archivoLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { onCargarArchivo(it) } }

    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(color = TextDefaultBlack)),
                LocalContentColor provides TextDefaultBlack
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            FondoScreenDefault()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ContenedorPrincipal)
            ) {
                TituloGeneralScreens(texto = "Formulario para crear nómina")
                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                )
                {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    )
                    {
                        // ──────────────── INSTITUCIÓN ────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundDefault, shape = RoundedCornerShape(8.dp))
                        ) {
                            SelectorFirebase("Institución", "instituciones", institucion) { institucion = it }
                        }

                        // ──────────────── FILA DOBLE ────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Columna izquierda
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BackgroundDefault, shape = RoundedCornerShape(8.dp))
                                ) {
                                    SelectorFirebase("Docente", "docentes", docente) { docente = it }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BackgroundDefault, shape = RoundedCornerShape(8.dp))
                                ) {
                                    SelectorFirebase("Curso", "cursos", curso) { curso = it }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BackgroundDefault, shape = RoundedCornerShape(8.dp))
                                ) {
                                    SelectorFirebase("Especialidad", "especialidades", especialidad) { especialidad = it }
                                }
                            }

                            // Columna derecha
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BackgroundDefault, shape = RoundedCornerShape(8.dp))
                                ) {
                                    SelectorFirebase("Asignatura", "asignaturas", asignatura) { asignatura = it }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BackgroundDefault, shape = RoundedCornerShape(8.dp))
                                ) {
                                    SelectorFirebase("Paralelo", "paralelos", paralelo) { paralelo = it }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BackgroundDefault, shape = RoundedCornerShape(8.dp))
                                ) {
                                    SelectorFirebase("Periodo Lectivo", "periodos", periodo) { periodo = it }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 🔹 Vista previa de tabla o carga
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(BackgroundDefault)
                        .fillMaxWidth()
                )
                {
                    if (datosTabla.isEmpty() && datos.isNotEmpty()) datosTabla = datos

                    if (datosTabla.isNotEmpty())
                    {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, BordeGris, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            val encabezados = datosTabla[0]
                            val filasDatos = datosTabla.drop(1)

                            // Control de ancho de cada columna
                            val pesosColumnas = generarPesos(encabezados.size)


                            // Encabezados
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(FondoGris)
                                    .padding(6.dp)
                            ) {
                                encabezados.forEachIndexed { index, titulo ->
                                    val peso = pesosColumnas.getOrElse(index) { 0.1f }
                                    Text(
                                        text = titulo,
                                        modifier = Modifier
                                            .weight(peso)
                                            .padding(4.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextoClaroLight
                                    )
                                }
                            }

                            // Filas de datos con fondo tipo "zebra"
                            filasDatos.forEachIndexed { idx, fila ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (idx % 2 == 0) BackgroundDefault
                                            else BackgroundDefault.copy(alpha = 0.6f)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    fila.forEachIndexed { index, celda ->
                                        val peso = pesosColumnas.getOrElse(index) { 0.1f }
                                        Text(
                                            text = celda,
                                            modifier = Modifier
                                                .weight(peso)
                                                .padding(4.dp),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                        }
                    }
                    else
                    {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, BordeGris, RoundedCornerShape(8.dp))
                                .padding(16.dp)
                                .clickable(enabled = !isBusy) {
                                    archivoLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            VistaPreviaTablaExcel(modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 🔹 Botones inferiores
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                )
                {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CustomButton(
                            text = "Cargar nómina de Excel",
                            borderColor = ButtonDarkSecondary,
                            onClick = {
                                if (!isBusy) {
                                    archivoLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    )
                    {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CustomButton(
                                text = if (!isSaving) "Guardar" else "Guardando…",
                                borderColor = ButtonDarkPrimary,
                                onClick = {
                                    if (isBusy) return@CustomButton
                                    when {
                                        institucion.isBlank() || periodo.isBlank() || docente.isBlank() ||
                                                curso.isBlank() || paralelo.isBlank() ||
                                                asignatura.isBlank() || especialidad.isBlank() -> {
                                            mensajealert(context, "⚠️  Faltan campos obligatorios")
                                        }
                                        datosTabla.isEmpty() -> {
                                            mensajealert(context, "⚠️  Cargue la nómina en excel")
                                        }
                                        else -> {
                                            isSaving = true
                                            guardarNominaEnFirestore(
                                                institucion, docente, curso, paralelo,
                                                asignatura, especialidad, periodo, datosTabla,
                                                onSuccess = {
                                                    isSaving = false
                                                    mensajealert(context, "✅  Nómina guardada correctamente")
                                                    onSuccessGuardar()
                                                    institucion = ""; docente = ""; curso = ""; paralelo = ""
                                                    asignatura = ""; especialidad = ""; periodo = ""
                                                    onBack()
                                                },
                                                onDuplicate = {
                                                    isSaving = false
                                                    mensajealert(context, "⚠️  Esa nómina ya existe")
                                                },
                                                onError = { error ->
                                                    isSaving = false
                                                    mensajealert(context, "❌  Error: $error")
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CustomButton(
                                text = "Volver",
                                borderColor = ButtonDarkGray,
                                onClick = { if (!isBusy) onBack() }
                            )
                        }
                    }
                }
            }

            LoadingDotsOverlay(isLoading = isSaving)
        }
    }
}



private val TablaExcelSaver = androidx.compose.runtime.saveable.listSaver<List<List<String>>, ArrayList<String>>(
    save = { tabla -> tabla.map { ArrayList(it) } },
    restore = { guardado -> guardado.map { it.toList() } }
)

fun guardarNominaEnFirestore(
    institucion: String,
    docente: String,
    curso: String,
    paralelo: String,
    asignatura: String,
    especialidad: String,
    periodo: String,
    datos: List<List<String>>,
    onSuccess: () -> Unit,
    onDuplicate: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val rutaNominas = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")

    if (datos.isEmpty()) {
        onError("No hay datos para guardar"); return
    }

    val encabezadosExcel = datos.firstOrNull().orEmpty()
    val filasExcel = if (datos.size > 1) datos.drop(1) else emptyList()

    if (filasExcel.isEmpty()) {
        onError("La hoja está vacía (solo encabezado)"); return
    }

    // 🔹 Verificar duplicados antes de guardar
    rutaNominas
        .whereEqualTo("institucion", institucion)
        .whereEqualTo("docente", docente)
        .whereEqualTo("curso", curso)
        .whereEqualTo("paralelo", paralelo)
        .whereEqualTo("asignatura", asignatura)
        .whereEqualTo("especialidad", especialidad)
        .whereEqualTo("periodo", periodo)
        .get()
        .addOnSuccessListener { snap ->
            if (!snap.isEmpty) { onDuplicate(); return@addOnSuccessListener }

            // ────────────────────────────────────────────────
            // Estructura Firestore:
            // col1 = "ID" (generado)
            // col2 = "NRO" (valor original del Excel)
            // col3.. = demás columnas del Excel
            // ────────────────────────────────────────────────
            val totalCols = encabezadosExcel.size + 1 // +1 por la nueva col "ID"
            fun keyCol(i: Int) = "col$i"

            val encabezadoFirestore = buildMap<String, Any> {
                put("col1", "ID")  // ID generado por el sistema
                put("col2", "NRO") // ID/Número del Excel
                for (i in 1 until encabezadosExcel.size) {
                    put("col${i + 2}", encabezadosExcel[i])
                }
            }

            val filasTabla = mutableListOf<Map<String, Any>>()
            filasTabla += encabezadoFirestore

            filasExcel.forEachIndexed { index, fila ->
                val idUnico = idUnicoEstudiante()
                val obj = buildMap<String, Any> {
                    put("col1", idUnico)                              // ID generado
                    put("col2", (fila.getOrNull(0) ?: "").toString()) // NRO original
                    for (i in 1 until encabezadosExcel.size) {
                        put("col${i + 2}", (fila.getOrNull(i) ?: "").toString().trim())
                    }
                }
                filasTabla += obj
            }

            val nomina = hashMapOf(
                "institucion" to institucion,
                "docente" to docente,
                "curso" to curso,
                "paralelo" to paralelo,
                "asignatura" to asignatura,
                "especialidad" to especialidad,
                "periodo" to periodo,
                "tabla" to filasTabla,
                "timestamp" to System.currentTimeMillis()
            )

            rutaNominas.add(nomina)
                .addOnSuccessListener { docRef ->
                    docRef.update("idNomina", docRef.id)
                        .addOnFailureListener { /* no bloqueante */ }

                    val insumosCount = com.holman.sgd.resources.calificaciones.TablaConfig.INSUMOS_COUNT
                    inicializarCalificacionesPorTrimestres(
                        nominaDocRef = docRef,
                        insumosCount = insumosCount
                    ) { ok ->
                        if (ok) onSuccess()
                        else onError("Nómina creada, pero falló la inicialización de calificaciones por trimestres.")
                    }
                }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Error desconocido al guardar nómina")
                }
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error al verificar duplicados")
        }
}




///
private fun inicializarCalificacionesPorTrimestres(
    nominaDocRef: DocumentReference,
    insumosCount: Int,
    onDone: (Boolean) -> Unit
) {
    val TAG = "InitTrimestres"
    val NOMBRE_INFORME = "InformeAnual"   // ← nombre unificado del documento de informe

    nominaDocRef.get()
        .addOnSuccessListener { doc ->
            if (!doc.exists()) {
                Log.e(TAG, "❌ Nómina no existe")
                onDone(false); return@addOnSuccessListener
            }

            // Leer tabla guardada en la nómina
            val tabla: List<Map<String, Any?>> =
                (doc.get("tabla") as? List<*>)?.mapNotNull { item ->
                    (item as? Map<*, *>)?.let { raw ->
                        mapOf(
                            "col1" to (raw["col1"] as? String),
                            "col2" to (raw["col2"] as? String),
                            "col3" to (raw["col3"] as? String),
                            "col4" to (raw["col4"] as? String),
                        )
                    }
                } ?: emptyList()

            if (tabla.size <= 1) {
                Log.e(TAG, "⚠️ Tabla vacía o solo encabezado")
                onDone(false); return@addOnSuccessListener
            }

            val estudiantes = tabla.drop(1) // sin encabezado
            val secciones = listOf("PrimerTrimestre", "SegundoTrimestre", "TercerTrimestre", NOMBRE_INFORME)

            val db = nominaDocRef.firestore
            val now = System.currentTimeMillis()

            // 1) Crear documentos base de secciones + _meta (Informe SIN pesos)
            val batch0 = db.batch()
            secciones.forEach { sec ->
                val secDoc = nominaDocRef.collection("calificaciones").document(sec)

                val baseDoc: MutableMap<String, Any> = mutableMapOf(
                    "seccion" to sec,                                   // nombre de la sección
                    "tipo" to if (sec == NOMBRE_INFORME) "informe" else "trimestre",
                    "createdAt" to now,
                    "updatedAt" to now
                )

                if (sec != NOMBRE_INFORME) {
                    // Solo para trimestres
                    baseDoc["insumosCount"] = insumosCount
                    baseDoc["weights"] = mapOf("formativa" to 0.7, "sumativa" to 0.3)
                }
                batch0.set(secDoc, baseDoc)
            }
            batch0.set(
                nominaDocRef.collection("calificaciones").document("_meta"),
                mapOf("secciones" to secciones, "createdAt" to now)
            )

            batch0.commit()
                .addOnSuccessListener {
                    // 2) Insertar alumnos por sección (≤450 ops por batch)
                    data class SetOp(val ref: DocumentReference, val data: Any)
                    val ops = mutableListOf<SetOp>()

                    secciones.forEach { sec ->
                        val secDoc = nominaDocRef.collection("calificaciones").document(sec)
                        estudiantes.forEachIndexed { index, fila ->
                            val id  = (fila["col1"] as? String)?.trim().orEmpty()
                            val nro = (fila["col2"] as? String)?.trim().orEmpty()
                            val ced = (fila["col3"] as? String)?.trim().orEmpty()
                            val nom = (fila["col4"] as? String)?.trim().orEmpty()
                            if (id.isBlank()) return@forEachIndexed

                            val alumnoRef = secDoc.collection("insumos").document(id)

                            val payload: Map<String, Any?> =
                                if (sec == NOMBRE_INFORME) {
                                    // Informe anual: SOLO promedios/supletorio (sin actividades)
                                    mapOf(
                                        "seccion" to sec,
                                        "numero" to (nro.toIntOrNull() ?: (index + 1)),
                                        "cedula" to ced,
                                        "nombre" to nom,
                                        "PromedioT1" to null,
                                        "PromedioT2" to null,
                                        "PromedioT3" to null,
                                        "Supletorio" to null,
                                        "PromedioFinal" to null,
                                        "createdAt" to now,
                                        "updatedAt" to now
                                    )
                                } else {
                                    // Trimestres: con actividades y campos sumativos
                                    val actividades: List<Any?> = List(insumosCount) { null }
                                    mapOf(
                                        "seccion" to sec,
                                        "numero" to (nro.toIntOrNull() ?: (index + 1)),
                                        "cedula" to ced,
                                        "nombre" to nom,
                                        "actividades" to actividades,
                                        "proyecto" to null,
                                        "evaluacion" to null,
                                        "refuerzo" to null,
                                        "mejora" to null,
                                        "createdAt" to now,
                                        "updatedAt" to now
                                    )
                                }

                            ops += SetOp(alumnoRef, payload)
                        }
                    }

                    val MAX_OPS_PER_BATCH = 450
                    val batches = mutableListOf<List<SetOp>>()
                    var i = 0
                    while (i < ops.size) {
                        val end = minOf(i + MAX_OPS_PER_BATCH, ops.size)
                        batches += ops.subList(i, end).toList()
                        i = end
                    }

                    fun commitNextBatch(idx: Int) {
                        if (idx >= batches.size) {
                            Log.i(TAG, "✅ Secciones (3 trimestres + $NOMBRE_INFORME) creadas")
                            onDone(true); return
                        }
                        val batch = db.batch()
                        batches[idx].forEach { op -> batch.set(op.ref, op.data) }
                        batch.commit()
                            .addOnSuccessListener { commitNextBatch(idx + 1) }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "❌ Falló batch ${idx + 1}: ${e.message}", e)
                                onDone(false)
                            }
                    }

                    if (batches.isEmpty()) onDone(true) else commitNextBatch(0)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ No se pudieron crear los docs de secciones: ${e.message}", e)
                    onDone(false)
                }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "❌ Error leyendo nómina: ${e.message}", e)
            onDone(false)
        }
}


private inline fun String?.ifNullOrBlank(defaultValue: () -> String): String {
    return if (this == null || this.isBlank()) defaultValue() else this
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFirebase(
    label: String,
    coleccion: String,
    valor: String,
    onValorSeleccionado: (String) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var opciones by remember { mutableStateOf<List<String>>(emptyList()) }

    // Cargar opciones de Firestore solo una vez
    LaunchedEffect(coleccion) {
        cargarListadoFirestore(coleccion) { opciones = it }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (opciones.isEmpty()) {
                mensajealert(
                    context,
                    "⚠️ No hay $label registrados. Cree uno en configuraciones."
                )
                expanded = false
            } else {
                expanded = !expanded
            }
        }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = if (valor.isEmpty()) "" else valor,
            onValueChange = {},
            label = { Text(label, color = TextDefaultBlack) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )

        // 🎨 Colores personalizados (solo cambiados)
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = BackgroundDefault // 🔹 Fondo del menú
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDefault), // 🔹 Fondo de cada ítem
                    colors = MenuDefaults.itemColors(
                        textColor = TextDefaultBlack // 🔹 Color de texto de ítem
                    ),
                    text = {
                        Text(
                            text = opcion,
                            color = TextDefaultBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onValorSeleccionado(opcion)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun cargarListadoFirestore(
    coleccion: String,
    onSuccess: (List<String>) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("gestionAcademica")
        .document("datosGenerales")
        .collection(coleccion)
        // 🔹 Orden alfabético por campo "nombre"
        .orderBy("nombre", com.google.firebase.firestore.Query.Direction.ASCENDING)
        .get()
        .addOnSuccessListener { snap ->
            val lista = snap.documents.mapNotNull { it.getString("nombre") }
            onSuccess(lista)
        }
        .addOnFailureListener {
            onSuccess(emptyList())
        }
}



fun procesarArchivoExcel(context: Context, uri: Uri): List<List<String>> {
    val filas = mutableListOf<List<String>>()
    val formatter = DataFormatter()
    val sdf = SimpleDateFormat("yyyy-MM-dd")

    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            XSSFWorkbook(input).use { wb ->
                val sheet = wb.getSheetAt(0)

                // 1) Determinar cuántas columnas reales tiene la hoja
                var maxCols = 0
                for (r in sheet) {
                    maxCols = maxOf(maxCols, r.lastCellNum.toInt().coerceAtLeast(0))
                }

                // 2) Recorrer filas respetando celdas vacías
                for (row in sheet) {
                    val fila = MutableList(maxCols) { "" }

                    for (col in 0 until maxCols) {
                        val cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
                        val valor = when (cell?.cellType) {
                            CellType.STRING  -> cell.stringCellValue
                            CellType.NUMERIC -> {
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    sdf.format(cell.dateCellValue)
                                } else {
                                    val d = cell.numericCellValue
                                    if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
                                }
                            }
                            CellType.BOOLEAN -> cell.booleanCellValue.toString()
                            CellType.FORMULA -> formatter.formatCellValue(cell) // eval simple
                            else -> ""
                        }.trim()

                        fila[col] = valor
                    }

                    // Omitir filas totalmente vacías
                    if (fila.any { it.isNotEmpty() }) filas += fila
                }
            }
        } ?: mensajealert(context, "No se pudo abrir el archivo")
    } catch (e: Exception) {
        mensajealert(context, "Error al leer Excel: ${e.message}")
    }

    return filas
}




private fun idUnicoEstudiante(): String {
    return "std_" + UUID.randomUUID().toString().replace("-", "").take(16)
}


fun generarPesos(numCols: Int): List<Float> {
    val base = listOf(5f, 15f, 35f, 30f, 15f) // tu perfil
    return if (numCols <= base.size) base.take(numCols).map { it / 100f }
    else {
        val restante = (100f - base.sum()).coerceAtLeast(0f)
        val extra = if (numCols > base.size) restante / (numCols - base.size) else 0f
        (base + List(numCols - base.size) { extra }).map { it / 100f }
    }
}
