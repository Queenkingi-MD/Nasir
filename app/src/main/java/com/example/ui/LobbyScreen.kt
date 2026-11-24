package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.signOut
import com.example.data.PartyRoom
import com.example.data.UserProfile
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassCardHighlight
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.MicLiveEmerald
import com.example.ui.theme.MicMutedRed
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StageNightBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VelvetPlumSurface

@Composable
fun LobbyScreen(
    userProfile: UserProfile?,
    roomsState: UiState<List<PartyRoom>>,
    activeRoomId: String?,
    isAdmin: Boolean,
    onJoinRoom: (String) -> Unit,
    onCreateRoom: (String) -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAdmin: () -> Unit,
    onBackToRoom: () -> Unit,
    onExitSession: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newRoomTitle by remember { mutableStateOf("") }

    BackHandler(enabled = activeRoomId != null) {
        onBackToRoom()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(StageNightBg, VelvetPlumSurface, StageNightBg))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 640.dp)
                .align(Alignment.Center)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Mehfil Lobby 🎉",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Khush aamdeed, ${userProfile?.displayName ?: "Dost"}!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isAdmin) {
                        Surface(
                            color = RoyalGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .border(1.dp, RoyalGold.copy(alpha = 0.6f), RoundedCornerShape(50))
                                .clickable { onOpenAdmin() }
                                .testTag("lobby_admin_button")
                        ) {
                            Text(
                                text = "🛡️ Admin",
                                style = MaterialTheme.typography.labelMedium,
                                color = RoyalGold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Surface(
                        color = MicMutedRed.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .border(1.dp, MicMutedRed.copy(alpha = 0.45f), RoundedCornerShape(50))
                            .clickable {
                                if (onExitSession != null) {
                                    onExitSession()
                                } else {
                                    signOut(credentialManager, scope)
                                }
                            }
                            .testTag("sign_out_button")
                    ) {
                        Text(
                            text = "🚪 Bahar",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Wallet quick card
            Surface(
                color = GlassCardSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, ElectricViolet.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                    .clickable { onOpenWallet() }
                    .testTag("lobby_wallet_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "👛 Mera Wallet aur Withdraw",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "🪙 ${userProfile?.coins ?: 0} Coins   •   💎 ${userProfile?.diamonds ?: 0} Diamonds",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoyalGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        color = NeonMagenta,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Withdraw 💸",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🔴 Live Party Rooms",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("create_room_button")
                ) {
                    Text("➕ Naya Room", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (roomsState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonMagenta)
                        }
                    }
                    is UiState.Error -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚠️ ${roomsState.message}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MicMutedRed
                            )
                        }
                    }
                    is UiState.Success -> {
                        if (roomsState.data.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("🎤", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Abhi koi live room nahi hai. Naya Party Room shuru karein!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.navigationBarsPadding()
                            ) {
                                items(roomsState.data, key = { it.id }) { room ->
                                    RoomLobbyCard(
                                        room = room,
                                        onJoin = { onJoinRoom(room.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = GlassCardSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("🎉 Naya Party Room Banayein") },
            text = {
                Column {
                    Text("Apni mehfil ka pyara sa naam likhein:")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newRoomTitle,
                        onValueChange = { newRoomTitle = it },
                        singleLine = true,
                        placeholder = { Text("Maslan: Raat ki Mehfil 🌙") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonMagenta,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRoomTitle.trim().length >= 2) {
                            onCreateRoom(newRoomTitle.trim())
                            newRoomTitle = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta)
                ) {
                    Text("Shuru Karein 🎤")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Band Karein", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun RoomLobbyCard(
    room: PartyRoom,
    onJoin: () -> Unit
) {
    Surface(
        color = GlassCardHighlight,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .clickable { onJoin() }
            .testTag("join_room_card_${room.id}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .width(52.dp)
                        .background(
                            Brush.linearGradient(listOf(NeonMagenta, ElectricViolet)),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎙️", fontSize = 26.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔴 LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MicLiveEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "🪑 8 Seats",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanGlow
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = room.roomTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "👑 Host: ${room.hostName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                color = NeonMagenta,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Shamil Hon",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
    }
}
