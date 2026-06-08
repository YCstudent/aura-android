package com.edistrive.aura.ui.screens.digital

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edistrive.aura.ui.theme.AuraTokens

data class QuickQuestion(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val prompt: String,
    val tint: Color
)

val quickQuestions = listOf(
    QuickQuestion(
        emoji = "🤒",
        title = "感冒发烧",
        subtitle = "怎么判断是普通感冒还是流感",
        prompt = "我有点感冒发烧，怎么判断是普通感冒还是流感？需要去医院吗？",
        tint = Color(0xFF1A8080)
    ),
    QuickQuestion(
        emoji = "💊",
        title = "用药咨询",
        subtitle = "正在吃的药能一起吃吗",
        prompt = "我正在吃布洛芬，可以同时吃感冒灵颗粒吗？",
        tint = Color(0xFFF08C5C)
    ),
    QuickQuestion(
        emoji = "❤️",
        title = "慢病管理",
        subtitle = "高血压日常怎么注意",
        prompt = "高血压患者日常饮食和运动需要注意什么？",
        tint = Color(0xFFE15F8E)
    ),
    QuickQuestion(
        emoji = "🏥",
        title = "就诊建议",
        subtitle = "应该挂哪个科室",
        prompt = "我最近经常头晕、疲劳，应该挂哪个科室？",
        tint = Color(0xFF6F7BD4)
    )
)

@Composable
fun WelcomeView(
    username: String,
    onQuickQuestion: (String) -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    val displayName = username.ifBlank { "朋友" }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        appeared = true
    }

    val appearAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f)
    )
    val appearOffset by animateFloatAsState(
        targetValue = if (appeared) 0f else 16f,
        animationSpec = spring(dampingRatio = 0.85f)
    )

    val glowTransition = rememberInfiniteTransition(label = "glow")
    val glowPulse by glowTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "glowPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = appearAlpha
                translationY = appearOffset
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Icon with static glow — radial gradient drawn behind (no blur, no animation)
        Box(
            modifier = Modifier.size(132.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow ring: radial gradient from primary to transparent, pulse animation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = glowPulse }
                    .drawBehind {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = 66.dp.toPx()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AuraTokens.Primary.copy(alpha = 0.18f),
                                    AuraTokens.Primary.copy(alpha = 0.06f),
                                    AuraTokens.Primary.copy(alpha = 0f)
                                ),
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center
                        )
                    }
            )
            // Shadow: radial gradient from semi-transparent black to transparent
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .drawBehind {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = 42.dp.toPx()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center
                        )
                    }
            )
            // Icon: gradient circle
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(AuraTokens.Primary, AuraTokens.Primary2),
                            start = Offset.Zero, end = Offset.Infinite
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 36.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Hi, $displayName",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AuraTokens.Primary.copy(alpha = 0.10f))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = AuraTokens.Primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "权威来源 · 个性化建议",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AuraTokens.Primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "我是医小智，您的智能健康助手\n有任何健康、用药和就诊问题都可以问我",
            fontSize = 15.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "不知道问什么？试试这些",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B7280)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in quickQuestions.indices step 2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickQuestionCard(
                        q = quickQuestions[i],
                        onClick = { onQuickQuestion(quickQuestions[i].prompt) },
                        modifier = Modifier.weight(1f)
                    )
                    if (i + 1 < quickQuestions.size) {
                        QuickQuestionCard(
                            q = quickQuestions[i + 1],
                            onClick = { onQuickQuestion(quickQuestions[i + 1].prompt) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun QuickQuestionCard(q: QuickQuestion, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFAFBFC)),
                    startY = 0f, endY = Float.POSITIVE_INFINITY
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // Icon circle with gradient
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(8.dp, CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(q.tint, q.tint.copy(alpha = 0.75f)),
                        start = Offset.Zero, end = Offset.Infinite
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(q.emoji, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            q.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A1A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            q.subtitle,
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            maxLines = 2,
            lineHeight = 17.sp
        )
    }
}
