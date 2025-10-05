package com.holman.sgd.resources

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.holman.sgd.ui.theme.VioletaClaroLight

@SuppressLint("ContextCastToActivity")
@Composable
fun Tutoria() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VioletaClaroLight),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Contenido de Tutoria", fontSize = 22.sp, fontWeight = FontWeight.Medium)
    }
}