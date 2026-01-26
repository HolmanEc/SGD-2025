///////////////////

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.holman.sgd.resources.InfoItem
import com.holman.sgd.resources.NominaReviewCard
import com.holman.sgd.ui.theme.TextDefaultBlack
import com.google.firebase.firestore.SetOptions
import com.holman.sgd.resources.FondoScreenDefault
import com.holman.sgd.resources.NominaResumen
import com.holman.sgd.resources.TituloGeneralScreens
import com.holman.sgd.resources.calificaciones.TablaConfig
import com.holman.sgd.resources.components.ContenedorPrincipal
import com.holman.sgd.resources.darken
import com.holman.sgd.resources.lighten
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.holman.sgd.resources.components.FirestorePaths
import com.holman.sgd.resources.components.generarIdUnicoEstudianteNominaFirebase


fun syncCalificacionesSeccionesConTabla(
    nominaId: String,
    estudiantesActuales: List<EstudianteNomina>,
    insumosCount: Int = TablaConfig.INSUMOS_COUNT,
    onDone: (Boolean, String?) -> Unit = { _, _ -> }
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onDone(false, "Usuario no autenticado"); return }

    val nominaRef = FirestorePaths.nominaDoc(uid, nominaId)

    val idsActuales = estudiantesActuales.map { it.idUnico }.toSet()
    val now = System.currentTimeMillis()

    // 1) Crear/asegurar documentos base de cada sección (por si no existen)
    val baseWrites = FirestorePaths.SECCIONES_TABLAS_INSUMOS.map { sec ->
        val secDoc = FirestorePaths.calificacionesSeccion(nominaRef, sec)

        val base = mutableMapOf<String, Any>(
            "seccion" to sec,
            "tipo" to if (sec == FirestorePaths.SECCION_INFORME) "INFORME" else "TRIMESTRE",
            "updatedAt" to now
        )

        if (sec != FirestorePaths.SECCION_INFORME) {
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
            //    - Crear faltantes / actualizar base
            fun procesarSeccion(idx: Int) {
                if (idx >= FirestorePaths.SECCIONES_TABLAS_INSUMOS.size) {
                    onDone(true, null); return
                }

                val sec = FirestorePaths.SECCIONES_TABLAS_INSUMOS[idx]
                val secDoc = FirestorePaths.calificacionesSeccion(nominaRef, sec)

                // ✅ Subcolección alumnos/insumos controlada por FirestorePaths
                val colAlumnos = FirestorePaths.insumos(nominaRef, sec)

                colAlumnos.get()
                    .addOnSuccessListener { snap ->
                        val existentesIds = snap.documents.map { it.id }.toSet()

                        val batchOps = mutableListOf<Pair<DocumentReference, Any?>>()

                        // 2.a) Borrar huérfanos
                        snap.documents.forEach { d ->
                            if (d.id !in idsActuales) {
                                batchOps += d.reference to null // null = delete
                            }
                        }

                        // 2.b) Crear faltantes / asegurar esquema
                        estudiantesActuales.forEachIndexed { index, est ->
                            val alumnoRef = FirestorePaths.insumoDoc(nominaRef, sec, est.idUnico)

                            if (est.idUnico !in existentesIds) {
                                val data: Map<String, Any?> =
                                    if (sec == FirestorePaths.SECCION_INFORME) {
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
                                // Existe: actualiza número/cedula/nombre (merge)
                                val merge = mapOf(
                                    "numero" to (index + 1),
                                    "cedula" to est.cedula,
                                    "nombre" to est.nombre,
                                    "updatedAt" to now
                                )
                                batchOps += alumnoRef to merge
                            }
                        }

                        // 2.c) Commit en lotes (≤450 ops por batch)
                        val MAX = 450
                        val chunks = batchOps.chunked(MAX)

                        fun commitChunk(cidx: Int) {
                            if (cidx >= chunks.size) {
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





fun syncAsistenciasConTabla(
    nominaId: String,
    estudiantesActuales: List<EstudianteNomina>,
    onDone: (Boolean, String?) -> Unit = { _, _ -> }
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onDone(false, "Usuario no autenticado"); return }

    val colAsistencias = FirestorePaths.asistencias(uid, nominaId)

    // IDs actuales (orden estable = el orden en que recibes la lista)
    val idsEnOrden = estudiantesActuales.map { it.idUnico }
    val idsSet = idsEnOrden.toSet()

    // Para migración legacy: "cedula_XXXXXXXX" -> idUnico actual
    val cedToId: Map<String, String> = estudiantesActuales
        .mapNotNull { est ->
            val ced = est.cedula.trim().uppercase().filter { it.isLetterOrDigit() }
            if (ced.isBlank()) null else ced to est.idUnico
        }
        .toMap()

    fun parseBool(v: Any?): Boolean = when (v) {
        is Boolean -> v
        is String -> v.equals("true", ignoreCase = true)
        is Number -> v.toInt() != 0
        else -> false
    }

    colAsistencias.get()
        .addOnSuccessListener { snap ->
            if (snap.isEmpty) { onDone(true, null); return@addOnSuccessListener }

            // Guardamos operaciones para batch
            // payload null => delete
            val ops = mutableListOf<Pair<DocumentReference, Map<String, Any>?>>()

            snap.documents.forEach { doc ->
                val data = (doc.data ?: emptyMap<String, Any>()).toMutableMap()

                // 1) Migrar claves legacy "cedula_XXXX" al idUnico actual (si se puede)
                //    (esto evita que "solo se arregle" al entrar y guardar esa fecha)
                val migrado = mutableMapOf<String, Boolean>()  // idUnico -> presente
                data.forEach { (k, v) ->
                    val presente = parseBool(v)
                    if (!presente) return@forEach // solo nos importa preservar los true (los false los rellenamos luego)

                    when {
                        // ya es idUnico actual
                        k in idsSet -> {
                            migrado[k] = true
                        }

                        // legacy: "cedula_XXXXXXXX"
                        k.startsWith("cedula_", ignoreCase = true) -> {
                            val ced = k.removePrefix("cedula_").trim().uppercase().filter { it.isLetterOrDigit() }
                            val id = cedToId[ced]
                            if (id != null) migrado[id] = true
                        }

                        // otras claves legacy (nombre/numero/hash): no se pueden mapear de forma segura aquí
                        else -> Unit
                    }
                }

                // 2) Construir mapa CANÓNICO: todos los IDs actuales presentes en el doc
                //    - Si el doc ya tiene ese id, respeta su valor
                //    - Si no, false
                //    - Si migración encontró true, fuerza true
                var anyTrue = false
                val nuevo = linkedMapOf<String, Any>()

                for (id in idsEnOrden) {
                    val actual = parseBool(data[id])
                    val finalVal = actual || (migrado[id] == true)
                    if (finalVal) anyTrue = true
                    nuevo[id] = finalVal
                }

                // 3) Política asistencia vacía: si no hay ningún true, elimina el doc
                if (!anyTrue) {
                    ops += doc.reference to null
                } else {
                    ops += doc.reference to nuevo
                }
            }

            // 4) Commit por lotes (≤450 ops)
            val MAX = 450
            val chunks = ops.chunked(MAX)

            fun commitChunk(i: Int) {
                if (i >= chunks.size) { onDone(true, null); return }
                val batch = db.batch()
                chunks[i].forEach { (ref, payload) ->
                    if (payload == null) batch.delete(ref)
                    else batch.set(ref, payload) // overwrite total del doc
                }
                batch.commit()
                    .addOnSuccessListener { commitChunk(i + 1) }
                    .addOnFailureListener { e -> onDone(false, e.localizedMessage) }
            }

            if (chunks.isEmpty()) onDone(true, null) else commitChunk(0)
        }
        .addOnFailureListener { e ->
            onDone(false, e.localizedMessage ?: "Error sincronizando asistencias")
        }
}









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
                        TituloGeneralScreens(texto = "No hay nóminas Guardadas")
                    }
                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                        //.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    ) {
                        TituloGeneralScreens(texto = "Nóminas Guardadas")
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


////

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ScreenRevisarDetalleNomina(
    nomina: NominaResumen,
    headerColor: Color,
    initialEstudiantes: List<EstudianteNomina>? = null,
    onBack: () -> Unit
) {
    // ⇨ CAMBIO: pesos centralizados para no equivocarnos
    val ColW = object {
        val Cedula = 0.15f
        val Estudiante = 0.35f
        val Representante = 0.20f
        val Contacto = 0.18f
        val Accion = 0.12f
    }


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

    val fontSizeEstudiantes = 12.sp
    val isBusy by remember { derivedStateOf { isLoading || isSaving } }

    fun normalize(list: List<EstudianteNomina>) =
        list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nombre.trim() })
            .map {
                it.copy(
                    nombre = it.nombre.trim(),
                    cedula = it.cedula.trim(),
                    representante = it.representante.trim(),
                    contacto = it.contacto.trim()
                )
            }

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

    Scaffold(containerColor = BackgroundDefault)
    { _ ->

        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(color = TextDefaultBlack)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                FondoScreenDefault()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ContenedorPrincipal)
                )
                {
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

                            val headerLight = headerColor.lighten(0.50f)
                            val headerDark = headerColor.darken(0.12f)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                            )
                            {
                                Column(
                                    modifier = Modifier
                                        .shadow(
                                            elevation = 6.dp,
                                            shape = RoundedCornerShape(8.dp),
                                            clip = false
                                        )
                                )
                                {
                                    // ───────── Encabezado de columnas ─────────
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .background(headerColor, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                            .padding(vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    )
                                    {
                                        val fs = fontSizeEstudiantes
                                        Text(
                                            text = "CÉDULA",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = fs,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .weight(ColW.Cedula) // ⇨ CAMBIO
                                                .padding(horizontal = 4.dp)
                                            //.background(Color(0xFFE0E0E0))
                                        )
                                        Text(
                                            text = "ESTUDIANTE",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = fs,
                                            textAlign = TextAlign.Left,
                                            modifier = Modifier
                                                .weight(ColW.Estudiante) // ⇨ CAMBIO
                                                .padding(horizontal = 4.dp)
                                            //.background(Color(0xFFE8DCB9))
                                        )
                                        Text(
                                            text = "REPRESENTANTE",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = fs,
                                            textAlign = TextAlign.Left,
                                            modifier = Modifier
                                                .weight(ColW.Representante) // ⇨ CAMBIO
                                                .padding(horizontal = 4.dp)
                                            //.background(Color(0xFF85C9C3))
                                        )
                                        Text(
                                            text = "CONTACTO",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = fs,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .weight(ColW.Contacto) // ⇨ CAMBIO
                                                .padding(horizontal = 4.dp)
                                            //.background(Color(0xFFBEA9E8))
                                        )
                                        Text(
                                            text = "ACCIÓN",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = fs,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .weight(ColW.Accion) // ⇨ CAMBIO
                                                .padding(horizontal = 4.dp)
                                            //.background(Color(0xFFE59595))
                                        )
                                    }

                                    // ───────── Lista principal ─────────
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(headerLight, shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                            .padding(bottom = 16.dp),
                                    )
                                    {
                                        itemsIndexed(estudiantes, key = { _, it -> it.idUnico }) { index, est ->

                                            val editCed  = editingField?.first == index && editingField?.second == "cedula"
                                            val editNom  = editingField?.first == index && editingField?.second == "nombre"
                                            val editRep  = editingField?.first == index && editingField?.second == "representante"
                                            val editCont = editingField?.first == index && editingField?.second == "contacto"

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // CÉDULA
                                                if (editCed) {
                                                    val fr = remember { FocusRequester() }
                                                    OutlinedTextField(
                                                        value = est.cedula,
                                                        onValueChange = { v ->
                                                            val filtrado = v.uppercase().filter { it.isLetterOrDigit() }
                                                            estudiantes = estudiantes.toMutableList().also {
                                                                it[index] = it[index].copy(cedula = filtrado)
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
                                                            .weight(ColW.Cedula) // ⇨ CAMBIO
                                                            .fillMaxWidth()
                                                            .focusRequester(fr)
                                                            .onFocusChanged { if (it.isFocused) editingField = index to "cedula" },
                                                        keyboardOptions = KeyboardOptions(
                                                            keyboardType = KeyboardType.Ascii,
                                                            imeAction = ImeAction.Done
                                                        ),
                                                        keyboardActions = KeyboardActions(onDone = { editingField = null })
                                                    )
                                                    LaunchedEffect(Unit) { fr.requestFocus() }
                                                } else {
                                                    Text(
                                                        text = est.cedula.ifEmpty { "Cédula" },
                                                        fontSize = fontSizeEstudiantes,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier
                                                            .weight(ColW.Cedula) // ⇨ CAMBIO
                                                            .padding(horizontal = 4.dp)
                                                            .fillMaxWidth()
                                                            .clickable(enabled = !isBusy) {
                                                                editingField = index to "cedula"
                                                            }
                                                    )
                                                }

                                                // ESTUDIANTE
                                                if (editNom) {
                                                    val fr = remember { FocusRequester() }
                                                    OutlinedTextField(
                                                        value = est.nombre,
                                                        onValueChange = { v ->
                                                            val filtrado = v.uppercase().filter { it.isLetter() || it.isWhitespace() }
                                                            estudiantes = estudiantes.toMutableList().also {
                                                                it[index] = it[index].copy(nombre = filtrado)
                                                            }
                                                        },
                                                        label = { Text("Nombre", fontSize = fontSizeEstudiantes) },
                                                        textStyle = LocalTextStyle.current.copy(fontSize = fontSizeEstudiantes),
                                                        singleLine = true,
                                                        enabled = !isBusy,
                                                        modifier = Modifier
                                                            .weight(ColW.Estudiante) // ⇨ CAMBIO
                                                            .fillMaxWidth()
                                                            .focusRequester(fr)
                                                            .onFocusChanged { if (it.isFocused) editingField = index to "nombre" },
                                                        keyboardOptions = KeyboardOptions(
                                                            keyboardType = KeyboardType.Text,
                                                            imeAction = ImeAction.Done
                                                        ),
                                                        keyboardActions = KeyboardActions(onDone = { editingField = null })
                                                    )
                                                    LaunchedEffect(Unit) { fr.requestFocus() }
                                                } else {
                                                    Text(
                                                        text = est.nombre.ifEmpty { "Nombre" },
                                                        fontSize = fontSizeEstudiantes,
                                                        textAlign = TextAlign.Start,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier
                                                            .weight(ColW.Estudiante) // ⇨ CAMBIO
                                                            .padding(horizontal = 4.dp)
                                                            .fillMaxWidth()
                                                            .clickable(enabled = !isBusy) { editingField = index to "nombre" }
                                                    )
                                                }

                                                // REPRESENTANTE
                                                if (editRep) {
                                                    val fr = remember { FocusRequester() }
                                                    OutlinedTextField(
                                                        value = est.representante,
                                                        onValueChange = { v ->
                                                            val filtrado = v.uppercase()
                                                            estudiantes = estudiantes.toMutableList().also {
                                                                it[index] = it[index].copy(representante = filtrado)
                                                            }
                                                        },
                                                        label = { Text("Representante", fontSize = fontSizeEstudiantes) },
                                                        textStyle = LocalTextStyle.current.copy(fontSize = fontSizeEstudiantes),
                                                        singleLine = true,
                                                        enabled = !isBusy,
                                                        modifier = Modifier
                                                            .weight(ColW.Representante) // ⇨ CAMBIO
                                                            .fillMaxWidth()
                                                            .focusRequester(fr)
                                                            .onFocusChanged { if (it.isFocused) editingField = index to "representante" },
                                                        keyboardOptions = KeyboardOptions(
                                                            keyboardType = KeyboardType.Text,
                                                            imeAction = ImeAction.Done
                                                        ),
                                                        keyboardActions = KeyboardActions(onDone = { editingField = null })
                                                    )
                                                    LaunchedEffect(Unit) { fr.requestFocus() }
                                                } else {
                                                    Text(
                                                        text = est.representante.ifEmpty { "Representante" },
                                                        fontSize = fontSizeEstudiantes,
                                                        textAlign = TextAlign.Start,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier
                                                            .weight(ColW.Representante) // ⇨ CAMBIO
                                                            .padding(horizontal = 4.dp)
                                                            .fillMaxWidth()
                                                            .clickable(enabled = !isBusy) { editingField = index to "representante" }
                                                    )
                                                }

                                                // CONTACTO (solo el dato; sin botón eliminar)
                                                if (editCont) {
                                                    val fr = remember { FocusRequester() }
                                                    OutlinedTextField(
                                                        value = est.contacto,
                                                        onValueChange = { v ->
                                                            val filtrado = v.filter { it.isDigit() || it in setOf('+', ' ', '-', '(', ')') }
                                                            estudiantes = estudiantes.toMutableList().also {
                                                                it[index] = it[index].copy(contacto = filtrado)
                                                            }
                                                        },
                                                        label = { Text("Contacto", fontSize = fontSizeEstudiantes) },
                                                        textStyle = LocalTextStyle.current.copy(
                                                            fontSize = fontSizeEstudiantes,
                                                            textAlign = TextAlign.Center
                                                        ),
                                                        singleLine = true,
                                                        enabled = !isBusy,
                                                        modifier = Modifier
                                                            .weight(ColW.Contacto) // ⇨ CAMBIO (antes 0.30f)
                                                            .fillMaxWidth()
                                                            .focusRequester(fr)
                                                            .onFocusChanged { if (it.isFocused) editingField = index to "contacto" },
                                                        keyboardOptions = KeyboardOptions(
                                                            keyboardType = KeyboardType.Phone,
                                                            imeAction = ImeAction.Done
                                                        ),
                                                        keyboardActions = KeyboardActions(onDone = { editingField = null })
                                                    )
                                                    LaunchedEffect(Unit) { fr.requestFocus() }
                                                } else {
                                                    Text(
                                                        text = est.contacto.ifEmpty { "—" },
                                                        fontSize = fontSizeEstudiantes,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier
                                                            .weight(ColW.Contacto) // ⇨ CAMBIO
                                                            .padding(horizontal = 4.dp)
                                                            .fillMaxWidth()
                                                            .clickable(enabled = !isBusy) { editingField = index to "contacto" }
                                                    )
                                                }

                                                // ACCIÓN (columna aparte)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(ColW.Accion) // ⇨ CAMBIO
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
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Eliminar estudiante",
                                                            tint = Color.Red
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // ───────── Zona de acciones ─────────
                            Column(modifier = Modifier.fillMaxWidth())
                            {
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
                            // Normalización coherente con tus campos de edición
                            val cedNorm = ced.trim().uppercase().filter { it.isLetterOrDigit() }
                            val nomNorm = nom.trim().uppercase().filter { it.isLetter() || it.isWhitespace() }

                            if (cedNorm.isBlank() || nomNorm.isBlank()) {
                                mensajealert(context, "⚠️ Cédula y nombre son obligatorios.")
                                return@AddEstudianteDialog
                            }

                            // 🔍 Validar duplicado por CÉDULA o por NOMBRE
                            val yaExiste = estudiantes.any { est ->
                                val cedEst = est.cedula.trim().uppercase().filter { c -> c.isLetterOrDigit() }
                                val nomEst = est.nombre.trim().uppercase().filter { c -> c.isLetter() || c.isWhitespace() }

                                cedEst == cedNorm || nomEst == nomNorm
                            }

                            if (yaExiste) {
                                mensajealert(context, "⚠️ Ya existe un estudiante con esa cédula o nombre.")
                                return@AddEstudianteDialog
                            }

                            val nuevo = EstudianteNomina(
                                idUnico = generarIdUnicoEstudianteNominaFirebase(cedNorm, nomNorm),
                                cedula = cedNorm,
                                nombre = nomNorm,
                                representante = "",
                                contacto = ""
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
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onError("Usuario no autenticado"); onResult(false); return }


    // 🔤 Normalizamos todos los campos a minúsculas antes de consultar
    val instNorm = institucion.trim().lowercase()
    val docNorm = docente.trim().lowercase()
    val cursoNorm = curso.trim().lowercase()
    val paraleloNorm = paralelo.trim().lowercase()
    val asigNorm = asignatura.trim().lowercase()
    val especNorm = especialidad.trim().lowercase()
    val periodoNorm = periodo.trim().lowercase()

    FirestorePaths.cursos(uid)
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
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onError("Usuario no autenticado"); return }


    val data = mapOf(
        "institucion" to nominaActualizada.institucion,
        "docente" to nominaActualizada.docente,
        "curso" to nominaActualizada.curso,
        "paralelo" to nominaActualizada.paralelo,
        "asignatura" to nominaActualizada.asignatura,
        "especialidad" to nominaActualizada.especialidad,
        "periodo" to nominaActualizada.periodo
    )

    val docNomina = FirestorePaths.nominaDoc(uid, nominaId)

    docNomina.set(data, SetOptions.merge())
        .addOnSuccessListener {
            // Si mantienes otra colección "resumenNominas", duplica aquí el set() con merge.
            onSuccess()
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error al guardar")
        }
}

data class EstudianteNomina(
    val idUnico: String,          // ID generado/estable
    var cedula: String,
    var nombre: String,
    var representante: String = "",
    var contacto: String = ""
)

fun cargarEstudiantesDeNomina(
    nominaId: String,
    onSuccess: (List<EstudianteNomina>) -> Unit,
    onError: (String) -> Unit
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onError("Usuario no autenticado"); return }

    // ✅ USAMOS LA RUTA DEFINIDA EN TU OBJECT FirestorePaths
    FirestorePaths.nominaDoc(uid, nominaId)
        .get()
        .addOnSuccessListener { doc ->
            if (!doc.exists()) {
                onError("No existe la nómina en la ruta: ${doc.reference.path}")
                return@addOnSuccessListener
            }

            @Suppress("UNCHECKED_CAST")
            val tabla = (doc.get("tabla") as? List<Map<String, Any?>>).orEmpty()
            if (tabla.isEmpty()) {
                onSuccess(emptyList())
                return@addOnSuccessListener
            }

            // --- Lógica de detección de columnas (se mantiene igual) ---
            val header = tabla.first()

            fun findIndexByTitle(vararg posibles: String): Int? {
                val want = posibles.map { it.trim().uppercase() }.toSet()
                header.entries.forEach { (k, v) ->
                    if (k.startsWith("col") && (v as? String)?.trim()?.uppercase() in want) {
                        return k.removePrefix("col").toIntOrNull()
                    }
                }
                return null
            }

            val idxID   = findIndexByTitle("ID") ?: 1
            val idxCed  = findIndexByTitle("CÉDULA", "CEDULA") ?: 3
            val idxNom  = findIndexByTitle("ESTUDIANTE", "ALUMNO", "NOMBRE") ?: 4
            val idxRep  = findIndexByTitle("REPRESENTANTE", "TUTOR", "APODERADO") ?: 5
            val idxCont = findIndexByTitle("CONTACTO", "TELÉFONO", "TELEFONO", "CELULAR") ?: 6

            fun get(row: Map<String, Any?>, idx: Int): String =
                (row["col$idx"] as? String)?.trim().orEmpty()

            val filas = tabla.drop(1)
            val lista = filas.map { row ->
                val id  = get(row, idxID).ifBlank { null }
                val ced = get(row, idxCed)
                val nom = get(row, idxNom)
                val rep = get(row, idxRep)
                val con = get(row, idxCont)

                EstudianteNomina(
                    idUnico = id ?: generarIdUnicoEstudianteNominaFirebase(ced, nom),
                    cedula = ced,
                    nombre = nom,
                    representante = rep,
                    contacto = con
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
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onError("Usuario no autenticado"); return }

    val nominaRef = FirestorePaths.nominaDoc(uid, nominaId)

    // Encabezado canónico
    val encabezado = mapOf(
        "col1" to "ID",
        "col2" to "NRO",
        "col3" to "CÉDULA",
        "col4" to "ESTUDIANTE",
        "col5" to "REPRESENTANTE",
        "col6" to "CONTACTO"
    )

    val filas = listOf(encabezado) + estudiantes.mapIndexed { index, est ->
        mapOf(
            "col1" to est.idUnico,
            "col2" to (index + 1).toString(),
            "col3" to est.cedula,
            "col4" to est.nombre,
            "col5" to est.representante,
            "col6" to est.contacto
        )
    }

    nominaRef.update("tabla", filas)
        .addOnSuccessListener {
            // 1) Sincroniza calificaciones (como ya lo tienes)
            syncCalificacionesSeccionesConTabla(
                nominaId = nominaId,
                estudiantesActuales = estudiantes,
                insumosCount = TablaConfig.INSUMOS_COUNT
            ) { ok, err ->
                if (!ok) {
                    onError(err ?: "No se pudo sincronizar calificaciones")
                    return@syncCalificacionesSeccionesConTabla
                }

                // 2) ✅ NUEVO: Sincroniza TODAS las asistencias guardadas (todas las fechas)
                syncAsistenciasConTabla(
                    nominaId = nominaId,
                    estudiantesActuales = estudiantes
                ) { okAsis, errAsis ->
                    if (!okAsis) {
                        onError(errAsis ?: "No se pudo sincronizar asistencias")
                    } else {
                        onSuccess()
                    }
                }
            }
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Error al actualizar la tabla de la nómina")
        }
}

fun eliminarNominaFirebaseCompleta(
    nominaId: String,
    subcolecciones: List<String> = listOf("asistencias", "calificaciones"),
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onError("Usuario no autenticado"); return }

    val docRef = FirestorePaths.nominaDoc(uid, nominaId)

    fun borrarColeccionPlano(
        colRef: CollectionReference,
        onSubDone: () -> Unit,
        onSubError: (Exception) -> Unit
    ) {
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

    fun borrarCalificacionesDeep(
        califRef: CollectionReference,
        onSubDone: () -> Unit,
        onSubError: (Exception) -> Unit
    ) {
        califRef.get()
            .addOnSuccessListener { snap ->
                fun procesarSeccion(i: Int) {
                    if (i >= snap.documents.size) { onSubDone(); return }

                    val secDoc = snap.documents[i]

                    if (secDoc.id.startsWith("_")) {
                        secDoc.reference.delete()
                            .addOnSuccessListener { procesarSeccion(i + 1) }
                            .addOnFailureListener(onSubError)
                        return
                    }

                    val alumnosCol = FirestorePaths.insumos(docRef, secDoc.id)

                    borrarColeccionPlano(
                        colRef = alumnosCol,
                        onSubDone = {
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

        when (subcolecciones[index]) {
            "asistencias" -> {
                val colRef = FirestorePaths.asistencias(uid, nominaId)
                borrarColeccionPlano(
                    colRef = colRef,
                    onSubDone = { borrarSiguienteSubcoleccion(index + 1) },
                    onSubError = { e -> onError(e.localizedMessage ?: "Error al borrar asistencias") }
                )
            }

            "calificaciones" -> {
                val califRef = FirestorePaths.calificaciones(docRef)
                borrarCalificacionesDeep(
                    califRef = califRef,
                    onSubDone = { borrarSiguienteSubcoleccion(index + 1) },
                    onSubError = { e -> onError(e.localizedMessage ?: "Error al borrar calificaciones") }
                )
            }

            else -> {
                onError("Subcolección no permitida: ${subcolecciones[index]}")
            }
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
            dismissOnClickOutside = true,
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