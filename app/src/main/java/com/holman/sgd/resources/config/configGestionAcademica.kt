package com.holman.sgd.resources.config

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.resources.FondoScreenDefault
import com.holman.sgd.resources.TituloGeneralScreens
import com.holman.sgd.resources.components.ContenedorPrincipal
import com.holman.sgd.resources.components.FirestorePaths
import com.holman.sgd.resources.mensajealert
import com.holman.sgd.resources.screens.isTablet
import com.holman.sgd.ui.theme.BackgroundDefault
import com.holman.sgd.ui.theme.ButtonDarkGray
import com.holman.sgd.ui.theme.ButtonDarkPrimary
import com.holman.sgd.ui.theme.TextDefaultBlack

data class AcademicItem(
    val id: String = "",
    val nombre: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionAcademicaScreen(
    onNavigateBack: () -> Unit
) {
    BackHandler(enabled = true) { onNavigateBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        FondoScreenDefault()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ContenedorPrincipal),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopStart
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TituloGeneralScreens(texto = "Gestión Académica")
                        Spacer(modifier = Modifier.width(8.dp))

                        val listState = rememberLazyListState()
                        val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

                        LazyRow(
                            state = listState,
                            flingBehavior = flingBehavior,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item { Box(Modifier.fillParentMaxWidth()) { InputCard(Icons.Default.School, FirestorePaths.CatalogKeys.INSTITUCIONES_LABEL, FirestorePaths.CatalogKeys.INSTITUCIONES_COL) } }
                            item { Box(Modifier.fillParentMaxWidth()) { InputCard(Icons.Default.Person, FirestorePaths.CatalogKeys.DOCENTES_LABEL, FirestorePaths.CatalogKeys.DOCENTES_COL) } }
                            item { Box(Modifier.fillParentMaxWidth()) { InputCard(Icons.Default.Class, FirestorePaths.CatalogKeys.CURSOS_LABEL, FirestorePaths.CatalogKeys.CURSOS_COL) } }
                            item { Box(Modifier.fillParentMaxWidth()) { InputCard(Icons.Default.People, FirestorePaths.CatalogKeys.PARALELOS_LABEL, FirestorePaths.CatalogKeys.PARALELOS_COL) } }
                            item { Box(Modifier.fillParentMaxWidth()) { InputCard(Icons.Default.AutoStories, FirestorePaths.CatalogKeys.ASIGNATURAS_LABEL, FirestorePaths.CatalogKeys.ASIGNATURAS_COL) } }
                            item { Box(Modifier.fillParentMaxWidth()) { InputCard(Icons.Default.Star, FirestorePaths.CatalogKeys.ESPECIALIDADES_LABEL, FirestorePaths.CatalogKeys.ESPECIALIDADES_COL) } }
                            item { Box(Modifier.fillParentMaxWidth()) { InputCard(Icons.Default.Event, FirestorePaths.CatalogKeys.PERIODOS_LABEL, FirestorePaths.CatalogKeys.PERIODOS_COL) } }
                        }
                    }
                }
            }

            CustomButton(
                text = "Volver",
                borderColor = ButtonDarkGray,
                onClick = onNavigateBack
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputCard(
    icon: ImageVector,
    titulo: String,
    coleccion: String
) {
    val context = LocalContext.current
    var texto by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<AcademicItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var editingItem by remember { mutableStateOf<AcademicItem?>(null) }
    var showDeleteDialog by remember { mutableStateOf<AcademicItem?>(null) }

    val isTablet = isTablet()

    LaunchedEffect(coleccion) {
        cargarItems(coleccion) { listaItems ->
            items = listaItems
            isLoading = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundDefault)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = titulo,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            OutlinedTextField(
                value = texto,
                onValueChange = { input ->
                    val coleccionLower = coleccion.trim().lowercase()

                    texto = when {
                        // --- LÓGICA PARA PERIODO LECTIVO ---
                        coleccionLower.contains("periodo") -> {
                            val soloNumeros = input.filter { it.isDigit() }

                            if (input.length < texto.length) {
                                // Si el usuario borra mientras está el formato completo "2024 - 2025"
                                // regresamos a los 3 primeros dígitos para facilitar la edición
                                if (texto.contains("-") && soloNumeros.length >= 4) {
                                    soloNumeros.take(3)
                                } else {
                                    soloNumeros
                                }
                            } else {
                                when {
                                    // Al llegar a 4 dígitos, autocompletamos y bloqueamos
                                    soloNumeros.length == 4 -> {
                                        val anioInicio = soloNumeros.toIntOrNull() ?: 0
                                        "$anioInicio - ${anioInicio + 1}"
                                    }
                                    // Permitimos escribir mientras sea menos de 4
                                    soloNumeros.length < 4 -> soloNumeros
                                    // Si ya está completo, ignoramos cualquier entrada extra
                                    else -> texto
                                }
                            }
                        }

                        // 1. MAYÚSCULAS TOTALES para Instituciones y Paralelos
                        coleccionLower == "instituciones" || coleccionLower == "paralelos" -> {
                            input.uppercase().filter { it.isLetter() || it.isWhitespace() }
                        }

                        // 2. CAPITALIZAR CADA PALABRA para el resto
                        else -> {
                            input.split(" ").joinToString(" ") { palabra ->
                                if (palabra.isNotEmpty()) {
                                    palabra.lowercase().replaceFirstChar { char ->
                                        if (char.isLowerCase()) char.titlecase() else char.toString()
                                    }
                                } else {
                                    ""
                                }
                            }
                        }
                    }
                },
                label = { Text(if (editingItem != null) "Editando..." else "Crear nuevo item") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (coleccion.trim().lowercase().contains("periodo")) {
                        KeyboardType.Number
                    } else {
                        KeyboardType.Text
                    }
                ),
                textStyle = LocalTextStyle.current.copy(
                    color = TextDefaultBlack,
                    fontSize = 14.sp
                )
            )
            ///////////







            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(if (editingItem != null) 0.75f else 1f)) {
                    CustomButton(
                        text = if (editingItem != null) "Actualizar" else "Guardar",
                        borderColor = ButtonDarkPrimary,
                        onClick = {
                            if (texto.isNotBlank()) {
                                if (editingItem != null) {
                                    editarItem(coleccion, editingItem!!.id, texto.trim()) {
                                        mensajealert(context, "✅  Editado correctamente")
                                        texto = ""
                                        editingItem = null
                                        cargarItems(coleccion) { items = it }
                                    }
                                } else {
                                    val nombreTrimmed = texto.trim()
                                    val existeItem = items.any { it.nombre.equals(nombreTrimmed, ignoreCase = true) }

                                    if (existeItem) {
                                        mensajealert(context, "⚠️  Este elemento ya existe")
                                    } else {
                                        crearItem(coleccion, nombreTrimmed) {
                                            mensajealert(context, "✅  Guardado en ${coleccion.lowercase()}")
                                            texto = ""
                                            cargarItems(coleccion) { items = it }
                                        }
                                    }
                                }
                            } else {
                                mensajealert(context, "⚠️  Ingresa un valor")
                            }
                        }
                    )
                }

                if (editingItem != null) {
                    Column(
                        modifier = Modifier.weight(0.25f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isTablet) {
                            CustomButton(
                                text = "Cancelar",
                                borderColor = ButtonDarkGray,
                                onClick = {
                                    texto = ""
                                    editingItem = null
                                }
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    texto = ""
                                    editingItem = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancelar",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (items.isEmpty()) {
                Text(
                    text = "No hay elementos creados",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                Text(
                    text = "Elementos existentes:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items) { item ->
                        ItemRow(
                            item = item,
                            onEdit = {
                                editingItem = item
                                texto = item.nombre
                            },
                            onDelete = { showDeleteDialog = item }
                        )
                    }
                }
            }
        }
    }

    showDeleteDialog?.let { item ->
        Dialog(onDismissRequest = { showDeleteDialog = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BackgroundDefault,
                tonalElevation = 0.dp,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Confirmar eliminación",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "¿Estás seguro de que quieres eliminar '${item.nombre}'?",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            CustomButton(
                                text = "Eliminar",
                                borderColor = MaterialTheme.colorScheme.error,
                                onClick = {
                                    eliminarItem(coleccion, item.id) {
                                        mensajealert(context, "✅  Eliminado correctamente")
                                        showDeleteDialog = null
                                        cargarItems(coleccion) { items = it }
                                    }
                                }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            CustomButton(
                                text = "Cancelar",
                                borderColor = ButtonDarkGray,
                                onClick = { showDeleteDialog = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemRow(
    item: AcademicItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundDefault),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.nombre,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color.Blue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// =====================================================
// ✅ FIRESTORE (AHORA: datosGenerales POR USUARIO)
// Ruta: gestionAcademica/{uid}/datosGenerales/catalogos/{coleccion}/{item}
// =====================================================

fun cargarItems(coleccion: String, onSuccess: (List<AcademicItem>) -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: run { onSuccess(emptyList()); return }

    FirestorePaths.datosGeneralesColeccion(uid, coleccion)
        .orderBy("nombre", Query.Direction.ASCENDING)
        .get()
        .addOnSuccessListener { documents ->
            val items = documents.map { doc ->
                AcademicItem(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            }
            onSuccess(items.sortedBy { it.nombre.lowercase() })
        }
        .addOnFailureListener {
            onSuccess(emptyList())
        }
}

fun crearItem(coleccion: String, nombre: String, onSuccess: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val nombreFinal = if (coleccion.equals("paralelos", ignoreCase = true)) {
        nombre.trim().uppercase()
    } else {
        nombre.trim()
    }

    val data = mapOf(
        "nombre" to nombreFinal,
        "timestamp" to System.currentTimeMillis()
    )

    FirestorePaths.datosGeneralesColeccion(uid, coleccion)
        .add(data)
        .addOnSuccessListener { onSuccess() }
}

fun editarItem(coleccion: String, id: String, nuevoNombre: String, onSuccess: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val nombreFinal = if (coleccion.equals("paralelos", ignoreCase = true)) {
        nuevoNombre.trim().uppercase()
    } else {
        nuevoNombre.trim()
    }

    FirestorePaths.datosGeneralesColeccion(uid, coleccion)
        .document(id)
        .update("nombre", nombreFinal)
        .addOnSuccessListener { onSuccess() }
}

fun eliminarItem(coleccion: String, id: String, onSuccess: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    FirestorePaths.datosGeneralesColeccion(uid, coleccion)
        .document(id)
        .delete()
        .addOnSuccessListener { onSuccess() }
}
