package com.taskgullak.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReferralPageSection(
    referralCode: String,
    referralRewardInr: Int,
    onShareReferral: () -> Unit,
    ReferralIllustration: @Composable (Modifier) -> Unit
) {
    // Point 9: Keep ONLY the existing vertical bouncing animation (no extra animations)
    val bounceTransition = rememberInfiniteTransition(label = "referral_bounce_only")
    val bounceOffsetY by bounceTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "referral_vertical_bounce"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Point 9: Shifted slightly upward with reduced top padding (6.dp instead of 28.dp+)
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Bouncing Hero Illustration
        Box(
            modifier = Modifier
                .offset(y = bounceOffsetY.dp)
                .padding(top = 4.dp, bottom = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            ReferralIllustration(Modifier.size(150.dp))
        }

        // Referral Info & Code Card (Positioned higher for balanced visual hierarchy)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Invite Friends & Earn ₹$referralRewardInr",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Share your referral code and earn instant rewards when your friend completes their first task.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = referralCode,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        TextButton(onClick = onShareReferral) {
                            Text("Share Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
