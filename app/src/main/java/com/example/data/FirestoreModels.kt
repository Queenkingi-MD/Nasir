package com.example.data

import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val avatarKey: String = "sana",
    val coins: Long = 0L,
    val diamonds: Long = 0L,
    val totalEarnedRs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PartyRoom(
    @DocumentId val id: String = "",
    val roomTitle: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val roomStatus: String = "LIVE",
    val activeSpeakersCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomSeat(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val seatNumber: Long = 0L,
    val seatRole: String = "GUEST",
    val occupantUid: String = "",
    val occupantName: String = "",
    val avatarKey: String = "empty",
    val micOn: Boolean = false,
    val occupied: Boolean = false,
    val diamondsReceived: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderAvatarKey: String = "sana",
    val messageText: String = "",
    val messageType: String = "CHAT",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val method: String = "EASYPAISA",
    val accountNumber: String = "",
    val coinsAmount: Long = 0L,
    val rupeesAmount: Long = 0L,
    val status: String = "PENDING",
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class WithdrawMethod(val wireValue: String, val label: String, val emoji: String) {
    EASYPAISA("EASYPAISA", "Easypaisa", "📱"),
    JAZZCASH("JAZZCASH", "JazzCash", "💳");

    companion object {
        fun fromWire(raw: String): WithdrawMethod =
            entries.firstOrNull { it.wireValue == raw } ?: EASYPAISA
    }
}

enum class WithdrawStatus(val wireValue: String, val romanUrduLabel: String, val emoji: String) {
    PENDING("PENDING", "Admin ki manzoori baqi hai", "⏳"),
    APPROVED("APPROVED", "Approve ho gaya - Paisa bhej diya gaya", "✅"),
    REJECTED("REJECTED", "Reject ho gaya - Coins wapis mil gaye", "❌");

    companion object {
        fun fromWire(raw: String): WithdrawStatus =
            entries.firstOrNull { it.wireValue == raw } ?: PENDING
    }
}

data class GiftItem(
    val id: String,
    val nameRomanUrdu: String,
    val emoji: String,
    val coinCost: Long,
    val diamondReward: Long
)

object GiftCatalog {
    val items = listOf(
        GiftItem("rose", "Phool", "🌹", 50L, 50L),
        GiftItem("heart", "Dil", "❤️", 100L, 100L),
        GiftItem("car", "Gari", "🚗", 500L, 500L),
        GiftItem("diamond", "Heera", "💎", 1000L, 1000L)
    )
}

fun avatarDrawableForKey(key: String): Int? = when (key) {
    "host" -> R.drawable.img_avatar_host_1795514874512
    "sana" -> R.drawable.img_avatar_sana_1795514874877
    "ali" -> R.drawable.img_avatar_ali_1795514874513
    "zoya" -> R.drawable.img_avatar_zoya_1795514874657
    else -> null
}
