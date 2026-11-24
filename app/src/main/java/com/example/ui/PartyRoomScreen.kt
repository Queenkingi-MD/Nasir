package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.ChatMessage
import com.example.data.GiftCatalog
import com.example.data.GiftItem
import com.example.data.PartyRoom
import com.example.data.RoomSeat
import com.example.data.UserProfile
import com.example.data.avatarDrawableForKey
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassCardHighlight
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.MicLiveEmerald
import com.example.ui.theme.MicMutedRed
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StageNightBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VelvetPlumSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyRoomScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    activeRoom: PartyRoom?,
    seatsState: UiState<List<RoomSeat>>,
    messagesState: UiState<List<ChatMessage>>,
    giftBanner: GiftBannerEvent?,
    micAudioLevel: Float,
    isAdmin: Boolean,
    isBusy: Boolean,
    onSeatTap: (Int) -> Unit,
    onToggleMic: (Boolean) -> Unit,
    onSendChat: (String) -> Unit,
    onSendGift: (RoomSeat, GiftItem) -> Unit,
    onReceiveAppreciationGift: () -> Unit,
    onSpinWheel: (Long, (Long) -> Unit) -> Unit,
    onLeaveRoom: () -> Unit,
    onOpenLobby: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val context = LocalContext.current
    var chatInput by remember { mutableStateOf("") }
    var showGiftSheet by remember { mutableStateOf(false) }
    var showGameSheet by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onToggleMic(granted)
    }

    val seatsList = (seatsState as? UiState.Success)?.data ?: emptyList()
    val mySeat = seatsList.firstOrNull { it.occupied && it.occupantUid == currentUserId }
    val isMyMicOn = mySeat?.micOn == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF14062A), StageNightBg, Color(0xFF1A0933))
                )
            )
    ) {
        // Atmospheric stage spotlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ElectricViolet.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.16f),
                    radius = size.width * 0.65f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.16f),
                radius = size.width * 0.65f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonMagenta.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.35f),
                    radius = size.width * 0.45f
                ),
                center = Offset(size.width * 0.85f, size.height * 0.35f),
                radius = size.width * 0.45f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .widthIn(max = 640.dp)
                .align(Alignment.Center)
        ) {
            // 1. Top Header Bar (Room info + Wallet pill + Admin pill)
            RoomTopHeader(
                roomTitle = activeRoom?.roomTitle ?: "Mehfil-e-Yaari 🎉",
                coins = userProfile?.coins ?: 0L,
                diamonds = userProfile?.diamonds ?: 0L,
                isAdmin = isAdmin,
                onOpenLobby = onOpenLobby,
                onOpenWallet = onOpenWallet,
                onOpenAdmin = onOpenAdmin
            )

            // 2. Upper 8 Circular Seats Stage (1 Host + 7 Guests)
            PartySeatsStage(
                seatsState = seatsState,
                currentUserId = currentUserId,
                micAudioLevel = micAudioLevel,
                onSeatTap = { seat ->
                    if (seat.occupied && seat.occupantUid == currentUserId) {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!seat.micOn && !hasPerm) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            onToggleMic(hasPerm)
                        }
                    } else {
                        onSeatTap(seat.seatNumber.toInt())
                    }
                }
            )

            // Floating Gift / Win Notification Banner
            AnimatedVisibility(
                visible = giftBanner != null,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut()
            ) {
                giftBanner?.let { event ->
                    Surface(
                        color = NeonMagenta.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .border(1.dp, RoyalGold, RoundedCornerShape(50))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(event.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = event.text,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. Large CHAT Box ("Neeche bada sa CHAT box rakho")
            PartyChatBox(
                messagesState = messagesState,
                chatInput = chatInput,
                onChatInputChange = { chatInput = it },
                onSendChat = { msg ->
                    onSendChat(msg)
                    chatInput = ""
                },
                onReceiveAppreciationGift = onReceiveAppreciationGift,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // 4. Bottom 4 Action Buttons (Mic 🎤, Gift 🎁, Game 🎮, Leave ❌)
            PartyBottomActionBar(
                isMicOn = isMyMicOn,
                onMicClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!isMyMicOn && !hasPerm) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        onToggleMic(hasPerm)
                    }
                },
                onGiftClick = { showGiftSheet = true },
                onGameClick = { showGameSheet = true },
                onLeaveClick = onLeaveRoom
            )
        }
    }

    if (showGiftSheet) {
        GiftBottomSheet(
            seats = seatsList.filter { it.occupied },
            userCoins = userProfile?.coins ?: 0L,
            isBusy = isBusy,
            onDismiss = { showGiftSheet = false },
            onSendGift = { targetSeat, gift ->
                onSendGift(targetSeat, gift)
                showGiftSheet = false
            }
        )
    }

    if (showGameSheet) {
        LuckyWheelGameSheet(
            userCoins = userProfile?.coins ?: 0L,
            isBusy = isBusy,
            onDismiss = { showGameSheet = false },
            onSpin = onSpinWheel
        )
    }
}

@Composable
private fun RoomTopHeader(
    roomTitle: String,
    coins: Long,
    diamonds: Long,
    isAdmin: Boolean,
    onOpenLobby: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            color = GlassCardSurface.copy(alpha = 0.85f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .weight(1f, fill = false)
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                .clickable { onOpenLobby() }
                .testTag("rooms_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔴", fontSize = 11.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = roomTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(4.dp))
                Text("🏠", fontSize = 13.sp)
            }
        }

        Spacer(Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isAdmin) {
                Surface(
                    color = RoyalGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .border(1.dp, RoyalGold, RoundedCornerShape(50))
                        .clickable { onOpenAdmin() }
                        .testTag("admin_pill_button")
                ) {
                    Text(
                        text = "🛡️",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            Surface(
                color = GlassCardHighlight,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .border(1.dp, RoyalGold.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .clickable { onOpenWallet() }
                    .testTag("wallet_pill_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🪙$coins",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoyalGold
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "💎$diamonds",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanGlow
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("👛", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun PartySeatsStage(
    seatsState: UiState<List<RoomSeat>>,
    currentUserId: String,
    micAudioLevel: Float,
    onSeatTap: (RoomSeat) -> Unit
) {
    Surface(
        color = GlassCardSurface.copy(alpha = 0.58f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
    ) {
        when (seatsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonMagenta)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚠️ ${seatsState.message}", color = MicMutedRed)
                }
            }
            is UiState.Success -> {
                val seats = seatsState.data.ifEmpty {
                    (0..7).map { i ->
                        RoomSeat(
                            seatNumber = i.toLong(),
                            seatRole = if (i == 0) "HOST" else "GUEST",
                            occupied = false
                        )
                    }
                }
                val topRow = seats.take(4)
                val bottomRow = seats.drop(4).take(4)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        topRow.forEach { seat ->
                            CircularPartySeat(
                                seat = seat,
                                isMe = seat.occupied && seat.occupantUid == currentUserId,
                                micAudioLevel = micAudioLevel,
                                onClick = { onSeatTap(seat) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        bottomRow.forEach { seat ->
                            CircularPartySeat(
                                seat = seat,
                                isMe = seat.occupied && seat.occupantUid == currentUserId,
                                micAudioLevel = micAudioLevel,
                                onClick = { onSeatTap(seat) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularPartySeat(
    seat: RoomSeat,
    isMe: Boolean,
    micAudioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHost = seat.seatNumber == 0L || seat.seatRole == "HOST"
    val infiniteTransition = rememberInfiniteTransition(label = "seatPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val dynamicScale = if (isMe && seat.micOn) 1f + (micAudioLevel * 0.14f) else pulseScale

    val ringColor = when {
        !seat.occupied -> Color.White.copy(alpha = 0.22f)
        isHost && seat.micOn -> RoyalGold
        seat.micOn -> MicLiveEmerald
        else -> ElectricViolet.copy(alpha = 0.6f)
    }

    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 2.dp)
            .testTag("party_seat_${seat.seatNumber}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(66.dp),
            contentAlignment = Alignment.Center
        ) {
            if (seat.occupied && seat.micOn) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .scale(dynamicScale)
                        .clip(CircleShape)
                        .border(2.dp, ringColor.copy(alpha = 0.45f), CircleShape)
                )
            }

            // Circular Photo / Empty Seat
            val drawableRes = avatarDrawableForKey(seat.avatarKey)
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        if (seat.occupied) GlassCardHighlight else Color.Black.copy(alpha = 0.28f)
                    )
                    .border(
                        width = if (isHost) 2.5.dp else 1.8.dp,
                        color = ringColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (seat.occupied && drawableRes != null) {
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = seat.occupantName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (seat.occupied) {
                    Text("😎", fontSize = 26.sp)
                } else {
                    Text("🪑", fontSize = 22.sp)
                }
            }

            // Host Crown badge on top-left
            if (isHost) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(CircleShape)
                        .background(StageNightBg)
                        .border(1.dp, RoyalGold, CircleShape)
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text("👑", fontSize = 11.sp)
                }
            }

            // Mic Indicator ("mic ka nishan") on bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !seat.occupied -> VelvetPlumSurface
                            seat.micOn -> MicLiveEmerald
                            else -> MicMutedRed
                        }
                    )
                    .border(1.5.dp, StageNightBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (seat.occupied && seat.micOn) "🎤" else "🔇",
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = when {
                !seat.occupied -> "Guest ${seat.seatNumber}"
                isHost -> if (isMe) "Aap (Host)" else seat.occupantName
                isMe -> "Aap ⭐"
                else -> seat.occupantName
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (seat.occupied) TextPrimary else TextMuted,
            fontWeight = if (isHost || isMe) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        if (seat.occupied && seat.diamondsReceived > 0L) {
            Text(
                text = "💎${seat.diamondsReceived}",
                style = MaterialTheme.typography.labelSmall,
                color = CyanGlow,
                fontSize = 10.sp
            )
        } else {
            Text(
                text = if (isHost) "Host" else if (seat.occupied) "Guest" else "Baithain +",
                style = MaterialTheme.typography.labelSmall,
                color = if (isHost) RoyalGold else TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun PartyChatBox(
    messagesState: UiState<List<ChatMessage>>,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    onSendChat: (String) -> Unit,
    onReceiveAppreciationGift: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Surface(
        color = GlassCardSurface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chat Header + Gift Rain appreciation trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "💬 Mehfil Chat Box",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Surface(
                    color = RoyalGold.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .border(1.dp, RoyalGold.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .clickable { onReceiveAppreciationGift() }
                        .testTag("receive_gift_button")
                ) {
                    Text(
                        text = "🎁 Gift Paayein",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoyalGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Messages Stream
            Box(modifier = Modifier.weight(1f)) {
                when (messagesState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonMagenta)
                        }
                    }
                    is UiState.Error -> {
                        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("⚠️ ${messagesState.message}", color = MicMutedRed)
                        }
                    }
                    is UiState.Success -> {
                        if (messages.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "💬 Chat abhi khali hai — pehla paigham bhejein!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    ChatMessageBubble(msg)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Roman Urdu Reaction Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Assalam-o-Alaikum 👋",
                    "Wah wah! 👏",
                    "Zabardast 🔥",
                    "Shukriya 🙏"
                ).forEach { quick ->
                    Surface(
                        color = GlassCardHighlight,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.clickable { onSendChat(quick) }
                    ) {
                        Text(
                            text = quick,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Chat Text Input + Send Emoji Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = onChatInputChange,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.chat_placeholder),
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonMagenta,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.Black.copy(alpha = 0.28f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.28f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input")
                )

                Spacer(Modifier.width(8.dp))

                Surface(
                    color = if (chatInput.isNotBlank()) NeonMagenta else GlassCardHighlight,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(enabled = chatInput.isNotBlank()) {
                            onSendChat(chatInput)
                        }
                        .testTag("send_chat_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📤", fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(msg: ChatMessage) {
    val bgColor = when (msg.messageType) {
        "GIFT" -> NeonMagenta.copy(alpha = 0.20f)
        "GAME" -> ElectricViolet.copy(alpha = 0.22f)
        "SYSTEM" -> RoyalGold.copy(alpha = 0.14f)
        else -> Color.Black.copy(alpha = 0.24f)
    }
    val borderColor = when (msg.messageType) {
        "GIFT" -> NeonMagenta.copy(alpha = 0.55f)
        "GAME" -> CyanGlow.copy(alpha = 0.45f)
        "SYSTEM" -> RoyalGold.copy(alpha = 0.45f)
        else -> Color.White.copy(alpha = 0.08f)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            val badgeEmoji = when (msg.messageType) {
                "GIFT" -> "🎁"
                "GAME" -> "🎮"
                "SYSTEM" -> "📢"
                else -> "💬"
            }
            Text(badgeEmoji, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                if (msg.messageType == "CHAT") {
                    Text(
                        text = msg.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanGlow,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = msg.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun PartyBottomActionBar(
    isMicOn: Boolean,
    onMicClick: () -> Unit,
    onGiftClick: () -> Unit,
    onGameClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PartyActionButton(
            emoji = if (isMicOn) "🎤" else "🔇",
            label = if (isMicOn) "Mic ON" else "Mic OFF",
            accentColor = if (isMicOn) MicLiveEmerald else MicMutedRed,
            onClick = onMicClick,
            modifier = Modifier
                .weight(1f)
                .testTag("mic_button")
        )
        PartyActionButton(
            emoji = "🎁",
            label = "Gift",
            accentColor = NeonMagenta,
            onClick = onGiftClick,
            modifier = Modifier
                .weight(1f)
                .testTag("gift_button")
        )
        PartyActionButton(
            emoji = "🎮",
            label = "Game",
            accentColor = ElectricViolet,
            onClick = onGameClick,
            modifier = Modifier
                .weight(1f)
                .testTag("game_button")
        )
        PartyActionButton(
            emoji = "❌",
            label = "Leave",
            accentColor = Color(0xFFFF6B6B),
            onClick = onLeaveClick,
            modifier = Modifier
                .weight(1f)
                .testTag("leave_button")
        )
    }
}

@Composable
private fun PartyActionButton(
    emoji: String,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.20f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .heightIn(min = 56.dp)
            .border(1.2.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GiftBottomSheet(
    seats: List<RoomSeat>,
    userCoins: Long,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSendGift: (RoomSeat, GiftItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSeat by remember(seats) { mutableStateOf(seats.firstOrNull()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VelvetPlumSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎁 Tohfa Bhejein", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "🪙 $userCoins Coins",
                    style = MaterialTheme.typography.titleMedium,
                    color = RoyalGold,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Kis ko bhejna hai? (Seat Chunein):",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seats.forEach { seat ->
                    val isSelected = selectedSeat?.seatNumber == seat.seatNumber
                    Surface(
                        color = if (isSelected) NeonMagenta else GlassCardSurface,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (isSelected) RoyalGold else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(50)
                            )
                            .clickable { selectedSeat = seat }
                    ) {
                        Text(
                            text = if (seat.seatNumber == 0L) "👑 ${seat.occupantName}" else seat.occupantName,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GiftCatalog.items.forEach { gift ->
                    val canAfford = userCoins >= gift.coinCost
                    Surface(
                        color = GlassCardHighlight,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (canAfford) RoyalGold.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(18.dp)
                            )
                            .clickable(enabled = !isBusy && canAfford && selectedSeat != null) {
                                selectedSeat?.let { target -> onSendGift(target, gift) }
                            }
                            .testTag("gift_item_${gift.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(gift.emoji, fontSize = 30.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = gift.nameRomanUrdu,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "🪙 ${gift.coinCost}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (canAfford) RoyalGold else MicMutedRed
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LuckyWheelGameSheet(
    userCoins: Long,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSpin: (Long, (Long) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rotationTarget by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotationTarget,
        animationSpec = tween(durationMillis = 900),
        label = "wheelSpin"
    )
    var lastWinText by remember { mutableStateOf("50 Coins lagayein aur 5x tak jeetein! 🎲") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VelvetPlumSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("🎮 Qismat ka Chakka", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "🪙 Mojooda Balance: $userCoins Coins",
                style = MaterialTheme.typography.titleMedium,
                color = RoyalGold
            )

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .rotate(animatedRotation)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(NeonMagenta, ElectricViolet, RoyalGold, MicLiveEmerald, NeonMagenta)
                        )
                    )
                    .border(4.dp, RoyalGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = StageNightBg,
                    shape = CircleShape,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎲", fontSize = 32.sp)
                    }
                }
            }

            Text(
                text = lastWinText,
                style = MaterialTheme.typography.bodyLarge,
                color = CyanGlow,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        rotationTarget += 720f
                        onSpin(50L) { won ->
                            lastWinText = if (won > 0) "🎉 Mubarak! Aap ne $won Coins jeet liye!"
                            else "😅 Is bar khali gaya — phir koshish karein!"
                        }
                    },
                    enabled = !isBusy && userCoins >= 50L,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("spin_50_button")
                ) {
                    Text("🎲 50 Coins", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        rotationTarget += 1080f
                        onSpin(100L) { won ->
                            lastWinText = if (won > 0) "🎉 Zabardast! Aap ne $won Coins jeet liye!"
                            else "😅 Is bar khali gaya — phir koshish karein!"
                        }
                    },
                    enabled = !isBusy && userCoins >= 100L,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("spin_100_button")
                ) {
                    Text("🔥 100 Coins", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}
