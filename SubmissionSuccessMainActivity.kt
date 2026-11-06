package com.example.offerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF11113F)
private val Muted = Color(0xFF85849F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SubmissionSuccessScreen(
                    rewardAmount = "₹25",
                    paymentTime = "7–8 days",
                    onBackToOffers = { finish() }
                )
            }
        }
    }
}

@Composable
fun SubmissionSuccessScreen(
    rewardAmount: String,
    paymentTime: String,
    onBackToOffers: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFFFBFF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF1C9), Color(0xFFF8F4FF), Color.White)
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(38.dp), ambientColor = Color(0xFFB9A8E8))
                    .clip(RoundedCornerShape(38.dp))
                    .background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeroSection()

                Spacer(Modifier.height(8.dp))
                Text(
                    "Submission successful!",
                    color = Ink,
                    fontSize = 25.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text("Your proof has been submitted.", color = Muted, fontSize = 17.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("It will be verified soon.", color = Muted, fontSize = 17.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))

                RewardCard(rewardAmount, paymentTime)
                Spacer(Modifier.height(34.dp))
                TrustIndicators()
                Spacer(Modifier.height(34.dp))

                Button(
                    onClick = onBackToOffers,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(66.dp)
                        .shadow(10.dp, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF7050F2), Color(0xFF3547E8))),
                                RoundedCornerShape(22.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Back to offers", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun HeroSection() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFD95D), Color(0xFFFFEBA2), Color(0xFFFFF8E6), Color.White)
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val wave = Path().apply {
                moveTo(0f, size.height * .70f)
                cubicTo(size.width * .27f, size.height * .87f, size.width * .68f, size.height * .61f, size.width, size.height * .73f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(wave, Color.White.copy(alpha = .88f))
        }

        BouncingCoin(Modifier.align(Alignment.CenterStart).padding(start = 34.dp).offset(y = 2.dp), 52.dp, 1550)
        BouncingCoin(Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 42.dp), 48.dp, 1850)
        BouncingCoin(Modifier.align(Alignment.CenterEnd).padding(end = 94.dp).offset(y = 42.dp), 38.dp, 1350)

        val pulse = rememberInfiniteTransition(label = "smileyPulse")
        val scale by pulse.animateFloat(
            initialValue = 1f,
            targetValue = 1.035f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "smileyScale"
        )
        Image(
            painter = painterResource(R.drawable.heart_smile_3d),
            contentDescription = "3D heart eyes smiley",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 24.dp)
                .size(178.dp)
                .scale(scale)
        )
    }
}

@Composable
private fun BouncingCoin(modifier: Modifier = Modifier, size: Dp, duration: Int) {
    val transition = rememberInfiniteTransition(label = "coinBounce")
    val y by transition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(duration, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "coinY"
    )
    Image(
        painter = painterResource(R.drawable.rupee_coin_3d),
        contentDescription = "Gold rupee coin",
        contentScale = ContentScale.Fit,
        modifier = modifier.offset(y = y.dp).size(size)
    )
}

@Composable
private fun RewardCard(reward: String, payout: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF5F1FF), Color(0xFFF0EDFF))))
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        RewardRow(icon = "✓", iconColor = Color(0xFF17B967), label = "Reward amount", value = reward)
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFDCD7ED)))
        Spacer(Modifier.height(18.dp))
        RewardRow(icon = "▦", iconColor = Color(0xFF6744DC), label = "You will get paid in", value = payout)
    }
}

@Composable
private fun RewardRow(icon: String, iconColor: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(76.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = iconColor.copy(alpha = .25f))
                .background(Brush.verticalGradient(listOf(iconColor.copy(alpha = .18f), iconColor.copy(alpha = .08f))), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(48.dp).background(iconColor, CircleShape), contentAlignment = Alignment.Center) {
                Text(icon, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(18.dp))
        Column {
            Text(label, color = Muted, fontSize = 16.sp)
            Text(value, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun TrustIndicators() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        TrustItem("₹", "On-time\npayments", Color(0xFFE3A21C), Color(0xFFFFF0C9))
        TrustItem("✓", "Trusted", Color(0xFF7048DF), Color(0xFFECE4FF))
        TrustItem("+", "100%\nreliable", Color(0xFF18B968), Color(0xFFE0FFEC))
    }
}

@Composable
private fun TrustItem(symbol: String, label: String, tint: Color, bg: Color) {
    Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(78.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = tint.copy(alpha = .22f))
                .background(bg, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(48.dp).background(tint, CircleShape), contentAlignment = Alignment.Center) {
                Text(symbol, color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color(0xFF25243B), fontSize = 15.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
    }
}
