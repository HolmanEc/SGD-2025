package com.holman.sgd.resources.nominas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.resources.NominaResumen
import com.holman.sgd.resources.cargarNominasDesdeFirestore
import com.holman.sgd.resources.mensajealert
import com.holman.sgd.ui.theme.ButtonDarkError
import com.holman.sgd.ui.theme.ButtonDarkPrimary
import kotlin.collections.plus
import kotlin.text.ifEmpty
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.firestore.CollectionReference
import com.holman.sgd.resources.LoadingDotsOverlay
import com.holman.sgd.ui.theme.ButtonDarkGray
import com.holman.sgd.ui.theme.ButtonDarkSuccess
import kotlinx.coroutines.launch
import com.holman.sgd.resources.screens.isTablet
import com.holman.sgd.ui.theme.BackgroundDefault
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import com.holman.sgd.resources.NominaHeaderCard
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.holman.sgd.resources.FloatingSaveButton
import com.holman.sgd.resources.InfoItem
import com.holman.sgd.resources.NominaReviewCard
import com.holman.sgd.ui.theme.TextDefaultBlack
import androidx.compose.ui.zIndex
import com.google.firebase.firestore.SetOptions
import com.holman.sgd.resources.FondoScreenDefault
import com.holman.sgd.resources.TituloScreenNominas
import com.holman.sgd.resources.calificaciones.TablaConfig
import com.holman.sgd.resources.components.ContenedorPrincipal
import java.util.UUID




private val SECCIONES_CALIF = listOf("PrimerTrimestre", "SegundoTrimestre", "TercerTrimestre", "Informe")
private const val SUBCOLECCION_ALUMNOS = "insumos"

fun syncCalificacionesSeccionesConTabla(
    nominaId: String,
    estudiantesActuales: List<EstudianteNomina>,
    insumosCount: Int = TablaConfig.INSUMOS_COUNT,
    onDone: (Boolean, String?) -> Unit = { _, _ -> }
) {
    val db = FirebaseFirestore.getInstance()
    val nominaRef = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)

    val idsActuales = estudiantesActuales.map { it.idUnico }.toSet()
    val now = System.currentTimeMillis()

    // 1) Leer/crear documentos base de cada sección (por si no existen)
    val baseWrites = SECCIONES_CALIF.map { sec ->
        val secDoc = nominaRef.collection("calificaciones").document(sec)
        val base = mutableMapOf<String, Any>(
            "seccion" to sec,
            "tipo" to if (sec == "Informe") "informe" else "trimestre",
            "updatedAt" to now
        )
        if (sec != "Informe") {
            base["insumosCount"] = insumosCount
            base["weights"] = mapOf("formativa" to 0.7, "sumativa" to 0.3)
        }
        secDoc.set(base, SetOptions.merge())
    }

    com.google.android.gms.tasks.Tasks.whenAllComplete(baseWrites)
        .addOnSuccessListener {
            // 2) Por cada sección:
            //    - Leer alumnos existentes
            //    - Borrar huérfanos
            //    - Crear faltantes (payload por tipo)
            fun procesarSeccion(idx: Int) {
                if (idx >= SECCIONES_CALIF.size) {
                    onDone(true, null); return
                }
                val sec = SECCIONES_CALIF[idx]
                val secDoc = nominaRef.collection("calificaciones").document(sec)
                val colAlumnos = secDoc.collection(SUBCOLECCION_ALUMNOS)

                colAlumnos.get()
                    .addOnSuccessListener { snap ->
                        val existentesIds = snap.documents.map { it.id }.toSet()

                        val batchOps = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Any?>>()

                        // 2.a) Borrar huérfanos
                        snap.documents.forEach { d ->
                            if (d.id !in idsActuales) {
                                batchOps += d.reference to null // null = delete
                            }
                        }

                        // 2.b) Crear faltantes / asegurar esquema
                        estudiantesActuales.forEachIndexed { index, est ->
                            val alumnoRef = colAlumnos.document(est.idUnico)
                            if (est.idUnico !in existentesIds) {
                                val data: Map<String, Any?> =
                                    if (sec == "Informe") {
                                        mapOf(
                                            "seccion" to sec,
                                            "numero" to (index + 1),
                                            "cedula" to est.cedula,
                                            "nombre" to est.nombre,
                                            "PromedioT1" to null,
                                            "PromedioT2" to null,
                                            "PromedioT3" to null,
                                            "Supletorio" to null,
                                            "PromedioFinal" to null,
                                            "createdAt" to now,
                                            "updatedAt" to now
                                        )
                                    } else {
                                        mapOf(
                                            "seccion" to sec,
                                            "numero" to (index + 1),
                                            "cedula" to est.cedula,
                                            "nombre" to est.nombre,
                                            "actividades" to List(insumosCount) { null },
                                            "proyecto" to null,
                                            "evaluacion" to null,
                                            "refuerzo" to null,
                                            "mejora" to null,
                                            "createdAt" to now,
                                            "updatedAt" to now
                                        )
                                    }
                                batchOps += alumnoRef to data
                            } else {
                                // Existe: opcionalmente actualiza número/cedula/nombre (merge)
                                val merge = mapOf(
                                    "numero" to (index + 1),
                                    "cedula" to est.cedula,
                                    "nombre" to est.nombre,
                                    "updatedAt" to now
                                )
                                batchOps += alumnoRef to merge
                            }
                        }

                        // 2.c) Commit en lotes (≤450 sets/updates/deletes por batch)
                        val MAX = 450
                        val chunks = batchOps.chunked(MAX)

                        fun commitChunk(cidx: Int) {
                            if (cidx >= chunks.size) {
                                // siguiente sección
                                procesarSeccion(idx + 1); return
                            }
                            val b = db.batch()
                            chunks[cidx].forEach { (ref, payload) ->
                                if (payload == null) b.delete(ref)
                                else b.set(ref, payload, SetOptions.merge())
                            }
                            b.commit()
                                .addOnSuccessListener { commitChunk(cidx + 1) }
                                .addOnFailureListener { e -> onDone(false, e.localizedMessage) }
                        }
                        if (chunks.isEmpty()) procesarSeccion(idx + 1) else commitChunk(0)
                    }
                    .addOnFailureListener { e -> onDone(false, e.localizedMessage) }
            }

            procesarSeccion(0)
        }
        .addOnFailureListener { e -> onDone(false, e.localizedMessage) }
}

/////



@Composable
fun revisarNomina(onBack: () -> Unit)
{
    val context = LocalContext.current

    var nominas by remember { mutableStateOf<List<NominaResumen>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // ⬇️ ESTADOS PERSISTENTES (sobreviven rotación)
    var nominaIdAbierta by rememberSaveable { mutableStateOf<String?>(null) }
    var headerColorArgb by rememberSaveable { mutableStateOf<Int?>(null) }

    // Estados de navegación interna (no necesitan persistir como objeto)
    // ❌ var nominaAEditar by remember { ... }  ← ya no se usa
    // ❌ var headerColorSelected by remember { ... }  ← ya no se usa

    // Confirmación de borrado
    var nominaAEliminar by remember { mutableStateOf<NominaResumen?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    // Pre-carga antes de abrir el detalle
    var preloading by remember { mutableStateOf(false) }
    var preloadedEstudiantes by remember { mutableStateOf<List<EstudianteNomina>?>(null) }

    val isBusy by remember { derivedStateOf { isLoading || isDeleting || preloading } }

    // Modal edición de metadatos
    var nominaParaEditar by remember { mutableStateOf<NominaResumen?>(null) }

    // Back jerárquico
    BackHandler(enabled = true) {
        when {
            isBusy -> Unit
            nominaParaEditar != null -> nominaParaEditar = null
            nominaAEliminar != null -> nominaAEliminar = null
            nominaIdAbierta != null -> {
                // cerrar detalle persistente
                nominaIdAbierta = null
                headerColorArgb = null
                preloadedEstudiantes = null
            }
            else -> onBack()
        }
    }

    // Cargar nóminas
    LaunchedEffect(Unit) {
        cargarNominasDesdeFirestore(
            onSuccess = {
                nominas = it
                isLoading = false
            },
            onError = {
                error = it
                isLoading = false
            }
        )
    }

    // ⬇️ SI HAY DETALLE ABIERTO, muéstralo (y NO vuelvas a la lista aunque nominas todavía cargue)
    nominaIdAbierta?.let { abiertoId ->
        val nominaEncontrada = nominas.firstOrNull { it.id == abiertoId }
        if (nominaEncontrada == null) {
            // Aún no se reconstruye la nómina desde la lista tras rotación: muestra loader, no la lista
            Box(Modifier.fillMaxSize()) {
                FondoScreenDefault()
                LoadingDotsOverlay(isLoading = true)
            }
            return
        } else {
            ScreenRevisarDetalleNomina(
                nomina = nominaEncontrada,
                headerColor = headerColorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                initialEstudiantes = preloadedEstudiantes,
                onBack = {
                    nominaIdAbierta = null
                    headerColorArgb = null
                    preloadedEstudiantes = null
                }
            )
            return
        }
    }

    // ===== LISTA (tu diseño original) =====
    Box(modifier = Modifier.fillMaxSize())
    {
        FondoScreenDefault()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ContenedorPrincipal),
            verticalArrangement = Arrangement.Top
        )  {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopStart
            ) {
                when {
                    isLoading -> {
                        Spacer(Modifier.size(1.dp))
                    }
                    error != null -> Text(
                        text = "❌ Error: $error",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                    nominas.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        TituloScreenNominas(texto = "No hay nóminas Guardadas")
                    }
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            //.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    ) {
                        TituloScreenNominas(texto = "Nóminas Guardadas")
                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            ListaNominas(
                                nominas = nominas,
                                onRevisar = { n, color ->
                                    if (isBusy) return@ListaNominas

                                    preloading = true
                                    preloadedEstudiantes = null

                                    cargarEstudiantesDeNomina(
                                        nominaId = n.id,
                                        onSuccess = { lista ->
                                            preloadedEstudiantes = lista
                                            preloading = false

                                            // ⬇️ Persistimos PRIMITIVOS (sobreviven a rotación)
                                            nominaIdAbierta = n.id
                                            headerColorArgb = color.toArgb()
                                        },
                                        onError = { msg ->
                                            preloading = false
                                            mensajealert(context, "❌ $msg")
                                        }
                                    )
                                },
                                onBorrar = { if (!isBusy) nominaAEliminar = it },
                                isBusy = isBusy,
                                onEditarDatos = { if (!isBusy) nominaParaEditar = it }
                            )
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
                    onClick = { if (!isBusy) onBack() }
                )
            }
        }

        LoadingDotsOverlay(isLoading = isBusy)
    }

    // —— Modal Confirmación Borrar
    nominaAEliminar?.let { nomina ->
        ModalConfirmarBorrar(
            nomina = nomina,
            onConfirm = {
                nominaAEliminar = null
                isDeleting = true
                eliminarNominaFirebaseCompleta(
                    nominaId = nomina.id,
                    subcolecciones = listOf("asistencias", "calificaciones"),
                    onSuccess = {
                        nominas = nominas.filterNot { it.id == nomina.id }
                        isDeleting = false
                        mensajealert(context, "✅  La nómina ha sido eliminada completamente.")
                        if (nominaIdAbierta == nomina.id) {
                            nominaIdAbierta = null
                            headerColorArgb = null
                            preloadedEstudiantes = null
                        }
                    },
                    onError = { msg ->
                        isDeleting = false
                        mensajealert(context, "❌  Error al eliminar: $msg")
                    }
                )
            },
            onCancel = { nominaAEliminar = null }
        )
    }

    // —— Modal Editar Metadatos
    nominaParaEditar?.let { n ->
        ModalEditarNomina(
            nomina = n,
            onDismiss = { nominaParaEditar = null },
            onSaved = { actualizada ->
                nominas = nominas.map { if (it.id == actualizada.id) actualizada else it }
                nominaParaEditar = null
                mensajealert(context, "✅  Nómina actualizada correctamente.")
            }
        )
    }
}

@Composable
fun ListaNominas(
    nominas: List<NominaResumen>,
    onRevisar: (NominaResumen, Color) -> Unit,
    onBorrar: (NominaResumen) -> Unit,
    isBusy: Boolean = false,
    onEditarDatos: (NominaResumen) -> Unit
) {
    val isTablet = isTablet()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        itemsIndexed(nominas) { index, nomina ->
            NominaReviewCard(
                nomina = nomina,
                index = index,
                isTablet = isTablet,
                isBusy = isBusy,
                onRevisar = onRevisar,
                onBorrar = onBorrar,
                onEditar = { onEditarDatos(nomina) } // 👈 dispara el modal de edición
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ScreenRevisarDetalleNomina(
    nomina: NominaResumen,
    headerColor: Color,
    initialEstudiantes: List<EstudianteNomina>? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var estudiantes by remember { mutableStateOf<List<EstudianteNomina>>(initialEstudiantes ?: emptyList()) }
    var estudiantesOriginal by remember { mutableStateOf<List<EstudianteNomina>>(initialEstudiantes ?: emptyList()) }
    var isLoading by remember { mutableStateOf(initialEstudiantes == null) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingField by remember { mutableStateOf<Pair<Int, String>?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var scrollToId by remember { mutableStateOf<String?>(null) }

    val fontSizeEstudiantes = 14.sp
    val isBusy by remember { derivedStateOf { isLoading || isSaving } }

    fun normalize(list: List<EstudianteNomina>) =
        list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nombre.trim() })
            .map { it.copy(nombre = it.nombre.trim(), cedula = it.cedula.trim()) }

    fun hasChanges(): Boolean = normalize(estudiantes) != normalize(estudiantesOriginal)

    fun guardarAhora() {
        if (!hasChanges()) {
            mensajealert(context, "ℹ️ No hay cambios para guardar.")
            return
        }
        val hayVacios = estudiantes.any { it.cedula.isBlank() || it.nombre.isBlank() }
        if (hayVacios) {
            mensajealert(context, "⚠️ Completa cédula y nombre en todos los estudiantes.")
            return
        }

        isSaving = true
        val estudiantesOrdenados = normalize(estudiantes)

        actualizarEstudiantesDeNomina(
            nomina.id,
            estudiantesOrdenados,
            onSuccess = {
                mensajealert(context, "✅ Datos actualizados")
                editingField = null
                cargarEstudiantesDeNomina(
                    nominaId = nomina.id,
                    onSuccess = { lista ->
                        estudiantes = lista
                        estudiantesOriginal = lista
                        coroutineScope.launch { listState.scrollToItem(0) }
                        isSaving = false
                    },
                    onError = { msg ->
                        mensajealert(context, "⚠️ No se pudo recargar: $msg")
                        isSaving = false
                    }
                )
            },
            onError = { msg ->
                mensajealert(context, "❌ Error: $msg")
                isSaving = false
            }
        )
    }

    // Back dentro del detalle
    BackHandler(enabled = true) {
        if (isBusy) return@BackHandler
        onBack()
    }

    LaunchedEffect(scrollToId, estudiantes) {
        val id = scrollToId ?: return@LaunchedEffect
        val idx = estudiantes.indexOfFirst { it.idUnico == id }
        if (idx >= 0) listState.animateScrollToItem(idx)
        scrollToId = null
    }

    if (initialEstudiantes == null) {
        LaunchedEffect(nomina.id) {
            cargarEstudiantesDeNomina(
                nominaId = nomina.id,
                onSuccess = {
                    estudiantes = it
                    estudiantesOriginal = it
                    isLoading = false
                },
                onError = {
                    error = it
                    isLoading = false
                }
            )
        }
    }

    Scaffold(containerColor = BackgroundDefault) { _ ->

        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(color = TextDefaultBlack)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                FondoScreenDefault()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ContenedorPrincipal)
                ) {
                    NominaHeaderCard(
                        nomina = nomina,
                        backgroundColor = headerColor,
                        onClick = null
                    )

                    Spacer(Modifier.height(12.dp))

                    when {
                        isLoading -> {
                            Spacer(Modifier.height(1.dp))
                        }
                        error != null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("❌ Error: $error")
                        }
                        else -> {
                            // Encabezado de columnas
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(headerColor, shape = RoundedCornerShape(8.dp))
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CÉDULA",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = fontSizeEstudiantes,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .weight(0.25f)
                                        .fillMaxWidth()
                                )
                                Text(
                                    text = "ESTUDIANTE",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = fontSizeEstudiantes,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .weight(0.6f)
                                        .fillMaxWidth()
                                )
                                Text(
                                    text = "ACCIÓN",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = fontSizeEstudiantes,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .weight(0.15f)
                                        .fillMaxWidth()
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Lista principal
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(headerColor, shape = RoundedCornerShape(8.dp))
                                    .padding(vertical = 16.dp, horizontal = 16.dp),
                            ) {
                                itemsIndexed(estudiantes, key = { _, it -> it.idUnico }) { index, est ->
                                    val isCedulaEditing = editingField?.first == index && editingField?.second == "cedula"
                                    val isNombreEditing = editingField?.first == index && editingField?.second == "nombre"

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 🔹 CÉDULA
                                        if (isCedulaEditing) {
                                            val focusRequesterCedula = remember { FocusRequester() }
                                            OutlinedTextField(
                                                value = est.cedula,
                                                onValueChange = { nuevo ->
                                                    val filtrado = nuevo.uppercase().filter { it.isLetterOrDigit() }
                                                    estudiantes = estudiantes.toMutableList().also { lista ->
                                                        lista[index] = lista[index].copy(cedula = filtrado)
                                                    }
                                                },
                                                label = { Text("Cédula", fontSize = fontSizeEstudiantes) },
                                                textStyle = LocalTextStyle.current.copy(
                                                    fontSize = fontSizeEstudiantes,
                                                    textAlign = TextAlign.Center
                                                ),
                                                singleLine = true,
                                                enabled = !isBusy,
                                                modifier = Modifier
                                                    .weight(0.25f)
                                                    .fillMaxWidth()
                                                    .focusRequester(focusRequesterCedula)
                                                    .onFocusChanged { if (it.isFocused) editingField = index to "cedula" },
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Ascii,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(onDone = { editingField = null })
                                            )
                                            LaunchedEffect(Unit) { focusRequesterCedula.requestFocus() }
                                        } else {
                                            Text(
                                                text = est.cedula.ifEmpty { "Cédula" },
                                                fontSize = fontSizeEstudiantes,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .weight(0.25f)
                                                    .fillMaxWidth()
                                                    .clickable(enabled = !isBusy) { editingField = index to "cedula" }
                                            )
                                        }

                                        // 🔹 NOMBRE
                                        if (isNombreEditing) {
                                            val focusRequesterNombre = remember { FocusRequester() }
                                            OutlinedTextField(
                                                value = est.nombre,
                                                onValueChange = { nuevo ->
                                                    val filtrado = nuevo.uppercase().filter { it.isLetter() || it.isWhitespace() }
                                                    estudiantes = estudiantes.toMutableList().also { lista ->
                                                        lista[index] = lista[index].copy(nombre = filtrado)
                                                    }
                                                },
                                                label = { Text("Nombre", fontSize = fontSizeEstudiantes) },
                                                textStyle = LocalTextStyle.current.copy(
                                                    fontSize = fontSizeEstudiantes,
                                                    textAlign = TextAlign.Left
                                                ),
                                                singleLine = true,
                                                enabled = !isBusy,
                                                modifier = Modifier
                                                    .weight(0.6f)
                                                    .fillMaxWidth()
                                                    .focusRequester(focusRequesterNombre)
                                                    .onFocusChanged { if (it.isFocused) editingField = index to "nombre" },
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Text,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(onDone = { editingField = null })
                                            )
                                            LaunchedEffect(Unit) { focusRequesterNombre.requestFocus() }
                                        } else {
                                            Text(
                                                text = est.nombre.ifEmpty { "Nombre" },
                                                fontSize = fontSizeEstudiantes,
                                                textAlign = TextAlign.Left,
                                                modifier = Modifier
                                                    .weight(0.6f)
                                                    .fillMaxWidth()
                                                    .clickable(enabled = !isBusy) { editingField = index to "nombre" }
                                                    .padding(start = 32.dp)
                                            )
                                        }

                                        // 🔹 ACCIÓN
                                        Box(
                                            modifier = Modifier
                                                .weight(0.15f)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (isBusy) return@IconButton
                                                    editingField = null
                                                    estudiantes = estudiantes.toMutableList().also { it.removeAt(index) }
                                                },
                                                enabled = !isBusy
                                            ) {
                                                Icon(Icons.Default.Delete, "Eliminar estudiante", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // *** ZONA DE ACCIONES AL FONDO ***
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Fila 1: SOLO "Agregar estudiante"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CustomButton(
                                        text = "Agregar estudiante",
                                        borderColor = ButtonDarkSuccess,
                                        onClick = {
                                            if (isBusy) return@CustomButton
                                            val hayVacios = estudiantes.any { it.cedula.isBlank() || it.nombre.isBlank() }
                                            if (hayVacios) {
                                                mensajealert(context, "⚠️ Completa el registro vacío antes de agregar otro.")
                                                return@CustomButton
                                            }
                                            showAddDialog = true
                                        }
                                    )
                                }

                                // Fila 2: DOS COLUMNAS: Guardar | Volver
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.weight(1f)) {
                                        CustomButton(
                                            text = if (isSaving) "Guardando..." else "Guardar",
                                            borderColor = ButtonDarkPrimary,
                                            onClick = { if (!isBusy) guardarAhora() }
                                        )
                                    }
                                    Box(Modifier.weight(1f)) {
                                        CustomButton(
                                            text = "Volver",
                                            borderColor = ButtonDarkGray,
                                            onClick = { if (!isBusy) onBack() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAddDialog) {
                    AddEstudianteDialog(
                        onDismiss = { showAddDialog = false },
                        onConfirm = { ced, nom ->
                            val existe = estudiantes.any {
                                it.cedula.equals(ced, ignoreCase = true) &&
                                        it.nombre.equals(nom, ignoreCase = true)
                            }
                            if (existe) {
                                mensajealert(context, "⚠️ Ya existe un estudiante con esos datos.")
                                return@AddEstudianteDialog
                            }
                            val nuevo = EstudianteNomina(
                                idUnico = generarIdUnicoEstudianteNomina(ced, nom),
                                cedula = ced,
                                nombre = nom
                            )
                            val listaOrdenada = (estudiantes + nuevo)
                                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nombre.trim() })

                            estudiantes = listaOrdenada
                            showAddDialog = false
                            scrollToId = nuevo.idUnico
                        }
                    )
                }

                LoadingDotsOverlay(isLoading = isBusy)
            }
        }
    }
}


////
fun existeNominaIgualPorCampos(
    institucion: String,
    docente: String,
    curso: String,
    paralelo: String,
    asignatura: String,
    especialidad: String,
    periodo: String,
    excluirId: String?,
    onResult: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    // 🔤 Normalizamos todos los campos a minúsculas antes de consultar
    val instNorm = institucion.trim().lowercase()
    val docNorm = docente.trim().lowercase()
    val cursoNorm = curso.trim().lowercase()
    val paraleloNorm = paralelo.trim().lowercase()
    val asigNorm = asignatura.trim().lowercase()
    val especNorm = especialidad.trim().lowercase()
    val periodoNorm = periodo.trim().lowercase()

    db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .get()
        .addOnSuccessListener { snap ->
            val hayDuplicado = snap.documents.any { doc ->
                if (doc.id == excluirId) return@any false
                val d = doc.data ?: return@any false

                (d["institucion"] as? String)?.trim()?.lowercase() == instNorm &&
                        (d["docente"] as? String)?.trim()?.lowercase() == docNorm &&
                        (d["curso"] as? String)?.trim()?.lowercase() == cursoNorm &&
                        (d["paralelo"] as? String)?.trim()?.lowercase() == paraleloNorm &&
                        (d["asignatura"] as? String)?.trim()?.lowercase() == asigNorm &&
                        (d["especialidad"] as? String)?.trim()?.lowercase() == especNorm &&
                        (d["periodo"] as? String)?.trim()?.lowercase() == periodoNorm
            }

            onResult(hayDuplicado)
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error verificando duplicados")
        }
}

fun actualizarDatosNomina(
    nominaId: String,
    nominaActualizada: NominaResumen,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    val data = mapOf(
        "institucion" to nominaActualizada.institucion,
        "docente" to nominaActualizada.docente,
        "curso" to nominaActualizada.curso,
        "paralelo" to nominaActualizada.paralelo,
        "asignatura" to nominaActualizada.asignatura,
        "especialidad" to nominaActualizada.especialidad,
        "periodo" to nominaActualizada.periodo
    )

    val docNomina = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)

    docNomina.set(data, SetOptions.merge())
        .addOnSuccessListener {
            // Si mantienes otra colección "resumenNominas", duplica aquí el set() con merge.
            onSuccess()
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error al guardar")
        }
}

fun asegurarCalificacionesParaEstudiantes(
    nominaId: String,
    estudiantesActuales: List<EstudianteNomina>,
    onDone: () -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    val nominaRef = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)

    val califRef = nominaRef.collection("calificaciones")

    // 1) Leer _config para saber cuántos insumos formativos usar (fallback si no está)
    califRef.document("_config").get().addOnSuccessListener { cfg ->
        val insumosCount = (cfg.getLong("insumosCount") ?: TablaConfig.INSUMOS_COUNT.toLong()).toInt()

        // 2) Leer existentes para no duplicar
        califRef.get().addOnSuccessListener { snap ->
            val existentes = snap.documents.map { it.id }.toSet()
            val batch = db.batch()

            estudiantesActuales.forEachIndexed { index, est ->
                val id = est.idUnico
                if (id !in existentes) {
                    val docRef = califRef.document(id)
                    val data = mapOf(
                        "numero" to (index + 1),
                        "cedula" to est.cedula,
                        "nombre" to est.nombre,
                        "actividades" to List(insumosCount) { null },
                        "proyecto" to null,
                        "evaluacion" to null,
                        "refuerzo" to null,
                        "mejora" to null,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(docRef, data)
                }
            }

            if (!snap.isEmpty || estudiantesActuales.isNotEmpty()) {
                batch.commit().addOnCompleteListener { onDone() }
            } else {
                onDone()
            }
        }.addOnFailureListener { onDone() }
    }.addOnFailureListener { onDone() }
}
data class EstudianteNomina(
    val idUnico: String,    // ID único basado en cédula
    var cedula: String,
    var nombre: String
)

fun generarIdUnicoEstudianteNomina(cedula: String, nombre: String): String {
    return "legacy_${(cedula.ifEmpty { nombre }).uppercase().hashCode()}"
}

fun cargarEstudiantesDeNomina(
    nominaId: String,
    onSuccess: (List<EstudianteNomina>) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)
        .get()
        .addOnSuccessListener { doc ->
            if (!doc.exists()) {
                onError("No existe la nómina"); return@addOnSuccessListener
            }

            // ✅ Cast seguro: List<*> -> Map<*, *> -> Map<String, Any?>
            val tabla: List<Map<String, Any?>> =
                (doc.get("tabla") as? List<*>)?.mapNotNull { item ->
                    (item as? Map<*, *>)?.let { raw ->
                        // Normalizamos solo las claves que nos interesan
                        mapOf(
                            "col1" to (raw["col1"] as? String),      // ID
                            "col2" to (raw["col2"] as? String),      // Nro
                            "col3" to (raw["col3"] as? String),      // Cédula
                            "col4" to (raw["col4"] as? String),      // Estudiante
                            // Fallbacks legacy por si existen claves con nombres antiguos
                            "Cédula" to (raw["Cédula"] as? String),
                            "Estudiante" to (raw["Estudiante"] as? String)
                        )
                    }
                } ?: emptyList()

            if (tabla.isEmpty()) {
                onSuccess(emptyList()); return@addOnSuccessListener
            }

            // Saltar encabezado si lo hay
            val filas = tabla.drop(1)

            val lista = filas.map { fila ->
                val idPersistido  = (fila["col1"] as? String)?.trim().orEmpty()
                val cedPersistida = (fila["col3"] as? String)?.trim().orEmpty()
                val nomPersistido = (fila["col4"] as? String)?.trim().orEmpty()

                // Fallback legacy
                val cedula = if (cedPersistida.isNotEmpty())
                    cedPersistida
                else
                    ((fila["Cédula"] ?: fila["col2"] ?: "") as? String)?.trim().orEmpty()

                val nombre = if (nomPersistido.isNotEmpty())
                    nomPersistido
                else
                    ((fila["Estudiante"] ?: fila["col4"] ?: "") as? String)?.trim().orEmpty()

                val idUnico = if (idPersistido.isNotEmpty()) idPersistido
                else generarIdUnicoEstudianteNomina(cedula, nombre) // legacy estable

                EstudianteNomina(
                    idUnico = idUnico,
                    cedula = cedula,
                    nombre = nombre
                )
            }

            onSuccess(lista)
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error al leer nómina")
        }
}

fun actualizarEstudiantesDeNomina(
    nominaId: String,
    estudiantes: List<EstudianteNomina>,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val nominaRef = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)

    // Construir tabla con el nuevo orden (col1=ID, col2=Nro, col3=Cédula, col4=Estudiante)
    val encabezado = mapOf(
        "col1" to "ID",
        "col2" to "Nro",
        "col3" to "Cédula",
        "col4" to "Estudiante"
    )

    val filas = listOf(encabezado) + estudiantes.mapIndexed { index, est ->
        mapOf(
            "col1" to est.idUnico,
            "col2" to (index + 1).toString(),
            "col3" to est.cedula,
            "col4" to est.nombre
        )
    }

    nominaRef.update("tabla", filas)
        .addOnSuccessListener {
            // 1) Sincroniza calificaciones (crea faltantes y borra huérfanos en TODAS las secciones)
            syncCalificacionesSeccionesConTabla(
                nominaId = nominaId,
                estudiantesActuales = estudiantes,
                insumosCount = TablaConfig.INSUMOS_COUNT
            ) { ok, err ->
                if (!ok) {
                    onError(err ?: "No se pudo sincronizar calificaciones")
                    return@syncCalificacionesSeccionesConTabla
                }
                // 2) Limpia asistencias huérfanas (documentos por fecha)
                limpiarTodasLasAsistenciasHuerfanas(
                    nominaId = nominaId,
                    estudiantesActuales = estudiantes
                )
                onSuccess()
            }
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error al actualizar la tabla de la nómina")
        }
}

fun limpiarTodasLasAsistenciasHuerfanas(
    nominaId: String,
    estudiantesActuales: List<EstudianteNomina>
) {
    val db = FirebaseFirestore.getInstance()
    val rutaAsistencias = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)
        .collection("asistencias")

    // Obtener todos los documentos de asistencias
    rutaAsistencias.get().addOnSuccessListener { querySnapshot ->
        val idsActuales = estudiantesActuales.map { it.idUnico }.toSet()
        val nombresActuales = estudiantesActuales.map { it.nombre }.toSet()

        querySnapshot.documents.forEach { document ->
            val asistenciaActual = document.data?.toMutableMap() ?: mutableMapOf()
            var huboLimpieza = false

            val iterator = asistenciaActual.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val claveAsistencia = entry.key

                // Verificar si la clave corresponde a un estudiante actual
                val esEstudianteActual = idsActuales.contains(claveAsistencia) ||
                        nombresActuales.contains(claveAsistencia)

                if (!esEstudianteActual) {
                    iterator.remove()
                    huboLimpieza = true
                }
            }

            // Actualizar documento si hubo limpieza
            if (huboLimpieza) {
                if (asistenciaActual.isEmpty()) {
                    document.reference.delete()
                } else {
                    document.reference.set(asistenciaActual)
                }
            }
        }
    }
}

fun limpiarCalificacionesHuerfanas(
    nominaId: String,
    estudiantesActuales: List<EstudianteNomina>
) {
    val db = FirebaseFirestore.getInstance()
    val idsActuales = estudiantesActuales.map { it.idUnico }.toSet()

    val colCalificaciones = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)
        .collection("calificaciones")

    colCalificaciones.get().addOnSuccessListener { snap ->
        val batch = db.batch()
        snap.documents.forEach { doc ->
            val id = doc.id
            if (!idsActuales.contains(id)) {
                batch.delete(doc.reference) // ← borrar huérfano
            }
        }
        if (!snap.isEmpty) {
            batch.commit()
        }
    }
}

fun eliminarNominaFirebaseCompleta(
    nominaId: String,
    subcolecciones: List<String> = listOf("asistencias", "calificaciones"),
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val docRef = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)

    fun borrarColeccionPlano(colRef: CollectionReference, onSubDone: () -> Unit, onSubError: (Exception) -> Unit) {
        colRef.limit(500).get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) { onSubDone(); return@addOnSuccessListener }
                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener { borrarColeccionPlano(colRef, onSubDone, onSubError) }
                    .addOnFailureListener(onSubError)
            }
            .addOnFailureListener(onSubError)
    }

    fun borrarColeccionCalificacionesDeep(califRef: CollectionReference, onSubDone: () -> Unit, onSubError: (Exception) -> Unit) {
        califRef.get()
            .addOnSuccessListener { snap ->
                // Borra subcolecciones "insumos" de cada sección y luego el doc de sección
                fun procesarSeccion(i: Int) {
                    if (i >= snap.documents.size) { onSubDone(); return }
                    val secDoc = snap.documents[i]
                    if (secDoc.id.startsWith("_")) {
                        // docs meta; delete directo
                        secDoc.reference.delete()
                            .addOnSuccessListener { procesarSeccion(i + 1) }
                            .addOnFailureListener(onSubError)
                        return
                    }
                    val alumnosCol = secDoc.reference.collection(SUBCOLECCION_ALUMNOS)
                    borrarColeccionPlano(alumnosCol,
                        onSubDone = {
                            // después de limpiar alumnos, borra el doc de la sección
                            secDoc.reference.delete()
                                .addOnSuccessListener { procesarSeccion(i + 1) }
                                .addOnFailureListener(onSubError)
                        },
                        onSubError = onSubError
                    )
                }
                procesarSeccion(0)
            }
            .addOnFailureListener(onSubError)
    }

    fun borrarSiguienteSubcoleccion(index: Int) {
        if (index >= subcolecciones.size) {
            docRef.delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error al borrar nómina") }
            return
        }
        val nombre = subcolecciones[index]
        val colRef = docRef.collection(nombre)
        if (nombre == "calificaciones") {
            borrarColeccionCalificacionesDeep(
                califRef = colRef,
                onSubDone = { borrarSiguienteSubcoleccion(index + 1) },
                onSubError = { e -> onError(e.localizedMessage ?: "Error al borrar calificaciones") }
            )
        } else {
            borrarColeccionPlano(
                colRef = colRef,
                onSubDone = { borrarSiguienteSubcoleccion(index + 1) },
                onSubError = { e -> onError(e.localizedMessage ?: "Error al borrar subcolección: $nombre") }
            )
        }
    }
    borrarSiguienteSubcoleccion(0)
}

@Composable
fun ModalConfirmarBorrar(
    nomina: NominaResumen,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    headerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val shape = RoundedCornerShape(12.dp)
    val isTablet = isTablet() // 👈 Usa tu misma función de detección

    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = shape,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            color = BackgroundDefault,
            contentColor = TextDefaultBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 40.dp)
            ) {
                // 🏷️ Título
                Text(
                    text = "¿Está seguro de borrar la nómina?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = LocalContentColor.current.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(12.dp))

                // ⚠️ Banda de alerta centrada
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Advertencia",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Esta acción es irreversible.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 📋 Resumen responsive
                if (isTablet) {
                    // ✅ DOBLE COLUMNA en tablet
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InfoItem(Icons.Default.School, "Institución", nomina.institucion)
                            InfoItem(Icons.Default.Person, "Docente", nomina.docente)
                            InfoItem(
                                Icons.Default.Class,
                                "Curso",
                                "${nomina.curso}  ${nomina.paralelo}"
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InfoItem(Icons.Default.AutoStories, "Asignatura", nomina.asignatura)
                            InfoItem(Icons.Default.Star, "Especialidad", nomina.especialidad)
                            InfoItem(Icons.Default.Event, "Periodo", nomina.periodo)
                        }
                    }
                } else {
                    // ✅ UNA SOLA COLUMNA en móvil
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        InfoItem(Icons.Default.School, "Institución", nomina.institucion)
                        InfoItem(Icons.Default.Person, "Docente", nomina.docente)
                        InfoItem(
                            Icons.Default.Class,
                            "Curso",
                            "${nomina.curso}  ${nomina.paralelo}"
                        )
                        InfoItem(Icons.Default.AutoStories, "Asignatura", nomina.asignatura)
                        InfoItem(Icons.Default.Star, "Especialidad", nomina.especialidad)
                        InfoItem(Icons.Default.Event, "Periodo", nomina.periodo)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 🔘 Acciones (50/50)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        CustomButton(
                            text = "Sí, borrar",
                            borderColor = ButtonDarkError,
                            onClick = onConfirm
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        CustomButton(
                            text = "Cancelar",
                            borderColor = ButtonDarkGray,
                            onClick = onCancel
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ModalEditarNomina(
    nomina: NominaResumen,
    onDismiss: () -> Unit,
    onSaved: (NominaResumen) -> Unit
) {
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }

    var institucion by rememberSaveable(nomina.id) { mutableStateOf(nomina.institucion) }
    var docente by rememberSaveable(nomina.id) { mutableStateOf(nomina.docente) }
    var curso by rememberSaveable(nomina.id) { mutableStateOf(nomina.curso) }
    var paralelo by rememberSaveable(nomina.id) { mutableStateOf(nomina.paralelo) }
    var asignatura by rememberSaveable(nomina.id) { mutableStateOf(nomina.asignatura) }
    var especialidad by rememberSaveable(nomina.id) { mutableStateOf(nomina.especialidad) }
    var periodo by rememberSaveable(nomina.id) { mutableStateOf(nomina.periodo) }

    // Bloquea BACK mientras guarda
    BackHandler(enabled = isSaving) { /* no-op */ }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isSaving,
            dismissOnClickOutside = !isSaving,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.6f),
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                shape = RoundedCornerShape(12.dp),
                color = BackgroundDefault
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDefault)
                        .padding(vertical = 20.dp, horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 🔹 Título
                    Text(
                        text = "Editar datos de la nómina",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDefaultBlack
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(color = TextDefaultBlack.copy(alpha = 0.2f))

                    // 🔹 Campos
                    OutlinedTextField(
                        value = institucion,
                        onValueChange = { institucion = it },
                        label = { Text("Institución", color = TextDefaultBlack) },
                        singleLine = true,
                        readOnly = isSaving,
                        textStyle = LocalTextStyle.current.copy(color = TextDefaultBlack),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docente,
                        onValueChange = { docente = it },
                        label = { Text("Docente", color = TextDefaultBlack) },
                        singleLine = true,
                        readOnly = isSaving,
                        textStyle = LocalTextStyle.current.copy(color = TextDefaultBlack),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = curso,
                            onValueChange = { curso = it },
                            label = { Text("Curso", color = TextDefaultBlack) },
                            singleLine = true,
                            readOnly = isSaving,
                            textStyle = LocalTextStyle.current.copy(color = TextDefaultBlack),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paralelo,
                            onValueChange = { paralelo = it.uppercase() },
                            label = { Text("Paralelo", color = TextDefaultBlack) },
                            singleLine = true,
                            readOnly = isSaving,
                            textStyle = LocalTextStyle.current.copy(color = TextDefaultBlack),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = asignatura,
                        onValueChange = { asignatura = it },
                        label = { Text("Asignatura", color = TextDefaultBlack) },
                        singleLine = true,
                        readOnly = isSaving,
                        textStyle = LocalTextStyle.current.copy(color = TextDefaultBlack),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = especialidad,
                        onValueChange = { especialidad = it },
                        label = { Text("Especialidad", color = TextDefaultBlack) },
                        singleLine = true,
                        readOnly = isSaving,
                        textStyle = LocalTextStyle.current.copy(color = TextDefaultBlack),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = periodo,
                        onValueChange = { periodo = it },
                        label = { Text("Periodo", color = TextDefaultBlack) },
                        singleLine = true,
                        readOnly = isSaving,
                        textStyle = LocalTextStyle.current.copy(color = TextDefaultBlack),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = TextDefaultBlack.copy(alpha = 0.2f))
                    Spacer(Modifier.height(4.dp))

                    // 🔹 Botones (mantienen sus colores originales)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(Modifier.weight(1f)) {
                            CustomButton(
                                text = if (isSaving) "Guardando..." else "Guardar",
                                borderColor = ButtonDarkPrimary,
                                onClick = {
                                    if (isSaving) return@CustomButton

                                    val vacios = listOf(
                                        institucion, docente, curso, paralelo, asignatura, periodo
                                    ).any { it.trim().isEmpty() }

                                    if (vacios) {
                                        mensajealert(context, "⚠️ Completa todos los campos obligatorios.")
                                        return@CustomButton
                                    }

                                    isSaving = true
                                    val actualizado = nomina.copy(
                                        institucion = institucion.trim(),
                                        docente = docente.trim(),
                                        curso = curso.trim(),
                                        paralelo = paralelo.trim().uppercase(),
                                        asignatura = asignatura.trim(),
                                        especialidad = especialidad.trim(),
                                        periodo = periodo.trim()
                                    )

                                    existeNominaIgualPorCampos(
                                        institucion = actualizado.institucion,
                                        docente = actualizado.docente,
                                        curso = actualizado.curso,
                                        paralelo = actualizado.paralelo,
                                        asignatura = actualizado.asignatura,
                                        especialidad = actualizado.especialidad,
                                        periodo = actualizado.periodo,
                                        excluirId = nomina.id,
                                        onResult = { duplicada ->
                                            if (duplicada) {
                                                isSaving = false
                                                mensajealert(context, "⚠️ Ya existe una nómina con esos datos.")
                                            } else {
                                                actualizarDatosNomina(
                                                    nominaId = nomina.id,
                                                    nominaActualizada = actualizado,
                                                    onSuccess = {
                                                        isSaving = false
                                                        onSaved(actualizado)
                                                    },
                                                    onError = { msg ->
                                                        isSaving = false
                                                        mensajealert(context, "❌ Error al guardar: $msg")
                                                    }
                                                )
                                            }
                                        },
                                        onError = { msg ->
                                            isSaving = false
                                            mensajealert(context, "❌ No se pudo verificar duplicados: $msg")
                                        }
                                    )
                                }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            CustomButton(
                                text = "Cancelar",
                                borderColor = ButtonDarkGray,
                                onClick = { if (!isSaving) onDismiss() }
                            )
                        }
                    }
                }
            }

            // 🔹 Overlay al guardar
            if (isSaving) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) { awaitPointerEvent() }
                            }
                        }
                ) {
                    LoadingDotsOverlay(isLoading = true)
                }
            }
        }
    }
}


@Composable
private fun AddEstudianteDialog(
    onDismiss: () -> Unit,
    onConfirm: (cedula: String, nombre: String) -> Unit
) {
    var cedula by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val focusCedula = remember { FocusRequester() }
    val focusNombre = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            shape = RoundedCornerShape(12.dp),
            color = BackgroundDefault,
            modifier = Modifier
                .fillMaxWidth(if (isTablet()) 0.5f else 0.94f)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Nuevo estudiante",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cedula,
                    onValueChange = { input ->
                        cedula = input.uppercase().filter { it.isLetterOrDigit() }
                    },
                    label = { Text("Cédula") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusCedula)
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { input ->
                        nombre = input.uppercase().filter { it.isLetter() || it.isWhitespace() }
                    },
                    label = { Text("Nombre completo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (cedula.isBlank() || nombre.isBlank()) {
                                error = "Completa cédula y nombre."
                            } else onConfirm(cedula.trim(), nombre.trim())
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusNombre)
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) {
                        CustomButton(
                            text = "Cancelar",
                            borderColor = ButtonDarkGray,
                            onClick = onDismiss
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        CustomButton(
                            text = "Agregar",
                            borderColor = ButtonDarkSuccess,
                            onClick = {
                                if (cedula.isBlank() || nombre.isBlank()) {
                                    error = "Completa cédula y nombre."
                                } else onConfirm(cedula.trim(), nombre.trim())
                            }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { focusCedula.requestFocus() }
}






private fun idUnicoEstudiante(): String =
    "std_" + UUID.randomUUID().toString().replace("-", "").take(16)

fun crearEstudianteVacio(): EstudianteNomina =
    EstudianteNomina(idUnico = idUnicoEstudiante(), cedula = "", nombre = "")


// ------------



fun actualizarEstudiantesDeNominaX(
    nominaId: String,
    estudiantes: List<EstudianteNomina>,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val nominaRef = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)

    // Construir tabla con el nuevo orden (col1=ID, col2=Nro, col3=Cédula, col4=Estudiante)
    val encabezado = mapOf(
        "col1" to "ID",
        "col2" to "Nro",
        "col3" to "Cédula",
        "col4" to "Estudiante"
    )

    val filas = listOf(encabezado) + estudiantes.mapIndexed { index, est ->
        mapOf(
            "col1" to est.idUnico,                // ✅ ID inmutable (ya existente)
            "col2" to (index + 1).toString(),     // Nro
            "col3" to est.cedula,
            "col4" to est.nombre
        )
    }

    nominaRef.update("tabla", filas)
        .addOnSuccessListener {
            // Asegurar calificaciones y limpiar huérfanos (firmas iguales a tus funciones actuales)
            asegurarCalificacionesParaEstudiantes(
                nominaId = nominaId,
                estudiantesActuales = estudiantes
            ) {
                limpiarCalificacionesHuerfanas(
                    nominaId = nominaId,
                    estudiantesActuales = estudiantes
                )
                limpiarTodasLasAsistenciasHuerfanas(
                    nominaId = nominaId,
                    estudiantesActuales = estudiantes
                )
                onSuccess()
            }
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error al actualizar la tabla de la nómina")
        }
}

fun eliminarNominaFirebaseCompletaX(
    nominaId: String,
    subcolecciones: List<String> = listOf("asistencias", "calificaciones"),
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val docRef = db.collection("gestionAcademica")
        .document("gestionNominas")
        .collection("nominasEstudiantes")
        .document(nominaId)

    fun borrarSubcoleccionEnLotes(
        colRef: CollectionReference,
        onSubDone: () -> Unit,
        onSubError: (Exception) -> Unit
    ) {
        colRef.limit(500).get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    onSubDone(); return@addOnSuccessListener
                }
                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener { borrarSubcoleccionEnLotes(colRef, onSubDone, onSubError) }
                    .addOnFailureListener(onSubError)
            }
            .addOnFailureListener(onSubError)
    }

    fun borrarSiguienteSubcoleccion(index: Int) {
        if (index >= subcolecciones.size) {
            docRef.delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error al borrar nómina") }
            return
        }
        val nombre = subcolecciones[index]
        borrarSubcoleccionEnLotes(
            colRef = docRef.collection(nombre),
            onSubDone = { borrarSiguienteSubcoleccion(index + 1) },
            onSubError = { e -> onError(e.localizedMessage ?: "Error al borrar subcolección: $nombre") }
        )
    }
    borrarSiguienteSubcoleccion(0)
}




