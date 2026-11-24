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
import com.example.data.PartyRoom
import com.example.data.RoomSeat
import com.example.data.UserProfile
import com.example.data.WithdrawMethod
import com.example.data.WithdrawRequest
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class DemoPartyViewModel : ViewModel() {

    companion object {
        const val DEMO_UID = "demo_mehfil_user"
    }

    private val _destination = MutableStateFlow(AppDestination.PARTY_ROOM)
    val destination: StateFlow<AppDestination> = _destination.asStateFlow()

    private val _activeRoomId = MutableStateFlow("demo_room_1")
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            id = DEMO_UID,
            userId = DEMO_UID,
            displayName = "Mehfil User",
            avatarKey = "sana",
            coins = 1500L,
            diamonds = 250L,
            totalEarnedRs = 0L
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _rooms = MutableStateFlow(
        listOf(
            PartyRoom(
                id = "demo_room_1",
                roomTitle = "Mehfil-e-Yaari 🎉",
                hostUid = "host_hamza",
                hostName = "Hamza",
                roomStatus = "LIVE",
                activeSpeakersCount = 4L
            ),
            PartyRoom(
                id = "demo_room_2",
                roomTitle = "Raat ki Mehfil 🌙",
                hostUid = "guest_zoya",
                hostName = "Zoya",
                roomStatus = "LIVE",
                activeSpeakersCount = 3L
            )
        )
    )
    val rooms: StateFlow<List<PartyRoom>> = _rooms.asStateFlow()

    private val _seats = MutableStateFlow(
        listOf(
            RoomSeat("0", "demo_room_1", 0L, "HOST", "host_hamza", "Hamza", "host", micOn = true, occupied = true, diamondsReceived = 180L),
            RoomSeat("1", "demo_room_1", 1L, "GUEST", DEMO_UID, "Mehfil User", "sana", micOn = true, occupied = true, diamondsReceived = 90L),
            RoomSeat("2", "demo_room_1", 2L, "GUEST", "guest_ali", "Ali", "ali", micOn = false, occupied = true, diamondsReceived = 60L),
            RoomSeat("3", "demo_room_1", 3L, "GUEST", "guest_zoya", "Zoya", "zoya", micOn = true, occupied = true, diamondsReceived = 110L),
            RoomSeat("4", "demo_room_1", 4L, "GUEST", "", "", "empty", micOn = false, occupied = false, diamondsReceived = 0L),
            RoomSeat("5", "demo_room_1", 5L, "GUEST", "", "", "empty", micOn = false, occupied = false, diamondsReceived = 0L),
            RoomSeat("6", "demo_room_1", 6L, "GUEST", "", "", "empty", micOn = false, occupied = false, diamondsReceived = 0L),
            RoomSeat("7", "demo_room_1", 7L, "GUEST", "", "", "empty", micOn = false, occupied = false, diamondsReceived = 0L)
        )
    )
    val seats: StateFlow<List<RoomSeat>> = _seats.asStateFlow()

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "m1",
                roomId = "demo_room_1",
                senderUid = "host_hamza",
                senderName = "Hamza",
                senderAvatarKey = "host",
                messageText = "Welcome! Aap andar aa gayi! Mehfil Party Room me khush aamdeed 🎉",
                messageType = "SYSTEM",
                createdAt = Timestamp.now()
            ),
            ChatMessage(
                id = "m2",
                roomId = "demo_room_1",
                senderUid = "guest_zoya",
                senderName = "Zoya",
                senderAvatarKey = "zoya",
                messageText = "Assalam-o-Alaikum sab doston ko! 🎤✨",
                messageType = "CHAT",
                createdAt = Timestamp.now()
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _withdrawRequests = MutableStateFlow<List<WithdrawRequest>>(emptyList())
    val withdrawRequests: StateFlow<List<WithdrawRequest>> = _withdrawRequests.asStateFlow()

    private val _feedbackBanner = MutableStateFlow<String?>("Welcome! Aap andar aa gayi! 🎉 (1500 Coins)")
    val feedbackBanner: StateFlow<String?> = _feedbackBanner.asStateFlow()

    private val _giftBanner = MutableStateFlow<GiftBannerEvent?>(null)
    val giftBanner: StateFlow<GiftBannerEvent?> = _giftBanner.asStateFlow()

    private val _micAudioLevel = MutableStateFlow(0f)
    val micAudioLevel: StateFlow<Float> = _micAudioLevel.asStateFlow()

    private var micMonitorJob: Job? = null
    private var msgCounter = 10

    init {
        viewModelScope.launch {
            delay(3200L)
            if (_feedbackBanner.value?.startsWith("Welcome!") == true) {
                _feedbackBanner.value = null
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
        val clean = title.trim().take(50)
        if (clean.length < 2) {
            showFeedback("Room ka naam kam az kam 2 haroof ka rakhein.")
            return
        }
        val newId = "demo_room_${System.currentTimeMillis()}"
        val newRoom = PartyRoom(
            id = newId,
            roomTitle = clean,
            hostUid = DEMO_UID,
            hostName = _userProfile.value.displayName,
            roomStatus = "LIVE",
            activeSpeakersCount = 1L
        )
        _rooms.value = listOf(newRoom) + _rooms.value
        _activeRoomId.value = newId
        _destination.value = AppDestination.PARTY_ROOM
        showFeedback("Naya room '$clean' ban gaya! 🎉")
    }

    fun sitOnSeat(seatNumber: Int) {
        val target = _seats.value.firstOrNull { it.seatNumber.toInt() == seatNumber } ?: return
        if (target.occupied && target.occupantUid != DEMO_UID) {
            showFeedback("Yeh seat pehle se bhari hui hai!")
            return
        }
        _seats.value = _seats.value.map { seat ->
            when {
                seat.occupantUid == DEMO_UID && seat.seatNumber.toInt() != seatNumber -> {
                    seat.copy(occupantUid = "", occupantName = "", avatarKey = "empty", micOn = false, occupied = false)
                }
                seat.seatNumber.toInt() == seatNumber -> {
                    seat.copy(
                        occupantUid = DEMO_UID,
                        occupantName = _userProfile.value.displayName,
                        avatarKey = _userProfile.value.avatarKey,
                        micOn = true,
                        occupied = true
                    )
                }
                else -> seat
            }
        }
        showFeedback("Aap Seat #${seatNumber + 1} par baith gaye hain! 🎤")
    }

    fun toggleMic(hasAudioPermission: Boolean) {
        val mySeat = _seats.value.firstOrNull { it.occupied && it.occupantUid == DEMO_UID }
        if (mySeat == null) {
            val emptySeat = _seats.value.firstOrNull { !it.occupied }
            if (emptySeat == null) {
                showFeedback("Tamam 8 seats bhari hui hain! 🪑")
                return
            }
            sitOnSeat(emptySeat.seatNumber.toInt())
            if (hasAudioPermission) startMicMeter()
            return
        }
        val newState = !mySeat.micOn
        _seats.value = _seats.value.map {
            if (it.seatNumber == mySeat.seatNumber) it.copy(micOn = newState) else it
        }
        if (newState) {
            if (hasAudioPermission) startMicMeter()
            showFeedback("Mic ON ho gaya 🎤")
        } else {
            stopMicMeter()
            showFeedback("Mic OFF (Mute) kar diya gaya 🔇")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startMicMeter() {
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
                Log.w("DemoPartyVM", "Mic meter unavailable: ${e.message}")
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

    private fun stopMicMeter() {
        micMonitorJob?.cancel()
        micMonitorJob = null
        _micAudioLevel.value = 0f
    }

    fun sendChat(text: String) {
        val clean = text.trim().take(240)
        if (clean.isBlank()) return
        appendMessage(_userProfile.value.displayName, clean, "CHAT")
    }

    fun sendGift(targetSeat: RoomSeat, gift: GiftItem) {
        val cur = _userProfile.value
        if (cur.coins < gift.coinCost) {
            showFeedback("Aap ke paas kafi Coins nahi hain! (${gift.coinCost} Coins chahiye)")
            return
        }
        val isSelf = targetSeat.occupantUid == DEMO_UID
        _userProfile.value = cur.copy(
            coins = cur.coins - gift.coinCost,
            diamonds = if (isSelf) cur.diamonds + gift.diamondReward else cur.diamonds
        )
        _seats.value = _seats.value.map { seat ->
            if (seat.seatNumber == targetSeat.seatNumber) {
                seat.copy(diamondsReceived = seat.diamondsReceived + gift.diamondReward)
            } else seat
        }
        val text = "${cur.displayName} ne ${targetSeat.occupantName} ko ${gift.emoji} ${gift.nameRomanUrdu} bheja! (+${gift.diamondReward} 💎)"
        appendMessage(cur.displayName, text, "GIFT")
        triggerGiftBanner(gift.emoji, text)
    }

    fun triggerIncomingGiftFromRoom() {
        val cur = _userProfile.value
        val gift = GiftCatalog.items.random()
        val guestName = listOf("Hamza", "Zoya", "Ali").random()
        _userProfile.value = cur.copy(
            coins = cur.coins + gift.diamondReward,
            diamonds = cur.diamonds + gift.diamondReward
        )
        _seats.value = _seats.value.map { seat ->
            if (seat.occupantUid == DEMO_UID) {
                seat.copy(diamondsReceived = seat.diamondsReceived + gift.diamondReward)
            } else seat
        }
        val text = "$guestName ne aapko ${gift.emoji} ${gift.nameRomanUrdu} bheja! (+${gift.diamondReward} Coins & 💎)"
        appendMessage(guestName, text, "GIFT")
        triggerGiftBanner(gift.emoji, text)
    }

    fun spinLuckyWheel(betCoins: Long, onResult: (Long) -> Unit) {
        val cur = _userProfile.value
        if (cur.coins < betCoins) {
            showFeedback("Game khelne ke liye kam az kam $betCoins Coins chahiye.")
            return
        }
        val multiplier = listOf(0, 1, 2, 3, 5, 0, 2, 1).random()
        val won = betCoins * multiplier
        val newCoins = (cur.coins - betCoins + won).coerceAtLeast(0L)
        _userProfile.value = cur.copy(coins = newCoins)
        val msg = if (won > 0) "🎮 ${cur.displayName} ne Qismat ka Chakka ghumaya aur $won Coins jeet liye! 🎉"
        else "🎮 ${cur.displayName} ne Qismat ka Chakka ghumaya — agly bar behtar qismat! 🎲"
        appendMessage(cur.displayName, msg, "GAME")
        if (won > 0) triggerGiftBanner("🎮", "Mubarak! Aap ne $won Coins jeet liye! 🎉")
        onResult(won)
    }

    fun leaveRoomToLobby() {
        stopMicMeter()
        _seats.value = _seats.value.map { seat ->
            if (seat.occupantUid == DEMO_UID) {
                seat.copy(occupantUid = "", occupantName = "", avatarKey = "empty", micOn = false, occupied = false)
            } else seat
        }
        _destination.value = AppDestination.LOBBY
    }

    fun convertDiamonds(amount: Long) {
        val cur = _userProfile.value
        if (amount <= 0L || cur.diamonds < amount) {
            showFeedback("Aap ke paas itne Diamonds mojud nahi hain.")
            return
        }
        _userProfile.value = cur.copy(
            diamonds = cur.diamonds - amount,
            coins = cur.coins + amount
        )
        showFeedback("$amount Diamonds 💎 Coins 🪙 me badal gaye!")
    }

    fun submitWithdraw(
        method: WithdrawMethod,
        accountNumber: String,
        coinsAmountText: String,
        onSuccess: () -> Unit
    ) {
        val cur = _userProfile.value
        val amount = coinsAmountText.trim().toLongOrNull() ?: 0L
        val cleanNum = accountNumber.trim()
        if (amount < 1000L) {
            showFeedback("Kam az kam 1000 Coins (= 1000 Rs) withdraw ho sakte hain.")
            return
        }
        if (!cleanNum.matches(Regex("^03[0-9]{9}$"))) {
            showFeedback("Sahi 11 hindson ka number likhein (maslan 03001234567).")
            return
        }
        if (cur.coins < amount) {
            showFeedback("Aap ke Wallet me kafi Coins nahi hain.")
            return
        }
        val req = WithdrawRequest(
            id = "demo_req_${System.currentTimeMillis()}",
            userId = DEMO_UID,
            userName = cur.displayName,
            method = method.wireValue,
            accountNumber = cleanNum,
            coinsAmount = amount,
            rupeesAmount = amount,
            status = "PENDING",
            adminNote = "Admin ki manzoori ka intezar hai",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _userProfile.value = cur.copy(coins = cur.coins - amount)
        _withdrawRequests.value = listOf(req) + _withdrawRequests.value
        showFeedback("Withdraw request bhej di gayi! Demo Admin Panel me approve karein ✅")
        onSuccess()
    }

    fun decideWithdrawRequest(request: WithdrawRequest, approve: Boolean) {
        _withdrawRequests.value = _withdrawRequests.value.map { req ->
            if (req.id == request.id && req.status == "PENDING") {
                req.copy(
                    status = if (approve) "APPROVED" else "REJECTED",
                    adminNote = if (approve) "Admin ne approve kar ke Rs. ${req.rupeesAmount} bhej diye!"
                    else "Request mustarad — Coins wapis mil gaye."
                )
            } else req
        }
        val cur = _userProfile.value
        if (approve) {
            _userProfile.value = cur.copy(totalEarnedRs = cur.totalEarnedRs + request.rupeesAmount)
            showFeedback("Request approve ho gayi — Rs. ${request.rupeesAmount} bhej diye gaye! ✅")
        } else {
            _userProfile.value = cur.copy(coins = cur.coins + request.coinsAmount)
            showFeedback("Request reject ho gayi — Coins wapis mil gaye! ❌")
        }
    }

    private fun appendMessage(senderName: String, text: String, type: String) {
        msgCounter++
        val newMsg = ChatMessage(
            id = "demo_msg_$msgCounter",
            roomId = _activeRoomId.value ?: "demo_room_1",
            senderUid = DEMO_UID,
            senderName = senderName,
            senderAvatarKey = "sana",
            messageText = text,
            messageType = type,
            createdAt = Timestamp.now()
        )
        _messages.value = _messages.value + newMsg
    }

    private fun showFeedback(msg: String) {
        _feedbackBanner.value = msg
        viewModelScope.launch {
            delay(3600L)
            if (_feedbackBanner.value == msg) _feedbackBanner.value = null
        }
    }

    private fun triggerGiftBanner(emoji: String, text: String) {
        val event = GiftBannerEvent(System.currentTimeMillis(), emoji, text)
        _giftBanner.value = event
        viewModelScope.launch {
            delay(3200L)
            if (_giftBanner.value?.id == event.id) _giftBanner.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMicMeter()
    }
}
