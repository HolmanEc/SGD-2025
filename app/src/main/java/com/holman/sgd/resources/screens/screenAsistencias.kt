package com.holman.sgd.resources

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.holman.sgd.resources.components.getColorsCardsInicio
import com.holman.sgd.ui.theme.BackgroundDefault
import com.holman.sgd.ui.theme.ButtonDarkGray
import com.holman.sgd.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

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
    val idUnico: String,         // 🔹 ID único inmutable basado en cédula
    val numero: String,
    val cedula: String,          // 🔹 Cédula del estudiante (col2)
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

//////////////
@Composable
fun Asistencias(navController: NavHostController) {
    var nominas by remember { mutableStateOf<List<NominaResumen>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedNomina by remember { mutableStateOf<NominaResumen?>(null) }
    var selectedNominaColor by remember { mutableStateOf<Color?>(null) }

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

    if (selectedNomina != null) {
        ScreenNominaDetalleAsistencia(
            nomina = selectedNomina!!,
            headerColor = selectedNominaColor ?: EncabezadoEnDetalleNominas,
            onBack = {
                selectedNomina = null
                selectedNominaColor = null
            }
        )
    } else {
        Box( // 👈 Box raíz para poder superponer el overlay
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
                        // 👇 Antes: CircularProgressIndicator()
                        isLoading -> {
                            // Dejamos el espacio, el overlay cubrirá toda la pantalla
                            Spacer(Modifier.size(1.dp))
                        }
                        error != null -> {
                            Text(
                                "❌ Error: $error",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        nominas.isEmpty() -> {
                            Text(
                                "📋 No hay nóminas guardadas.",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📋 Nóminas para Asistencia",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
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
                                            onClick = { colorSeleccionado ->
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

            // 👇 Tu overlay de puntos durante la carga de NÓMINAS
            LoadingDotsOverlay(isLoading = isLoading)
        }
    }
}


/////////////
// 🔹 Selector de fecha con calendario
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(fecha: String, onFechaSeleccionada: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = fecha,
        onValueChange = {},
        readOnly = true,
        label = { Text("Fecha") },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (showDialog) {
        val hoy = LocalDate.now()
        val millisHoy = hoy.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = millisHoy,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val dayOfWeek = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).dayOfWeek
                    return dayOfWeek.value in 1..5
                }
            })

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        val nuevaFecha = "%04d-%02d-%02d".format(localDate.year, localDate.monthValue, localDate.dayOfMonth)
                        onFechaSeleccionada(nuevaFecha)
                    }
                    showDialog = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = state, showModeToggle = true) }
    }
}

////////

// 🔹 Pantalla de detalle de asistencia
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ScreenNominaDetalleAsistencia(
    nomina: NominaResumen,
    headerColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val alumnos = remember { mutableStateListOf<EstudianteAsistencia>() }
    var isLoading by remember { mutableStateOf(true) }
    var fecha by remember { mutableStateOf(getHoy()) }
    var isSaving by remember { mutableStateOf(false) }

    // Estado unificado para overlay y visibilidad del FAB
    val isBusy by remember { derivedStateOf { isLoading || isSaving } }

    LaunchedEffect(nomina.id) {
        val db = FirebaseFirestore.getInstance()
        db.collection("gestionAcademica")
            .document("gestionNominas")
            .collection("nominasEstudiantes")
            .document(nomina.id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val tabla = document.get("tabla") as? List<Map<String, Any>> ?: emptyList()
                    alumnos.clear()
                    alumnos.addAll(
                        tabla.drop(1).mapIndexed { index, fila ->
                            val cedula = fila["col2"]?.toString() ?: ""
                            val nombre = fila["col3"]?.toString() ?: "Alumno desconocido"
                            val numero = (index + 1).toString()
                            EstudianteAsistencia(
                                idUnico = generarIdUnicoEstudiante(cedula, nombre),
                                numero = numero,
                                cedula = cedula,
                                nombre = nombre
                            )
                        }
                    )

                    cargarAsistenciaExistente(
                        nominaId = nomina.id,
                        fecha = fecha,
                        onSuccess = { asistenciaMap ->
                            aplicarAsistenciaCargada(alumnos, asistenciaMap)
                            limpiarAsistenciasHuerfanas(nomina.id, fecha, alumnos)
                        },
                        onError = {}
                    )
                } else {
                    mensajealert(context, "❌ Nómina no encontrada")
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                mensajealert(context, "❌ Error cargando alumnos")
            }
    }

    LaunchedEffect(fecha) {
        if (alumnos.isNotEmpty()) {
            cargarAsistenciaExistente(
                nominaId = nomina.id,
                fecha = fecha,
                onSuccess = { asistenciaMap ->
                    aplicarAsistenciaCargada(alumnos, asistenciaMap)
                    limpiarAsistenciasHuerfanas(nomina.id, fecha, alumnos)
                },
                onError = {}
            )
        }
    }

    Scaffold(
        containerColor = BackgroundDefault,
        floatingActionButton = {
            FloatingSaveButton(
                visible = !isBusy,
                onClick = {
                    isSaving = true
                    guardarAsistenciaFirestore(
                        nominaId = nomina.id,
                        fecha = fecha,
                        alumnos = alumnos,
                        onSuccess = {
                            mensajealert(context, "✅ Asistencia guardada.")
                            isSaving = false
                        },
                        onError = { msg ->
                            mensajealert(context, "❌ Error: $msg")
                            isSaving = false
                        }
                    )
                },
                modifier = Modifier.offset(x = (-4).dp, y = 2.dp),
            )
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CustomButton(
                        text = "Volver a nóminas",
                        borderColor = ButtonDarkGray,
                        onClick = onBack
                    )
                }

                Spacer(Modifier.height(16.dp))

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

                when {
                    isLoading -> {
                        Spacer(Modifier.size(1.dp))
                    }
                    alumnos.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay alumnos registrados", fontSize = 16.sp, color = TextDefaultBlack)
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(alumnos) { estudiante ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
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
                                                uncheckedColor = ButtonDarkGray
                                            )
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text("${estudiante.numero}. ${estudiante.nombre}", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LoadingDotsOverlay(isLoading = isBusy)
        }
    }
}



/////////
////////////////////
// 🔹 Cargar asistencia existente
fun cargarAsistenciaExistente(
    nominaId: String,
    fecha: String,
    onSuccess: (Map<String, Boolean>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)
        .collection("asistencias")
        .document(fecha)
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

// 🔹 Aplicar asistencia cargada con compatibilidad hacia atrás
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

// 🔹 Limpiar asistencias huérfanas (de estudiantes que ya no existen)
fun limpiarAsistenciasHuerfanas(
    nominaId: String,
    fecha: String,
    alumnosActuales: List<EstudianteAsistencia>
) {
    val db = FirebaseFirestore.getInstance()
    val rutaAsistencias = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)
        .collection("asistencias")
        .document(fecha)

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

// 🔹 Guardar asistencia usando ID único
fun guardarAsistenciaFirestore(
    nominaId: String,
    fecha: String,
    alumnos: List<EstudianteAsistencia>,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val rutaAsistencias = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)
        .collection("asistencias")

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
    db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
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
