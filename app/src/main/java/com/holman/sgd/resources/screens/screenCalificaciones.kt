package com.holman.sgd.resources

import android.annotation.SuppressLint
import androidx.compose.foundation.background
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
    var estudiantes by remember { mutableStateOf<List<TablaConfig.EstudianteCalificacion>>(emptyList()) }
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
                    text = "Volver a nóminas",
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
                            val colores = TablaColors.fromNomina(headerColor) // usa el color de la nómina
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
