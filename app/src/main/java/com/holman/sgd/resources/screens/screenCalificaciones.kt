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
import androidx.compose.runtime.saveable.rememberSaveable
import com.holman.sgd.resources.components.ContenedorPrincipal
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

    // ⬇️ NUEVO: estado controlado por nómina (id → TerminoEval)
    val terminosElegidos = remember { mutableStateMapOf<String, TerminoEval>() }

    var detalleOnBackRequest by remember { mutableStateOf<(() -> Unit)?>(null) }
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
        // ⬇️ Al abrir detalle, usamos el término guardado de esa nómina (por defecto T1)
        val initialTermino = terminosElegidos[selectedNomina!!.id] ?: TerminoEval.T1

        ScreenNominaDetalleCalificaciones(
            nomina = selectedNomina!!,
            headerColor = selectedNominaColor ?: EncabezadoEnDetalleNominas,
            onBack = {
                selectedNomina = null
                selectedNominaColor = null
                detalleOnBackRequest = null
                prefetchEstudiantes = null
            },
            onRegisterBackRequest = { handler -> detalleOnBackRequest = handler },
            initialEstudiantes = prefetchEstudiantes,
            skipInitialLoad = prefetchEstudiantes != null,
            // ⬇️ NUEVO: pasa el término inicial
            initialTermino = initialTermino
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            FondoScreenDefault()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ContenedorPrincipal)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                        //.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> Spacer(Modifier.size(1.dp))
                        error != null -> Text("❌ Error: $error", color = MaterialTheme.colorScheme.error)
                        nominas.isEmpty() ->Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            TituloScreenNominas(texto = "No hay nóminas Guardadas")
                        }
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TituloScreenNominas(texto = "Nóminas Guardadas")
                                Spacer(modifier = Modifier.width(8.dp))

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    itemsIndexed(nominas) { index, nomina ->
                                        NominaCardCalificaciones(
                                            nomina = nomina,
                                            index = index,
                                            // ⬇️ Clic de la card: se abre con el término seleccionado para esa nómina
                                            onClick = { colorSeleccionado: Color ->
                                                isOpeningDetail = true

                                                // usa el término elegido para ESA nómina (o T1 por defecto)
                                                val terminoElegido = terminosElegidos[nomina.id] ?: TerminoEval.T1

                                                cargarDatosDesdeFirestore(
                                                    nominaId = nomina.id,
                                                    termino  = terminoElegido,          // 👈 FALTABA
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
                                            },
                                            // ⬇️ Hacemos el selector controlado por nómina
                                            termSelected = terminosElegidos[nomina.id] ?: TerminoEval.T1,
                                            onTermChange = { nuevo ->
                                                terminosElegidos[nomina.id] = nuevo
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CustomButton(
                        text = "Volver",
                        borderColor = ButtonDarkGray,
                        onClick = { navController.popBackStack()}
                    )
                }
            }

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
    skipInitialLoad: Boolean = false,
    // Término con el que entras al detalle (T1/T2/T3/INF)
    initialTermino: TerminoEval = TerminoEval.T1
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // ---- Estados base ----
    var isLoading by remember { mutableStateOf(true) }
    var estudiantes by remember { mutableStateOf<List<TablaConfig.EstudianteCalificacion>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }
    val isBusy by remember { derivedStateOf { isLoading || isSaving } }

    // ⬇️ término visible en el detalle (por si luego quieres mostrar selector aquí)
    var termino by rememberSaveable { mutableStateOf(initialTermino) }

    // Para detectar cambios
    var baseline by remember { mutableStateOf<Map<String, List<Double?>>>(emptyMap()) }

    // ---- Helpers de comparación ----
    fun eqNota(a: Double?, b: Double?, eps: Double = 1e-6) =
        if (a == null && b == null) true else if (a == null || b == null) false else kotlin.math.abs(a - b) < eps

    fun notasCambiaron(actual: List<Double?>, anterior: List<Double?>): Boolean {
        // Para TRIMESTRE: 10 formativas + 4 sumativas editables = 14
        // Para INFORME: solo SUPLETORIO es editable (pero por simplicidad dejamos 14; no rompe)
        val editableCols = TablaConfig.INSUMOS_COUNT + 4
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

    // ---- Guardar ----
    fun saveIfDirty(showToast: Boolean, onDone: () -> Unit = {}) {
        val cambios = estudiantesModificados()
        if (cambios.isEmpty()) {
            if (showToast) mensajealert(context, "ℹ️ No hay cambios para guardar.")
            onDone(); return
        }
        isSaving = true
        guardarDatosEnFirestore(
            nominaId = nomina.id,
            termino = termino,                     // 👈 usa el término
            estudiantes = cambios
        ) {
            baseline = estudiantes.associate { e -> e.idUnico to e.notas.map { it } }
            isSaving = false
            if (showToast) mensajealert(context, "✅  Calificaciones guardadas.")
            onDone()
        }
    }


    // ---- Recargar ----
    fun refresh(fromSave: Boolean) {
        if (!fromSave) isLoading = true
        cargarDatosDesdeFirestore(
            nominaId = nomina.id,
            termino = termino,                    // 👈 usa el término
            onSuccess = { lista ->
                estudiantes = lista
                baseline = lista.associate { e -> e.idUnico to e.notas.map { it } }
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

    // ---- Carga inicial (con prefetch) ----
    LaunchedEffect(Unit) {
        if (initialEstudiantes != null) {
            estudiantes = initialEstudiantes
            baseline = initialEstudiantes.associate { e -> e.idUnico to e.notas.map { it } }
            isLoading = false
        }
    }

    // ---- Carga desde backend si no vino prefetch ----
    LaunchedEffect(nomina.id) {
        if (skipInitialLoad) return@LaunchedEffect
        refresh(fromSave = false)
    }

    // ---- Integración con back físico: guarda si hay cambios ----
    LaunchedEffect(nomina.id, estudiantes, baseline, isBusy) {
        onRegisterBackRequest {
            if (!isBusy && tieneCambiosPendientes()) {
                saveIfDirty(showToast = false) { onBack() }
            } else {
                onBack()
            }
        }
    }

    // ---- Guardado silencioso al background ----
    DisposableEffect(lifecycleOwner, estudiantes, baseline) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                val cambios = estudiantesModificados()
                if (cambios.isNotEmpty()) {
                    guardarDatosEnFirestore(
                        nominaId = nomina.id,
                        termino  = termino,              // 👈 FALTABA
                        estudiantes = cambios
                    ) { /* silencioso */ }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    // ---- UI ----
    Scaffold(containerColor = BackgroundDefault) {
        FondoScreenDefault()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(ContenedorPrincipal),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ---- CONTENIDO PRINCIPAL ----
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NominaHeaderCard(
                        nomina = nomina,
                        backgroundColor = headerColor,
                        termino = termino,
                        onClick = null
                    )

                    Spacer(Modifier.height(16.dp))

                    when {
                        isLoading -> Spacer(Modifier.size(1.dp))
                        else -> {
                            val colores = TablaColors.fromNomina(headerColor)

                            // ⬇️ Elige qué tabla mostrar según 'termino'
                            when (termino) {
                                TerminoEval.INF -> {
                                    TablaInforme(
                                        estudiantes = estudiantes,
                                        nominaId = nomina.id,
                                        onRefresh = { refresh(fromSave = false) },
                                        colores = colores
                                    )
                                }
                                TerminoEval.T1, TerminoEval.T2, TerminoEval.T3 -> {
                                    TablaTrimetre(
                                        estudiantes = estudiantes,
                                        nominaId = nomina.id,
                                        onRefresh = { refresh(fromSave = false) },
                                        colores = colores
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CustomButton(
                            text = if (isSaving) "Guardando…" else "Guardar",
                            borderColor = ButtonDarkPrimary,
                            onClick = {
                                if (!isBusy) saveIfDirty(showToast = true)
                            }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
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
                    }
                }
            }

            if (isBusy) {
                LoadingDotsOverlay(isLoading = true)
            }
        }
    }
}


private fun terminoToSeccion(t: TerminoEval): String = when (t) {
    TerminoEval.T1  -> "PrimerTrimestre"
    TerminoEval.T2  -> "SegundoTrimestre"
    TerminoEval.T3  -> "TercerTrimestre"
    TerminoEval.INF -> "InformeAnual"
}

fun cargarDatosDesdeFirestore(
    nominaId: String,
    termino: TerminoEval,
    onSuccess: (List<TablaConfig.EstudianteCalificacion>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val refNomina = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)

    val seccion = terminoToSeccion(termino)
    val docSeccion = refNomina.collection("calificaciones").document(seccion)
    val colInsumos = docSeccion.collection("insumos")

    // 1) Roster desde la tabla de la nómina (col1..col4)
    refNomina.get()
        .addOnSuccessListener { nominaDoc ->
            val tabla = nominaDoc.get("tabla") as? List<Map<String, Any?>> ?: emptyList()
            val filas = tabla.drop(1)
            data class R(val id: String, val nro: Int, val ced: String, val nom: String)
            val roster = filas.mapIndexed { idx, fila ->
                val id  = (fila["col1"] as? String).orEmpty().trim()
                val nro = ((fila["col2"] as? String).orEmpty().trim()).toIntOrNull() ?: (idx + 1)
                val ced = (fila["col3"] as? String).orEmpty().trim()
                val nom = (fila["col4"] as? String).orEmpty().trim()
                R(id, nro, ced, nom)
            }
            if (roster.isEmpty()) { onSuccess(emptyList()); return@addOnSuccessListener }

            // 2) Meta para insumosCount (solo trimestres)
            docSeccion.get()
                .addOnSuccessListener { meta ->
                    val insumosCount =
                        if (termino == TerminoEval.INF) 0
                        else (meta.getLong("insumosCount") ?: TablaConfig.INSUMOS_COUNT.toLong()).toInt()

                    // 3) Leer insumos
                    colInsumos.get()
                        .addOnSuccessListener { snap ->
                            val porId = snap.documents.associateBy({ it.id }, { it.data ?: emptyMap<String, Any?>() })

                            val lista = roster.sortedBy { it.nro }.map { r ->
                                val data = porId[r.id]

                                if (termino == TerminoEval.INF) {
                                    // Informe: T1,T2,T3,Suple → posiciones 0..3
                                    val t1 = (data?.get("PromedioT1") as? Number)?.toDouble()
                                    val t2 = (data?.get("PromedioT2") as? Number)?.toDouble()
                                    val t3 = (data?.get("PromedioT3") as? Number)?.toDouble()
                                    val su = (data?.get("Supletorio") as? Number)?.toDouble()
                                    val notas = mutableListOf<Double?>(t1, t2, t3, su)
                                    TablaConfig.EstudianteCalificacion(
                                        idUnico = r.id, numero = r.nro, nombre = r.nom, notas = notas
                                    )
                                } else {
                                    // Trimestre: actividades + 4 sumativas
                                    val acts = (data?.get("actividades") as? List<*>)
                                        ?.map { (it as? Number)?.toDouble() }
                                        ?.let { l -> if (l.size >= insumosCount) l.take(insumosCount) else l + List(insumosCount - l.size){ null } }
                                        ?: List(insumosCount) { null }
                                    val proyecto   = (data?.get("proyecto") as? Number)?.toDouble()
                                    val evaluacion = (data?.get("evaluacion") as? Number)?.toDouble()
                                    val refuerzo   = (data?.get("refuerzo") as? Number)?.toDouble()
                                    val mejora     = (data?.get("mejora") as? Number)?.toDouble()

                                    val notas = (acts + listOf(proyecto, evaluacion, refuerzo, mejora)).toMutableList()
                                    TablaConfig.EstudianteCalificacion(
                                        idUnico = r.id, numero = r.nro, nombre = r.nom, notas = notas
                                    )
                                }
                            }
                            onSuccess(lista)
                        }
                        .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error leyendo insumos ($seccion)") }
                }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error leyendo sección $seccion") }
        }
        .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error leyendo nómina") }
}

fun guardarDatosEnFirestore(
    nominaId: String,
    termino: TerminoEval,
    estudiantes: List<TablaConfig.EstudianteCalificacion>,
    onComplete: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val refNomina = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)

    val seccion = terminoToSeccion(termino)
    val docSeccion = refNomina.collection("calificaciones").document(seccion)
    val colInsumos = docSeccion.collection("insumos")

    // Para trimestres, necesito saber insumosCount
    docSeccion.get()
        .addOnSuccessListener { meta ->
            val insumosCount =
                if (termino == TerminoEval.INF) 0
                else (meta.getLong("insumosCount") ?: TablaConfig.INSUMOS_COUNT.toLong()).toInt()

            val batch = db.batch()

            estudiantes.forEach { est ->
                val docRef = colInsumos.document(est.idUnico)
                val data = when (termino) {
                    TerminoEval.INF -> {
                        val suple = est.notas.getOrNull(3)
                        mapOf(
                            // solo lo editable
                            "Supletorio" to suple,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    }
                    else -> {
                        mapOf(
                            "actividades" to est.notas.take(insumosCount),
                            "proyecto"    to est.notas.getOrNull(insumosCount + 0),
                            "evaluacion"  to est.notas.getOrNull(insumosCount + 1),
                            "refuerzo"    to est.notas.getOrNull(insumosCount + 2),
                            "mejora"      to est.notas.getOrNull(insumosCount + 3),
                            "updatedAt"   to System.currentTimeMillis()
                        )
                    }
                }
                batch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
            }

            batch.commit().addOnCompleteListener { onComplete() }
        }
        .addOnFailureListener { onComplete() }
}

