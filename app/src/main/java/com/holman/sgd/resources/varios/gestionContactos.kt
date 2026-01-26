package com.holman.sgd.resources.varios

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.resources.LoadingDotsOverlay
import com.holman.sgd.resources.TituloGeneralScreens
import com.holman.sgd.resources.components.ContenedorPrincipal
import com.holman.sgd.resources.components.FirestorePaths
import com.holman.sgd.resources.components.Transparencia.SHADOW
import com.holman.sgd.ui.theme.BackgroundDefault
import com.holman.sgd.ui.theme.ButtonDarkError
import com.holman.sgd.ui.theme.ButtonDarkGray
import com.holman.sgd.ui.theme.ButtonDarkPrimary
import com.holman.sgd.ui.theme.ButtonDarkSuccess
import com.holman.sgd.ui.theme.TextDefaultBlack
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.Normalizer

// =====================================================
// ✅ 1) MODELOS UI
// =====================================================
private enum class TipoTelefono(val label: String) {
    TELEFONO1("Celular"),
    TELEFONO2("Telefono"),
    TELEFONO3("Contacto");

    companion object {
        fun safeFromString(value: String?): TipoTelefono {
            return try {
                if (value.isNullOrBlank()) TELEFONO1 else valueOf(value)
            } catch (_: Exception) {
                TELEFONO1
            }
        }
    }
}

private data class TelefonoUI(
    val tipo: TipoTelefono,
    val numero: String
)

private data class ContactoUI(
    val id: String,
    val apellidosNombres: String,
    val representante: String,
    val telefonos: List<TelefonoUI>
)

private fun contactosSubcoleccion(uid: String) =
    FirestorePaths.contactos(uid)

/**
 * Listener realtime del DOC usuarios/{uid}
 * - Si el doc NO existe -> lista vacía
 */
private fun listenUserDoc(
    uid: String,
    onChange: (List<ContactoUI>) -> Unit,
    onError: (String) -> Unit
): ListenerRegistration {
    // Usamos el índice de Firestore para traer los últimos actualizados primero
    return contactosSubcoleccion(uid)
        .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        .addSnapshotListener { snap, e ->
            if (e != null) {
                onError(e.message ?: "Error leyendo Firestore")
                return@addSnapshotListener
            }

            val list = snap?.documents?.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null

                // Parseo de los teléfonos desde la lista de mapas en el documento
                val telefonosRaw = data["telefonos"] as? List<*>
                val telefonos = telefonosRaw?.mapNotNull { item ->
                    val mm = item as? Map<*, *> ?: return@mapNotNull null
                    TelefonoUI(
                        tipo = TipoTelefono.safeFromString(mm["tipo"] as? String),
                        numero = (mm["numero"] as? String).orEmpty()
                    )
                } ?: emptyList()

                ContactoUI(
                    id = doc.id, // El ID ahora es el nombre del documento
                    apellidosNombres = (data["apellidosNombres"] as? String).orEmpty(),
                    representante = (data["representante"] as? String).orEmpty(),
                    telefonos = telefonos
                )
            } ?: emptyList()

            onChange(list)
        }
}
/**
 * ✅ UPSERT (crear/editar) un contacto:
 * - Lee doc actual
 * - Inserta/Reemplaza contacto en el array
 * - Crea el doc SOLO aquí si no existía
 */
private suspend fun upsertContactoInUserDoc(
    uid: String,
    contactoId: String?,
    apellidosNombres: String,
    representante: String,
    telefonos: List<TelefonoUI>
) {
    val subCol = contactosSubcoleccion(uid)

    // Si contactoId es null, Firestore genera un ID automático único
    val docRef = if (contactoId.isNullOrBlank()) subCol.document() else subCol.document(contactoId)

    val telefonosClean = telefonos
        .map { t -> mapOf("tipo" to t.tipo.name, "numero" to t.numero.trim()) }
        .filter { (it["numero"] as String).isNotEmpty() }

    val payload = hashMapOf(
        "id" to docRef.id,
        "apellidosNombres" to apellidosNombres.trim(),
        "representante" to representante.trim(),
        "telefonos" to telefonosClean,
        "updatedAt" to FieldValue.serverTimestamp() // Timestamp exacto del servidor
    )

    // Si es nuevo, registramos la fecha de creación
    if (contactoId.isNullOrBlank()) {
        payload["createdAt"] = FieldValue.serverTimestamp()
    }

    docRef.set(payload, SetOptions.merge()).await()
}
/**
 * ✅ DELETE (eliminar 1 contacto):
 * - Lee doc actual
 * - Quita el contacto del array
 * - Si queda vacío => BORRA el doc completo (todo limpio)
 */
private suspend fun deleteContactoFromUserDoc(uid: String, contactoId: String) {
    // Borrado directo por ID de documento
    contactosSubcoleccion(uid).document(contactoId).delete().await()
}
// =====================================================
// ✅ 3) UTILIDADES DE BÚSQUEDA + RESALTADO
// =====================================================
private fun normalizeText(input: String): String {
    val trimmed = input.trim().lowercase()
    val normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
    return normalized.replace("\\p{Mn}+".toRegex(), "")
}

private fun digitsOnly(input: String): String = buildString {
    input.forEach { ch -> if (ch.isDigit()) append(ch) }
}

private data class DigitMap(val digits: String, val indices: IntArray)

private fun buildDigitMap(raw: String): DigitMap {
    val sb = StringBuilder()
    val idx = ArrayList<Int>(raw.length)
    raw.forEachIndexed { i, ch ->
        if (ch.isDigit()) {
            sb.append(ch)
            idx.add(i)
        }
    }
    return DigitMap(sb.toString(), idx.toIntArray())
}

private fun highlightTokens(text: String, tokens: List<String>, highlight: SpanStyle): AnnotatedString {
    if (tokens.isEmpty()) return AnnotatedString(text)

    val lower = text.lowercase()
    val matches = mutableListOf<Pair<Int, Int>>()

    tokens
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .forEach { token ->
            val t = token.lowercase()
            var start = 0
            while (true) {
                val i = lower.indexOf(t, startIndex = start)
                if (i < 0) break
                matches.add(i to (i + t.length))
                start = i + t.length
            }
        }

    if (matches.isEmpty()) return AnnotatedString(text)

    val merged = matches.sortedBy { it.first }
        .fold(mutableListOf<Pair<Int, Int>>()) { acc, (s, e) ->
            if (acc.isEmpty()) acc.add(s to e)
            else {
                val (ls, le) = acc.last()
                if (s <= le) acc[acc.lastIndex] = ls to maxOf(le, e)
                else acc.add(s to e)
            }
            acc
        }

    return buildAnnotatedString {
        var cursor = 0
        for ((s, e) in merged) {
            if (cursor < s) append(text.substring(cursor, s))
            withStyle(highlight) { append(text.substring(s, e)) }
            cursor = e
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}

private fun highlightDigitsInRawNumber(raw: String, queryDigits: String, highlight: SpanStyle): AnnotatedString {
    val q = queryDigits.trim()
    if (q.isBlank()) return AnnotatedString(raw)

    val map = buildDigitMap(raw)
    val pos = map.digits.indexOf(q)
    if (pos < 0) return AnnotatedString(raw)

    val startRaw = map.indices[pos]
    val endRaw = map.indices[pos + q.length - 1] + 1

    return buildAnnotatedString {
        append(raw.substring(0, startRaw))
        withStyle(highlight) { append(raw.substring(startRaw, endRaw)) }
        append(raw.substring(endRaw))
    }
}

// =====================================================
// ✅ 4) ABRIR TEL / WA / SMS
// =====================================================
private fun openPhoneChooser(context: Context, rawNumber: String) {
    val cleaned = rawNumber.trim()
        .replace(" ", "")
        .replace("-", "")
        .replace("(", "")
        .replace(")", "")

    if (cleaned.isBlank()) return

    val DEFAULT_COUNTRY_CODE = "593"

    fun toWaDigits(number: String): String {
        val n = number.replace("[^0-9+]".toRegex(), "")
        val withPlus = when {
            n.startsWith("+") -> n
            n.startsWith("0") && n.length >= 9 -> "+$DEFAULT_COUNTRY_CODE${n.drop(1)}"
            n.all { it.isDigit() } && n.length >= 8 -> "+$DEFAULT_COUNTRY_CODE$n"
            else -> n
        }
        return withPlus.replace("+", "").replace("[^0-9]".toRegex(), "")
    }

    val waDigits = toWaDigits(cleaned)

    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned"))
    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleaned"))

    val waUri = Uri.parse("https://wa.me/$waDigits")
    val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply { setPackage("com.whatsapp") }
    val waBusinessIntent = Intent(Intent.ACTION_VIEW, waUri).apply { setPackage("com.whatsapp.w4b") }

    val pm: PackageManager = context.packageManager
    fun canHandle(intent: Intent): Boolean = intent.resolveActivity(pm) != null

    val extraIntents = mutableListOf<Intent>()
    if (canHandle(waIntent)) extraIntents.add(waIntent)
    if (canHandle(waBusinessIntent)) extraIntents.add(waBusinessIntent)
    if (canHandle(smsIntent)) extraIntents.add(smsIntent)

    val baseIntent = when {
        canHandle(dialIntent) -> dialIntent
        extraIntents.isNotEmpty() -> extraIntents.removeAt(0)
        else -> return
    }

    val chooser = Intent.createChooser(baseIntent, "Abrir con...")
    if (extraIntents.isNotEmpty()) {
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray())
    }
    context.startActivity(chooser)
}

// =====================================================
// ✅ 5) SCREEN PRINCIPAL (Firestore por usuario en su doc)
// =====================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionContactosScreen(
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val contactos = remember { mutableStateListOf<ContactoUI>() }

    // ✅ Estados separados (este es el fix del “pantallazo”)
    var isLoadingList by remember { mutableStateOf(true) }   // carga/escucha lista
    var isSaving by remember { mutableStateOf(false) }       // guardar/eliminar en modales

    var errorMsg by remember { mutableStateOf<String?>(null) }

    // ✅ usuario actual
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid }

    // ✅ listener del usuario (NO crea doc si no existe)
    DisposableEffect(uid) {
        var reg: ListenerRegistration? = null

        if (uid.isNullOrBlank()) {
            isLoadingList = false
            errorMsg = "No hay sesión iniciada. Inicia sesión para ver tus contactos."
        } else {
            isLoadingList = true
            reg = listenUserDoc(
                uid = uid,
                onChange = { list ->
                    contactos.clear()
                    contactos.addAll(list)
                    isLoadingList = false
                },
                onError = { msg ->
                    errorMsg = msg
                    isLoadingList = false
                }
            )
        }

        onDispose { reg?.remove() }
    }

    // ✅ Search
    var search by remember { mutableStateOf("") }

    // ✅ Editor
    var showEditor by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var apellidosNombres by remember { mutableStateOf("") }
    var representante by remember { mutableStateOf("") }
    val telefonosEdit = remember { mutableStateListOf<TelefonoUI>() }

    // ✅ Delete confirm
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    // ✅ Dropdown robusto
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    // ======================
    // ✅ FILTRO (buscador)
    // ======================
    val qNorm = normalizeText(search)
    val qDigits = digitsOnly(search)
    val rawTokens = search.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val tokensText = rawTokens.filter { token -> token.any { it.isLetter() } }.map { normalizeText(it) }

    val filtrados = contactos.filter { c ->
        val textBlob = normalizeText(
            buildString {
                append(c.apellidosNombres).append(' ')
                append(c.representante).append(' ')
                c.telefonos.forEach {
                    append(it.tipo.label).append(' ')
                    append(it.numero).append(' ')
                }
            }
        )

        val textOk = tokensText.isEmpty() || tokensText.all { t -> textBlob.contains(t) }
        val digitsOk = qDigits.isBlank() || c.telefonos.any { t -> digitsOnly(t.numero).contains(qDigits) }

        textOk && digitsOk && (qNorm.isNotBlank() || qDigits.isNotBlank() || search.isBlank())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ContenedorPrincipal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ======================
            // ✅ Header fijo
            // ======================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TituloGeneralScreens(texto = "Gestionar contactos")
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Registra Apellidos y Nombres, Representante y varios teléfonos.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextDefaultBlack,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Buscar (nombre, representante, tipo o teléfono)") },
                    colors = outlinedColors()
                )

                Spacer(Modifier.height(16.dp))

                CustomButton(
                    text = "Agregar nuevo contacto",
                    borderColor = ButtonDarkPrimary,
                    onClick = {
                        focusManager.clearFocus()
                        if (uid.isNullOrBlank()) {
                            errorMsg = "No hay sesión iniciada."
                            return@CustomButton
                        }

                        editingId = null
                        apellidosNombres = ""
                        representante = ""
                        telefonosEdit.clear()
                        telefonosEdit.add(TelefonoUI(TipoTelefono.TELEFONO1, ""))
                        expandedIndex = null
                        showEditor = true
                    }
                )
            }

            // ======================
            // ✅ Lista con scroll
            // ======================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (filtrados.isEmpty()) {
                    Text(
                        text = if (isLoadingList) "" else "No hay coincidencias.",
                        fontSize = 14.sp,
                        color = TextDefaultBlack.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    filtrados.forEach { c ->
                        ContactoCard(
                            contacto = c,
                            queryRawTokens = rawTokens,
                            queryDigits = qDigits,
                            onEdit = {
                                focusManager.clearFocus()
                                editingId = c.id
                                apellidosNombres = c.apellidosNombres
                                representante = c.representante

                                telefonosEdit.clear()
                                if (c.telefonos.isEmpty()) telefonosEdit.add(TelefonoUI(TipoTelefono.TELEFONO1, ""))
                                else telefonosEdit.addAll(c.telefonos)

                                expandedIndex = null
                                showEditor = true
                            },
                            onDelete = {
                                focusManager.clearFocus()
                                deleteTargetId = c.id
                                showDeleteConfirm = true
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // ======================
            // ✅ Volver
            // ======================
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CustomButton(
                    text = "Volver",
                    borderColor = ButtonDarkGray,
                    onClick = {
                        focusManager.clearFocus()
                        onBack()
                    }
                )
            }
        }

        // ✅ Overlay SOLO para carga/listener (ya NO aparece al Guardar/Eliminar en modal)
        LoadingDotsOverlay(isLoading = isLoadingList)
    }

    // =====================================================
    // ✅ DIALOG: Crear/Editar
    // =====================================================
    if (showEditor) {
        val dialogShape = RoundedCornerShape(20.dp)
        AlertDialog(
            onDismissRequest = {
                expandedIndex = null
                showEditor = false
            },
            containerColor = BackgroundDefault,
            tonalElevation = 6.dp,
            shape = dialogShape,
            modifier = Modifier.shadow(14.dp, dialogShape, clip = false),
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (editingId == null) "Nuevo contacto" else "Editar contacto",
                        color = TextDefaultBlack,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {

                    OutlinedTextField(
                        value = apellidosNombres,
                        onValueChange = { nuevo -> apellidosNombres = nuevo.uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Apellidos y Nombres") },
                        colors = outlinedColors()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = representante,
                        onValueChange = { nuevo -> representante = nuevo.uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Representante") },
                        colors = outlinedColors()
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Teléfonos",
                        color = TextDefaultBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    telefonosEdit.forEachIndexed { index, telefono ->
                        val expanded = expandedIndex == index

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expandedIndex = if (expanded) null else index },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = telefono.tipo.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("Tipo") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = outlinedColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expandedIndex = null },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BackgroundDefault)
                                ) {
                                    TipoTelefono.values().forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t.label) },
                                            onClick = {
                                                telefonosEdit[index] = telefono.copy(tipo = t)
                                                expandedIndex = null
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            OutlinedTextField(
                                value = telefono.numero,
                                onValueChange = { input ->
                                    val soloNumeros = input.filter { it.isDigit() }
                                    telefonosEdit[index] = telefono.copy(numero = soloNumeros)
                                },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                label = { Text("Número ${index + 1}") },
                                colors = outlinedColors(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                trailingIcon = {
                                    if (telefonosEdit.size > 1) {
                                        IconButton(onClick = {
                                            telefonosEdit.removeAt(index)
                                            if (expandedIndex == index) expandedIndex = null
                                            else if (expandedIndex != null && expandedIndex!! > index) {
                                                expandedIndex = expandedIndex!! - 1
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Eliminar",
                                                tint = ButtonDarkError
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "+ Teléfono",
                            color = ButtonDarkPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { telefonosEdit.add(TelefonoUI(TipoTelefono.TELEFONO1, "")) }
                                .padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CustomButton(
                            text = "Cancelar",
                            borderColor = ButtonDarkGray,
                            onClick = {
                                if (isSaving) return@CustomButton
                                expandedIndex = null
                                showEditor = false
                                focusManager.clearFocus()
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        CustomButton(
                            text = if (isSaving) "Guardando..." else "Guardar",
                            borderColor = ButtonDarkSuccess,
                            onClick = {
                                focusManager.clearFocus()

                                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                                if (currentUid.isNullOrBlank()) {
                                    errorMsg = "No hay sesión iniciada."
                                    return@CustomButton
                                }

                                val nombreClean = apellidosNombres.trim()
                                if (nombreClean.isBlank()) return@CustomButton

                                val repClean = representante.trim()
                                val telClean = telefonosEdit
                                    .map { it.copy(numero = it.numero.trim()) }
                                    .filter { it.numero.isNotEmpty() }
                                    .distinctBy { it.numero }

                                if (isSaving) return@CustomButton

                                scope.launch {
                                    isSaving = true
                                    try {
                                        upsertContactoInUserDoc(
                                            uid = currentUid,
                                            contactoId = editingId,
                                            apellidosNombres = nombreClean,
                                            representante = repClean,
                                            telefonos = telClean
                                        )
                                        expandedIndex = null
                                        showEditor = false
                                    } catch (e: Exception) {
                                        errorMsg = e.message ?: "Error guardando contacto"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        )
                    }
                }
            },
            dismissButton = {}
        )
    }

    // =====================================================
    // ✅ DIALOG: Eliminar
    // =====================================================
    if (showDeleteConfirm) {
        val dialogShape = RoundedCornerShape(20.dp)

        AlertDialog(
            onDismissRequest = {
                deleteTargetId = null
                showDeleteConfirm = false
                focusManager.clearFocus()
            },
            containerColor = BackgroundDefault,
            tonalElevation = 6.dp,
            shape = dialogShape,
            modifier = Modifier.shadow(14.dp, dialogShape, clip = false),
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Eliminar contacto",
                        color = TextDefaultBlack,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = "¿Seguro que deseas eliminar este contacto?",
                    color = TextDefaultBlack,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CustomButton(
                            text = "Cancelar",
                            borderColor = ButtonDarkGray,
                            onClick = {
                                if (isSaving) return@CustomButton
                                deleteTargetId = null
                                showDeleteConfirm = false
                                focusManager.clearFocus()
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        CustomButton(
                            text = if (isSaving) "Eliminando..." else "Eliminar",
                            borderColor = ButtonDarkError,
                            onClick = {
                                focusManager.clearFocus()

                                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                                val id = deleteTargetId

                                if (currentUid.isNullOrBlank()) {
                                    errorMsg = "No hay sesión iniciada."
                                    return@CustomButton
                                }
                                if (id.isNullOrBlank()) return@CustomButton

                                if (isSaving) return@CustomButton

                                scope.launch {
                                    isSaving = true
                                    try {
                                        deleteContactoFromUserDoc(currentUid, id)
                                        deleteTargetId = null
                                        showDeleteConfirm = false
                                    } catch (e: Exception) {
                                        errorMsg = e.message ?: "Error eliminando contacto"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        )
                    }
                }
            },
            dismissButton = {}
        )
    }

    // ✅ Dialog Error
    if (errorMsg != null) {
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            containerColor = BackgroundDefault,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Aviso", fontWeight = FontWeight.Bold, color = TextDefaultBlack) },
            text = { Text(errorMsg.orEmpty(), color = TextDefaultBlack) },
            confirmButton = { TextButton(onClick = { errorMsg = null }) { Text("OK") } }
        )
    }
}


// =====================================================
// ✅ 6) CARD DE CONTACTO (con resaltado)
// =====================================================
@Composable
private fun ContactoCard(
    contacto: ContactoUI,
    queryRawTokens: List<String>,
    queryDigits: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    val highlightStyle = SpanStyle(
        background = ButtonDarkPrimary.copy(alpha = 0.14f),
        fontWeight = FontWeight.ExtraBold,
        color = TextDefaultBlack
    )

    val tokensForHighlight = queryRawTokens
        .map { it.trim() }
        .filter { it.isNotBlank() && it.any(Char::isLetter) }

    val qDigitsClean = queryDigits.trim()
    val initials = remember(contacto.apellidosNombres) { initialsOf(contacto.apellidosNombres) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundDefault),
        border = BorderStroke(1.dp, TextDefaultBlack.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = ButtonDarkGray.copy(alpha = 0.10f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ButtonDarkGray
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = highlightTokens(contacto.apellidosNombres, tokensForHighlight, highlightStyle),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = TextDefaultBlack,
                        maxLines = 2
                    )

                    if (contacto.representante.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = highlightTokens(
                                "Representante: ${contacto.representante}",
                                tokensForHighlight,
                                highlightStyle
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDefaultBlack.copy(alpha = 0.65f),
                            maxLines = 2
                        )
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = ButtonDarkPrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = ButtonDarkError)
                }
            }

            Spacer(Modifier.height(12.dp))

            HorizontalDivider(thickness = 0.5.dp, color = ButtonDarkGray.copy(alpha = 0.14f))

            Spacer(Modifier.height(10.dp))

            // Teléfonos
            if (contacto.telefonos.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    contacto.telefonos.forEach { t ->
                        val tipo = highlightTokens("${t.tipo.label}:", tokensForHighlight, highlightStyle)

                        val numero = if (qDigitsClean.isNotBlank()) {
                            highlightDigitsInRawNumber(t.numero, qDigitsClean, highlightStyle)
                        } else {
                            highlightTokens(t.numero, tokensForHighlight, highlightStyle)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { openPhoneChooser(context, t.numero) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ButtonDarkSuccess
                            )

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = tipo,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextDefaultBlack.copy(alpha = 0.70f)
                            )

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = numero,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDefaultBlack,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = ButtonDarkGray.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Sin teléfonos registrados",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = TextDefaultBlack.copy(alpha = 0.45f)
                )
            }
        }
    }
}
private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val a = parts.getOrNull(0)?.firstOrNull()?.uppercaseChar()
    val b = parts.getOrNull(1)?.firstOrNull()?.uppercaseChar()
    return buildString {
        append(a ?: '?')
        if (b != null) append(b)
    }
}

// =====================================================
// ✅ 7) COLORES OutlinedTextField
// =====================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextDefaultBlack,
    unfocusedTextColor = TextDefaultBlack,
    focusedBorderColor = ButtonDarkPrimary,
    unfocusedBorderColor = ButtonDarkGray.copy(alpha = 0.7f),
    focusedLabelColor = TextDefaultBlack,
    unfocusedLabelColor = TextDefaultBlack.copy(alpha = 0.7f),
    cursorColor = TextDefaultBlack
)
