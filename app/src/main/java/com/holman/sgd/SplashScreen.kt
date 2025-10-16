package com.holman.sgd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AppStartSplashGate(
    modifier: Modifier = Modifier,
    minShowMillis: Long = 350L,
    initializer: suspend () -> Unit = {},
    splashContent: @Composable () -> Unit = { DefaultSplash() },
    content: @Composable () -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val t0 = System.currentTimeMillis()
        initializer()
        val elapsed = System.currentTimeMillis() - t0
        val remain = (minShowMillis - elapsed).coerceAtLeast(0L)
        if (remain > 0) delay(remain)
        showSplash = false
    }

    Box(modifier.fillMaxSize()) {
        content()                 // se dibuja debajo
        if (showSplash) splashContent() // SPLASH 100% opaco encima desde el primer frame
    }
}

@Composable
fun PostLoginSplashOverlay(
    visible: Boolean,
    splashContent: @Composable () -> Unit = { DefaultSplash() }
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120, easing = LinearEasing)),
        exit = fadeOut(tween(180, easing = FastOutSlowInEasing))
    ) {
        splashContent()
    }
}

@Composable
private fun DefaultSplash() {
    // Usa EXACTAMENTE el mismo color que en bg_app_window.xml
    val background = Color(0xFF212121)
    val logoSize = 110.dp // igual que android:width/height del layer-list

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background) // opaco desde el primer frame
    ) {
        // Logo fijo, centrado (no se mueve jamás)
        Image(
            painter = painterResource(id = R.drawable.logo_launcher),
            contentDescription = null,
            modifier = Modifier
                .size(logoSize)
                .align(Alignment.Center)
        )

        // Loader absoluto (no empuja al logo). Opcional: pequeño retraso.
        var showLoader by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(120); showLoader = true }
        if (showLoader) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.Center)
                    .offset(y = logoSize / 2 + 20.dp),
                color = Color.White.copy(alpha = 0.9f),
                strokeWidth = 3.dp
            )
        }
    }
}
