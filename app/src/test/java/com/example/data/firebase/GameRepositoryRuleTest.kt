package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun profileLifecycle_authenticatedOwnerSucceeds_crossUserFails() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = GameRepository(firestore, auth)

        val profile = withTimeout(TIMEOUT_MS) { repo.ensureProfile("Alice").getOrThrow() }
        assertEquals(aliceUid, profile.userId)

        val updateRes = withTimeout(TIMEOUT_MS) { repo.updateFavoriteFighter("kaelith") }
        assertTrue(updateRes.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProfile(aliceUid).first { it?.favoriteFighterId == "kaelith" }
        }
        assertEquals("kaelith", observed?.favoriteFighterId)

        // Bob cannot read Alice's profile
        signInTestUser(BOB_EMAIL)
        val bobRepo = GameRepository(firestore, auth)
        val err = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeProfile(aliceUid).first() }
        }.exceptionOrNull()
        val fsErr = checkNotNull(err?.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $err" }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
    }

    @Test
    fun matchRoomLifecycle_hostCreate_guestJoin_combatSync_and_finish() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GameRepository(firestore, auth)
        val room = withTimeout(TIMEOUT_MS) {
            aliceRepo.createMatchRoom("Alice", "ryuji", "cyber_street").getOrThrow()
        }
        assertEquals(aliceUid, room.hostId)
        assertEquals("waiting", room.status)

        // Bob joins by room code
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = GameRepository(firestore, auth)
        val joined = withTimeout(TIMEOUT_MS) {
            bobRepo.joinMatchRoom(room.roomCode, "Bob").getOrThrow()
        }
        assertEquals(bobUid, joined.guestId)
        assertEquals("selecting", joined.status)

        // Both select fighters and start
        assertTrue(withTimeout(TIMEOUT_MS) {
            bobRepo.updateLobbySelection(room.roomCode, isHost = false, fighterId = "sora", ready = true).isSuccess
        })
        signInTestUser(ALICE_EMAIL)
        assertTrue(withTimeout(TIMEOUT_MS) {
            aliceRepo.updateLobbySelection(room.roomCode, isHost = true, fighterId = "titan", ready = true, startFight = true).isSuccess
        })

        // Combat sync & finish
        assertTrue(withTimeout(TIMEOUT_MS) {
            aliceRepo.updateCombatState(room.roomCode, isHost = true, x = 0.4, y = 0.0, facing = 1, action = "classic", actionSeq = 1L, blocking = false, ulti = 20, dealtDmg = 90).isSuccess
        })
        assertTrue(withTimeout(TIMEOUT_MS) {
            aliceRepo.finishMatch(room.roomCode, winnerSide = "p1", p1RoundsWon = 2L, p2RoundsWon = 0L).isSuccess
        })
    }

    @Test
    fun observeMyRooms_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = GameRepository(firestore, auth)
        val err = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) { repo.observeMyRooms("some_uid").first() }
        }.exceptionOrNull()
        val fsErr = checkNotNull(err?.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $err" }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_clash@test.com"
        const val BOB_EMAIL = "bob_clash@test.com"
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
