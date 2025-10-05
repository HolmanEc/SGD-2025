package com.holman.sgd.resources.config

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.resources.mensajealert
import com.holman.sgd.resources.screens.isTablet
import com.holman.sgd.ui.theme.BackgroundDefault
import com.holman.sgd.ui.theme.ButtonDarkGray
import com.holman.sgd.ui.theme.ButtonDarkPrimary

// Data class para los items
data class AcademicItem(
    val id: String = "",
    val nombre: String = "",
    val timestamp: Long = 0L
)
// 🔹 NUEVA PANTALLA COMPLETA PARA GESTIÓN ACADÉMICA — BOTÓN ABAJO FIJO
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionAcademicaScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDefault)
            .padding(16.dp)
    ) {
        // 🔹 Contenedor del contenido principal (LazyRow con tarjetas)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopStart
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundDefault)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Título
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Gestión Académica",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    val listState = rememberLazyListState()
                    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
                    LazyRow(
                        state = listState,
                        flingBehavior = flingBehavior,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Box(Modifier.fillParentMaxWidth()) {
                                InputCard(Icons.Default.School, "Instituciones", "instituciones")
                            }
                        }
                        item {
                            Box(Modifier.fillParentMaxWidth()) {
                                InputCard(Icons.Default.Person, "Docentes", "docentes")
                            }
                        }
                        item {
                            Box(Modifier.fillParentMaxWidth()) {
                                InputCard(Icons.Default.Class, "Cursos", "cursos")
                            }
                        }
                        item {
                            Box(Modifier.fillParentMaxWidth()) {
                                InputCard(Icons.Default.People, "Paralelos", "paralelos")
                            }
                        }
                        item {
                            Box(Modifier.fillParentMaxWidth()) {
                                InputCard(Icons.Default.AutoStories, "Asignaturas", "asignaturas")
                            }
                        }
                        item {
                            Box(Modifier.fillParentMaxWidth()) {
                                InputCard(Icons.Default.Star, "Especialidades", "especialidades")
                            }
                        }
                        item {
                            Box(Modifier.fillParentMaxWidth()) {
                                InputCard(Icons.Default.Event, "Periodos Lectivos", "periodos")
                            }
                        }
                    }
                }
            }
        }
        // 🔹 Botón Volver siempre fijo al fondo (estilo como en Asistencias)
        CustomButton(
            text = "Volver",
            borderColor = ButtonDarkGray,
            onClick = onNavigateBack
        )
    }
}


////
// 🔹 CARD EXPANDIDO CON LISTADO, EDICIÓN Y ELIMINACIÓN
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
    val db = FirebaseFirestore.getInstance()

    val isTablet = isTablet()

    // Cargar items al iniciar
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Encabezado con ícono + título
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
                onValueChange = { texto = it },
                label = { Text(if (editingItem != null) "Editando..." else "Nuevo item") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
                                            mensajealert(context, "✅  Guardado en $coleccion")
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
                ){
                    items(items) { item ->
                        ItemRow(
                            item = item,
                            onEdit = {
                                editingItem = item
                                texto = item.nombre
                            },
                            onDelete = {
                                showDeleteDialog = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog de confirmación para eliminar
    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor = BackgroundDefault,
            title = {
                Text(
                    "Confirmar eliminación",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que quieres eliminar '${item.nombre}'?",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
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
        )
    }
}

/////////////////
/////////////////

// 🔹 COMPONENTE PARA CADA ITEM EN LA LISTA
@Composable
fun ItemRow(
    item: AcademicItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color.Blue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
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





// 🔹 FUNCIONES PARA FIRESTORE
fun cargarItems(coleccion: String, onSuccess: (List<AcademicItem>) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("gestionAcademica")
        .document("datosGenerales")
        .collection(coleccion)
        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { documents ->
            val items = documents.map { doc ->
                AcademicItem(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            }
            onSuccess(items)
        }
        .addOnFailureListener {
            onSuccess(emptyList())
        }
}

fun crearItem(coleccion: String, nombre: String, onSuccess: () -> Unit) {
    val data = mapOf(
        "nombre" to nombre,
        "timestamp" to System.currentTimeMillis()
    )
    FirebaseFirestore.getInstance()
        .collection("gestionAcademica")
        .document("datosGenerales")
        .collection(coleccion)
        .add(data)
        .addOnSuccessListener { onSuccess() }
}

fun editarItem(coleccion: String, id: String, nuevoNombre: String, onSuccess: () -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("gestionAcademica")
        .document("datosGenerales")
        .collection(coleccion)
        .document(id)
        .update("nombre", nuevoNombre)
        .addOnSuccessListener { onSuccess() }
}

fun eliminarItem(coleccion: String, id: String, onSuccess: () -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("gestionAcademica")
        .document("datosGenerales")
        .collection(coleccion)
        .document(id)
        .delete()
        .addOnSuccessListener { onSuccess() }
}