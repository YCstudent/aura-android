package com.edistrive.aura.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropperScreen(
    imageUri: Uri,
    onCrop: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var loadError by remember { mutableStateOf(false) }

    val originalBitmap = remember(imageUri) {
        runCatching {
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    if (originalBitmap == null) {
        if (!loadError) {
            loadError = true
            // Show error and dismiss after short delay
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1500)
                onDismiss()
            }
        }
        // Show error overlay
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("无法加载图片，请重试", color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("返回", color = Color(0xFF1A8080), fontSize = 16.sp)
                }
            }
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetXPx by remember { mutableFloatStateOf(0f) }
    var offsetYPx by remember { mutableFloatStateOf(0f) }
    var cropSizePx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("裁剪头像", fontSize = 17.sp) },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text("取消", color = Color.White, fontSize = 16.sp)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            if (cropSizePx > 0) {
                                val cropped = cropBitmap(
                                    original = originalBitmap,
                                        scale = scale,
                                        offsetXPx = offsetXPx,
                                        offsetYPx = offsetYPx,
                                        screenCropSizePx = cropSizePx
                                    )
                                    val file = File(context.cacheDir, "avatar_cropped.jpg")
                                    file.outputStream().use { out ->
                                        cropped.compress(Bitmap.CompressFormat.JPEG, 70, out)
                                    }
                                    onCrop(file)
                                }
                        }) {
                            Text(
                                "完成",
                                color = Color(0xFF1A8080),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Black
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Crop area: min(screenWidth, screenHeight) - 80dp
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val cropAreaDp = minOf(maxWidth, maxHeight) - 80.dp

                    Box(
                        modifier = Modifier
                            .size(cropAreaDp)
                            .clipToBounds()
                            .onSizeChanged { size ->
                                cropSizePx = size.width.toFloat()
                            }
                    ) {
                        // Image with scaledToFill (ContentScale.Crop) + user zoom/pan
                        Image(
                            bitmap = originalBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetXPx
                                    translationY = offsetYPx
                                }
                        )

                        // Circular border + semi-transparent overlay with cutout
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val circleRadius = size.minDimension / 2f
                            val circleCenter = Offset(size.width / 2f, size.height / 2f)

                            // Semi-transparent overlay with circular cutout via EvenOdd fill
                            val overlayPath = androidx.compose.ui.graphics.Path().apply {
                                addRect(
                                    androidx.compose.ui.geometry.Rect(
                                        0f, 0f, size.width, size.height
                                    )
                                )
                                addOval(
                                    androidx.compose.ui.geometry.Rect(
                                        circleCenter.x - circleRadius,
                                        circleCenter.y - circleRadius,
                                        circleCenter.x + circleRadius,
                                        circleCenter.y + circleRadius
                                    )
                                )
                                fillType = PathFillType.EvenOdd
                            }
                            drawPath(overlayPath, color = Color.Black.copy(alpha = 0.5f))

                            // White circular border (3px)
                            drawCircle(
                                color = Color.White,
                                radius = circleRadius,
                                center = circleCenter,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }

                    // Gesture overlay (covers only the crop area)
                    Box(
                        modifier = Modifier
                            .size(cropAreaDp)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 5.0f)
                                    offsetXPx += pan.x
                                    offsetYPx += pan.y
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Hint text + reset button (matching iOS)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "双指缩放，单指拖动调整位置",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = {
                            scale = 1f
                            offsetXPx = 0f
                            offsetYPx = 0f
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        )
                    ) {
                        Text(
                            "重置",
                            color = Color(0xFF1A8080),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

/**
 * Crop bitmap matching iOS ImageCropperView.cropImage() logic exactly.
 *
 * iOS flow:
 * 1. outputSize = 800
 * 2. sizeRatio = outputSize / screenCropSize
 * 3. scaledToFill draw size calculation
 * 4. Apply user scale
 * 5. Apply scaled offset
 * 6. Render with circular clip at 800x800
 */
private fun cropBitmap(
    original: Bitmap,
    scale: Float,
    offsetXPx: Float,
    offsetYPx: Float,
    screenCropSizePx: Float
): Bitmap {
    val outputSize = 800f
    val sizeRatio = outputSize / screenCropSizePx

    val imageAspect = original.width.toFloat() / original.height.toFloat()
    val drawWidth: Float
    val drawHeight: Float

    if (imageAspect > 1f) {
        // Landscape: height fills the frame, width overflows
        drawWidth = outputSize * imageAspect * scale
        drawHeight = outputSize * scale
    } else {
        // Portrait: width fills the frame, height overflows
        drawWidth = outputSize * scale
        drawHeight = outputSize / imageAspect * scale
    }

    val scaledOffsetX = offsetXPx * sizeRatio
    val scaledOffsetY = offsetYPx * sizeRatio

    // Draw origin centered + user offset
    val drawOriginX = (outputSize - drawWidth) / 2f + scaledOffsetX
    val drawOriginY = (outputSize - drawHeight) / 2f + scaledOffsetY

    val result = Bitmap.createBitmap(outputSize.toInt(), outputSize.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    // Circular clip path
    val clipPath = Path().apply {
        addOval(RectF(0f, 0f, outputSize, outputSize), Path.Direction.CW)
    }
    canvas.clipPath(clipPath)

    // Draw image with scale/offset applied
    val destRect = RectF(drawOriginX, drawOriginY, drawOriginX + drawWidth, drawOriginY + drawHeight)
    canvas.drawBitmap(original, null, destRect, Paint().apply { isAntiAlias = true; isFilterBitmap = true })

    return result
}
