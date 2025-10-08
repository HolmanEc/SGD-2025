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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.holman.sgd.resources.mensajealert
import com.holman.sgd.resources.screens.isTablet
import com.holman.sgd.ui.theme.*


private const val USUARIOS_COLLECTION = "usuarios" // ajusta si se llama distinto

fun sendResetEmail(
    auth: FirebaseAuth,
    context: Context,
    email: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onSuccess: () -> Unit = {}
) {
    val mail = email.trim().lowercase()
    if (mail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
        mensajealert(context, "Por favor, ingresa un correo válido.")
        return
    }

    auth.sendPasswordResetEmail(mail)
        .addOnSuccessListener {
            // Éxito real: muestra siempre el mensaje claro
            mensajealert(context, "Te enviamos un enlace para restablecer tu contraseña.")
            onSuccess()
        }
        .addOnFailureListener { e ->
            val code = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode ?: ""
            when (code) {
                "ERROR_OPERATION_NOT_ALLOWED" -> {
                    // Método correo/contraseña deshabilitado en la consola
                    mensajealert(context, "Habilita Correo/Contraseña en Firebase → Authentication → Método de acceso.")
                }
                "ERROR_USER_NOT_FOUND" -> {
                    // No existe en Auth: verifica tu BD para dar feedback útil
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
                        }
                        .addOnFailureListener { fe ->
                            mensajealert(context, fe.localizedMessage ?: "Error al verificar el registro.")
                        }
                }
                else -> {
                    // Otros errores (conectividad, etc.)
                    mensajealert(context, e.localizedMessage ?: "No se pudo enviar el correo.")
                }
            }
        }
}

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    // Preferencias seguras
    private val PREFS_NAME = "login_prefs_secure"
    private val KEY_REMEMBER = "remember_email"
    private val KEY_EMAIL = "email_only"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // Política: siempre deslogueado al abrir
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
            LoginScreen(
                initialEmail = if (remembered) savedEmail else "",
                initialRemember = remembered,
                onLogin = { email, password, remember ->
                    signInUser(
                        email = email,
                        password = password,
                        onSuccess = {
                            if (remember) {
                                securePrefs.edit()
                                    .putBoolean(KEY_REMEMBER, true)
                                    .putString(KEY_EMAIL, email)
                                    .apply()
                            } else {
                                securePrefs.edit().clear().apply()
                            }
                            goToMainAndFinish()
                        },
                        onError = { msg -> mensajealert(this, msg) }
                    )
                }
            )
        }
    }

    private fun goToMainAndFinish() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Login con Firebase
// Login con Firebase (envía verificación solo la PRIMERA vez)
// Login con Firebase (sin exigir verificación de correo)
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

        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(mail, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(mapFirebaseLoginError(e))
            }
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

@Composable
fun LoginScreen(
    initialEmail: String,
    initialRemember: Boolean,
    onLogin: (String, String, Boolean) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var rememberEmail by remember { mutableStateOf(initialRemember) }

    val commonDomains = listOf(
        "demo.com", "gmail.com", "hotmail.com", "outlook.com", "yahoo.com", "icloud.com", "live.com"
    )

    var emailFieldFocused by remember { mutableStateOf(false) }
    var suggestionsExpanded by remember { mutableStateOf(false) }

    val localPart = email.substringBefore("@", missingDelimiterValue = email)
    val typedDomainPart = email.substringAfter("@", missingDelimiterValue = "")

    val suggestions = remember(email) {
        when {
            email.isBlank() -> emptyList()
            !email.contains("@") -> commonDomains.map { "$localPart@$it" }
            else -> commonDomains
                .filter { it.startsWith(typedDomainPart, ignoreCase = true) }
                .map { "$localPart@$it" }
        }
    }

    val passwordFocusRequester = remember { FocusRequester() }

    // Colores “raya”
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
                .padding(horizontal = 90.dp)
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

            // ===== EMAIL =====
            Box(modifier = Modifier.fillMaxWidth()) {
                var textFieldWidthPx by remember { mutableStateOf(0) }
                val density = LocalDensity.current

                TextField(
                    value = email,
                    onValueChange = {
                        email = it.lowercase()
                        suggestionsExpanded =
                            emailFieldFocused &&
                                    email.isNotBlank() &&
                                    (!email.contains("@") || typedDomainPart.isNotEmpty()) &&
                                    suggestions.isNotEmpty()
                    },
                    label = { Text("Correo electrónico", color = labelText) },
                    placeholder = { Text("tu@correo.com", color = hintText) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = hintText
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            emailFieldFocused = it.isFocused
                            suggestionsExpanded =
                                it.isFocused &&
                                        email.isNotBlank() &&
                                        (!email.contains("@") || typedDomainPart.isNotEmpty()) &&
                                        suggestions.isNotEmpty()
                        }
                        .onGloballyPositioned { coords -> textFieldWidthPx = coords.size.width },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
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

            // ===== PASSWORD =====
            var showPass by rememberSaveable { mutableStateOf(false) }
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña", color = labelText) },
                placeholder = { Text("••••••••", color = hintText) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = hintText
                    )
                },
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
                keyboardActions = KeyboardActions(onDone = { onLogin(email, password, rememberEmail) }),
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


            // ===== Recordar correo + Olvidé mi contraseña =====
            if (isTablet()) {
                // ✅ Tablet: en una sola fila como ya lo tenías
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = rememberEmail,
                        onCheckedChange = { rememberEmail = it },
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
                            .clickable {
                                if (email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    sendResetEmail(auth, context, email)
                                } else {
                                    resetEmail = email
                                    showResetDialog = true
                                }
                            }
                            .padding(start = 8.dp)
                    )
                }
            } else {
                // 📱 Teléfono: uno debajo del otro
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
                        modifier = Modifier.clickable {
                            if (email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                sendResetEmail(auth, context, email)
                            } else {
                                resetEmail = email
                                showResetDialog = true
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Botón login
            CustomButton(
                text = "Iniciar sesión",
                borderColor = ButtonWhitePrimary,
                onClick = { onLogin(email, password, rememberEmail) }
            )
        }
    }

    // ===== Diálogo de restablecer contraseña =====
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Restablecer contraseña",
                        color = TextDefaultBlack,
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
                    Text(
                        "Ingresa tu correo para enviarte el enlace de restablecimiento.",
                        color = TextDefaultBlack
                    )
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it.lowercase() },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextDefaultBlack,
                            unfocusedTextColor = TextDefaultBlack,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = TextDefaultBlack,
                            focusedIndicatorColor = ButtonDarkSuccess,
                            unfocusedIndicatorColor = TextDefaultBlack
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
                                sendResetEmail(auth, context, resetEmail) {
                                    showResetDialog = false
                                }
                            }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CustomButton(
                            text = "Cancelar",
                            borderColor = ButtonDarkGray,
                            onClick = { showResetDialog = false }
                        )
                    }
                }
            },
            dismissButton = {}, // ✅ Ya no usamos dismissButton
            containerColor = Color.White
        )
    }

}
