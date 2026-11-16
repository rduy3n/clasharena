package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.GameRepository
import com.example.data.firebase.MatchRoom
import com.example.game.data.BotDifficulty
import com.example.game.data.FighterRoster
import com.example.game.data.GameMode
import com.example.game.data.TrainingOpponentMode
import com.example.game.engine.ArenaWorldState
import com.example.game.engine.CombatAction
import com.example.game.engine.CombatEngine
import com.example.game.engine.MatchPhase
import com.example.game.engine.PlayerInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BattleViewModel(
    private val repository: GameRepository,
    private val currentUserId: String
) : ViewModel() {

    private val engine = CombatEngine()

    private val _world = MutableStateFlow<ArenaWorldState?>(null)
    val world: StateFlow<ArenaWorldState?> = _world.asStateFlow()

    private val _timeMs = MutableStateFlow(0L)
    val timeMs: StateFlow<Long> = _timeMs.asStateFlow()

    private val _onlineRoom = MutableStateFlow<MatchRoom?>(null)
    val onlineRoom: StateFlow<MatchRoom?> = _onlineRoom.asStateFlow()

    private val _localSide = MutableStateFlow(1)
    val localSide: StateFlow<Int> = _localSide.asStateFlow()

    private val _onlineError = MutableStateFlow<String?>(null)
    val onlineError: StateFlow<String?> = _onlineError.asStateFlow()

    private var moveLeftHeld = false
    private var moveRightHeld = false
    private var blockHeld = false
    private var jumpQueued = false
    private var classicQueued = false
    private var baseQueued = false
    private var ultiQueued = false

    private var botDifficulty: BotDifficulty = BotDifficulty.NORMAL
    private var loopJob: Job? = null
    private var roomObserveJob: Job? = null
    private var activeRoomCode: String? = null
    private var lastSyncedRound = 1L
    private var lastSyncedActionSeq = -1L
    private var lastSyncTimeMs = 0L
    private var resultRecorded = false
    private var roundTransitionSent = false

    fun startLocalMatch(
        mode: GameMode,
        p1FighterId: String,
        p2FighterId: String,
        stageId: String,
        difficulty: BotDifficulty = BotDifficulty.NORMAL,
        trainingOpponentMode: TrainingOpponentMode = TrainingOpponentMode.STATIONARY
    ) {
        stopAll()
        botDifficulty = difficulty
        resultRecorded = false
        _localSide.value = 1

        val initial = engine.createInitialState(
            mode = mode,
            stage = FighterRoster.getStage(stageId),
            p1Def = FighterRoster.getFighter(p1FighterId),
            p2Def = FighterRoster.getFighter(p2FighterId),
            botDifficulty = difficulty,
            trainingOpponentMode = trainingOpponentMode
        )
        _world.value = initial

        loopJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (isActive) {
                delay(16L)
                val now = System.currentTimeMillis()
                val dt = (now - lastTick).coerceIn(8L, 40L)
                lastTick = now
                _timeMs.value = now

                val input = consumeInput()
                val current = _world.value ?: continue
                val updated = engine.step(current, dt, input, botDifficulty)
                _world.value = updated

                if (current.mode == GameMode.VS_BOT && updated.phase == MatchPhase.MATCH_OVER && !resultRecorded) {
                    resultRecorded = true
                    launch {
                        repository.recordMatchResult(won = updated.winnerSide == 1)
                    }
                }
            }
        }
    }

    fun startOnlineMatch(roomCode: String) {
        stopAll()
        activeRoomCode = roomCode
        resultRecorded = false
        roundTransitionSent = false
        lastSyncedActionSeq = -1L

        roomObserveJob = viewModelScope.launch {
            repository.observeMatchRoom(roomCode)
                .catch { error ->
                    if (error is CancellationException && error.cause == null) throw error
                    Log.w("BattleVM", "Online room observe error", error)
                    _onlineError.value = error.message ?: "Connexion perdue."
                }
                .collect { room ->
                    if (room == null) {
                        _onlineError.value = "Le salon a été fermé."
                        return@collect
                    }
                    _onlineRoom.value = room
                    val side = if (room.hostId == currentUserId) 1 else 2
                    _localSide.value = side

                    if (_world.value == null) {
                        val initWorld = engine.createInitialState(
                            mode = GameMode.ONLINE,
                            stage = FighterRoster.getStage(room.stageId),
                            p1Def = FighterRoster.getFighter(room.hostFighterId),
                            p2Def = FighterRoster.getFighter(room.guestFighterId)
                        ).copy(
                            phase = MatchPhase.FIGHTING,
                            phaseTimerMs = 0L,
                            bannerText = ""
                        )
                        _world.value = initWorld
                        lastSyncedRound = room.roundNumber
                        startOnlineLoop(roomCode, side)
                    } else {
                        applyRemoteRoomState(room, side)
                    }
                }
        }
    }

    private fun startOnlineLoop(roomCode: String, side: Int) {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (isActive) {
                delay(16L)
                val now = System.currentTimeMillis()
                val dt = (now - lastTick).coerceIn(8L, 40L)
                lastTick = now
                _timeMs.value = now

                val current = _world.value ?: continue
                if (current.phase != MatchPhase.FIGHTING) continue

                val input = consumeInput()
                val updated = engine.stepOnlineLocal(current, dt, side, input)
                _world.value = updated

                val localFighter = if (side == 1) updated.p1 else updated.p2
                val actionChanged = localFighter.actionSeq != lastSyncedActionSeq
                val periodicDue = (now - lastSyncTimeMs) >= 85L

                if (actionChanged || periodicDue) {
                    lastSyncedActionSeq = localFighter.actionSeq
                    lastSyncTimeMs = now
                    launch {
                        repository.updateCombatState(
                            roomCode = roomCode,
                            isHost = (side == 1),
                            x = localFighter.x.toDouble(),
                            y = localFighter.y.toDouble(),
                            facing = localFighter.facing,
                            action = localFighter.action.wireName,
                            actionSeq = localFighter.actionSeq,
                            blocking = localFighter.isBlocking,
                            ulti = localFighter.ultiMeter,
                            dealtDmg = localFighter.dealtDamageTotal
                        )
                    }
                }

                // Host checks round / match victory deterministically
                if (side == 1 && !roundTransitionSent && (updated.p1.hp <= 0 || updated.p2.hp <= 0)) {
                    roundTransitionSent = true
                    val roundWinner = if (updated.p2.hp <= 0) 1 else 2
                    val nextP1Wins = updated.p1RoundsWon + if (roundWinner == 1) 1 else 0
                    val nextP2Wins = updated.p2RoundsWon + if (roundWinner == 2) 1 else 0
                    if (nextP1Wins >= 2 || nextP2Wins >= 2) {
                        val winnerSideStr = if (nextP1Wins >= 2) "p1" else "p2"
                        launch {
                            repository.finishMatch(
                                roomCode = roomCode,
                                winnerSide = winnerSideStr,
                                p1RoundsWon = nextP1Wins.toLong(),
                                p2RoundsWon = nextP2Wins.toLong()
                            )
                        }
                    } else {
                        launch {
                            delay(900L)
                            repository.advanceRound(
                                roomCode = roomCode,
                                nextRound = (updated.roundNumber + 1).toLong(),
                                p1RoundsWon = nextP1Wins.toLong(),
                                p2RoundsWon = nextP2Wins.toLong()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun applyRemoteRoomState(room: MatchRoom, localSide: Int) {
        val current = _world.value ?: return

        // Round change reset
        if (room.roundNumber != lastSyncedRound) {
            lastSyncedRound = room.roundNumber
            roundTransitionSent = false
            _world.value = current.copy(
                phase = MatchPhase.FIGHTING,
                roundNumber = room.roundNumber.toInt(),
                p1RoundsWon = room.p1RoundsWon.toInt(),
                p2RoundsWon = room.p2RoundsWon.toInt(),
                bannerText = "ROUND ${room.roundNumber}",
                projectiles = emptyList(),
                p1 = current.p1.copy(
                    x = 0.25f, y = 0f, vy = 0f, facing = 1,
                    hp = current.p1.def.stats.maxHp,
                    ultiMeter = 0, dealtDamageTotal = 0, action = CombatAction.IDLE
                ),
                p2 = current.p2.copy(
                    x = 0.75f, y = 0f, vy = 0f, facing = -1,
                    hp = current.p2.def.stats.maxHp,
                    ultiMeter = 0, dealtDamageTotal = 0, action = CombatAction.IDLE
                )
            )
            return
        }

        if (room.status == "finished") {
            val winnerSideInt = when (room.winnerSide) {
                "p1" -> 1
                "p2" -> 2
                else -> 0
            }
            if (!resultRecorded && winnerSideInt != 0) {
                resultRecorded = true
                viewModelScope.launch {
                    repository.recordMatchResult(won = winnerSideInt == localSide)
                }
            }
            _world.value = current.copy(
                phase = MatchPhase.MATCH_OVER,
                p1RoundsWon = room.p1RoundsWon.toInt(),
                p2RoundsWon = room.p2RoundsWon.toInt(),
                winnerSide = winnerSideInt,
                bannerText = "VICTOIRE !"
            )
            return
        }

        val p1Hp = (current.p1.def.stats.maxHp - room.p2DealtDmg.toInt()).coerceAtLeast(0)
        val p2Hp = (current.p2.def.stats.maxHp - room.p1DealtDmg.toInt()).coerceAtLeast(0)

        val updatedP1 = if (localSide == 1) {
            current.p1.copy(hp = p1Hp)
        } else {
            current.p1.copy(
                x = room.p1X.toFloat(),
                y = room.p1Y.toFloat(),
                facing = room.p1Facing.toInt(),
                action = CombatAction.fromWire(room.p1Action),
                actionSeq = room.p1ActionSeq,
                isBlocking = room.p1Blocking,
                ultiMeter = room.p1Ulti.toInt(),
                hp = p1Hp
            )
        }

        val updatedP2 = if (localSide == 2) {
            current.p2.copy(hp = p2Hp)
        } else {
            current.p2.copy(
                x = room.p2X.toFloat(),
                y = room.p2Y.toFloat(),
                facing = room.p2Facing.toInt(),
                action = CombatAction.fromWire(room.p2Action),
                actionSeq = room.p2ActionSeq,
                isBlocking = room.p2Blocking,
                ultiMeter = room.p2Ulti.toInt(),
                hp = p2Hp
            )
        }

        _world.value = current.copy(
            p1 = updatedP1,
            p2 = updatedP2,
            p1RoundsWon = room.p1RoundsWon.toInt(),
            p2RoundsWon = room.p2RoundsWon.toInt(),
            roundNumber = room.roundNumber.toInt()
        )
    }

    // --- Input bindings ---
    fun setMoveLeft(held: Boolean) { moveLeftHeld = held }
    fun setMoveRight(held: Boolean) { moveRightHeld = held }
    fun setBlock(held: Boolean) { blockHeld = held }
    fun triggerJump() { jumpQueued = true }
    fun triggerClassic() { classicQueued = true }
    fun triggerBaseAbility() { baseQueued = true }
    fun triggerUlti() { ultiQueued = true }

    private fun consumeInput(): PlayerInput {
        val dir = (if (moveRightHeld) 1f else 0f) - (if (moveLeftHeld) 1f else 0f)
        val input = PlayerInput(
            moveDir = dir,
            jumpPressed = jumpQueued,
            blockHeld = blockHeld,
            classicPressed = classicQueued,
            baseAbilityPressed = baseQueued,
            ultiPressed = ultiQueued
        )
        jumpQueued = false
        classicQueued = false
        baseQueued = false
        ultiQueued = false
        return input
    }

    // --- Training Mode Controls ---
    fun resetTrainingHealth() {
        val current = _world.value ?: return
        _world.value = engine.resetTrainingHealth(current)
    }

    fun setTrainingOpponentMode(mode: TrainingOpponentMode) {
        val current = _world.value ?: return
        _world.value = current.copy(
            training = current.training.copy(opponentMode = mode)
        )
    }

    fun setTrainingBotDifficulty(difficulty: BotDifficulty) {
        val current = _world.value ?: return
        botDifficulty = difficulty
        _world.value = current.copy(
            training = current.training.copy(botDifficulty = difficulty)
        )
    }

    fun toggleTrainingAutoRegen() {
        val current = _world.value ?: return
        _world.value = current.copy(
            training = current.training.copy(autoRegenHp = !current.training.autoRegenHp)
        )
    }

    fun toggleTrainingInfiniteUlti() {
        val current = _world.value ?: return
        val nextInf = !current.training.infiniteUlti
        _world.value = current.copy(
            p1 = if (nextInf) current.p1.copy(ultiMeter = 100) else current.p1,
            training = current.training.copy(infiniteUlti = nextInf)
        )
    }

    fun selectTrainingChallenge(index: Int) {
        val current = _world.value ?: return
        val challenges = current.p1.def.comboChallenges
        if (challenges.isEmpty()) return
        val safeIdx = index.coerceIn(0, challenges.lastIndex)
        _world.value = current.copy(
            training = current.training.copy(
                selectedChallengeIndex = safeIdx,
                activeChallenge = challenges[safeIdx],
                currentStepIndex = 0
            )
        )
    }

    fun switchTrainingFighters(p1Id: String, p2Id: String) {
        val current = _world.value ?: return
        val p1Def = FighterRoster.getFighter(p1Id)
        val p2Def = FighterRoster.getFighter(p2Id)
        val next = engine.createInitialState(
            mode = GameMode.TRAINING,
            stage = current.stage,
            p1Def = p1Def,
            p2Def = p2Def,
            botDifficulty = current.training.botDifficulty,
            trainingOpponentMode = current.training.opponentMode
        )
        _world.value = next.copy(
            training = next.training.copy(
                autoRegenHp = current.training.autoRegenHp,
                infiniteUlti = current.training.infiniteUlti
            )
        )
    }

    fun stopAll() {
        loopJob?.cancel()
        loopJob = null
        roomObserveJob?.cancel()
        roomObserveJob = null
        activeRoomCode = null
        moveLeftHeld = false
        moveRightHeld = false
        blockHeld = false
        jumpQueued = false
        classicQueued = false
        baseQueued = false
        ultiQueued = false
    }

    override fun onCleared() {
        super.onCleared()
        stopAll()
    }
}
