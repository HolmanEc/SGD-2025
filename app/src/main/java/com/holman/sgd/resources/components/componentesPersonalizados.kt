package com.holman.sgd.resources

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextOverflow
import com.holman.sgd.resources.components.getColorsCardsInicio
import com.holman.sgd.resources.screens.isTablet
import com.holman.sgd.ui.theme.*
import androidx.compose.foundation.Image
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.layout.ContentScale
import com.holman.sgd.ui.theme.BackgroundDefault

@Composable
fun FondoScreenDefault() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDefault)
        )
        Image(
            painter = androidx.compose.ui.res.painterResource(
                id = com.holman.sgd.R.drawable.fondodefault
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.06f
        )
    }
}



@Composable
fun CustomButton(
    text: String,
    borderColor: Color,
    onClick: () -> Unit,
    buttonHeight: Dp = 45.dp
) {
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    // 🎨 Animación del relleno
    val backgroundColor by animateColorAsState(
        targetValue = if (pressed) borderColor.copy(alpha = 0.8f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "boxButtonBackground"
    )

    // 📌 Texto en minúscula para comparación
    val lowerText = text.lowercase()

    // 🔍 Detectar icono si el texto contiene alguna de las palabras clave
    val icon: ImageVector? = when {
        lowerText.contains("iniciar") -> Icons.Filled.MeetingRoom
        lowerText.contains("volver") -> Icons.AutoMirrored.Filled.ArrowBack
        lowerText.contains("guardar") -> Icons.Default.Save
        lowerText.contains("cargar") -> Icons.Default.CloudUpload
        lowerText.contains("agregar") -> Icons.Default.Add
        lowerText.contains("cancelar") -> Icons.Default.Close
        lowerText.contains("actualizar") -> Icons.Default.Refresh
        lowerText.contains("eliminar") -> Icons.Default.Delete
        lowerText.contains("borrar") -> Icons.Default.Delete
        else -> null // ❗ Si no contiene ninguna palabra -> sin icono
    }

    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .height(buttonHeight)
            .background(backgroundColor, RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            awaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = {
                        scope.launch { onClick() }
                    }
                )
            }
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 3f
                        setShadowLayer(12f, 0f, 0f, borderColor.toArgb())
                    }

                    val gradient = LinearGradientShader(
                        from = Offset(0f, 0f),
                        to = Offset(size.width, 0f),
                        colors = listOf(borderColor, borderColor, borderColor),
                        colorStops = listOf(0f, 0.5f, 1f)
                    )
                    paint.shader = gradient

                    canvas.nativeCanvas.drawRoundRect(
                        1f, 1f,
                        size.width - 1f, size.height - 1f,
                        size.height / 2,
                        size.height / 2,
                        paint
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // ✅ Mostrar icono si se reconoció alguna palabra
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (pressed) TextoBotonClaro else borderColor,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 6.dp)
                )
            }

            Text(
                text = text.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = if (pressed) TextoBotonClaro else borderColor
            )
        }
    }
}

@Composable
fun NominaCard(
    nomina: NominaResumen,
    index: Int,
    onClick: (Color) -> Unit
) {
    val cardColors = getColorsCardsInicio()
    val backgroundColor = cardColors[index % cardColors.size]
    val fontColorCard = TextDefaultBlack
    val shape = RoundedCornerShape(12.dp)
    val isTablet = isTablet() // 👈 detección de dispositivo

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 4.dp, shape = shape, clip = false)
            .clip(shape)
            .clickable { onClick(backgroundColor) },
        shape = shape,
        color = backgroundColor,
        contentColor = fontColorCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 30.dp, horizontal = 40.dp)
        ) {
            Text(
                text = nomina.institucion,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = LocalContentColor.current.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))

            if (isTablet) {
                // ✅ Tablet: dos columnas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        InfoItem(Icons.Default.Person, "Docente", nomina.docente)
                        InfoItem(Icons.Default.Class, "Curso", nomina.curso)
                        InfoItem(Icons.Default.People, "Paralelo", nomina.paralelo)
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        InfoItem(Icons.Default.AutoStories, "Asignatura", nomina.asignatura)
                        InfoItem(Icons.Default.Star, "Especialidad", nomina.especialidad)
                        InfoItem(Icons.Default.Event, "Periodo", nomina.periodo)
                    }
                }
            } else {
                // ✅ Móvil: una sola columna
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    InfoItem(Icons.Default.Person, "Docente", nomina.docente)
                    InfoItem(Icons.Default.Class, "Curso", nomina.curso)
                    InfoItem(Icons.Default.People, "Paralelo", nomina.paralelo)
                    InfoItem(Icons.Default.Book, "Asignatura", nomina.asignatura)
                    InfoItem(Icons.Default.Star, "Especialidad", nomina.especialidad)
                    InfoItem(Icons.Default.Event, "Periodo", nomina.periodo)
                }
            }
        }
    }
}

@Composable
fun NominaHeaderCard(
    nomina: NominaResumen,
    backgroundColor: Color,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)
    val fontColorCard = TextDefaultBlack

    val clickableMod = if (onClick != null) Modifier.clip(shape).clickable { onClick() } else Modifier

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = shape, clip = false)
            .then(clickableMod),
        shape = shape,
        color = backgroundColor,
        contentColor = fontColorCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 30.dp, horizontal = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = nomina.institucion,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HorizontalDivider(thickness = 1.dp, color = LocalContentColor.current.copy(alpha = 0.5f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoItem(Icons.Default.AutoStories, "Asignatura", nomina.asignatura)
                    InfoItem(Icons.Default.Person, "Docente", nomina.docente)
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    InfoItem(Icons.Default.Class, "Curso", "${nomina.curso} ${nomina.paralelo} - ${nomina.especialidad}")
                    InfoItem(Icons.Default.Event, "Periodo", nomina.periodo)
                }
            }
        }
    }
}

@Composable
fun NominaReviewCard(
    nomina: NominaResumen,
    index: Int,
    isTablet: Boolean,
    isBusy: Boolean,
    onRevisar: (NominaResumen, Color) -> Unit,
    onBorrar: (NominaResumen) -> Unit,
    onEditar: (NominaResumen) -> Unit
) {
    val cardColors = getColorsCardsInicio()
    val backgroundColor = cardColors[index % cardColors.size]
    val fontColorCard = TextDefaultBlack
    val shape = RoundedCornerShape(12.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 4.dp, shape = shape, clip = false)
            .clip(shape)
            .clickable(enabled = !isBusy) { onRevisar(nomina, backgroundColor) },
        shape = shape,
        color = backgroundColor,
        contentColor = fontColorCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 40.dp)
        ) {
            // 🏫 Encabezado con botones de editar y eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nomina.institucion,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Row {
                    IconButton(
                        onClick = { if (!isBusy) onEditar(nomina) },
                        enabled = !isBusy
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar nómina",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { if (!isBusy) onBorrar(nomina) },
                        enabled = !isBusy
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar nómina",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = LocalContentColor.current.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            InfoItem(Icons.Default.Person, "Docente", nomina.docente)
                            InfoItem(Icons.Default.Class, "Curso", nomina.curso)
                            InfoItem(Icons.Default.People, "Paralelo", nomina.paralelo)
                        }
                        Spacer(Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            InfoItem(Icons.Default.AutoStories, "Asignatura", nomina.asignatura)
                            InfoItem(Icons.Default.Star, "Especialidad", nomina.especialidad)
                            InfoItem(Icons.Default.Event, "Periodo", nomina.periodo)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        InfoItem(Icons.Default.Person, "Docente", nomina.docente)
                        InfoItem(Icons.Default.Class, "Curso", nomina.curso)
                        InfoItem(Icons.Default.People, "Paralelo", nomina.paralelo)
                        InfoItem(Icons.Default.Book, "Asignatura", nomina.asignatura)
                        InfoItem(Icons.Default.Star, "Especialidad", nomina.especialidad)
                        InfoItem(Icons.Default.Event, "Periodo", nomina.periodo)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
fun LoadingDotsOverlay(
    isLoading: Boolean,
    //backgroundImage: Painter = painterResource(R.drawable.fondoloading),
    imageAlpha: Float = 0.05f,      // Transparencia de la imagen (0 = invisible, 1 = opaca)
    scrimAlpha: Float = 0.35f       // Opacidad de la máscara oscura
) {
    if (!isLoading) return

    Box(modifier = Modifier.fillMaxSize()) {
        // 1) Imagen semitransparente sobre tu UI existente
        //Image(
        //    painter = backgroundImage,
        //    contentDescription = null,
        //    contentScale = ContentScale.Crop,
        //   modifier = Modifier
        //       .fillMaxSize()
        //       .alpha(imageAlpha)
        // )

        // 2) Máscara oscura encima de la imagen
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        // 3) Puntos animados arriba de todo
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val transition = rememberInfiniteTransition(label = "dots")
                val offsetY by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = -20f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * 150)
                    ),
                    label = "dot-$index"
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .offset(y = offsetY.dp)
                        .background(colorPuntos, shape = CircleShape) // o MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RadialLoader() {
    Box(
        modifier = Modifier
            .size(175.dp, 80.dp),
        contentAlignment = Alignment.Center
    ) {
        // 🌀 Círculo grande (equivalente a ::before)
        val infiniteRotation1 = rememberInfiniteTransition(label = "rot1")
        val rotation1 by infiniteRotation1.animateFloat(
            initialValue = 0f,
            targetValue = -360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing)
            ),
            label = "rotation1"
        )

        Box(
            modifier = Modifier
                .size(70.dp)
                .graphicsLayer { rotationZ = rotation1 }
                .background(Color(0xFFFF3D00), shape = CircleShape)
                .drawBehind {
                    drawCircle(Color.White, radius = 8.dp.toPx(), center = center)
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(center.x, 0f))
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(0f, center.y))
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(size.width, center.y))
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(center.x, size.height))
                }
        )

        // 🌀 Círculo pequeño (equivalente a ::after)
        val infiniteRotation2 = rememberInfiniteTransition(label = "rot2")
        val rotation2 by infiniteRotation2.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing)
            ),
            label = "rotation2"
        )

        Box(
            modifier = Modifier
                .size(45.dp)
                .graphicsLayer { rotationZ = rotation2 }
                .background(Color(0xFFFF3D00), shape = CircleShape)
                .drawBehind {
                    drawCircle(Color.White, radius = 5.dp.toPx(), center = center)
                    drawCircle(Color.White, radius = 2.5.dp.toPx(), center = Offset(center.x, 0f))
                    drawCircle(Color.White, radius = 2.5.dp.toPx(), center = Offset(0f, center.y))
                    drawCircle(Color.White, radius = 2.5.dp.toPx(), center = Offset(size.width, center.y))
                    drawCircle(Color.White, radius = 2.5.dp.toPx(), center = Offset(center.x, size.height))
                }
        )
    }
}

@Composable
fun FloatingSaveButton(
    visible: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Save,
    contentDesc: String = "Guardar",
    container: Color = BtnFlotanteGuardarEnNomiba,
    content: Color = TextDefaultWhite,
    cornerRadius: Float = 16f
) {
    if (!visible) return

    // Detecta si el botón está siendo presionado
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animación de escala cuando se presiona
    val targetScale = if (isPressed) 0.95f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fabScale"
    )

    FloatingActionButton(
        onClick = onClick,
        interactionSource = interactionSource,
        containerColor = container,
        contentColor = content,
        shape = RoundedCornerShape(cornerRadius),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 10.dp
        ),
        modifier = modifier.graphicsLayer(
            scaleX = scale,
            scaleY = scale
        )
    ) {
        Icon(icon, contentDescription = contentDesc)
    }
}

@Composable
fun FloatingExportButton(
    visible: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.FileDownload,
    contentDesc: String = "Exportar",
    container: Color = BtnFlotanteExportarNotas,
    content: Color = TextDefaultWhite,
    cornerRadius: Float = 16f
) {
    if (!visible) return

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = if (isPressed) 0.95f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fabExportScale"
    )

    FloatingActionButton(
        onClick = onClick,
        interactionSource = interactionSource,
        containerColor = container,
        contentColor = content,
        shape = RoundedCornerShape(cornerRadius),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 10.dp
        ),
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Icon(icon, contentDescription = contentDesc)
    }
}



@Composable
fun VistaPreviaTablaExcel(
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título
        Text(
            text = "Vista previa de la nómina",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Subtítulo
        Text(
            text = "Cargue un archivo Excel (.xlsx) para ver aquí la lista de estudiantes.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Tabla fantasma — ocupa todo el alto restante
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Encabezado (3 columnas)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FondoGris.copy(alpha = 0.4f))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("ID", "CÉDULA", "ESTUDIANTE").forEach { col ->
                    Text(
                        text = col,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextoClaroLight
                        )
                    )
                }
            }

            // Filas fantasma distribuidas para llenar el espacio
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(8) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(FondoGris.copy(alpha = 0.2f))
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = BordeGris.copy(alpha = 0.3f))
                }
            }
        }

        // Pie sutil
        Text(
            text = "Esta tabla se actualizará automáticamente al cargar la nómina.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}


@Suppress("DEPRECATION") // evita el warning de 'view' obsoleto
fun mensajealert(context: Context, message: String) {
    val textView = TextView(context).apply {
        text = message
        setTextColor(android.graphics.Color.BLACK)
        textSize = 14f
        gravity = Gravity.CENTER
        setPadding(40, 30, 40, 30)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 40f
            setColor(android.graphics.Color.WHITE)
        }
    }

    val cardView = androidx.cardview.widget.CardView(context).apply {
        radius = 40f
        cardElevation = 12f
        setCardBackgroundColor(android.graphics.Color.WHITE)
        addView(textView)
        setContentPadding(0, 0, 0, 0)
    }

    val toast = Toast(context).apply {
        duration = Toast.LENGTH_SHORT
        view = cardView
        val yOffset = (16 * context.resources.displayMetrics.density).toInt()
        setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, yOffset)
    }

    toast.show()
}



////////////////////

@Composable
fun LoadingDotsOverlayx(isLoading: Boolean) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            RadialLoader()
        }
    }
}


@Composable
fun CustomButtonokokokoko(
    text: String,
    borderColor: Color,
    onClick: () -> Unit,
    buttonHeight: Dp = 45.dp
) {
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    // 🎨 Animación del relleno
    val backgroundColor by animateColorAsState(
        targetValue = if (pressed) borderColor.copy(alpha = 0.8f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "boxButtonBackground"
    )

    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .height(buttonHeight)
            .background(backgroundColor, RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            // Espera hasta que el usuario suelte
                            awaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = {
                        // 🔹 Lanza acción al final del tap
                        scope.launch {
                            onClick()
                        }
                    }
                )
            }
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 3f
                        setShadowLayer(
                            12f, 0f, 0f,
                            borderColor.toArgb()
                        )
                    }

                    val gradient = LinearGradientShader(
                        from = Offset(0f, 0f),
                        to = Offset(size.width, 0f),
                        colors = listOf(
                            borderColor,
                            borderColor,
                            //Color(0xFFF11BEE),
                            borderColor
                        ),
                        colorStops = listOf(0f, 0.5f, 1f)
                    )
                    paint.shader = gradient

                    canvas.nativeCanvas.drawRoundRect(
                        1f, 1f,
                        size.width - 1f, size.height - 1f,
                        size.height / 2,
                        size.height / 2,
                        paint
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = if (pressed) TextoBotonClaro  else borderColor
        )
    }
}


@Composable
fun CustomButton1(
    text: String,
    borderColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isPressed) borderColor.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = borderColor
        ),
        border = null,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .height(48.dp)
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = borderColor.toArgb()
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 4f
                        setShadowLayer(
                            10f,
                            0f,
                            0f,
                            borderColor.copy(alpha = 0.9f).toArgb()
                        )
                    }

                    // 🔲 Rectángulo con esquinas redondeadas
                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        35f, // radio X
                        35f, // radio Y
                        paint
                    )
                }
            }
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun CustomButtonok(
    text: String,
    borderColor: Color,
    onClick: () -> Unit,
    buttonHeight: Dp = 45.dp
) {
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    // 🎨 Animación del relleno
    val backgroundColor by animateColorAsState(
        targetValue = if (pressed) borderColor.copy(alpha = 0.8f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "boxButtonBackground"
    )

    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .height(buttonHeight)
            .background(backgroundColor, RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            // 👇 espera a que el usuario suelte
                            awaitRelease()
                        } finally {
                            // 👇 dale un respiro a la animación del fondo
                            scope.launch {
                                delay(80) // siempre se verá aunque sea tap rápido
                                pressed = false
                            }
                        }
                    },
                    onTap = {
                        scope.launch { onClick() }
                    }
                )
            }
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 3f
                        setShadowLayer(
                            12f, 0f, 0f,
                            borderColor.toArgb()
                        )
                    }

                    val gradient = LinearGradientShader(
                        from = Offset(0f, 0f),
                        to = Offset(size.width, 0f),
                        colors = listOf(
                            borderColor,
                            Color(0xFFF11BEE),
                            borderColor
                        ),
                        colorStops = listOf(0f, 0.5f, 1f)
                    )
                    paint.shader = gradient

                    canvas.nativeCanvas.drawRoundRect(
                        1f, 1f,
                        size.width - 1f, size.height - 1f,
                        size.height / 2,
                        size.height / 2,
                        paint
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = if (pressed) Color.White else borderColor
        )
    }
}

