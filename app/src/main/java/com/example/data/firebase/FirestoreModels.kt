package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Combattant",
    val favoriteFighterId: String = "ryuji",
    val wins: Long = 0L,
    val losses: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MatchRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val guestId: String = "",
    val guestName: String = "",
    val participantIds: List<String> = emptyList(),
    val status: String = "waiting",
    val stageId: String = "cyber_street",
    val hostFighterId: String = "ryuji",
    val guestFighterId: String = "kaelith",
    val hostReady: Boolean = false,
    val guestReady: Boolean = false,
    val roundNumber: Long = 1L,
    val p1RoundsWon: Long = 0L,
    val p2RoundsWon: Long = 0L,
    val winnerSide: String = "none",
    val p1X: Double = 0.25,
    val p1Y: Double = 0.0,
    val p1Facing: Long = 1L,
    val p1Action: String = "idle",
    val p1ActionSeq: Long = 0L,
    val p1Blocking: Boolean = false,
    val p1Ulti: Long = 0L,
    val p1DealtDmg: Long = 0L,
    val p2X: Double = 0.75,
    val p2Y: Double = 0.0,
    val p2Facing: Long = -1L,
    val p2Action: String = "idle",
    val p2ActionSeq: Long = 0L,
    val p2Blocking: Boolean = false,
    val p2Ulti: Long = 0L,
    val p2DealtDmg: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
