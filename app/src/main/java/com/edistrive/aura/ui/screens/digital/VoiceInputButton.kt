package com.edistrive.aura.ui.screens.digital

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.edistrive.aura.ui.theme.AuraTokens
import java.util.*

@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recognizedText by remember { mutableStateOf("") }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var hasPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )}

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            Toast.makeText(context, "需要麦克风权限才能使用语音输入", Toast.LENGTH_SHORT).show()
        }
    }

    // Waveform animation
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveHeights = (0..4).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 4f,
            targetValue = if (isRecording) (12f + index * 4f) else 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(150 + index * 50),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave$index"
        )
    }

    // Timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isRecording && isCancelling -> Color(0xFFFFE5E5)
                    isRecording -> Color(0xFFE6F7F7)
                    else -> Color(0xFFF5F5F5)
                }
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@detectDragGestures
                        }
                        // Start recording
                        isRecording = true
                        isCancelling = false
                        recognizedText = ""
                        startSpeechRecognition(context, { text ->
                            recognizedText = text
                        }, {
                            isRecording = false
                            if (recognizedText.isNotBlank() && !isCancelling) {
                                onResult(recognizedText)
                            }
                        }, {
                            isRecording = false
                            Toast.makeText(context, "语音识别失败，请重试", Toast.LENGTH_SHORT).show()
                        })
                    },
                    onDrag = { _, dragAmount ->
                        if (isRecording) {
                            isCancelling = dragAmount.y < -50f
                        }
                    },
                    onDragEnd = {
                        if (isRecording) {
                            stopSpeechRecognition()
                            isRecording = false
                            isCancelling = false
                        }
                    },
                    onDragCancel = {
                        if (isRecording) {
                            stopSpeechRecognition()
                            isRecording = false
                            isCancelling = false
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isRecording) {
                // Waveform
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    waveHeights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(h.value.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(AuraTokens.Primary)
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color(0xFF333333),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = when {
                    isRecording && isCancelling -> "松开取消"
                    isRecording -> "松开发送"
                    else -> "按住说话"
                },
                fontSize = 16.sp,
                color = when {
                    isRecording && isCancelling -> Color(0xFFFF3B30)
                    isRecording -> AuraTokens.Primary
                    else -> Color(0xFF333333)
                }
            )

            if (isRecording) {
                val mins = recordingSeconds / 60
                val secs = recordingSeconds % 60
                Text(
                    text = "${mins}:${secs.toString().padStart(2, '0')}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

// Global SpeechRecognizer instance
private var globalSpeechRecognizer: SpeechRecognizer? = null

private fun startSpeechRecognition(
    context: Context,
    onPartialResult: (String) -> Unit,
    onFinalResult: () -> Unit,
    onError: () -> Unit
) {
    if (SpeechRecognizer.isRecognitionAvailable(context).not()) {
        Toast.makeText(context, "设备不支持语音识别", Toast.LENGTH_SHORT).show()
        onError()
        return
    }

    globalSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    globalSpeechRecognizer?.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            onError()
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onPartialResult(matches[0])
            }
            onFinalResult()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onPartialResult(matches[0])
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
    }
    globalSpeechRecognizer?.startListening(intent)
}

private fun stopSpeechRecognition() {
    globalSpeechRecognizer?.destroy()
    globalSpeechRecognizer = null
}
