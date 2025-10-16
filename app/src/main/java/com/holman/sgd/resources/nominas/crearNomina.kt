package com.holman.sgd.resources.nominas

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import com.holman.sgd.resources.VistaPreviaTablaExcel
import java.util.UUID


@Composable
fun CrearNomina(
    onBack: () -> Unit,
    onCargarArchivo: (Uri) -> Unit,
    onSuccessGuardar: () -> Unit,
    datos: List<List<String>>
) {
    val context = LocalContext.current

    var institucion by remember { mutableStateOf("") }
    var periodo by remember { mutableStateOf("") }
    var docente by remember { mutableStateOf("") }
    var curso by remember { mutableStateOf("") }
    var paralelo by remember { mutableStateOf("") }
    var asignatura by remember { mutableStateOf("") }
    var especialidad by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    val isBusy by remember { derivedStateOf { isSaving } }

    BackHandler(enabled = true) {
        if (!isBusy) onBack()
    }

    val archivoLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onCargarArchivo(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FondoScreenDefault()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
        {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Formulario para Crear Nómina",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDefault, shape = RoundedCornerShape(8.dp))
            ) {
                SelectorFirebase("Institución", "instituciones", institucion) { institucion = it }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                )
                {
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

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                )
                {
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

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(BackgroundDefault)
                    .fillMaxWidth()
            ) {
                if (datos.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, BordeGris, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val encabezados = datos[0]
                        val filasDatos = datos.drop(1)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(FondoGris)
                                .padding(6.dp)
                        ) {
                            encabezados.forEachIndexed { index, titulo ->
                                val peso = when (index) { 0, 1 -> 1f else -> 3f }
                                Text(
                                    text = titulo,
                                    modifier = Modifier.weight(peso).padding(4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoClaroLight
                                )
                            }
                        }

                        filasDatos.forEach { fila ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                fila.forEachIndexed { index, celda ->
                                    val peso = when (index) { 0, 1 -> 1f else -> 3f }
                                    Text(
                                        text = celda,
                                        modifier = Modifier.weight(peso).padding(4.dp),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
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
                        VistaPreviaTablaExcel(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                ) {
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
                                    datos.isEmpty() -> {
                                        mensajealert(context, "⚠️  Cargue la nómina en excel")
                                    }
                                    else -> {
                                        isSaving = true
                                        guardarNominaEnFirestore(
                                            institucion, docente, curso, paralelo,
                                            asignatura, especialidad, periodo, datos,
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
        onError("No hay datos para guardar")
        return
    }
    val filasExcel = if (datos.size > 1) datos.drop(1) else emptyList()

    // 1) Verificar duplicados
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
            if (!snap.isEmpty) {
                onDuplicate(); return@addOnSuccessListener
            }

            // 2) TABLA con NUEVO ORDEN:
            //    col1 = ID (aleatorio), col2 = Nro, col3 = Cédula, col4 = Estudiante
            val encabezado = mapOf(
                "col1" to "ID",
                "col2" to "Nro",
                "col3" to "Cédula",
                "col4" to "Estudiante"
            )
            val filasTabla = mutableListOf<Map<String, Any>>()
            filasTabla += encabezado

            filasExcel.forEachIndexed { index, fila ->
                val nro    = (fila.getOrNull(0) ?: (index + 1).toString()).toString().trim()
                val cedula = (fila.getOrNull(1) ?: "").toString().trim()
                val nombre = (fila.getOrNull(2) ?: "").toString().trim()

                val idUnico = idUnicoEstudiante()  // ← YA NO depende de cédula/Nombre

                filasTabla += mapOf(
                    "col1" to idUnico,
                    "col2" to nro,
                    "col3" to cedula,
                    "col4" to nombre
                )
            }

            // 3) Documento de nómina (guardamos tabla con IDs aleatorios)
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

            // 4) Guardar → colocar idNomina → inicializar calificaciones usando col1 (ID)
            rutaNominas
                .add(nomina)
                .addOnSuccessListener { docRef ->
                    docRef.update("idNomina", docRef.id)
                        .addOnFailureListener { /* no bloqueante */ }

                    val insumosCount = com.holman.sgd.resources.calificaciones.TablaConfig.INSUMOS_COUNT
                    inicializarCalificacionesParaNomina(
                        nominaDocRef = docRef,
                        datos = datos,              // se mantiene la firma, pero adentro leeremos la TABLA del doc
                        insumosCount = insumosCount
                    ) { ok ->
                        if (ok) onSuccess()
                        else onError("Nómina creada, pero falló la inicialización de calificaciones.")
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

private fun inicializarCalificacionesParaNomina(
    nominaDocRef: DocumentReference,
    datos: List<List<String>>,      // se conserva por compatibilidad (no se usa para ID)
    insumosCount: Int,
    onDone: (Boolean) -> Unit
) {
    nominaDocRef.get()
        .addOnSuccessListener { doc ->
            // Cast seguro: List<*> -> Map<*, *> -> Map<String, Any?>
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

            if (tabla.size <= 1) { onDone(false); return@addOnSuccessListener }

            val cuerpo = tabla.drop(1)
            val batch = nominaDocRef.firestore.batch()

            cuerpo.forEachIndexed { index, fila ->
                val id  = (fila["col1"] as? String)?.trim().orEmpty()
                val nro = (fila["col2"] as? String)?.trim().orEmpty()
                val ced = (fila["col3"] as? String)?.trim().orEmpty()
                val nom = (fila["col4"] as? String)?.trim().orEmpty()

                if (id.isBlank()) return@forEachIndexed

                val docRef = nominaDocRef.collection("calificaciones").document(id)
                val data = mapOf(
                    "numero" to (nro.toIntOrNull() ?: (index + 1)),
                    "cedula" to ced,
                    "nombre" to nom,
                    "actividades" to List(insumosCount) { null },
                    "proyecto" to null,
                    "evaluacion" to null,
                    "refuerzo" to null,
                    "mejora" to null,
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, data)
            }

            val cfgRef = nominaDocRef.collection("calificaciones").document("_config")
            batch.set(
                cfgRef,
                mapOf(
                    "insumosCount" to insumosCount,
                    "weights" to mapOf("formativa" to 0.7, "sumativa" to 0.3)
                )
            )

            batch.commit()
                .addOnSuccessListener { onDone(true) }
                .addOnFailureListener { onDone(false) }
        }
        .addOnFailureListener { onDone(false) }
}

private inline fun String?.ifNullOrBlank(defaultValue: () -> String): String {
    return if (this == null || this.isBlank()) defaultValue() else this
}
//////////////////////////////////////////////////////////////////////////////////////////////
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

    // cargar de Firestore solo una vez para la colección indicada
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
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        onValorSeleccionado(opcion)
                        expanded = false
                    }
                )
            }
        }
    }
}

////////////////////////////

// Función que obtiene una lista de Strings desde una colección de Firestore.
fun cargarListadoFirestore(
    coleccion: String,
    onSuccess: (List<String>) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("gestionAcademica")
        .document("datosGenerales")
        .collection(coleccion)   // 🔹 aquí se busca dentro de la estructura nueva
        .orderBy("timestamp")
        .get()
        .addOnSuccessListener { snap ->
            val lista = snap.documents.mapNotNull { it.getString("nombre") }
            onSuccess(lista)
        }
        .addOnFailureListener { onSuccess(emptyList()) }
}

fun procesarArchivoExcel(context: Context, uri: Uri): List<List<String>> {
    val datos = mutableListOf<List<String>>()
    val formatter = DataFormatter()  // ← Agrega esto al inicio

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            for (row in sheet) {
                val fila = mutableListOf<String>()

                for ((i, cell) in row.withIndex()) {
                    val valor = when (i) {
                        0 -> if (cell.cellType == CellType.NUMERIC) cell.numericCellValue.toLong().toString()
                        else formatter.formatCellValue(cell)

                        1, 2 -> formatter.formatCellValue(cell)

                        else -> formatter.formatCellValue(cell)
                    }
                    fila.add(valor)
                }
                datos.add(fila)
            }
            workbook.close()
        } ?: run {
            mensajealert(context, "No se pudo abrir el archivo")
        }
    } catch (e: Exception) {
        mensajealert(context, "Error al leer Excel: ${e.message}")
    }
    return datos
}

//////////////////////////////////////////////////////////////////////////////////////////////
private fun idUnicoEstudiante(): String {
    return "std_" + UUID.randomUUID().toString().replace("-", "").take(16)
}