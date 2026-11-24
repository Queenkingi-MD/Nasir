package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.data.PartyRepository
import com.example.ui.AdminApprovalScreen
import com.example.ui.AppDestination
import com.example.ui.AuthScreen
import com.example.ui.DemoPartyViewModel
import com.example.ui.LobbyScreen
import com.example.ui.PartyRoomScreen
import com.example.ui.PartyViewModel
import com.example.ui.UiState
import com.example.ui.WalletScreen
import com.example.ui.theme.GlassCardHighlight
import com.example.ui.theme.MehfilPartyTheme
import com.example.ui.theme.RoyalGold
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MehfilPartyTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isDemoMode by rememberSaveable { mutableStateOf(false) }
    val user = currentUser

    when {
        user != null -> {
            isDemoMode = false
            key(user.uid) {
                AuthenticatedPartyRoot(currentUserId = user.uid)
            }
        }
        isDemoMode -> {
            key("demo_mode") {
                DemoPartyRoot(onExitDemo = { isDemoMode = false })
            }
        }
        else -> {
            AuthScreen(
                onAuthSuccess = { isDemoMode = false },
                onGuestDemoLogin = { isDemoMode = true }
            )
        }
    }
}

@Composable
private fun AuthenticatedPartyRoot(currentUserId: String) {
    val viewModel: PartyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PartyViewModel(PartyRepository(db), currentUserId)
            }
        }
    )

    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val activeRoomId by viewModel.activeRoomId.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val roomsState by viewModel.liveRoomsState.collectAsStateWithLifecycle()
    val seatsState by viewModel.seatsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val myWithdrawsState by viewModel.myWithdrawRequestsState.collectAsStateWithLifecycle()
    val adminWithdrawsState by viewModel.adminWithdrawRequestsState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val feedbackBanner by viewModel.feedbackBanner.collectAsStateWithLifecycle()
    val giftBanner by viewModel.giftBanner.collectAsStateWithLifecycle()
    val micAudioLevel by viewModel.micAudioLevel.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val activeRoom = (roomsState as? UiState.Success)?.data?.firstOrNull { it.id == activeRoomId }

    Box(modifier = Modifier.fillMaxSize()) {
        when (destination) {
            AppDestination.PARTY_ROOM -> {
                PartyRoomScreen(
                    currentUserId = currentUserId,
                    userProfile = userProfile,
                    activeRoom = activeRoom,
                    seatsState = seatsState,
                    messagesState = messagesState,
                    giftBanner = giftBanner,
                    micAudioLevel = micAudioLevel,
                    isAdmin = isAdmin,
                    isBusy = isBusy,
                    onSeatTap = { seatNum -> viewModel.sitOnSeat(seatNum) },
                    onToggleMic = { hasPerm -> viewModel.toggleMic(hasPerm) },
                    onSendChat = { msg -> viewModel.sendChat(msg) },
                    onSendGift = { seat, gift -> viewModel.sendGift(seat, gift) },
                    onReceiveAppreciationGift = { viewModel.triggerIncomingGiftFromRoom() },
                    onSpinWheel = { bet, onResult -> viewModel.spinLuckyWheel(bet, onResult) },
                    onLeaveRoom = { viewModel.leaveRoomToLobby() },
                    onOpenLobby = { viewModel.navigateTo(AppDestination.LOBBY) },
                    onOpenWallet = { viewModel.navigateTo(AppDestination.WALLET) },
                    onOpenAdmin = { viewModel.navigateTo(AppDestination.ADMIN) }
                )
            }

            AppDestination.LOBBY -> {
                LobbyScreen(
                    userProfile = userProfile,
                    roomsState = roomsState,
                    activeRoomId = activeRoomId,
                    isAdmin = isAdmin,
                    onJoinRoom = { roomId -> viewModel.enterRoom(roomId) },
                    onCreateRoom = { title -> viewModel.createNewRoom(title) },
                    onOpenWallet = { viewModel.navigateTo(AppDestination.WALLET) },
                    onOpenAdmin = { viewModel.navigateTo(AppDestination.ADMIN) },
                    onBackToRoom = { viewModel.navigateTo(AppDestination.PARTY_ROOM) }
                )
            }

            AppDestination.WALLET -> {
                WalletScreen(
                    userProfile = userProfile,
                    requestsState = myWithdrawsState,
                    isBusy = isBusy,
                    isAdmin = isAdmin,
                    onConvertDiamonds = { amount -> viewModel.convertDiamonds(amount) },
                    onSubmitWithdraw = { method, num, amt, onDone ->
                        viewModel.submitWithdraw(method, num, amt, onDone)
                    },
                    onOpenAdmin = { viewModel.navigateTo(AppDestination.ADMIN) },
                    onBack = {
                        viewModel.navigateTo(
                            if (activeRoomId != null) AppDestination.PARTY_ROOM else AppDestination.LOBBY
                        )
                    }
                )
            }

            AppDestination.ADMIN -> {
                AdminApprovalScreen(
                    requestsState = adminWithdrawsState,
                    isBusy = isBusy,
                    onApprove = { req -> viewModel.decideWithdrawRequest(req, true) },
                    onReject = { req -> viewModel.decideWithdrawRequest(req, false) },
                    onBack = { viewModel.navigateTo(AppDestination.WALLET) }
                )
            }
        }

        FeedbackBannerOverlay(
            feedbackBanner = feedbackBanner,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp, start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
private fun DemoPartyRoot(onExitDemo: () -> Unit) {
    val viewModel: DemoPartyViewModel = viewModel(key = "demo_party_vm")

    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val activeRoomId by viewModel.activeRoomId.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val seats by viewModel.seats.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val withdrawRequests by viewModel.withdrawRequests.collectAsStateWithLifecycle()
    val feedbackBanner by viewModel.feedbackBanner.collectAsStateWithLifecycle()
    val giftBanner by viewModel.giftBanner.collectAsStateWithLifecycle()
    val micAudioLevel by viewModel.micAudioLevel.collectAsStateWithLifecycle()

    val activeRoom = rooms.firstOrNull { it.id == activeRoomId }

    Box(modifier = Modifier.fillMaxSize()) {
        when (destination) {
            AppDestination.PARTY_ROOM -> {
                PartyRoomScreen(
                    currentUserId = DemoPartyViewModel.DEMO_UID,
                    userProfile = userProfile,
                    activeRoom = activeRoom,
                    seatsState = UiState.Success(seats),
                    messagesState = UiState.Success(messages),
                    giftBanner = giftBanner,
                    micAudioLevel = micAudioLevel,
                    isAdmin = true,
                    isBusy = false,
                    onSeatTap = { seatNum -> viewModel.sitOnSeat(seatNum) },
                    onToggleMic = { hasPerm -> viewModel.toggleMic(hasPerm) },
                    onSendChat = { msg -> viewModel.sendChat(msg) },
                    onSendGift = { seat, gift -> viewModel.sendGift(seat, gift) },
                    onReceiveAppreciationGift = { viewModel.triggerIncomingGiftFromRoom() },
                    onSpinWheel = { bet, onResult -> viewModel.spinLuckyWheel(bet, onResult) },
                    onLeaveRoom = { viewModel.leaveRoomToLobby() },
                    onOpenLobby = { viewModel.navigateTo(AppDestination.LOBBY) },
                    onOpenWallet = { viewModel.navigateTo(AppDestination.WALLET) },
                    onOpenAdmin = { viewModel.navigateTo(AppDestination.ADMIN) }
                )
            }

            AppDestination.LOBBY -> {
                LobbyScreen(
                    userProfile = userProfile,
                    roomsState = UiState.Success(rooms),
                    activeRoomId = activeRoomId,
                    isAdmin = true,
                    onJoinRoom = { roomId -> viewModel.enterRoom(roomId) },
                    onCreateRoom = { title -> viewModel.createNewRoom(title) },
                    onOpenWallet = { viewModel.navigateTo(AppDestination.WALLET) },
                    onOpenAdmin = { viewModel.navigateTo(AppDestination.ADMIN) },
                    onBackToRoom = { viewModel.navigateTo(AppDestination.PARTY_ROOM) },
                    onExitSession = onExitDemo
                )
            }

            AppDestination.WALLET -> {
                WalletScreen(
                    userProfile = userProfile,
                    requestsState = UiState.Success(withdrawRequests),
                    isBusy = false,
                    isAdmin = true,
                    onConvertDiamonds = { amount -> viewModel.convertDiamonds(amount) },
                    onSubmitWithdraw = { method, num, amt, onDone ->
                        viewModel.submitWithdraw(method, num, amt, onDone)
                    },
                    onOpenAdmin = { viewModel.navigateTo(AppDestination.ADMIN) },
                    onBack = {
                        viewModel.navigateTo(
                            if (activeRoomId != null) AppDestination.PARTY_ROOM else AppDestination.LOBBY
                        )
                    }
                )
            }

            AppDestination.ADMIN -> {
                AdminApprovalScreen(
                    requestsState = UiState.Success(withdrawRequests),
                    isBusy = false,
                    onApprove = { req -> viewModel.decideWithdrawRequest(req, true) },
                    onReject = { req -> viewModel.decideWithdrawRequest(req, false) },
                    onBack = { viewModel.navigateTo(AppDestination.WALLET) }
                )
            }
        }

        FeedbackBannerOverlay(
            feedbackBanner = feedbackBanner,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp, start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
private fun FeedbackBannerOverlay(
    feedbackBanner: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = feedbackBanner != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        feedbackBanner?.let { msg ->
            Surface(
                color = GlassCardHighlight.copy(alpha = 0.96f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = msg,
                    color = RoyalGold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
