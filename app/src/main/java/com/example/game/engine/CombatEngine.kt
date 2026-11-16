package com.example.game.engine

import androidx.compose.ui.graphics.Color
import com.example.game.data.ArenaStage
import com.example.game.data.BotDifficulty
import com.example.game.data.ComboInputStep
import com.example.game.data.FighterDefinition
import com.example.game.data.GameMode
import com.example.game.data.MoveEffectType
import com.example.game.data.MoveSlot
import com.example.game.data.MoveSpec
import com.example.game.data.TrainingOpponentMode
import kotlin.math.abs
import kotlin.random.Random

class CombatEngine(
    private var nextEntityId: Long = 1L
) {
    private var botDecisionAccumMs = 0L
    private var currentBotInput = PlayerInput()
    private var trainingIdleAccumMs = 0L

    fun createInitialState(
        mode: GameMode,
        stage: ArenaStage,
        p1Def: FighterDefinition,
        p2Def: FighterDefinition,
        botDifficulty: BotDifficulty = BotDifficulty.NORMAL,
        trainingOpponentMode: TrainingOpponentMode = TrainingOpponentMode.STATIONARY
    ): ArenaWorldState {
        val isTraining = mode == GameMode.TRAINING
        val p1 = FighterRuntime(
            side = 1,
            def = p1Def,
            x = 0.25f,
            facing = 1,
            hp = p1Def.stats.maxHp,
            ultiMeter = if (isTraining) 100 else 0
        )
        val p2 = FighterRuntime(
            side = 2,
            def = p2Def,
            x = 0.75f,
            facing = -1,
            hp = p2Def.stats.maxHp,
            ultiMeter = 0
        )
        val firstChallenge = p1Def.comboChallenges.firstOrNull()
        return ArenaWorldState(
            mode = mode,
            stage = stage,
            phase = if (isTraining) MatchPhase.FIGHTING else MatchPhase.ROUND_INTRO,
            phaseTimerMs = if (isTraining) 0L else 1500L,
            roundNumber = 1,
            roundTimeRemainingSec = 99,
            bannerText = if (isTraining) "" else "ROUND 1",
            p1 = p1,
            p2 = p2,
            training = TrainingHudState(
                opponentMode = trainingOpponentMode,
                botDifficulty = botDifficulty,
                autoRegenHp = true,
                infiniteUlti = true,
                selectedChallengeIndex = 0,
                activeChallenge = firstChallenge
            )
        )
    }

    fun resetTrainingHealth(state: ArenaWorldState): ArenaWorldState {
        return state.copy(
            p1 = state.p1.copy(
                hp = state.p1.def.stats.maxHp,
                ultiMeter = if (state.training.infiniteUlti) 100 else state.p1.ultiMeter,
                hitStunMs = 0L,
                comboCount = 0,
                comboDamage = 0
            ),
            p2 = state.p2.copy(
                hp = state.p2.def.stats.maxHp,
                hitStunMs = 0L,
                comboCount = 0,
                comboDamage = 0,
                x = 0.72f,
                y = 0f,
                vy = 0f,
                action = CombatAction.IDLE
            ),
            projectiles = emptyList(),
            popups = state.popups + FloatingPopup(
                id = nextId(),
                x = 0.5f,
                y = 0.35f,
                text = "PV RÉINITIALISÉS",
                color = Color(0xFF00E676)
            )
        )
    }

    fun step(
        state: ArenaWorldState,
        dtMs: Long,
        p1Input: PlayerInput,
        botDifficulty: BotDifficulty = BotDifficulty.NORMAL
    ): ArenaWorldState {
        val clampedDt = dtMs.coerceIn(1L, 50L)

        // Age visual effects regardless of phase
        var next = ageEffects(state, clampedDt)

        if (next.mode == GameMode.TRAINING) {
            return stepTraining(next, clampedDt, p1Input)
        }

        return when (next.phase) {
            MatchPhase.ROUND_INTRO -> {
                val rem = next.phaseTimerMs - clampedDt
                when {
                    rem > 600L -> next.copy(phaseTimerMs = rem, bannerText = "ROUND ${next.roundNumber}")
                    rem > 0L -> next.copy(phaseTimerMs = rem, bannerText = "COMBAT !")
                    else -> next.copy(phase = MatchPhase.FIGHTING, phaseTimerMs = 0L, bannerText = "")
                }
            }

            MatchPhase.FIGHTING -> {
                val p2Input = computeBotInput(next, clampedDt, botDifficulty)
                next = simulateCombatTick(next, clampedDt, p1Input, p2Input)

                // Round timer
                val newClockAccum = next.roundClockAccumMs + clampedDt
                var sec = next.roundTimeRemainingSec
                var clockRem = newClockAccum
                if (clockRem >= 1000L) {
                    clockRem -= 1000L
                    sec = (sec - 1).coerceAtLeast(0)
                }
                next = next.copy(roundTimeRemainingSec = sec, roundClockAccumMs = clockRem)

                // Check KO or Timeout
                val p1Dead = next.p1.hp <= 0
                val p2Dead = next.p2.hp <= 0
                if (p1Dead || p2Dead || sec <= 0) {
                    val roundWinner = when {
                        p2Dead && !p1Dead -> 1
                        p1Dead && !p2Dead -> 2
                        next.p1.hp.toFloat() / next.p1.def.stats.maxHp >= next.p2.hp.toFloat() / next.p2.def.stats.maxHp -> 1
                        else -> 2
                    }
                    val p1Wins = next.p1RoundsWon + if (roundWinner == 1) 1 else 0
                    val p2Wins = next.p2RoundsWon + if (roundWinner == 2) 1 else 0
                    val matchFinished = p1Wins >= 2 || p2Wins >= 2
                    val banner = if (p1Dead || p2Dead) "K.O. !" else "TEMPS ÉCOULÉ !"
                    next.copy(
                        phase = if (matchFinished) MatchPhase.MATCH_OVER else MatchPhase.ROUND_END,
                        phaseTimerMs = if (matchFinished) 0L else 2200L,
                        p1RoundsWon = p1Wins,
                        p2RoundsWon = p2Wins,
                        winnerSide = if (matchFinished) roundWinner else 0,
                        bannerText = banner,
                        p1 = if (p1Dead) next.p1.copy(action = CombatAction.KO) else next.p1,
                        p2 = if (p2Dead) next.p2.copy(action = CombatAction.KO) else next.p2
                    )
                } else {
                    next
                }
            }

            MatchPhase.ROUND_END -> {
                val rem = next.phaseTimerMs - clampedDt
                if (rem > 0L) {
                    next.copy(phaseTimerMs = rem)
                } else {
                    val nextRound = next.roundNumber + 1
                    next.copy(
                        phase = MatchPhase.ROUND_INTRO,
                        phaseTimerMs = 1500L,
                        roundNumber = nextRound,
                        roundTimeRemainingSec = 99,
                        roundClockAccumMs = 0L,
                        bannerText = "ROUND $nextRound",
                        projectiles = emptyList(),
                        p1 = next.p1.copy(
                            x = 0.25f,
                            y = 0f,
                            vy = 0f,
                            facing = 1,
                            hp = next.p1.def.stats.maxHp,
                            action = CombatAction.IDLE,
                            hitStunMs = 0L,
                            classicCooldownMs = 0L,
                            baseCooldownMs = 0L,
                            comboCount = 0,
                            comboDamage = 0,
                            dealtDamageTotal = 0
                        ),
                        p2 = next.p2.copy(
                            x = 0.75f,
                            y = 0f,
                            vy = 0f,
                            facing = -1,
                            hp = next.p2.def.stats.maxHp,
                            action = CombatAction.IDLE,
                            hitStunMs = 0L,
                            classicCooldownMs = 0L,
                            baseCooldownMs = 0L,
                            comboCount = 0,
                            comboDamage = 0,
                            dealtDamageTotal = 0
                        )
                    )
                }
            }

            MatchPhase.MATCH_OVER -> next
        }
    }

    private fun stepTraining(
        state: ArenaWorldState,
        dtMs: Long,
        p1Input: PlayerInput
    ): ArenaWorldState {
        val p2Input = when (state.training.opponentMode) {
            TrainingOpponentMode.STATIONARY -> PlayerInput()
            TrainingOpponentMode.AUTO_BLOCK -> PlayerInput(blockHeld = true)
            TrainingOpponentMode.AI_BOT -> computeBotInput(state, dtMs, state.training.botDifficulty)
        }
        var next = simulateCombatTick(state, dtMs, p1Input, p2Input)

        // Keep fighters alive in training mode (floor at 1 HP unless regenerating)
        if (next.p1.hp <= 0) {
            next = next.copy(p1 = next.p1.copy(hp = next.p1.def.stats.maxHp))
        }
        if (next.p2.hp <= 0) {
            next = next.copy(p2 = next.p2.copy(hp = 1))
        }

        // Infinite ulti option in training
        if (next.training.infiniteUlti && next.p1.ultiMeter < 100 && next.p1.action != CombatAction.ULTI) {
            next = next.copy(p1 = next.p1.copy(ultiMeter = 100))
        }

        // Auto-regen P2 HP when not in hitstun for 1.5s
        if (next.training.autoRegenHp && next.p2.hitStunMs <= 0L && next.p1.comboExpireMs <= 0L) {
            trainingIdleAccumMs += dtMs
            if (trainingIdleAccumMs >= 1400L && next.p2.hp < next.p2.def.stats.maxHp) {
                next = next.copy(p2 = next.p2.copy(hp = next.p2.def.stats.maxHp))
                trainingIdleAccumMs = 0L
            }
        } else {
            trainingIdleAccumMs = 0L
        }

        val bannerRem = (next.training.challengeSuccessBannerMs - dtMs).coerceAtLeast(0L)
        if (bannerRem != next.training.challengeSuccessBannerMs) {
            next = next.copy(training = next.training.copy(challengeSuccessBannerMs = bannerRem))
        }

        return next
    }

    /**
     * Simulation tick pour un combat local (ou pour simuler le joueur local en ligne).
     */
    fun simulateCombatTick(
        state: ArenaWorldState,
        dtMs: Long,
        p1Input: PlayerInput,
        p2Input: PlayerInput
    ): ArenaWorldState {
        var p1 = updateFighterKinematics(state.p1, p1Input, state.p2.x, dtMs)
        var p2 = updateFighterKinematics(state.p2, p2Input, state.p1.x, dtMs)

        // Start attacks from inputs
        val spawnEffects = mutableListOf<SlashEffect>()
        val spawnProjectiles = mutableListOf<Projectile>()

        p1 = tryTriggerAttack(p1, p1Input, spawnEffects, spawnProjectiles)
        p2 = tryTriggerAttack(p2, p2Input, spawnEffects, spawnProjectiles)

        var flashMs = state.ultiFlashMs
        var flashColor = state.ultiFlashColor
        if (p1.action == CombatAction.ULTI && p1.actionTimerMs <= dtMs) {
            flashMs = 340L
            flashColor = p1.def.primaryColor
        }
        if (p2.action == CombatAction.ULTI && p2.actionTimerMs <= dtMs) {
            flashMs = 340L
            flashColor = p2.def.primaryColor
        }

        // Resolve melee/dash/ulti hitboxes
        var nextState = state.copy(
            p1 = p1,
            p2 = p2,
            ultiFlashMs = flashMs,
            ultiFlashColor = flashColor,
            slashEffects = state.slashEffects + spawnEffects,
            projectiles = state.projectiles + spawnProjectiles
        )

        nextState = resolveAttackHit(nextState, attackerSide = 1)
        nextState = resolveAttackHit(nextState, attackerSide = 2)
        nextState = updateProjectiles(nextState, dtMs)
        nextState = preventOverlap(nextState)

        return nextState
    }

    /**
     * Mode En Ligne : met à jour le joueur local avec les inputs, et applique l'état distant sur l'adversaire.
     */
    fun stepOnlineLocal(
        state: ArenaWorldState,
        dtMs: Long,
        localSide: Int,
        localInput: PlayerInput
    ): ArenaWorldState {
        val clampedDt = dtMs.coerceIn(1L, 50L)
        var next = ageEffects(state, clampedDt)
        if (next.phase != MatchPhase.FIGHTING) return next

        val emptyInput = PlayerInput()
        val p1In = if (localSide == 1) localInput else emptyInput
        val p2In = if (localSide == 2) localInput else emptyInput

        val localFighter = if (localSide == 1) next.p1 else next.p2
        val remoteFighter = if (localSide == 1) next.p2 else next.p1

        val updatedLocal = updateFighterKinematics(localFighter, if (localSide == 1) p1In else p2In, remoteFighter.x, clampedDt)
        val effects = mutableListOf<SlashEffect>()
        val projs = mutableListOf<Projectile>()
        val attackedLocal = tryTriggerAttack(updatedLocal, if (localSide == 1) p1In else p2In, effects, projs)

        var flashMs = next.ultiFlashMs
        var flashColor = next.ultiFlashColor
        if (attackedLocal.action == CombatAction.ULTI && attackedLocal.actionTimerMs <= clampedDt) {
            flashMs = 320L
            flashColor = attackedLocal.def.primaryColor
        }

        next = next.copy(
            p1 = if (localSide == 1) attackedLocal else next.p1,
            p2 = if (localSide == 2) attackedLocal else next.p2,
            ultiFlashMs = flashMs,
            ultiFlashColor = flashColor,
            slashEffects = next.slashEffects + effects,
            projectiles = next.projectiles + projs
        )

        next = resolveAttackHit(next, attackerSide = localSide)
        next = updateProjectiles(next, clampedDt)
        return next
    }

    private fun updateFighterKinematics(
        f: FighterRuntime,
        input: PlayerInput,
        opponentX: Float,
        dtMs: Long
    ): FighterRuntime {
        val dtSec = dtMs / 1000f
        val newClassicCd = (f.classicCooldownMs - dtMs).coerceAtLeast(0L)
        val newBaseCd = (f.baseCooldownMs - dtMs).coerceAtLeast(0L)
        val newHitStun = (f.hitStunMs - dtMs).coerceAtLeast(0L)
        val newComboExp = (f.comboExpireMs - dtMs).coerceAtLeast(0L)

        // Gravity & vertical position
        var y = f.y + f.vy * dtSec
        var vy = f.vy - GRAVITY * dtSec
        if (y <= 0f) {
            y = 0f
            vy = 0f
        }

        // Hitstun overrides movement/actions
        if (newHitStun > 0L) {
            return f.copy(
                y = y,
                vy = vy,
                classicCooldownMs = newClassicCd,
                baseCooldownMs = newBaseCd,
                hitStunMs = newHitStun,
                comboExpireMs = newComboExp,
                comboCount = if (newComboExp == 0L) 0 else f.comboCount,
                comboDamage = if (newComboExp == 0L) 0 else f.comboDamage,
                isBlocking = false,
                action = CombatAction.HIT_STUN
            )
        }

        // Ongoing attack animation
        val isAttacking = f.action in setOf(CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI)
        if (isAttacking) {
            val nextTimer = f.actionTimerMs + dtMs
            var nextX = f.x
            // Dash movement during dash attacks
            val moveSpec = when (f.action) {
                CombatAction.CLASSIC -> f.def.classicMove
                CombatAction.BASE_ABILITY -> f.def.baseAbility
                CombatAction.ULTI -> f.def.ultimateMove
                else -> null
            }
            if (moveSpec != null && (moveSpec.effectType == MoveEffectType.DASH_STRIKE ||
                        moveSpec.effectType == MoveEffectType.ULTI_RUSH ||
                        moveSpec.effectType == MoveEffectType.ULTI_SLASH)
            ) {
                nextX = (nextX + f.facing * 0.82f * dtSec).coerceIn(0.08f, 0.92f)
            }

            if (nextTimer < f.actionDurationMs) {
                return f.copy(
                    x = nextX,
                    y = y,
                    vy = vy,
                    classicCooldownMs = newClassicCd,
                    baseCooldownMs = newBaseCd,
                    hitStunMs = 0L,
                    comboExpireMs = newComboExp,
                    comboCount = if (newComboExp == 0L) 0 else f.comboCount,
                    comboDamage = if (newComboExp == 0L) 0 else f.comboDamage,
                    actionTimerMs = nextTimer,
                    isBlocking = false
                )
            }
        }

        // Free movement / jump / block
        val facing = if (opponentX >= f.x) 1 else -1
        val blocking = input.blockHeld && y == 0f
        val moveSpeed = if (blocking) f.def.stats.moveSpeed * 0.25f else f.def.stats.moveSpeed
        val nextX = (f.x + input.moveDir * moveSpeed * dtSec).coerceIn(0.08f, 0.92f)

        if (input.jumpPressed && y == 0f && !blocking) {
            vy = f.def.stats.jumpPower
            y = 0.01f
        }

        val nextAction = when {
            blocking -> CombatAction.BLOCK
            y > 0.01f -> CombatAction.JUMP
            abs(input.moveDir) > 0.1f -> CombatAction.WALK
            else -> CombatAction.IDLE
        }

        return f.copy(
            x = nextX,
            y = y,
            vy = vy,
            facing = facing,
            classicCooldownMs = newClassicCd,
            baseCooldownMs = newBaseCd,
            hitStunMs = 0L,
            comboExpireMs = newComboExp,
            comboCount = if (newComboExp == 0L) 0 else f.comboCount,
            comboDamage = if (newComboExp == 0L) 0 else f.comboDamage,
            isBlocking = blocking,
            action = nextAction,
            actionTimerMs = 0L,
            actionDurationMs = 0L
        )
    }

    private fun tryTriggerAttack(
        f: FighterRuntime,
        input: PlayerInput,
        effects: MutableList<SlashEffect>,
        projectiles: MutableList<Projectile>
    ): FighterRuntime {
        if (f.hitStunMs > 0L || f.isBlocking) return f
        val alreadyAttacking = f.action in setOf(CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI)
        if (alreadyAttacking) return f

        val chosenMove: MoveSpec? = when {
            input.ultiPressed && f.ultiMeter >= f.def.ultimateMove.ultiCost -> f.def.ultimateMove
            input.baseAbilityPressed && f.baseCooldownMs <= 0L -> f.def.baseAbility
            input.classicPressed && f.classicCooldownMs <= 0L -> f.def.classicMove
            else -> null
        }

        if (chosenMove == null) return f

        val newAction = when (chosenMove.slot) {
            MoveSlot.CLASSIC -> CombatAction.CLASSIC
            MoveSlot.BASE_ABILITY -> CombatAction.BASE_ABILITY
            MoveSlot.ULTI -> CombatAction.ULTI
        }

        val nextUlti = if (chosenMove.slot == MoveSlot.ULTI) 0 else f.ultiMeter
        val nextClassicCd = if (chosenMove.slot == MoveSlot.CLASSIC) chosenMove.cooldownMs else f.classicCooldownMs
        val nextBaseCd = if (chosenMove.slot == MoveSlot.BASE_ABILITY) chosenMove.cooldownMs else f.baseCooldownMs

        if (chosenMove.effectType == MoveEffectType.PROJECTILE) {
            projectiles += Projectile(
                id = nextId(),
                ownerSide = f.side,
                x = (f.x + f.facing * 0.06f).coerceIn(0.05f, 0.95f),
                y = f.y + 0.08f,
                vx = f.facing * 0.82f,
                damage = chosenMove.damage,
                knockback = chosenMove.knockback,
                ultiGain = chosenMove.ultiGainOnHit,
                color = f.def.primaryColor,
                accentColor = f.def.accentColor,
                isTornado = f.def.id == "sora"
            )
        } else {
            effects += SlashEffect(
                id = nextId(),
                x = f.x,
                y = f.y,
                facing = f.facing,
                color = if (chosenMove.slot == MoveSlot.ULTI) f.def.accentColor else f.def.primaryColor,
                effectType = chosenMove.effectType,
                range = chosenMove.range,
                maxAgeMs = chosenMove.activeMs + 120L
            )
        }

        return f.copy(
            action = newAction,
            actionTimerMs = 0L,
            actionDurationMs = chosenMove.startupMs + chosenMove.activeMs,
            actionSeq = f.actionSeq + 1L,
            hasDealtHitForSeq = false,
            ultiMeter = nextUlti,
            classicCooldownMs = nextClassicCd,
            baseCooldownMs = nextBaseCd
        )
    }

    private fun resolveAttackHit(state: ArenaWorldState, attackerSide: Int): ArenaWorldState {
        val attacker = if (attackerSide == 1) state.p1 else state.p2
        val defender = if (attackerSide == 1) state.p2 else state.p1

        if (attacker.hasDealtHitForSeq) return state

        val move = when (attacker.action) {
            CombatAction.CLASSIC -> attacker.def.classicMove
            CombatAction.BASE_ABILITY -> attacker.def.baseAbility
            CombatAction.ULTI -> attacker.def.ultimateMove
            else -> null
        } ?: return state

        if (move.effectType == MoveEffectType.PROJECTILE) {
            return state
        }
        if (attacker.actionTimerMs < move.startupMs) {
            return state
        }

        // Check horizontal & vertical hit range
        val dx = (defender.x - attacker.x) * attacker.facing
        val inFront = dx >= -0.05f && dx <= move.range
        val dy = abs(defender.y - attacker.y)
        val yTolerance = if (attacker.action == CombatAction.ULTI) 0.35f else 0.22f

        if (!inFront || dy > yTolerance) return state

        val (updatedAttacker, updatedDefender, popup, spark) = applyDamage(
            attacker = attacker.copy(hasDealtHitForSeq = true),
            defender = defender,
            rawDamage = move.damage,
            knockback = move.knockback,
            ultiGain = move.ultiGainOnHit,
            isUlti = move.slot == MoveSlot.ULTI,
            hitX = (attacker.x + defender.x) * 0.5f,
            hitY = defender.y + 0.12f
        )

        var nextTraining = state.training
        if (state.mode == GameMode.TRAINING && attackerSide == 1) {
            nextTraining = updateTrainingChallenge(
                training = nextTraining,
                moveSlot = move.slot,
                wasAirborne = attacker.y > 0.03f,
                comboHits = updatedAttacker.comboCount,
                comboDmg = updatedAttacker.comboDamage
            )
        }

        return state.copy(
            p1 = if (attackerSide == 1) updatedAttacker else updatedDefender,
            p2 = if (attackerSide == 2) updatedAttacker else updatedDefender,
            sparks = state.sparks + spark,
            popups = state.popups + popup,
            training = nextTraining
        )
    }

    private fun updateProjectiles(state: ArenaWorldState, dtMs: Long): ArenaWorldState {
        val dtSec = dtMs / 1000f
        val remaining = mutableListOf<Projectile>()
        var p1 = state.p1
        var p2 = state.p2
        val newSparks = mutableListOf<HitSpark>()
        val newPopups = mutableListOf<FloatingPopup>()
        var nextTraining = state.training

        for (proj in state.projectiles) {
            val nextX = proj.x + proj.vx * dtSec
            val nextTtl = proj.ttlMs - dtMs
            if (nextX < 0f || nextX > 1f || nextTtl <= 0L) continue

            val target = if (proj.ownerSide == 1) p2 else p1
            val attacker = if (proj.ownerSide == 1) p1 else p2
            val hitX = abs(nextX - target.x) <= 0.065f
            val hitY = abs(proj.y - (target.y + 0.09f)) <= 0.13f

            if (hitX && hitY) {
                val (uAtt, uDef, popup, spark) = applyDamage(
                    attacker = attacker,
                    defender = target,
                    rawDamage = proj.damage,
                    knockback = proj.knockback,
                    ultiGain = proj.ultiGain,
                    isUlti = false,
                    hitX = nextX,
                    hitY = proj.y
                )
                if (proj.ownerSide == 1) {
                    p1 = uAtt
                    p2 = uDef
                    if (state.mode == GameMode.TRAINING) {
                        nextTraining = updateTrainingChallenge(
                            training = nextTraining,
                            moveSlot = MoveSlot.BASE_ABILITY,
                            wasAirborne = false,
                            comboHits = uAtt.comboCount,
                            comboDmg = uAtt.comboDamage
                        )
                    }
                } else {
                    p2 = uAtt
                    p1 = uDef
                }
                newSparks += spark
                newPopups += popup
            } else {
                remaining += proj.copy(x = nextX, ttlMs = nextTtl)
            }
        }

        return state.copy(
            p1 = p1,
            p2 = p2,
            projectiles = remaining,
            sparks = state.sparks + newSparks,
            popups = state.popups + newPopups,
            training = nextTraining
        )
    }

    private data class HitOutcome(
        val attacker: FighterRuntime,
        val defender: FighterRuntime,
        val popup: FloatingPopup,
        val spark: HitSpark
    )

    private fun applyDamage(
        attacker: FighterRuntime,
        defender: FighterRuntime,
        rawDamage: Int,
        knockback: Float,
        ultiGain: Int,
        isUlti: Boolean,
        hitX: Float,
        hitY: Float
    ): HitOutcome {
        val blocked = defender.isBlocking
        val defenseFactor = 1f - (defender.def.stats.defensePercent / 100f)
        val blockMultiplier = when {
            !blocked -> 1f
            isUlti -> 0.55f // L'ulti perce partiellement la garde
            else -> 0.25f
        }
        val finalDmg = (rawDamage * defenseFactor * blockMultiplier).toInt().coerceAtLeast(5)
        val nextHp = (defender.hp - finalDmg).coerceAtLeast(0)

        val pushDir = if (defender.x >= attacker.x) 1f else -1f
        val actualKnockback = if (blocked) knockback * 0.45f else knockback
        val nextDefX = (defender.x + pushDir * actualKnockback).coerceIn(0.08f, 0.92f)
        val nextHitStun = when {
            blocked && !isUlti -> 0L
            isUlti -> 380L
            else -> 240L
        }

        val nextComboCount = if (!blocked) attacker.comboCount + 1 else attacker.comboCount
        val nextComboDmg = if (!blocked) attacker.comboDamage + finalDmg else attacker.comboDamage
        val nextUltiAttacker = (attacker.ultiMeter + if (blocked) ultiGain / 2 else ultiGain).coerceIn(0, 100)
        val nextUltiDefender = (defender.ultiMeter + 10).coerceIn(0, 100)

        val popupText = when {
            blocked && isUlti -> "PERCÉE -$finalDmg"
            blocked -> "BLOQUÉ -$finalDmg"
            isUlti -> "ULTIME ! -$finalDmg"
            else -> "-$finalDmg"
        }
        val popupColor = when {
            blocked -> Color(0xFF448AFF)
            isUlti -> Color(0xFFFFC400)
            else -> Color(0xFFFF5252)
        }

        return HitOutcome(
            attacker = attacker.copy(
                ultiMeter = nextUltiAttacker,
                comboCount = nextComboCount,
                comboDamage = nextComboDmg,
                comboExpireMs = if (!blocked) 1600L else attacker.comboExpireMs,
                dealtDamageTotal = attacker.dealtDamageTotal + finalDmg
            ),
            defender = defender.copy(
                x = nextDefX,
                hp = nextHp,
                ultiMeter = nextUltiDefender,
                hitStunMs = nextHitStun,
                action = if (nextHitStun > 0L) CombatAction.HIT_STUN else defender.action
            ),
            popup = FloatingPopup(
                id = nextId(),
                x = hitX,
                y = hitY + 0.05f,
                text = popupText,
                color = popupColor,
                isCritical = isUlti
            ),
            spark = HitSpark(
                id = nextId(),
                x = hitX,
                y = hitY,
                color = if (blocked) Color(0xFF448AFF) else attacker.def.accentColor,
                isUlti = isUlti
            )
        )
    }

    private fun updateTrainingChallenge(
        training: TrainingHudState,
        moveSlot: MoveSlot,
        wasAirborne: Boolean,
        comboHits: Int,
        comboDmg: Int
    ): TrainingHudState {
        val challenge = training.activeChallenge ?: return training.copy(
            lastComboHits = comboHits,
            lastComboDamage = comboDmg
        )
        val expected = challenge.steps.getOrNull(training.currentStepIndex)
        val matches = when (expected) {
            ComboInputStep.CLASSIC -> moveSlot == MoveSlot.CLASSIC
            ComboInputStep.JUMP_CLASSIC -> moveSlot == MoveSlot.CLASSIC && wasAirborne
            ComboInputStep.BASE_ABILITY -> moveSlot == MoveSlot.BASE_ABILITY
            ComboInputStep.ULTI -> moveSlot == MoveSlot.ULTI
            null -> false
        }

        val nextStep = if (matches) {
            training.currentStepIndex + 1
        } else {
            // Check if this hit restarts step 0
            val first = challenge.steps.firstOrNull()
            val matchesFirst = when (first) {
                ComboInputStep.CLASSIC -> moveSlot == MoveSlot.CLASSIC
                ComboInputStep.JUMP_CLASSIC -> moveSlot == MoveSlot.CLASSIC && wasAirborne
                ComboInputStep.BASE_ABILITY -> moveSlot == MoveSlot.BASE_ABILITY
                ComboInputStep.ULTI -> moveSlot == MoveSlot.ULTI
                null -> false
            }
            if (matchesFirst) 1 else 0
        }

        return if (nextStep >= challenge.steps.size) {
            training.copy(
                currentStepIndex = 0,
                challengeSuccessBannerMs = 1500L,
                completedChallengesCount = training.completedChallengesCount + 1,
                lastComboHits = comboHits,
                lastComboDamage = comboDmg
            )
        } else {
            training.copy(
                currentStepIndex = nextStep,
                lastComboHits = comboHits,
                lastComboDamage = comboDmg
            )
        }
    }

    private fun preventOverlap(state: ArenaWorldState): ArenaWorldState {
        val dist = abs(state.p1.x - state.p2.x)
        val minSep = 0.085f
        if (dist < minSep && abs(state.p1.y - state.p2.y) < 0.14f) {
            val push = (minSep - dist) * 0.5f
            val dir = if (state.p1.x <= state.p2.x) -1f else 1f
            return state.copy(
                p1 = state.p1.copy(x = (state.p1.x + dir * push).coerceIn(0.08f, 0.92f)),
                p2 = state.p2.copy(x = (state.p2.x - dir * push).coerceIn(0.08f, 0.92f))
            )
        }
        return state
    }

    private fun ageEffects(state: ArenaWorldState, dtMs: Long): ArenaWorldState {
        return state.copy(
            ultiFlashMs = (state.ultiFlashMs - dtMs).coerceAtLeast(0L),
            sparks = state.sparks.mapNotNull {
                val age = it.ageMs + dtMs
                if (age >= it.maxAgeMs) null else it.copy(ageMs = age)
            },
            slashEffects = state.slashEffects.mapNotNull {
                val age = it.ageMs + dtMs
                if (age >= it.maxAgeMs) null else it.copy(ageMs = age)
            },
            popups = state.popups.mapNotNull {
                val age = it.ageMs + dtMs
                if (age >= it.maxAgeMs) null else it.copy(ageMs = age, y = it.y + 0.08f * (dtMs / 1000f))
            }
        )
    }

    private fun computeBotInput(
        state: ArenaWorldState,
        dtMs: Long,
        difficulty: BotDifficulty
    ): PlayerInput {
        botDecisionAccumMs += dtMs
        if (botDecisionAccumMs < difficulty.reactionIntervalMs) {
            return currentBotInput
        }
        botDecisionAccumMs = 0L

        val bot = state.p2
        val player = state.p1
        val dist = abs(player.x - bot.x)
        val dirToPlayer = if (player.x > bot.x) 1f else -1f

        val playerAttacking = player.action in setOf(
            CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI
        )
        val incomingProj = state.projectiles.any {
            it.ownerSide == 1 && abs(it.x - bot.x) < 0.24f
        }

        if (incomingProj && Random.nextFloat() < difficulty.blockChance) {
            currentBotInput = PlayerInput(jumpPressed = true)
            return currentBotInput
        }

        if (playerAttacking && dist < 0.28f && Random.nextFloat() < difficulty.blockChance) {
            currentBotInput = PlayerInput(blockHeld = true)
            return currentBotInput
        }

        // Ulti check
        if (bot.ultiMeter >= 100 && dist <= bot.def.ultimateMove.range + 0.05f && Random.nextFloat() < difficulty.aggression) {
            currentBotInput = PlayerInput(ultiPressed = true)
            return currentBotInput
        }

        // Base ability check
        if (bot.baseCooldownMs <= 0L && dist <= bot.def.baseAbility.range && Random.nextFloat() < difficulty.aggression * 0.65f) {
            currentBotInput = PlayerInput(baseAbilityPressed = true)
            return currentBotInput
        }

        // Classic strike in melee range
        if (dist <= bot.def.classicMove.range && bot.classicCooldownMs <= 0L && Random.nextFloat() < difficulty.aggression) {
            currentBotInput = PlayerInput(classicPressed = true)
            return currentBotInput
        }

        // Approach or space
        val moveDir = when {
            dist > 0.16f -> dirToPlayer
            dist < 0.09f && Random.nextFloat() < 0.4f -> -dirToPlayer
            else -> 0f
        }
        currentBotInput = PlayerInput(moveDir = moveDir)
        return currentBotInput
    }

    private fun nextId(): Long = nextEntityId++

    companion object {
        private const val GRAVITY = 4.6f
    }
}
