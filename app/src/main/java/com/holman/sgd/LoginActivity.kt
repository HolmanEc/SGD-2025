// ===== IMPORTS =====
package com.holman.sgd

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.holman.sgd.resources.CustomButton
import com.holman.sgd.resources.LoadingDotsOverlay
import com.holman.sgd.resources.mensajealert
import com.holman.sgd.resources.screens.isTablet
import com.holman.sgd.ui.theme.*
import kotlinx.coroutines.delay

// ============================================================================
// CONSTANTES / CONFIG
// ============================================================================

private const val USUARIOS_COLLECTION = "usuarios" // ajusta si se llama distinto
private const val PREFS_NAME = "login_prefs_secure"
private const val KEY_REMEMBER = "remember_email"
private const val KEY_EMAIL = "email_only"

// ============================================================================
// ACTIVIDAD PRINCIPAL
// ============================================================================
class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase (si ya lo inicializas en Application, puedes quitar initializeApp aquí)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // Política: siempre deslogueado al abrir (se mantiene)
        auth.signOut()

        // Encrypted SharedPreferences
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val securePrefs = EncryptedSharedPreferences.create(
            this,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val remembered = securePrefs.getBoolean(KEY_REMEMBER, false)
        val savedEmail = securePrefs.getString(KEY_EMAIL, "") ?: ""

        setContent {
            AppStartSplashGate(
                minShowMillis = 400L,
                initializer = { /* precargas ligeras si quieres */ }
            ) {
                LoginScreen(
                    initialEmail = if (remembered) savedEmail else "",
                    initialRemember = remembered,
                    onLogin = { email, password, remember, reportResult ->
                        signInUser(
                            email = email,
                            password = password,
                            onSuccess = {
                                if (remember) {
                                    securePrefs.edit()
                                        .putBoolean(KEY_REMEMBER, true)
                                        .putString(KEY_EMAIL, email.trim().lowercase())
                                        .apply()
                                } else {
                                    securePrefs.edit().clear().apply()
                                }

                                reportResult(true)
                                goToMainAndFinish()
                            },
                            onError = { msg ->
                                reportResult(false)
                                mensajealert(this, msg)
                            }
                        )
                    }
                )
            }
        }
    }

    private fun goToMainAndFinish() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun signInUser(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val mail = email.trim().lowercase()

        if (mail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            onError("Ingresa un correo válido.")
            return
        }
        if (password.isBlank()) {
            onError("Ingresa tu contraseña.")
            return
        }

        // ✅ Mejora: usar la instancia auth ya creada
        auth.signInWithEmailAndPassword(mail, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(mapFirebaseLoginError(e)) }
    }

    private fun mapFirebaseLoginError(e: Exception): String {
        val code = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode ?: ""
        return when (code) {
            "ERROR_INVALID_EMAIL" -> "El correo no tiene un formato válido."
            "ERROR_USER_NOT_FOUND" -> "Ese usuario no existe."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" ->
                "Contraseña incorrecta."
            "ERROR_USER_DISABLED" -> "Esta cuenta está deshabilitada."
            "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos fallidos. Inténtalo más tarde."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Sin conexión. Verifica tu internet."
            "ERROR_OPERATION_NOT_ALLOWED" -> "El método de acceso está deshabilitado en Firebase."
            else -> e.localizedMessage ?: "Error de autenticación ($code)."
        }
    }
}

// ============================================================================
// COMPOSABLE: LOGIN SCREEN
// ============================================================================

@Composable
fun LoginScreen(
    initialEmail: String,
    initialRemember: Boolean,
    onLogin: (String, String, Boolean, (Boolean) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var rememberEmail by remember { mutableStateOf(initialRemember) }

    val commonDomains = remember {
        listOf("demo.com", "gmail.com", "hotmail.com", "outlook.com", "yahoo.com", "icloud.com", "live.com")
    }

    var emailFieldFocused by remember { mutableStateOf(false) }
    var suggestionsExpanded by remember { mutableStateOf(false) }

    val suggestions: List<String> = remember(email) {
        buildEmailSuggestions(email, commonDomains)
    }

    val typedDomainPart: String = email.substringAfter("@", missingDelimiterValue = "")

    val passwordFocusRequester = remember { FocusRequester() }

    // Colores (los tuyos)
    val fieldText = TextDefaultWhite
    val labelText = TextDefaultWhite.copy(alpha = 0.90f)
    val hintText = TextDefaultWhite.copy(alpha = 0.75f)
    val focusedLine = ButtonDarkSuccess
    val unfocusedLine = TextDefaultWhite.copy(alpha = 0.55f)

    // Reset password
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    // Overlay login
    var loggingIn by rememberSaveable { mutableStateOf(false) }

    // Overlay reset email
    var sendingResetEmail by rememberSaveable { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // ✅ NUEVO: focus requester para el campo del dialog
    val resetEmailFocusRequester = remember { FocusRequester() }

    // ✅ NUEVO: al abrir el dialog, pedir foco + mostrar teclado
    LaunchedEffect(showResetDialog) {
        if (showResetDialog) {
            delay(120)
            resetEmailFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Fondo
        Image(
            painter = painterResource(id = R.drawable.fondologin),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isTablet()) 90.dp else 28.dp)
                .padding(top = 56.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo_launcher),
                contentDescription = "Logo de la app",
                modifier = Modifier
                    .size(110.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Bienvenido:",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextDefaultWhite,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // ===================== EMAIL =====================
            Box(modifier = Modifier.fillMaxWidth()) {
                var textFieldWidthPx by remember { mutableStateOf(0) }
                val density = LocalDensity.current

                TextField(
                    value = email,
                    onValueChange = { newValue ->
                        val newEmail = newValue.lowercase()
                        email = newEmail

                        val newTypedDomain = newEmail.substringAfter("@", missingDelimiterValue = "")
                        val hasSuggestions = buildEmailSuggestions(newEmail, commonDomains).isNotEmpty()

                        suggestionsExpanded =
                            emailFieldFocused &&
                                    newEmail.isNotBlank() &&
                                    (!newEmail.contains("@") || newTypedDomain.isNotEmpty()) &&
                                    hasSuggestions
                    },
                    label = { Text("Correo electrónico", color = labelText) },
                    placeholder = { Text("tu@correo.com", color = hintText) },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = hintText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { st ->
                            emailFieldFocused = st.isFocused
                            suggestionsExpanded =
                                st.isFocused &&
                                        email.isNotBlank() &&
                                        (!email.contains("@") || typedDomainPart.isNotEmpty()) &&
                                        suggestions.isNotEmpty()
                        }
                        .onGloballyPositioned { coords -> textFieldWidthPx = coords.size.width },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = fieldText,
                        unfocusedTextColor = fieldText,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = fieldText,
                        focusedLabelColor = labelText,
                        unfocusedLabelColor = labelText,
                        focusedIndicatorColor = focusedLine,
                        unfocusedIndicatorColor = unfocusedLine
                    )
                )

                DropdownMenu(
                    expanded = suggestionsExpanded,
                    onDismissRequest = { suggestionsExpanded = false },
                    properties = PopupProperties(
                        focusable = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    ),
                    modifier = Modifier
                        .width(with(density) { textFieldWidthPx.toDp() })
                        .heightIn(max = 180.dp)
                        .background(BackgroundDefault)
                ) {
                    suggestions.take(6).forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontSize = 14.sp, color = TextDefaultBlack) },
                            onClick = {
                                email = option.lowercase()
                                suggestionsExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===================== PASSWORD =====================
            var showPass by rememberSaveable { mutableStateOf(false) }

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña", color = labelText) },
                placeholder = { Text("••••••••", color = hintText) },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = hintText) },
                trailingIcon = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(
                            imageVector = if (showPass) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = hintText
                        )
                    }
                },
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)

                        loggingIn = true
                        suggestionsExpanded = false

                        onLogin(email, password, rememberEmail) { success ->
                            if (!success) loggingIn = false
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = fieldText,
                    unfocusedTextColor = fieldText,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = fieldText,
                    focusedLabelColor = labelText,
                    unfocusedLabelColor = labelText,
                    focusedIndicatorColor = focusedLine,
                    unfocusedIndicatorColor = unfocusedLine
                )
            )

            Spacer(Modifier.height(12.dp))

            // ===================== RECORDAR / RESET =====================
            if (isTablet()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = rememberEmail,
                        onCheckedChange = { rememberEmail = it },
                        enabled = !loggingIn && !sendingResetEmail,
                        colors = CheckboxDefaults.colors(
                            checkedColor = ButtonDarkSuccess,
                            uncheckedColor = TextDefaultWhite,
                            checkmarkColor = TextDefaultWhite
                        )
                    )
                    Text("Recordar mi correo", color = TextDefaultWhite)

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = TextDefaultWhite.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clickable(enabled = !loggingIn && !sendingResetEmail) {
                                if (email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    sendingResetEmail = true
                                    sendResetEmail(
                                        auth = auth,
                                        context = context,
                                        email = email,
                                        onFinish = { sendingResetEmail = false }
                                    )
                                } else {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)

                                    resetEmail = email
                                    email = ""
                                    suggestionsExpanded = false
                                    showResetDialog = true
                                }
                            }
                            .padding(start = 8.dp)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberEmail,
                            onCheckedChange = { rememberEmail = it },
                            enabled = !loggingIn && !sendingResetEmail,
                            colors = CheckboxDefaults.colors(
                                checkedColor = ButtonDarkSuccess,
                                uncheckedColor = TextDefaultWhite,
                                checkmarkColor = TextDefaultWhite
                            )
                        )
                        Text("Recordar mi correo", color = TextDefaultWhite)
                    }

                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = TextDefaultWhite.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable(enabled = !loggingIn && !sendingResetEmail) {
                            if (email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                sendingResetEmail = true
                                sendResetEmail(
                                    auth = auth,
                                    context = context,
                                    email = email,
                                    onFinish = { sendingResetEmail = false }
                                )
                            } else {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)

                                resetEmail = email
                                email = ""
                                suggestionsExpanded = false
                                showResetDialog = true
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // ===================== BOTÓN LOGIN =====================
            CustomButton(
                text = "Iniciar sesión",
                borderColor = ButtonWhitePrimary,
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)

                    loggingIn = true
                    suggestionsExpanded = false

                    onLogin(email, password, rememberEmail) { success ->
                        if (!success) loggingIn = false
                    }
                }
            )
        }

        // ===================== OVERLAYS =====================
        PostLoginSplashOverlay(visible = loggingIn)
        LoadingDotsOverlay(isLoading = sendingResetEmail)
    }

    // ===================== DIALOG RESET =====================

    if (showResetDialog) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                onSurface = TextDefaultBlack,
                onSurfaceVariant = TextDefaultBlack,
                onBackground = TextDefaultBlack
            )
        ) {
            AlertDialog(
                onDismissRequest = { if (!sendingResetEmail) showResetDialog = false },
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Restablecer contraseña",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ingresa tu correo para enviarte el enlace de restablecimiento.")
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it.lowercase() },
                            label = { Text("Correo electrónico222222222") },
                            singleLine = true,
                            enabled = !sendingResetEmail,
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .focusRequester(resetEmailFocusRequester),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextDefaultBlack,
                                unfocusedTextColor = TextDefaultBlack,

                                // ✅ label siempre negro (sin morado)
                                focusedLabelColor = TextDefaultBlack,
                                unfocusedLabelColor = TextDefaultBlack,
                                disabledLabelColor = TextDefaultBlack.copy(alpha = 0.4f),
                                errorLabelColor = TextDefaultBlack,

                                cursorColor = TextDefaultBlack,

                                // ✅ borde fijo (no “engorda” visualmente como la línea del TextField)
                                focusedBorderColor = ButtonDarkSuccess,
                                unfocusedBorderColor = TextDefaultBlack.copy(alpha = 0.6f),
                                disabledBorderColor = TextDefaultBlack.copy(alpha = 0.3f),
                                errorBorderColor = ButtonDarkSuccess,

                                // ✅ sin fondo
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent
                            )
                        )

                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomButton(
                                text = "Enviar",
                                borderColor = ButtonDarkSuccess,
                                onClick = {
                                    val mail = resetEmail.trim().lowercase()
                                    if (mail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
                                        sendResetEmail(auth, context, mail)
                                        return@CustomButton
                                    }
                                    email = mail
                                    showResetDialog = false
                                    sendingResetEmail = true
                                    sendResetEmail(
                                        auth = auth,
                                        context = context,
                                        email = mail,
                                        onFinish = { sendingResetEmail = false }
                                    )
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomButton(
                                text = "Cancelar",
                                borderColor = ButtonDarkGray,
                                onClick = { if (!sendingResetEmail) showResetDialog = false }
                            )
                        }
                    }
                },
                dismissButton = {},
                containerColor = BackgroundDefault
            )
        }
    }

}


// ============================================================================
// FUNCIONES AUXILIARES
// ============================================================================

// Funcion que envia el correo de recuperacion.
fun sendResetEmail(
    auth: FirebaseAuth,
    context: Context,
    email: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onSuccess: () -> Unit = {},
    onFinish: () -> Unit = {}
) {
    val mail = email.trim().lowercase()

    if (mail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
        mensajealert(context, "Por favor, ingresa un correo válido.")
        onFinish()
        return
    }

    auth.sendPasswordResetEmail(mail)
        .addOnSuccessListener {
            mensajealert(context, "Si estas registrado, recibiras un correo de recuperación. Revisa tu Spam.")
            onSuccess()
            onFinish()
        }
        .addOnFailureListener { e ->

            // 1) Si es FirebaseAuthException, aquí viene errorCode (a veces)
            val authCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode ?: ""

            // 2) Otros tipos comunes (NO traen errorCode)
            val isTooMany =
                e is com.google.firebase.FirebaseTooManyRequestsException ||
                        authCode == "ERROR_TOO_MANY_REQUESTS" ||
                        authCode == "TOO_MANY_ATTEMPTS_TRY_LATER" ||
                        // fallback por texto típico (a veces viene así)
                        (e.message?.contains("blocked all requests", ignoreCase = true) == true) ||
                        (e.message?.contains("unusual activity", ignoreCase = true) == true)

            when {
                isTooMany -> {
                    mensajealert(
                        context,
                        "Has solicitado la recuperación muchas veces. Espera unos minutos y vuelve a intentarlo."
                    )
                    onFinish()
                }

                authCode == "ERROR_OPERATION_NOT_ALLOWED" -> {
                    mensajealert(
                        context,
                        "El método Correo/Contraseña está deshabilitado en Firebase. Habilítalo en Authentication → Método de acceso."
                    )
                    onFinish()
                }

                authCode == "ERROR_NETWORK_REQUEST_FAILED" -> {
                    mensajealert(context, "Sin conexión. Verifica tu internet e inténtalo de nuevo.")
                    onFinish()
                }

                authCode == "ERROR_USER_NOT_FOUND" -> {
                    firestore.collection(USUARIOS_COLLECTION)
                        .whereEqualTo("email", mail)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { snap ->
                            if (!snap.isEmpty) {
                                mensajealert(
                                    context,
                                    "Tu registro existe en la base de datos, pero la cuenta no está habilitada para iniciar sesión. Contacta al administrador."
                                )
                            } else {
                                mensajealert(context, "Ese correo no está registrado.")
                            }
                            onFinish()
                        }
                        .addOnFailureListener {
                            mensajealert(context, "Ocurrió un error al verificar el correo. Inténtalo de nuevo.")
                            onFinish()
                        }
                }

                else -> {
                    // ✅ Genérico 100% en español
                    mensajealert(context, e.localizedMessage ?: "El Sistema tuvo incovenientes. No se pudo enviar el correo.")
                    onFinish()
                }
            }
        }
}

//
// ============================================================================
// HELPERS (sugerencias email)
// ============================================================================

/**
 * Construye sugerencias en base a lo que el usuario va escribiendo:
 * - si no hay "@": sugiere localPart + @dominio
 * - si hay "@": filtra dominios que empiezan por lo tipeado
 */
private fun buildEmailSuggestions(
    email: String,
    commonDomains: List<String>
): List<String> {
    if (email.isBlank()) return emptyList()

    val localPart = email.substringBefore("@", missingDelimiterValue = email)
    val typedDomainPart = email.substringAfter("@", missingDelimiterValue = "")

    return if (!email.contains("@")) {
        commonDomains.map { "$localPart@$it" }
    } else {
        commonDomains
            .filter { it.startsWith(typedDomainPart, ignoreCase = true) }
            .map { "$localPart@$it" }
    }
}
