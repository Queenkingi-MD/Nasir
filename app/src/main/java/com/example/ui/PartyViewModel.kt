package com.example.ui

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.GiftCatalog
import com.example.data.GiftItem
import com.example.data.PartyRepository
import com.example.data.PartyRoom
import com.example.data.RoomSeat
import com.example.data.UserProfile
import com.example.data.WithdrawMethod
import com.example.data.WithdrawRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

enum class AppDestination {
    PARTY_ROOM,
    LOBBY,
    WALLET,
    ADMIN
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class GiftBannerEvent(
    val id: Long,
    val emoji: String,
    val text: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class PartyViewModel(
    private val repository: PartyRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _destination = MutableStateFlow(AppDestination.PARTY_ROOM)
    val destination: StateFlow<AppDestination> = _destination.asStateFlow()

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _feedbackBanner = MutableStateFlow<String?>(null)
    val feedbackBanner: StateFlow<String?> = _feedbackBanner.asStateFlow()

    private val _giftBanner = MutableStateFlow<GiftBannerEvent?>(null)
    val giftBanner: StateFlow<GiftBannerEvent?> = _giftBanner.asStateFlow()

    private val _micAudioLevel = MutableStateFlow(0f)
    val micAudioLevel: StateFlow<Float> = _micAudioLevel.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private var micMonitorJob: Job? = null

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFlowError("Profile load karne me masla aaya", error)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val isAdmin: StateFlow<Boolean> =
        repository.isCurrentUserAdmin(Firebase.auth.currentUser?.email, currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val liveRoomsState: StateFlow<UiState<List<PartyRoom>>> =
        repository.observeLiveRooms()
            .map<List<PartyRoom>, UiState<List<PartyRoom>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFlowError("Rooms load karne me masla aaya", error)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val seatsState: StateFlow<UiState<List<RoomSeat>>> =
        _activeRoomId
            .filterNotNull()
            .flatMapLatest { roomId -> repository.observeSeats(roomId) }
            .map<List<RoomSeat>, UiState<List<RoomSeat>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFlowError("Seats load karne me masla aaya", error)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val messagesState: StateFlow<UiState<List<ChatMessage>>> =
        _activeRoomId
            .filterNotNull()
            .flatMapLatest { roomId -> repository.observeMessages(roomId) }
            .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFlowError("Chat load karne me masla aaya", error)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myWithdrawRequestsState: StateFlow<UiState<List<WithdrawRequest>>> =
        repository.observeMyWithdrawRequests(currentUserId)
            .map<List<WithdrawRequest>, UiState<List<WithdrawRequest>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFlowError("Withdraw history load na ho saki", error)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val adminWithdrawRequestsState: StateFlow<UiState<List<WithdrawRequest>>> =
        isAdmin
            .flatMapLatest { admin ->
                if (admin) repository.observeAllWithdrawRequestsForAdmin()
                else flowOf(emptyList())
            }
            .map<List<WithdrawRequest>, UiState<List<WithdrawRequest>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFlowError("Admin requests load na ho sakin", error)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    init {
        bootstrapSession()
    }

    private fun bootstrapSession() {
        val firebaseUser = Firebase.auth.currentUser ?: return
        viewModelScope.launch {
            val profileRes = repository.ensureUserProfile(firebaseUser)
            val profile = profileRes.getOrNull() ?: UserProfile(
                userId = firebaseUser.uid,
                displayName = firebaseUser.displayName ?: "Mehmaan",
                avatarKey = "sana",
                coins = 1500L,
                diamonds = 250L
            )
            val roomRes = repository.ensureDefaultPartyRoom(profile)
            roomRes.onSuccess { roomId ->
                _activeRoomId.value = roomId
            }.onFailure { err ->
                showFeedback(err.message ?: "Room shuru karne me masla aaya")
            }
        }
    }

    fun navigateTo(dest: AppDestination) {
        _destination.value = dest
    }

    fun enterRoom(roomId: String) {
        _activeRoomId.value = roomId
        _destination.value = AppDestination.PARTY_ROOM
    }

    fun createNewRoom(title: String) {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.createRoomWithInitialSeats(
                title = title,
                hostUid = currentUserId,
                hostName = profile.displayName,
                hostAvatarKey = profile.avatarKey
            ).onSuccess { roomId ->
                _activeRoomId.value = roomId
                _destination.value = AppDestination.PARTY_ROOM
                showFeedback("Naya room ban gaya! 🎉")
            }.onFailure { err ->
                showFeedback(err.message ?: "Room nahi ban saka.")
            }
            _isBusy.value = false
        }
    }

    fun sitOnSeat(seatNumber: Int) {
        val roomId = _activeRoomId.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val seats = (seatsState.value as? UiState.Success)?.data ?: emptyList()
        val targetSeat = seats.firstOrNull { it.seatNumber.toInt() == seatNumber }
        if (targetSeat?.occupied == true && targetSeat.occupantUid != currentUserId) {
            showFeedback("Yeh seat pehle se bhari hui hai!")
            return
        }
        val myCurrentSeat = seats.firstOrNull { it.occupied && it.occupantUid == currentUserId }
        if (myCurrentSeat?.seatNumber?.toInt() == seatNumber) {
            return
        }
        viewModelScope.launch {
            repository.joinSeat(
                roomId = roomId,
                targetSeatNumber = seatNumber,
                previousSeatNumber = myCurrentSeat?.seatNumber?.toInt(),
                user = profile
            ).onSuccess {
                showFeedback("Aap Seat #${seatNumber + 1} par baith gaye hain! 🎤")
            }.onFailure { err ->
                showFeedback(err.message ?: "Seat par baithne me masla aaya.")
            }
        }
    }

    fun toggleMic(hasAudioPermission: Boolean) {
        val roomId = _activeRoomId.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val seats = (seatsState.value as? UiState.Success)?.data ?: emptyList()
        val mySeat = seats.firstOrNull { it.occupied && it.occupantUid == currentUserId }

        viewModelScope.launch {
            if (mySeat == null) {
                // Auto-claim first empty guest seat when user taps Mic
                val emptySeat = seats.firstOrNull { !it.occupied }
                if (emptySeat == null) {
                    showFeedback("Tamam 8 seats bhari hui hain! 🪑")
                    return@launch
                }
                repository.joinSeat(roomId, emptySeat.seatNumber.toInt(), null, profile)
                    .onSuccess {
                        if (hasAudioPermission) startMicMeter()
                        showFeedback("Aap ka Mic ON ho gaya! 🎤 Awaz live hai.")
                    }
                    .onFailure { showFeedback(it.message ?: "Mic ON nahi ho saka.") }
            } else {
                val newState = !mySeat.micOn
                repository.toggleMic(roomId, mySeat.seatNumber.toInt(), newState)
                    .onSuccess {
                        if (newState) {
                            if (hasAudioPermission) startMicMeter()
                            showFeedback("Mic ON ho gaya 🎤")
                        } else {
                            stopMicMeter()
                            showFeedback("Mic OFF (Mute) kar diya gaya 🔇")
                        }
                    }
                    .onFailure { showFeedback(it.message ?: "Mic tabdeel nahi ho saka.") }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startMicMeter() {
        if (micMonitorJob?.isActive == true) return
        micMonitorJob = viewModelScope.launch(Dispatchers.IO) {
            var recorder: AudioRecord? = null
            try {
                val minBuf = AudioRecord.getMinBufferSize(
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuf > 0) {
                    recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBuf
                    )
                    if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                        recorder.startRecording()
                        val buf = ShortArray(minBuf / 2)
                        while (isActive) {
                            val read = recorder.read(buf, 0, buf.size)
                            if (read > 0) {
                                var peak = 0
                                for (i in 0 until read) {
                                    val v = abs(buf[i].toInt())
                                    if (v > peak) peak = v
                                }
                                _micAudioLevel.value = (peak / 8000f).coerceIn(0f, 1f)
                            }
                            delay(90L)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PartyVM", "Mic hardware unavailable on device/emulator: ${e.message}")
            } finally {
                try {
                    recorder?.stop()
                    recorder?.release()
                } catch (_: Exception) {
                }
                _micAudioLevel.value = 0f
            }
        }
    }

    fun stopMicMeter() {
        micMonitorJob?.cancel()
        micMonitorJob = null
        _micAudioLevel.value = 0f
    }

    fun sendChat(text: String) {
        val roomId = _activeRoomId.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sendChatMessage(roomId, profile, text, "CHAT")
                .onFailure { showFeedback(it.message ?: "Paigham nahi bheja ja saka.") }
        }
    }

    fun sendGift(targetSeat: RoomSeat, gift: GiftItem) {
        val roomId = _activeRoomId.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.sendGift(roomId, profile, targetSeat, gift)
                .onSuccess {
                    triggerGiftBanner(
                        gift.emoji,
                        "${profile.displayName} ne ${targetSeat.occupantName} ko ${gift.emoji} ${gift.nameRomanUrdu} bheja!"
                    )
                }
                .onFailure { showFeedback(it.message ?: "Tohfa nahi bheja ja saka.") }
            _isBusy.value = false
        }
    }

    fun triggerIncomingGiftFromRoom() {
        val roomId = _activeRoomId.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val seats = (seatsState.value as? UiState.Success)?.data ?: emptyList()
        val mySeat = seats.firstOrNull { it.occupied && it.occupantUid == currentUserId }
        val gift = GiftCatalog.items.random()
        val guestName = listOf("Sana", "Ali", "Zoya", "Hamza").random()
        viewModelScope.launch {
            repository.receiveAppreciationGift(roomId, profile, mySeat, gift, guestName)
                .onSuccess {
                    triggerGiftBanner(
                        gift.emoji,
                        "$guestName ne aapko ${gift.emoji} ${gift.nameRomanUrdu} bheja! (+${gift.diamondReward} Coins & 💎)"
                    )
                }
                .onFailure { showFeedback(it.message ?: "Gift receive nahi ho saka.") }
        }
    }

    fun spinLuckyWheel(betCoins: Long, onResult: (Long) -> Unit) {
        val roomId = _activeRoomId.value ?: return
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val outcomes = listOf(0, 1, 2, 3, 5, 0, 2, 1)
        val multiplier = outcomes.random()
        viewModelScope.launch {
            _isBusy.value = true
            repository.playLuckyWheel(roomId, profile, betCoins, multiplier)
                .onSuccess { won ->
                    onResult(won)
                    if (won > 0) {
                        triggerGiftBanner("🎮", "Mubarak! Aap ne $won Coins jeet liye! 🎉")
                    }
                }
                .onFailure { showFeedback(it.message ?: "Game nahi chal saki.") }
            _isBusy.value = false
        }
    }

    fun leaveRoomToLobby() {
        stopMicMeter()
        val roomId = _activeRoomId.value
        val seats = (seatsState.value as? UiState.Success)?.data ?: emptyList()
        val mySeat = seats.firstOrNull { it.occupied && it.occupantUid == currentUserId }
        viewModelScope.launch {
            if (roomId != null && mySeat != null) {
                repository.leaveSeat(roomId, mySeat.seatNumber.toInt())
            }
            _destination.value = AppDestination.LOBBY
        }
    }

    fun convertDiamonds(amount: Long) {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            repository.convertDiamondsToCoins(profile, amount)
                .onSuccess { showFeedback("$amount Diamonds 💎 Coins 🪙 me badal gaye!") }
                .onFailure { showFeedback(it.message ?: "Conversion nakam rahi.") }
            _isBusy.value = false
        }
    }

    fun submitWithdraw(
        method: WithdrawMethod,
        accountNumber: String,
        coinsAmountText: String,
        onSuccess: () -> Unit
    ) {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val amount = coinsAmountText.trim().toLongOrNull() ?: 0L
        viewModelScope.launch {
            _isBusy.value = true
            repository.submitWithdrawRequest(profile, method, accountNumber, amount)
                .onSuccess {
                    showFeedback("Withdraw request bhej di gayi! Admin manzoori ke baad paisa milega ✅")
                    onSuccess()
                }
                .onFailure { err ->
                    showFeedback(err.message ?: "Withdraw request nahi bheji ja saki.")
                }
            _isBusy.value = false
        }
    }

    fun decideWithdrawRequest(request: WithdrawRequest, approve: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.processWithdrawDecision(request, approve)
                .onSuccess {
                    showFeedback(
                        if (approve) "Request approve ho gayi — Paisa bhej diya gaya! ✅"
                        else "Request reject ho gayi — Coins wapis kar diye gaye! ❌"
                    )
                }
                .onFailure { showFeedback(it.message ?: "Admin action nakam raha.") }
            _isBusy.value = false
        }
    }

    fun clearFeedback() {
        _feedbackBanner.value = null
    }

    private fun showFeedback(msg: String) {
        _feedbackBanner.value = msg
        viewModelScope.launch {
            delay(3600L)
            if (_feedbackBanner.value == msg) {
                _feedbackBanner.value = null
            }
        }
    }

    private fun triggerGiftBanner(emoji: String, text: String) {
        val event = GiftBannerEvent(System.currentTimeMillis(), emoji, text)
        _giftBanner.value = event
        viewModelScope.launch {
            delay(3200L)
            if (_giftBanner.value?.id == event.id) {
                _giftBanner.value = null
            }
        }
    }

    private fun <T> unwrapFlowError(fallback: String, error: Throwable): UiState<T> {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w("PartyVM", fallback, root)
        return UiState.Error(root.message ?: fallback)
    }

    override fun onCleared() {
        super.onCleared()
        stopMicMeter()
    }
}
