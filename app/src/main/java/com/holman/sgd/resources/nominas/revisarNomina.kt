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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
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
import com.holman.sgd.resources.calificaciones.TablaConfig
import java.util.UUID

@Composable
fun revisarNomina(onBack: () -> Unit)
{
    val context = LocalContext.current

    var nominas by remember { mutableStateOf<List<NominaResumen>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Estados de navegación interna
    var nominaAEditar by remember { mutableStateOf<NominaResumen?>(null) }
    var headerColorSelected by remember { mutableStateOf<Color?>(null) }

    // Confirmación de borrado
    var nominaAEliminar by remember { mutableStateOf<NominaResumen?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    // 👉 Estados NUEVOS para pre-carga antes de abrir el detalle
    var preloading by remember { mutableStateOf(false) }
    var pendingNomina by remember { mutableStateOf<NominaResumen?>(null) }
    var pendingHeaderColor by remember { mutableStateOf<Color?>(null) }
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
            nominaAEditar != null -> {
                nominaAEditar = null
                headerColorSelected = null
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

    // Detalle abierto
    nominaAEditar?.let { nomina ->
        ScreenRevisarDetalleNomina(
            nomina = nomina,
            headerColor = headerColorSelected ?: MaterialTheme.colorScheme.primary,
            initialEstudiantes = preloadedEstudiantes,
            onBack = {
                nominaAEditar = null
                headerColorSelected = null
                preloadedEstudiantes = null
            }
        )
        return
    }

    // Lista (se mantiene visible aun durante la pre-carga)
    Box(modifier = Modifier.fillMaxSize())
    {
        FondoScreenDefault()
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
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
                        Text(
                            text = "📋 No hay nóminas guardadas.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    ) {
                        Text(
                            text = "📋 Nóminas Guardadas",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                                .padding(top = 16.dp, bottom = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            ListaNominas(
                                nominas = nominas,
                                onRevisar = { n, color ->
                                    if (isBusy) return@ListaNominas

                                    // PRE-CARGA antes de navegar al detalle:
                                    preloading = true
                                    pendingNomina = n
                                    pendingHeaderColor = color
                                    preloadedEstudiantes = null

                                    cargarEstudiantesDeNomina(
                                        nominaId = n.id,
                                        onSuccess = { lista ->
                                            preloadedEstudiantes = lista
                                            preloading = false

                                            // Ya con datos → abrir el detalle
                                            nominaAEditar = pendingNomina
                                            headerColorSelected = pendingHeaderColor

                                            // limpiar pendings
                                            pendingNomina = null
                                            pendingHeaderColor = null
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CustomButton(
                    text = "Volver",
                    borderColor = ButtonDarkGray,
                    onClick = { if (!isBusy) onBack() }
                )
            }
        }

        // Overlay cubre carga inicial, borrado y pre-carga del detalle
        LoadingDotsOverlay(isLoading = isBusy)
    }

    // Modal Confirmación Borrar
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

    // Modal Editar Metadatos
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

    // Si trae initialEstudiantes, parte con ellos y NO muestres loading
    var estudiantes by remember { mutableStateOf<List<EstudianteNomina>>(initialEstudiantes ?: emptyList()) }
    var estudiantesOriginal by remember { mutableStateOf<List<EstudianteNomina>>(initialEstudiantes ?: emptyList()) }
    var isLoading by remember { mutableStateOf(initialEstudiantes == null) } // 👈 clave
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingField by remember { mutableStateOf<Pair<Int, String>?>(null) }

    val listState = rememberLazyListState()
    var scrollToEnd by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val fontSizeEstudiantes = 14.sp
    val isBusy by remember { derivedStateOf { isLoading || isSaving } }

    fun normalize(list: List<EstudianteNomina>) =
        list.sortedBy { it.nombre }.map { it.copy(nombre = it.nombre.trim(), cedula = it.cedula.trim()) }

    fun hasChanges(): Boolean = normalize(estudiantes) != normalize(estudiantesOriginal)

    // Back dentro del detalle (regresa a la lista)
    BackHandler(enabled = true) {
        if (isBusy) return@BackHandler
        onBack()
    }

    LaunchedEffect(scrollToEnd) {
        if (scrollToEnd && estudiantes.isNotEmpty()) {
            listState.animateScrollToItem(estudiantes.size - 1)
            scrollToEnd = false
        }
    }

    // Solo carga si NO recibimos datos precargados
    LaunchedEffect(nomina.id) {
        if (initialEstudiantes == null) {
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

    Scaffold(
        containerColor = BackgroundDefault,
        floatingActionButton = {
            FloatingSaveButton(
                visible = !isBusy,
                onClick = {
                    if (!hasChanges()) {
                        mensajealert(context, "ℹ️ No hay cambios para guardar.")
                        return@FloatingSaveButton
                    }
                    val hayVacios = estudiantes.any { it.cedula.isBlank() || it.nombre.isBlank() }
                    if (hayVacios) {
                        mensajealert(context, "⚠️ Completa cédula y nombre en todos los estudiantes.")
                        return@FloatingSaveButton
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
                },
                modifier = Modifier.offset(x = (-4).dp, y = -100.dp),
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
                        onClick = { if (!isBusy) onBack() }
                    )
                }

                Spacer(Modifier.height(16.dp))

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
                        Text("❌ Error: $error", color = Color.Red)
                    }
                    else -> {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(headerColor, shape = RoundedCornerShape(8.dp))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "  CÉDULA",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = fontSizeEstudiantes,
                                modifier = Modifier.weight(0.25f),
                                color = Color.Black
                            )
                            Text(
                                "  ESTUDIANTE",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = fontSizeEstudiantes,
                                modifier = Modifier.weight(0.6f),
                                color = Color.Black
                            )
                            Text(
                                "ACCIÓN",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = fontSizeEstudiantes,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.15f),
                                color = Color.Black
                            )
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(estudiantes, key = { _, it -> it.idUnico }) { index, est ->

                                val isCedulaEditing = editingField?.first == index && editingField?.second == "cedula"
                                val isNombreEditing = editingField?.first == index && editingField?.second == "nombre"

                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    // CÉDULA
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
                                            textStyle = LocalTextStyle.current.copy(fontSize = fontSizeEstudiantes),
                                            singleLine = true,
                                            enabled = !isBusy,
                                            modifier = Modifier
                                                .weight(0.25f)
                                                .focusRequester(focusRequesterCedula)
                                                .onFocusChanged { if (it.isFocused) editingField = index to "cedula" },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = {
                                                editingField = null
                                            })
                                        )
                                        LaunchedEffect(Unit) { focusRequesterCedula.requestFocus() }
                                    } else {
                                        Text(
                                            text = est.cedula.ifEmpty { "Cédula" },
                                            color = if (est.cedula.isEmpty()) Color.Gray else Color.Black,
                                            fontSize = fontSizeEstudiantes,
                                            modifier = Modifier
                                                .weight(0.25f)
                                                .clickable(enabled = !isBusy) { editingField = index to "cedula" }
                                                .padding(8.dp)
                                        )
                                    }

                                    // NOMBRE
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
                                            textStyle = LocalTextStyle.current.copy(fontSize = fontSizeEstudiantes),
                                            singleLine = true,
                                            enabled = !isBusy,
                                            modifier = Modifier
                                                .weight(0.6f)
                                                .focusRequester(focusRequesterNombre)
                                                .onFocusChanged { if (it.isFocused) editingField = index to "nombre" },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = {
                                                editingField = null
                                            })
                                        )
                                        LaunchedEffect(Unit) { focusRequesterNombre.requestFocus() }
                                    } else {
                                        Text(
                                            text = est.nombre.ifEmpty { "Nombre" },
                                            color = if (est.nombre.isEmpty()) Color.Gray else Color.Black,
                                            fontSize = fontSizeEstudiantes,
                                            modifier = Modifier
                                                .weight(0.6f)
                                                .clickable(enabled = !isBusy) { editingField = index to "nombre" }
                                                .padding(8.dp)
                                        )
                                    }

                                    // ACCIÓN
                                    val interaction = remember(est.idUnico) { MutableInteractionSource() }
                                    Box(Modifier.weight(0.15f).wrapContentWidth(Alignment.CenterHorizontally)) {
                                        IconButton(
                                            onClick = {
                                                if (isBusy) return@IconButton
                                                editingField = null
                                                estudiantes = estudiantes.toMutableList().also { it.removeAt(index) }
                                            },
                                            interactionSource = interaction,
                                            enabled = !isBusy
                                        ) {
                                            Icon(Icons.Default.Delete, "Eliminar estudiante", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CustomButton(
                                text = "Agregar estudiante",
                                borderColor = ButtonDarkSuccess,
                                onClick = {
                                    if (isBusy) return@CustomButton
                                    val hayVacios = estudiantes.any { it.cedula.isBlank() || it.nombre.isBlank() }
                                    if (hayVacios) {
                                        mensajealert(context, "⚠️ No puedes crear un nuevo registro si tienes otro vacío.")
                                        return@CustomButton
                                    }

                                    editingField?.let { (index, _) ->
                                        val est = estudiantes[index]
                                        if (est.cedula.isNotBlank() && est.nombre.isNotBlank()) {
                                            estudiantes = estudiantes.toMutableList().also { lista ->
                                                lista[index] = lista[index].copy(
                                                    idUnico = generarIdUnicoEstudianteNomina(est.cedula, est.nombre)
                                                )
                                            }
                                            editingField = null
                                        } else {
                                            mensajealert(context, "⚠️ Completa el estudiante antes de crear otro.")
                                            return@CustomButton
                                        }
                                    }

                                    val nuevoEstudiante = crearEstudianteVacio()
                                    estudiantes = estudiantes + nuevoEstudiante
                                    editingField = estudiantes.lastIndex to "cedula"
                                }
                            )
                        }
                    }
                }
            }

            LoadingDotsOverlay(isLoading = isBusy)
        }
    }
}


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
            usePlatformDefaultWidth = false // ⬅️ Full-screen dialog
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Contenedor del modal centrado (ajusta el ancho a gusto)
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.6f),
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                shape = RoundedCornerShape(12.dp),
                color = BackgroundDefault
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundDefault)
                            .padding(vertical = 20.dp, horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Editar datos de la nómina",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        HorizontalDivider()

                        OutlinedTextField(
                            value = institucion,
                            onValueChange = { institucion = it },
                            label = { Text("Institución") },
                            singleLine = true,
                            readOnly = isSaving,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = docente,
                            onValueChange = { docente = it },
                            label = { Text("Docente") },
                            singleLine = true,
                            readOnly = isSaving,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = curso,
                                onValueChange = { curso = it },
                                label = { Text("Curso") },
                                singleLine = true,
                                readOnly = isSaving,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = paralelo,
                                onValueChange = { paralelo = it.uppercase() },
                                label = { Text("Paralelo") },
                                singleLine = true,
                                readOnly = isSaving,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = asignatura,
                            onValueChange = { asignatura = it },
                            label = { Text("Asignatura") },
                            singleLine = true,
                            readOnly = isSaving,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = especialidad,
                            onValueChange = { especialidad = it },
                            label = { Text("Especialidad") },
                            singleLine = true,
                            readOnly = isSaving,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = periodo,
                            onValueChange = { periodo = it },
                            label = { Text("Periodo") },
                            singleLine = true,
                            readOnly = isSaving,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))

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
                                                    mensajealert(
                                                        context,
                                                        "⚠️ Ya existe una nómina con esos datos."
                                                    )
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
            }

            // Overlay FULL-SCREEN (hermano de Surface)
            if (isSaving) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) { awaitPointerEvent() } // consume toques
                            }
                        }
                ) {
                    LoadingDotsOverlay(isLoading = true)
                }
            }
        }
    }
}

private fun idUnicoEstudiante(): String =
    "std_" + UUID.randomUUID().toString().replace("-", "").take(16)

fun crearEstudianteVacio(): EstudianteNomina =
    EstudianteNomina(idUnico = idUnicoEstudiante(), cedula = "", nombre = "")
