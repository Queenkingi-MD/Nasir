package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleSignInButton
import com.example.auth.attemptAutoSignIn
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.MicMutedRed
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StageNightBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onGuestDemoLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Interactive Sign-In & Guest Demo buttons remain visible */ },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(StageNightBg, Color(0xFF1E0B3B), StageNightBg)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonMagenta.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.22f),
                    radius = size.minDimension * 0.65f
                ),
                center = Offset(size.width * 0.25f, size.height * 0.22f),
                radius = size.minDimension * 0.65f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ElectricViolet.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.65f),
                    radius = size.minDimension * 0.65f
                ),
                center = Offset(size.width * 0.8f, size.height * 0.65f),
                radius = size.minDimension * 0.65f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(6.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonMagenta, ElectricViolet)
                            )
                        )
                        .border(2.5.dp, RoyalGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👑🎤", fontSize = 36.sp)
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.auth_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.auth_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeatureChip("🎤 8 Gol Seats", RoyalGold)
                    FeatureChip("💬 Live Chat Box", CyanGlow)
                    FeatureChip("🎁 Phool, Dil, Gari", NeonMagenta)
                    FeatureChip("🎮 Party Game", ElectricViolet)
                    FeatureChip("💸 Easypaisa / JazzCash", Color(0xFF2EE59D))
                }
            }

            Surface(
                color = GlassCardSurface.copy(alpha = 0.90f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Starter Profile Preview Card ("Mehfil User" • 1500 Coins)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_avatar_sana_1795514874877),
                            contentDescription = "Mehfil User",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFFFF2E7E), CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mehfil User ✨",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "👛 Wallet: 1,500 Coins • 1000 Coins = 1000 Rs",
                                style = MaterialTheme.typography.labelMedium,
                                color = RoyalGold
                            )
                        }
                    }

                    authError?.let { msg ->
                        Surface(
                            color = MicMutedRed.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "⚠️ $msg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFFB4BE),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Primary Google Sign-In Button
                    GoogleSignInButton(
                        onAuthSuccess = {
                            authError = null
                            onAuthSuccess()
                        },
                        onAuthError = { msg -> authError = msg }
                    )

                    Spacer(Modifier.height(12.dp))

                    // Guest / Demo Mode Button
                    OutlinedButton(
                        onClick = onGuestDemoLogin,
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(2.dp, Color(0xFFFF2E7E)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFFFF2E7E)
                        ),
                        contentPadding = PaddingValues(vertical = 13.dp, horizontal = 20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("guest_demo_login_button")
                    ) {
                        Text(
                            text = "🎭 Guest / Demo se Andar Jao",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(text: String, accentColor: Color) {
    Surface(
        color = accentColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(50))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
