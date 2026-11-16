package com.example.game

import com.example.game.data.BotDifficulty
import com.example.game.data.FighterRoster
import com.example.game.data.GameMode
import com.example.game.data.TrainingOpponentMode
import com.example.game.engine.CombatEngine
import com.example.game.engine.PlayerInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatEngineTest {

    @Test
    fun allFighters_haveBalanced300PointStatBudget() {
        FighterRoster.fighters.forEach { fighter ->
            val sum = fighter.stats.ratingHp +
                fighter.stats.ratingAttack +
                fighter.stats.ratingSpeed +
                fighter.stats.ratingDefense +
                fighter.stats.ratingRange
            assertEquals("Fighter ${fighter.name} stat budget must equal 300", 300, sum)
        }
    }

    @Test
    fun trainingMode_comboAndResetHealth_worksCorrectly() {
        val engine = CombatEngine()
        val ryuji = FighterRoster.getFighter("ryuji")
        val kaelith = FighterRoster.getFighter("kaelith")
        val stage = FighterRoster.stages.first()

        var world = engine.createInitialState(
            mode = GameMode.TRAINING,
            stage = stage,
            p1Def = ryuji,
            p2Def = kaelith,
            botDifficulty = BotDifficulty.NORMAL,
            trainingOpponentMode = TrainingOpponentMode.STATIONARY
        ).let {
            // Put fighters within melee range
            it.copy(p1 = it.p1.copy(x = 0.45f), p2 = it.p2.copy(x = 0.57f))
        }

        // Trigger Classic attack and advance until active hit
        world = engine.step(world, 20L, PlayerInput(classicPressed = true))
        repeat(8) {
            world = engine.step(world, 20L, PlayerInput())
        }
        assertTrue("Dummy HP should decrease after Classic hit", world.p2.hp < kaelith.stats.maxHp)
        assertEquals(1, world.training.currentStepIndex)

        // Reset health restores full HP
        world = engine.resetTrainingHealth(world)
        assertEquals(kaelith.stats.maxHp, world.p2.hp)
        assertEquals(ryuji.stats.maxHp, world.p1.hp)
    }
}
