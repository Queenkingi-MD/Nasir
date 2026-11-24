package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class PartyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Aap ko pehle Google se Sign In karna hoga.")

    fun isCurrentUserAdmin(email: String?, uid: String): Flow<Boolean> = flow {
        val verifiedEmailAdmin =
            auth.currentUser?.isEmailVerified == true &&
                email.equals("sanakhanjani412@gmail.com", ignoreCase = true)
        if (verifiedEmailAdmin) {
            emit(true)
            return@flow
        }
        val docPath = "admins/$uid"
        emitAll(
            db.collection("admins").document(uid)
                .snapshots()
                .map { it.exists() }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(false)
                }
        )
    }

    suspend fun ensureUserProfile(firebaseUser: FirebaseUser): Result<UserProfile> {
        val uid = firebaseUser.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(userId = uid, displayName = "Mehmaan")
                Result.success(existing)
            } else {
                val rawName = firebaseUser.displayName?.trim().orEmpty()
                    .ifBlank { firebaseUser.email?.substringBefore("@").orEmpty() }
                    .ifBlank { "Mehmaan" }
                    .take(40)
                val avatarKey = listOf("sana", "ali", "zoya", "host")[(uid.hashCode().ushr(1)) % 4]
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to rawName,
                    "avatarKey" to avatarKey,
                    "coins" to 1500L,
                    "diamonds" to 250L,
                    "totalEarnedRs" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(
                    UserProfile(
                        id = uid,
                        userId = uid,
                        displayName = rawName,
                        avatarKey = avatarKey,
                        coins = 1500L,
                        diamonds = 250L,
                        totalEarnedRs = 0L
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val activeUid = requireUserId()
        check(activeUid == uid) { "User session changed." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    snap.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeLiveRooms(): Flow<List<PartyRoom>> = flow {
        val uid = requireUserId()
        val path = "party_rooms"
        emitAll(
            db.collection(path)
                .whereEqualTo("roomStatus", "LIVE")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PartyRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureDefaultPartyRoom(user: UserProfile): Result<String> {
        val path = "party_rooms"
        return try {
            val uid = requireUserId()
            val liveSnap = db.collection(path)
                .whereEqualTo("roomStatus", "LIVE")
                .limit(1)
                .get()
                .await()
            if (!liveSnap.isEmpty) {
                return Result.success(liveSnap.documents.first().id)
            }
            createRoomWithInitialSeats(
                title = "Mehfil-e-Yaari 🎉",
                hostUid = uid,
                hostName = user.displayName.ifBlank { "Host" }.take(32),
                hostAvatarKey = user.avatarKey
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createRoomWithInitialSeats(
        title: String,
        hostUid: String,
        hostName: String,
        hostAvatarKey: String
    ): Result<String> {
        val cleanTitle = title.trim().take(50)
        if (cleanTitle.length < 2) {
            return Result.failure(IllegalArgumentException("Room ka naam kam az kam 2 haroof ka rakhein."))
        }
        val roomRef = db.collection("party_rooms").document()
        val roomId = roomRef.id
        return try {
            val uid = requireUserId()
            check(uid == hostUid)
            val batch = db.batch()
            val cleanHostName = hostName.trim().ifBlank { "Host" }.take(40)
            val validAvatar = if (hostAvatarKey in listOf("host", "sana", "ali", "zoya")) hostAvatarKey else "host"

            batch.set(
                roomRef,
                mapOf(
                    "roomTitle" to cleanTitle,
                    "hostUid" to uid,
                    "hostName" to cleanHostName,
                    "roomStatus" to "LIVE",
                    "activeSpeakersCount" to 4L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val starterGuests = listOf(
                Triple("guest_sana", "Sana", "sana"),
                Triple("guest_ali", "Ali", "ali"),
                Triple("guest_zoya", "Zoya", "zoya")
            )

            for (i in 0..7) {
                val seatRef = roomRef.collection("seats").document(i.toString())
                val seatPayload = when (i) {
                    0 -> mapOf(
                        "roomId" to roomId,
                        "seatNumber" to 0L,
                        "seatRole" to "HOST",
                        "occupantUid" to uid,
                        "occupantName" to cleanHostName,
                        "avatarKey" to "host",
                        "micOn" to true,
                        "occupied" to true,
                        "diamondsReceived" to 120L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    in 1..3 -> {
                        val (gUid, gName, gAvatar) = starterGuests[i - 1]
                        mapOf(
                            "roomId" to roomId,
                            "seatNumber" to i.toLong(),
                            "seatRole" to "GUEST",
                            "occupantUid" to gUid,
                            "occupantName" to gName,
                            "avatarKey" to gAvatar,
                            "micOn" to (i != 2),
                            "occupied" to true,
                            "diamondsReceived" to (90L - i * 10L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    }
                    else -> mapOf(
                        "roomId" to roomId,
                        "seatNumber" to i.toLong(),
                        "seatRole" to "GUEST",
                        "occupantUid" to "",
                        "occupantName" to "",
                        "avatarKey" to "empty",
                        "micOn" to false,
                        "occupied" to false,
                        "diamondsReceived" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                }
                batch.set(seatRef, seatPayload)
            }

            val welcomeMsgRef = roomRef.collection("messages").document()
            batch.set(
                welcomeMsgRef,
                mapOf(
                    "roomId" to roomId,
                    "senderUid" to uid,
                    "senderName" to cleanHostName,
                    "senderAvatarKey" to validAvatar,
                    "messageText" to "Assalam-o-Alaikum! Mehfil Party Room me khush aamdeed 🎉🎤",
                    "messageType" to "SYSTEM",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(roomId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "party_rooms/$roomId")
            Result.failure(e)
        }
    }

    fun observeSeats(roomId: String): Flow<List<RoomSeat>> = flow {
        val uid = requireUserId()
        val path = "party_rooms/$roomId/seats"
        emitAll(
            db.collection("party_rooms").document(roomId).collection("seats")
                .whereEqualTo("roomId", roomId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RoomSeat::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.seatNumber }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(roomId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "party_rooms/$roomId/messages"
        emitAll(
            db.collection("party_rooms").document(roomId).collection("messages")
                .whereEqualTo("roomId", roomId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun joinSeat(
        roomId: String,
        targetSeatNumber: Int,
        previousSeatNumber: Int?,
        user: UserProfile
    ): Result<Unit> {
        val seatPath = "party_rooms/$roomId/seats/$targetSeatNumber"
        return try {
            val uid = requireUserId()
            val roomRef = db.collection("party_rooms").document(roomId)
            val batch = db.batch()
            if (previousSeatNumber != null && previousSeatNumber != targetSeatNumber) {
                val prevRef = roomRef.collection("seats").document(previousSeatNumber.toString())
                batch.update(
                    prevRef,
                    mapOf(
                        "occupantUid" to "",
                        "occupantName" to "",
                        "avatarKey" to "empty",
                        "micOn" to false,
                        "occupied" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val targetRef = roomRef.collection("seats").document(targetSeatNumber.toString())
            batch.update(
                targetRef,
                mapOf(
                    "occupantUid" to uid,
                    "occupantName" to user.displayName.take(40),
                    "avatarKey" to user.avatarKey,
                    "micOn" to true,
                    "occupied" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, seatPath)
            Result.failure(e)
        }
    }

    suspend fun leaveSeat(roomId: String, seatNumber: Int): Result<Unit> {
        val seatPath = "party_rooms/$roomId/seats/$seatNumber"
        return try {
            requireUserId()
            db.document(seatPath).update(
                mapOf(
                    "occupantUid" to "",
                    "occupantName" to "",
                    "avatarKey" to "empty",
                    "micOn" to false,
                    "occupied" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, seatPath)
            Result.failure(e)
        }
    }

    suspend fun toggleMic(roomId: String, seatNumber: Int, newMicOn: Boolean): Result<Unit> {
        val seatPath = "party_rooms/$roomId/seats/$seatNumber"
        return try {
            requireUserId()
            db.document(seatPath).update(
                mapOf(
                    "micOn" to newMicOn,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, seatPath)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        roomId: String,
        user: UserProfile,
        text: String,
        messageType: String = "CHAT"
    ): Result<Unit> {
        val cleanText = text.trim().take(240)
        if (cleanText.isBlank()) {
            return Result.failure(IllegalArgumentException("Khali paigham nahi bheja ja sakta."))
        }
        val msgRef = db.collection("party_rooms").document(roomId).collection("messages").document()
        return try {
            val uid = requireUserId()
            val payload = mapOf(
                "roomId" to roomId,
                "senderUid" to uid,
                "senderName" to user.displayName.ifBlank { "Mehmaan" }.take(40),
                "senderAvatarKey" to user.avatarKey,
                "messageText" to cleanText,
                "messageType" to messageType,
                "createdAt" to FieldValue.serverTimestamp()
            )
            msgRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendGift(
        roomId: String,
        sender: UserProfile,
        targetSeat: RoomSeat,
        gift: GiftItem
    ): Result<Unit> {
        if (sender.coins < gift.coinCost) {
            return Result.failure(
                IllegalStateException("Aap ke paas kafi Coins nahi hain! (${gift.coinCost} Coins chahiye)")
            )
        }
        if (!targetSeat.occupied) {
            return Result.failure(IllegalStateException("Kisi mojud seat ko chunein."))
        }
        val roomRef = db.collection("party_rooms").document(roomId)
        val userRef = db.collection("users").document(sender.userId)
        val seatRef = roomRef.collection("seats").document(targetSeat.seatNumber.toString())
        val msgRef = roomRef.collection("messages").document()
        return try {
            val uid = requireUserId()
            check(uid == sender.userId)
            val batch = db.batch()
            val isSelfGift = targetSeat.occupantUid == uid
            if (isSelfGift) {
                batch.update(
                    userRef,
                    mapOf(
                        "coins" to (sender.coins - gift.coinCost),
                        "diamonds" to (sender.diamonds + gift.diamondReward),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    userRef,
                    mapOf(
                        "coins" to (sender.coins - gift.coinCost),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                seatRef,
                mapOf(
                    "diamondsReceived" to (targetSeat.diamondsReceived + gift.diamondReward),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                msgRef,
                mapOf(
                    "roomId" to roomId,
                    "senderUid" to uid,
                    "senderName" to sender.displayName.take(40),
                    "senderAvatarKey" to sender.avatarKey,
                    "messageText" to "${sender.displayName} ne ${targetSeat.occupantName} ko ${gift.emoji} ${gift.nameRomanUrdu} bheja! (+${gift.diamondReward} 💎)",
                    "messageType" to "GIFT",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "party_rooms/$roomId/gift")
            Result.failure(e)
        }
    }

    suspend fun receiveAppreciationGift(
        roomId: String,
        user: UserProfile,
        mySeat: RoomSeat?,
        gift: GiftItem,
        fromGuestName: String
    ): Result<Unit> {
        val userRef = db.collection("users").document(user.userId)
        val roomRef = db.collection("party_rooms").document(roomId)
        val msgRef = roomRef.collection("messages").document()
        return try {
            val uid = requireUserId()
            check(uid == user.userId)
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "diamonds" to (user.diamonds + gift.diamondReward),
                    "coins" to (user.coins + gift.diamondReward),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (mySeat != null && mySeat.occupied) {
                val seatRef = roomRef.collection("seats").document(mySeat.seatNumber.toString())
                batch.update(
                    seatRef,
                    mapOf(
                        "diamondsReceived" to (mySeat.diamondsReceived + gift.diamondReward),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                msgRef,
                mapOf(
                    "roomId" to roomId,
                    "senderUid" to uid,
                    "senderName" to user.displayName.take(40),
                    "senderAvatarKey" to user.avatarKey,
                    "messageText" to "$fromGuestName ne aapko ${gift.emoji} ${gift.nameRomanUrdu} bheja! Aapko +${gift.diamondReward} Coins & 💎 mile!",
                    "messageType" to "GIFT",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun playLuckyWheel(
        roomId: String,
        user: UserProfile,
        betCoins: Long,
        winMultiplier: Int
    ): Result<Long> {
        if (user.coins < betCoins) {
            return Result.failure(IllegalStateException("Game khelne ke liye kam az kam $betCoins Coins chahiye."))
        }
        val winCoins = betCoins * winMultiplier
        val netDelta = winCoins - betCoins
        val newCoins = (user.coins + netDelta).coerceAtLeast(0L)
        val userRef = db.collection("users").document(user.userId)
        val msgRef = db.collection("party_rooms").document(roomId).collection("messages").document()
        return try {
            val uid = requireUserId()
            check(uid == user.userId)
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "coins" to newCoins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val resultMsg = if (winCoins > 0) {
                "🎮 ${user.displayName} ne Qismat ka Chakka ghumaya aur $winCoins Coins jeet liye! 🎉"
            } else {
                "🎮 ${user.displayName} ne Qismat ka Chakka ghumaya — agly bar behtar qismat! 🎲"
            }
            batch.set(
                msgRef,
                mapOf(
                    "roomId" to roomId,
                    "senderUid" to uid,
                    "senderName" to user.displayName.take(40),
                    "senderAvatarKey" to user.avatarKey,
                    "messageText" to resultMsg,
                    "messageType" to "GAME",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(winCoins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun convertDiamondsToCoins(user: UserProfile, amount: Long): Result<Unit> {
        if (amount <= 0L || user.diamonds < amount) {
            return Result.failure(IllegalStateException("Aap ke paas itne Diamonds mojud nahi hain."))
        }
        val capped = amount.coerceAtMost(5000L)
        val userRef = db.collection("users").document(user.userId)
        return try {
            val uid = requireUserId()
            check(uid == user.userId)
            userRef.update(
                mapOf(
                    "diamonds" to (user.diamonds - capped),
                    "coins" to (user.coins + capped),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawRequest(
        user: UserProfile,
        method: WithdrawMethod,
        accountNumber: String,
        coinsAmount: Long
    ): Result<Unit> {
        val cleanNum = accountNumber.trim()
        if (coinsAmount < 1000L) {
            return Result.failure(
                IllegalArgumentException("Kam az kam 1000 Coins (= 1000 Rs) withdraw ho sakte hain. Is se kam withdraw nahi ho sakta.")
            )
        }
        if (!cleanNum.matches(Regex("^03[0-9]{9}$"))) {
            return Result.failure(
                IllegalArgumentException("Sahi 11 hindson ka mobile number likhein (maslan 03001234567).")
            )
        }
        if (user.coins < coinsAmount) {
            return Result.failure(
                IllegalStateException("Aap ke Wallet me kafi Coins nahi hain. Mojooda Balance: ${user.coins} Coins.")
            )
        }
        val reqRef = db.collection("withdraw_requests").document()
        val userRef = db.collection("users").document(user.userId)
        return try {
            val uid = requireUserId()
            check(uid == user.userId)
            val batch = db.batch()
            batch.set(
                reqRef,
                mapOf(
                    "userId" to uid,
                    "userName" to user.displayName.ifBlank { "User" }.take(40),
                    "method" to method.wireValue,
                    "accountNumber" to cleanNum,
                    "coinsAmount" to coinsAmount,
                    "rupeesAmount" to coinsAmount,
                    "status" to "PENDING",
                    "adminNote" to "Admin ki manzoori ka intezar hai",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "coins" to (user.coins - coinsAmount),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reqRef.path)
            Result.failure(e)
        }
    }

    fun observeMyWithdrawRequests(uid: String): Flow<List<WithdrawRequest>> = flow {
        val activeUid = requireUserId()
        check(activeUid == uid)
        val path = "withdraw_requests"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WithdrawRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllWithdrawRequestsForAdmin(): Flow<List<WithdrawRequest>> = flow {
        val uid = requireUserId()
        val path = "withdraw_requests"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WithdrawRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun processWithdrawDecision(
        request: WithdrawRequest,
        approve: Boolean
    ): Result<Unit> {
        val reqRef = db.collection("withdraw_requests").document(request.id)
        val userRef = db.collection("users").document(request.userId)
        return try {
            requireUserId()
            val batch = db.batch()
            if (approve) {
                batch.update(
                    reqRef,
                    mapOf(
                        "status" to "APPROVED",
                        "adminNote" to "Admin ne approve kar ke Rs. ${request.rupeesAmount} bhej diye!",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    userRef,
                    mapOf(
                        "totalEarnedRs" to FieldValue.increment(request.rupeesAmount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    reqRef,
                    mapOf(
                        "status" to "REJECTED",
                        "adminNote" to "Request mustarad — Coins aap ke Wallet me wapis aa gaye.",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    userRef,
                    mapOf(
                        "coins" to FieldValue.increment(request.coinsAmount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }
}
