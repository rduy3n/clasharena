package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class GameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("L'utilisateur doit être connecté avec Google.")
    }

    suspend fun ensureProfile(displayName: String): Result<PlayerProfile> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: PlayerProfile(id = uid, userId = uid, displayName = displayName)
                Result.success(profile)
            } else {
                val safeName = displayName.trim().ifBlank { "Combattant" }.take(36)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "favoriteFighterId" to "ryuji",
                    "wins" to 0L,
                    "losses" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: PlayerProfile(id = uid, userId = uid, displayName = safeName)
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeProfile(userId: String): Flow<PlayerProfile?> {
        val docRef = db.collection(COLLECTION_USERS).document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun updateFavoriteFighter(fighterId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            docRef.update(
                mapOf(
                    "favoriteFighterId" to fighterId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordMatchResult(won: Boolean): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            val field = if (won) "wins" else "losses"
            docRef.update(
                mapOf(
                    field to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createMatchRoom(
        hostName: String,
        fighterId: String = "ryuji",
        stageId: String = "cyber_street"
    ): Result<MatchRoom> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val code = generateRoomCode()
        val docRef = db.collection(COLLECTION_ROOMS).document(code)
        val safeName = hostName.trim().ifBlank { "Joueur 1" }.take(36)
        val payload = mapOf(
            "roomCode" to code,
            "hostId" to uid,
            "hostName" to safeName,
            "guestId" to "",
            "guestName" to "",
            "participantIds" to listOf(uid),
            "status" to "waiting",
            "stageId" to stageId,
            "hostFighterId" to fighterId,
            "guestFighterId" to "kaelith",
            "hostReady" to false,
            "guestReady" to false,
            "roundNumber" to 1L,
            "p1RoundsWon" to 0L,
            "p2RoundsWon" to 0L,
            "winnerSide" to "none",
            "p1X" to 0.25,
            "p1Y" to 0.0,
            "p1Facing" to 1L,
            "p1Action" to "idle",
            "p1ActionSeq" to 0L,
            "p1Blocking" to false,
            "p1Ulti" to 0L,
            "p1DealtDmg" to 0L,
            "p2X" to 0.75,
            "p2Y" to 0.0,
            "p2Facing" to -1L,
            "p2Action" to "idle",
            "p2ActionSeq" to 0L,
            "p2Blocking" to false,
            "p2Ulti" to 0L,
            "p2DealtDmg" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            val room = docRef.get().await().toObject(
                MatchRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: MatchRoom(
                id = code,
                roomCode = code,
                hostId = uid,
                hostName = safeName,
                participantIds = listOf(uid),
                hostFighterId = fighterId,
                stageId = stageId
            )
            Result.success(room)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinMatchRoom(rawCode: String, guestName: String): Result<MatchRoom> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val code = rawCode.trim().uppercase()
        if (!ROOM_CODE_REGEX.matches(code)) {
            return Result.failure(IllegalArgumentException("Code salon invalide (6 caractères A-Z / 0-9)."))
        }
        val docRef = db.collection(COLLECTION_ROOMS).document(code)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalStateException("Salon introuvable : $code"))
            }
            val room = snapshot.toObject(
                MatchRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Données du salon invalides."))

            if (room.hostId == uid || uid in room.participantIds) {
                return Result.success(room)
            }
            if (room.status != "waiting" || room.guestId.isNotBlank()) {
                return Result.failure(IllegalStateException("Ce salon est déjà complet ou terminé."))
            }

            val safeGuest = guestName.trim().ifBlank { "Joueur 2" }.take(36)
            docRef.update(
                mapOf(
                    "guestId" to uid,
                    "guestName" to safeGuest,
                    "participantIds" to listOf(room.hostId, uid),
                    "status" to "selecting",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val updated = docRef.get().await().toObject(
                MatchRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: room.copy(
                guestId = uid,
                guestName = safeGuest,
                participantIds = listOf(room.hostId, uid),
                status = "selecting"
            )
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeMatchRoom(roomCode: String): Flow<MatchRoom?> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    MatchRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    fun observeMyRooms(userId: String): Flow<List<MatchRoom>> {
        return db.collection(COLLECTION_ROOMS)
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MatchRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_ROOMS)
                }
                throw error
            }
    }

    suspend fun updateLobbySelection(
        roomCode: String,
        isHost: Boolean,
        fighterId: String,
        ready: Boolean,
        stageId: String? = null,
        startFight: Boolean = false
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isHost) {
            updates["hostFighterId"] = fighterId
            updates["hostReady"] = ready
            if (stageId != null) {
                updates["stageId"] = stageId
            }
            if (startFight) {
                updates["status"] = "fighting"
                updates["roundNumber"] = 1L
                updates["p1RoundsWon"] = 0L
                updates["p2RoundsWon"] = 0L
                updates["winnerSide"] = "none"
                updates["p1DealtDmg"] = 0L
                updates["p2DealtDmg"] = 0L
            }
        } else {
            updates["guestFighterId"] = fighterId
            updates["guestReady"] = ready
            if (startFight) {
                updates["status"] = "fighting"
            }
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCombatState(
        roomCode: String,
        isHost: Boolean,
        x: Double,
        y: Double,
        facing: Int,
        action: String,
        actionSeq: Long,
        blocking: Boolean,
        ulti: Int,
        dealtDmg: Int
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        val prefix = if (isHost) "p1" else "p2"
        val updates = mapOf(
            "${prefix}X" to x.coerceIn(0.0, 1.0),
            "${prefix}Y" to y.coerceIn(0.0, 1.0),
            "${prefix}Facing" to if (facing >= 0) 1L else -1L,
            "${prefix}Action" to action.take(30),
            "${prefix}ActionSeq" to actionSeq.coerceAtLeast(0L),
            "${prefix}Blocking" to blocking,
            "${prefix}Ulti" to ulti.coerceIn(0, 100).toLong(),
            "${prefix}DealtDmg" to dealtDmg.coerceIn(0, 5000).toLong(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun advanceRound(
        roomCode: String,
        nextRound: Long,
        p1RoundsWon: Long,
        p2RoundsWon: Long
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        return try {
            docRef.update(
                mapOf(
                    "roundNumber" to nextRound,
                    "p1RoundsWon" to p1RoundsWon,
                    "p2RoundsWon" to p2RoundsWon,
                    "p1DealtDmg" to 0L,
                    "p2DealtDmg" to 0L,
                    "p1X" to 0.25,
                    "p1Y" to 0.0,
                    "p1Facing" to 1L,
                    "p1Action" to "idle",
                    "p1Blocking" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun finishMatch(
        roomCode: String,
        winnerSide: String,
        p1RoundsWon: Long? = null,
        p2RoundsWon: Long? = null
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        val updates = mutableMapOf<String, Any>(
            "status" to "finished",
            "winnerSide" to winnerSide,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (p1RoundsWon != null) updates["p1RoundsWon"] = p1RoundsWon
        if (p2RoundsWon != null) updates["p2RoundsWon"] = p2RoundsWon
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMatchRoom(roomCode: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(6) {
            repeat(6) {
                append(chars[Random.nextInt(chars.length)])
            }
        }
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_ROOMS = "match_rooms"
        private val ROOM_CODE_REGEX = Regex("^[A-Z0-9]{6}$")
    }
}
