////////////

package com.holman.sgd.resources

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.holman.sgd.resources.components.ContenedorPrincipal
import com.holman.sgd.ui.theme.BackgroundDefault
import com.holman.sgd.ui.theme.ButtonDarkGray
import com.holman.sgd.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.google.firebase.auth.FirebaseAuth
import com.holman.sgd.resources.components.FirestorePaths

// 🔹 Data class para representar una nómina
data class NominaResumen(
    val id: String = "",
    val institucion: String = "",
    val docente: String = "",
    val curso: String = "",
    val paralelo: String = "",
    val asignatura: String = "",
    val especialidad: String = "",
    val periodo: String = "",
    val timestamp: Long = 0L
)

// 🔹 Data class para asistencia de estudiante con ID único
data class EstudianteAsistencia(
    val idUnico: String,   // 🔹 ID único inmutable (col1 de la nómina)
    val numero: String,
    val cedula: String,
    val nombre: String,
    val presente: Boolean = false
)


// 🔹 Función para hoy (yyyy-MM-dd)
fun getHoy(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

// 🔹 Generar ID único basado en cédula (más estable que el nombre)
fun generarIdUnicoEstudiante(cedula: String, nombre: String): String {
    return if (cedula.isNotBlank()) {
        "cedula_${cedula.trim()}"
    } else {
        // Fallback si no hay cédula: usar hash del nombre
        "nombre_${nombre.trim().lowercase().hashCode()}"
    }
}

@Composable
fun Asistencias(navController: NavHostController) {
    val context = LocalContext.current

    var nominas by remember { mutableStateOf<List<NominaResumen>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }

    var error by remember { mutableStateOf<String?>(null) }

    var selectedNomina by remember { mutableStateOf<NominaResumen?>(null) }
    var selectedNominaColor by remember { mutableStateOf<Color?>(null) }

    // El hijo registrará aquí su handler de back (guardar si cambió y luego onBack)
    var detalleOnBackRequest by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Prefetch antes de abrir el detalle
    var isOpeningDetail by remember { mutableStateOf(false) }
    var prefetchAlumnos by remember { mutableStateOf<List<EstudianteAsistencia>?>(null) }
    var prefetchFecha by remember { mutableStateOf(getHoy()) }

    // Cargar nóminas al iniciar
    LaunchedEffect(Unit) {
        cargarNominasDesdeFirestore(
            onSuccess = { listaNominas ->
                nominas = listaNominas
                isLoading = false
            },
            onError = { errorMsg ->
                error = errorMsg
                isLoading = false
            }
        )
    }

    // El PADRE captura SIEMPRE el back del sistema
    androidx.activity.compose.BackHandler(enabled = true) {
        if (selectedNomina == null) {
            navController.popBackStack()
        } else {
            detalleOnBackRequest?.invoke() ?: run {
                selectedNomina = null
                selectedNominaColor = null
                prefetchAlumnos = null
            }
        }
    }

    if (selectedNomina != null) {
        ScreenNominaDetalleAsistencia(
            nomina = selectedNomina!!,
            headerColor = selectedNominaColor ?: EncabezadoEnDetalleNominas,
            onBack = {
                selectedNomina = null
                selectedNominaColor = null
                detalleOnBackRequest = null
                prefetchAlumnos = null
            },
            onRegisterBackRequest = { handler -> detalleOnBackRequest = handler },
            // ✅ datos precargados para entrar “ya listo”
            initialAlumnos = prefetchAlumnos,
            initialFecha = prefetchFecha,
            skipInitialLoad = prefetchAlumnos != null
        )
    } else {
        // Pantalla principal: lista de nóminas
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
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> Spacer(Modifier.size(1.dp)) // el overlay cubre
                        error != null -> Text(
                            "❌ Error: $error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        nominas.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            TituloGeneralScreens(texto = "No hay nóminas Guardadas")
                        }
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TituloGeneralScreens(texto = "Nóminas Guardadas")
                                Spacer(modifier = Modifier.width(8.dp))

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    itemsIndexed(nominas) { index, nomina ->
                                        NominaCardAsistencias(
                                            nomina = nomina,
                                            index = index,
                                            onClick = { colorSeleccionado ->
                                                // 🔹 PREFETCH: mantenemos la lista visible hasta cargar todo
                                                isOpeningDetail = true
                                                val hoy = getHoy()
                                                prefetchFecha = hoy

                                                cargarAlumnosAsistenciaPorNomina(
                                                    nominaId = nomina.id,
                                                    onSuccess = { lista ->
                                                        cargarAsistenciaExistente(
                                                            nominaId = nomina.id,
                                                            fecha = hoy,
                                                            onSuccess = { asistenciaMap ->
                                                                val mut = lista.toMutableList()
                                                                aplicarAsistenciaCargada(mut, asistenciaMap)
                                                                limpiarAsistenciasHuerfanas(nomina.id, hoy, mut)

                                                                prefetchAlumnos = mut
                                                                selectedNomina = nomina
                                                                selectedNominaColor = colorSeleccionado
                                                                isOpeningDetail = false
                                                            },
                                                            onError = {
                                                                // Si falla asistencia, abrimos con alumnos igual
                                                                prefetchAlumnos = lista
                                                                selectedNomina = nomina
                                                                selectedNominaColor = colorSeleccionado
                                                                isOpeningDetail = false
                                                            }
                                                        )
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

///
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(fecha: String, onFechaSeleccionada: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = fecha,
        onValueChange = {},
        readOnly = true,
        label = { Text("Fecha", color = TextDefaultBlack) },
        textStyle = LocalTextStyle.current.copy(color = TextDefaultBlack),
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Seleccionar fecha",
                    tint = TextDefaultBlack
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDefault),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = BackgroundDefault,
            unfocusedContainerColor = BackgroundDefault,
            disabledContainerColor = BackgroundDefault,
            focusedBorderColor = TextDefaultBlack,
            unfocusedBorderColor = TextDefaultBlack.copy(alpha = 0.6f),
            cursorColor = TextDefaultBlack
        )
    )

    if (showDialog) {
        val hoy = LocalDate.now()
        // ✅ 1) Inicializa en UTC (evita off-by-one)
        val millisHoy = hoy.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val state = rememberDatePickerState(
            initialSelectedDateMillis = millisHoy,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // ✅ 2) Evalúa el día en UTC (coherente con el DatePicker)
                    val dayOfWeek = Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneOffset.UTC).dayOfWeek
                    return dayOfWeek.value in 1..5
                }
            }
        )

        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
        {
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(16.dp))
            ){
                // 🔹 Densidad compacta (igual que antes)
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = density.density * 0.85f,
                        fontScale = density.fontScale * 0.95f
                    )
                ) {
                    Surface(
                        color = BackgroundDefault,
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                DatePicker(
                                    state = state,
                                    showModeToggle = true,
                                    modifier = Modifier.widthIn(max = 400.dp),
                                    colors = DatePickerDefaults.colors(
                                        containerColor = BackgroundDefault,
                                        titleContentColor = TextDefaultBlack,
                                        headlineContentColor = TextDefaultBlack,
                                        weekdayContentColor = TextDefaultBlack.copy(alpha = 0.7f),
                                        subheadContentColor = TextDefaultBlack,
                                        navigationContentColor = TextDefaultBlack
                                    )
                                )
                            }

                            Divider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = TextDefaultBlack.copy(alpha = 0.25f)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 16.dp, top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                FilledTonalIconButton(
                                    onClick = { showDialog = false },
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                                }

                                Spacer(Modifier.width(16.dp))

                                FilledTonalIconButton(
                                    onClick = {
                                        state.selectedDateMillis?.let { millis ->
                                            // ✅ 3) Lee la fecha en UTC (evita día -1)
                                            val localDate = Instant.ofEpochMilli(millis)
                                                .atZone(ZoneOffset.UTC)
                                                .toLocalDate()
                                            val nuevaFecha = "%04d-%02d-%02d".format(
                                                localDate.year, localDate.monthValue, localDate.dayOfMonth
                                            )
                                            onFechaSeleccionada(nuevaFecha)
                                        }
                                        showDialog = false
                                    },
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Aceptar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

///
/////////////////
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ScreenNominaDetalleAsistencia(
    nomina: NominaResumen,
    headerColor: Color,
    onBack: () -> Unit,
    onRegisterBackRequest: ((() -> Unit) -> Unit),
    initialAlumnos: List<EstudianteAsistencia>? = null,
    initialFecha: String? = null,
    skipInitialLoad: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val alumnos = remember { mutableStateListOf<EstudianteAsistencia>() }
    var isLoading by remember { mutableStateOf(true) }
    var fecha by remember { mutableStateOf(initialFecha ?: getHoy()) }
    var isSaving by remember { mutableStateOf(false) }

    val isBusy by remember { derivedStateOf { isLoading || isSaving } }
    var baseline by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    fun snapshotActual(): Map<String, Boolean> =
        alumnos.associate { it.idUnico to it.presente }

    fun hayCambios(): Boolean {
        val now = snapshotActual()
        if (baseline.size != now.size) return true
        return now.any { (k, v) -> baseline[k] != v }
    }

    fun saveIfDirty(showToast: Boolean, onDone: () -> Unit = {}) {
        if (!hayCambios()) {
            if (showToast) mensajealert(context, "ℹ️ No hay cambios para guardar.")
            onDone(); return
        }
        isSaving = true
        guardarAsistenciaFirestore(
            nominaId = nomina.id,
            fecha = fecha,
            alumnos = alumnos,
            onSuccess = {
                baseline = snapshotActual()
                isSaving = false
                if (showToast) mensajealert(context, "✅ Asistencia guardada.")
                onDone()
            },
            onError = { msg ->
                isSaving = false
                if (showToast) mensajealert(context, "❌ Error: $msg")
                onDone()
            }
        )
    }

    // Data precargada
    LaunchedEffect(Unit) {
        if (initialAlumnos != null) {
            alumnos.clear()
            alumnos.addAll(initialAlumnos)
            baseline = snapshotActual()
            isLoading = false
        }
    }

    // Carga inicial
    LaunchedEffect(nomina.id) {
        if (skipInitialLoad) return@LaunchedEffect
        isLoading = true
        cargarAlumnosAsistenciaPorNomina(
            nominaId = nomina.id,
            onSuccess = { lista ->
                alumnos.clear()
                alumnos.addAll(lista)
                cargarAsistenciaExistente(
                    nominaId = nomina.id,
                    fecha = fecha,
                    onSuccess = { asistenciaMap ->
                        aplicarAsistenciaCargada(alumnos, asistenciaMap)
                        limpiarAsistenciasHuerfanas(nomina.id, fecha, alumnos)
                        baseline = snapshotActual()
                        isLoading = false
                    },
                    onError = {
                        baseline = snapshotActual()
                        isLoading = false
                    }
                )
            },
            onError = { msg ->
                isLoading = false
                mensajealert(context, "❌ $msg")
            }
        )
    }

    // Al cambiar la fecha
    LaunchedEffect(fecha) {
        if (alumnos.isNotEmpty()) {
            cargarAsistenciaExistente(
                nominaId = nomina.id,
                fecha = fecha,
                onSuccess = { asistenciaMap ->
                    aplicarAsistenciaCargada(alumnos, asistenciaMap)
                    limpiarAsistenciasHuerfanas(nomina.id, fecha, alumnos)
                    baseline = snapshotActual()
                },
                onError = {
                    baseline = snapshotActual()
                }
            )
        }
    }

    // Back
    LaunchedEffect(nomina.id, alumnos, baseline, isBusy, fecha) {
        onRegisterBackRequest {
            if (!isBusy && hayCambios()) {
                saveIfDirty(showToast = false) { onBack() }
            } else onBack()
        }
    }

    // Autosave onStop
    DisposableEffect(lifecycleOwner, alumnos, baseline, fecha) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                if (hayCambios()) {
                    guardarAsistenciaFirestore(
                        nominaId = nomina.id,
                        fecha = fecha,
                        alumnos = alumnos,
                        onSuccess = { baseline = snapshotActual() },
                        onError = { /* silencioso */ }
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(containerColor = BackgroundDefault) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            FondoScreenDefault()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ContenedorPrincipal),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Encabezado + selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    NominaHeaderCard(
                        nomina = nomina,
                        backgroundColor = headerColor,
                        onClick = null
                    )

                    Spacer(Modifier.height(12.dp))

                    SelectorFecha(
                        fecha = fecha,
                        onFechaSeleccionada = { nuevaFecha -> fecha = nuevaFecha }
                    )

                    Spacer(Modifier.height(16.dp))
                }

                // LISTA SCROLLEABLE + FAB superpuesto SOLO a esta sección
                Box(
                    modifier = Modifier
                        .weight(1f)              // ocupa el alto disponible entre header y botón inferior
                        .fillMaxWidth()
                ) {
                    when {
                        isLoading -> {
                            // Mantén layout estable
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 4.dp)
                            ) { }
                        }
                        alumnos.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hay alumnos registrados",
                                    fontSize = 16.sp,
                                    color = TextDefaultBlack
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 4.dp) // para no tapar con el botón inferior
                                ) {
                                    items(alumnos) { estudiante ->
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            elevation = CardDefaults.cardElevation(2.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(BackgroundDefault) // Fondo bajo la tarjeta
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(BackgroundDefault) // Fondo de cada fila
                                                    .padding(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = estudiante.presente,
                                                    onCheckedChange = { nuevoValor ->
                                                        val index = alumnos.indexOf(estudiante)
                                                        if (index != -1) {
                                                            alumnos[index] = estudiante.copy(presente = nuevoValor)
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = ButtonDarkSuccess,
                                                        uncheckedColor = TextDefaultBlack
                                                    )
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    text = "${estudiante.numero}. ${estudiante.nombre}",
                                                    fontSize = 16.sp,
                                                    color = TextDefaultBlack
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!isBusy) {
                                    FloatingSaveButton(
                                        visible = true,
                                        onClick = { saveIfDirty(showToast = true) },
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .zIndex(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Botón inferior fijo
                CustomButton(
                    text = "Volver a nóminas",
                    borderColor = ButtonDarkGray,
                    onClick = {
                        if (!isBusy && hayCambios()) {
                            saveIfDirty(showToast = false) { onBack() }
                        } else onBack()
                    }
                )
            }

            LoadingDotsOverlay(isLoading = isBusy)
        }
    }
}


////

fun cargarAsistenciaExistente(
    nominaId: String,
    fecha: String,
    onSuccess: (Map<String, Boolean>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onError("Usuario no autenticado"); return }

    FirestorePaths.asistenciaDoc(uid, nominaId, fecha)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val asistenciaMap = mutableMapOf<String, Boolean>()
                document.data?.forEach { (k, v) ->
                    val boolVal = when (v) {
                        is Boolean -> v
                        is String -> v.equals("true", ignoreCase = true)
                        is Number -> v.toInt() != 0
                        else -> false
                    }
                    asistenciaMap[k] = boolVal
                }
                onSuccess(asistenciaMap)
            } else onSuccess(emptyMap())
        }
        .addOnFailureListener { exception -> onError(exception.localizedMessage ?: "Error desconocido") }
}

fun aplicarAsistenciaCargada(
    alumnos: MutableList<EstudianteAsistencia>,
    asistenciaMap: Map<String, Boolean>
) {
    for (i in alumnos.indices) {
        val alumno = alumnos[i]
        // Buscar asistencia por ID único, luego por número (compatibilidad), luego por nombre
        val presente = asistenciaMap[alumno.idUnico]
            ?: asistenciaMap[alumno.numero]
            ?: asistenciaMap[alumno.nombre]
            ?: false
        alumnos[i] = alumno.copy(presente = presente)
    }
}

fun limpiarAsistenciasHuerfanas(
    nominaId: String,
    fecha: String,
    alumnosActuales: List<EstudianteAsistencia>
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val rutaAsistencias = FirestorePaths.asistenciaDoc(uid, nominaId, fecha)

    rutaAsistencias.get().addOnSuccessListener { document ->
        if (document.exists()) {
            val asistenciaActual = document.data?.toMutableMap() ?: mutableMapOf()
            val idsActuales = alumnosActuales.map { it.idUnico }.toSet()
            val nombresActuales = alumnosActuales.map { it.nombre }.toSet()
            val numerosActuales = alumnosActuales.map { it.numero }.toSet()
            var huboLimpieza = false

            // Remover asistencias de estudiantes que ya no existen
            val iterator = asistenciaActual.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val claveAsistencia = entry.key

                // Verificar si la clave corresponde a un estudiante actual
                val esEstudianteActual = idsActuales.contains(claveAsistencia) ||
                        nombresActuales.contains(claveAsistencia) ||
                        numerosActuales.contains(claveAsistencia)

                if (!esEstudianteActual) {
                    iterator.remove()
                    huboLimpieza = true
                }
            }

            // Actualizar documento si hubo limpieza
            if (huboLimpieza) {
                if (asistenciaActual.isEmpty()) {
                    rutaAsistencias.delete()
                } else {
                    rutaAsistencias.set(asistenciaActual)
                }
            }
        }
    }
}


fun guardarAsistenciaFirestore(
    nominaId: String,
    fecha: String,
    alumnos: List<EstudianteAsistencia>,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onError("Usuario no autenticado"); return }

    val rutaAsistencias = FirestorePaths.asistencias(uid, nominaId)

    val hayAsistentes = alumnos.any { it.presente }

    if (!hayAsistentes) {
        rutaAsistencias.document(fecha)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error eliminando asistencia vacía") }
    } else {
        // 🔹 Guardar usando idUnico como clave
        val asistenciaMap = alumnos.associate { it.idUnico to it.presente }
        rutaAsistencias.document(fecha)
            .set(asistenciaMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error guardando asistencia") }
    }
}

// 🔹 Cargar nóminas
fun cargarNominasDesdeFirestore(
    onSuccess: (List<NominaResumen>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onError("Usuario no autenticado"); return }

    FirestorePaths.cursos(uid)
        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { documents ->
            val listaNominas = documents.map { document ->
                NominaResumen(
                    id = document.id,
                    institucion = document.getString("institucion") ?: "",
                    docente = document.getString("docente") ?: "",
                    curso = document.getString("curso") ?: "",
                    paralelo = document.getString("paralelo") ?: "",
                    asignatura = document.getString("asignatura") ?: "",
                    especialidad = document.getString("especialidad") ?: "",
                    periodo = document.getString("periodo") ?: "",
                    timestamp = document.getLong("timestamp") ?: 0L
                )
            }
            onSuccess(listaNominas)
        }
        .addOnFailureListener { exception ->
            onError(exception.localizedMessage ?: "Error desconocido")
        }
}



fun cargarAlumnosAsistenciaPorNomina(
    nominaId: String,
    onSuccess: (List<EstudianteAsistencia>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onError("Usuario no autenticado"); return }

    FirestorePaths.nominaDoc(uid, nominaId)
        .get()
        .addOnSuccessListener { document ->
            if (!document.exists()) {
                onError("Nómina no encontrada"); return@addOnSuccessListener
            }

            val tabla = document.get("tabla") as? List<Map<String, Any?>> ?: emptyList()
            if (tabla.isEmpty()) { onSuccess(emptyList()); return@addOnSuccessListener }

            val cuerpo = tabla.drop(1) // saltar encabezado

            val alumnos = cuerpo.mapIndexed { index, fila ->
                val col1 = (fila["col1"] as? String)?.trim().orEmpty() // ID (nuevo) o N° (antiguo)
                val col2 = (fila["col2"] as? String)?.trim().orEmpty() // Nro
                val col3 = (fila["col3"] as? String)?.trim().orEmpty() // Cédula
                val col4 = (fila["col4"] as? String)?.trim().orEmpty() // Estudiante

                // Compatibilidad con nóminas antiguas (sin col1=ID):
                val cedula = if (col3.isNotEmpty())
                    col3
                else
                    (fila["Cédula"] ?: fila["col2"] ?: "").toString().trim()

                val nombre = if (col4.isNotEmpty())
                    col4
                else
                    (fila["Estudiante"] ?: fila["col3"] ?: "").toString().trim()
                        .ifEmpty { "Alumno desconocido" }

                val numero = if (col2.isNotEmpty())
                    col2
                else
                    (fila["N°"] ?: fila["col1"] ?: (index + 1)).toString()

                // ✅ Usar SIEMPRE el ID de col1 cuando exista (nuevo esquema).
                // Fallback legacy SOLO si la nómina es vieja y no trae col1 como ID.
                val id = if (col1.isNotEmpty() && col1 != "ID")
                    col1
                else
                    generarIdUnicoEstudiante(cedula, nombre) // ↩︎ mismo esquema legacy de esta screen

                EstudianteAsistencia(
                    idUnico = id,
                    numero = numero,
                    cedula = cedula,
                    nombre = nombre
                )
            }

            onSuccess(alumnos)
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error al cargar nómina")
        }
}