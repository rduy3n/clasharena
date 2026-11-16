package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.data.BotDifficulty
import com.example.game.data.FighterDefinition
import com.example.game.data.FighterRoster
import com.example.game.data.GameMode
import com.example.game.data.TrainingOpponentMode
import com.example.game.engine.ArenaWorldState
import com.example.game.engine.FighterRuntime
import com.example.game.engine.MatchPhase
import com.example.game.engine.TrainingHudState
import com.example.ui.components.ArcadeControls
import com.example.ui.components.ArenaCanvas
import com.example.ui.components.MoveListSheet
import com.example.ui.theme.CrimsonHp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold
import com.example.ui.viewmodel.BattleViewModel

@Composable
fun BattleScreen(
    viewModel: BattleViewModel,
    onRematchLocal: () -> Unit,
    onExitBattle: () -> Unit
) {
    val worldState by viewModel.world.collectAsStateWithLifecycle()
    val timeMs by viewModel.timeMs.collectAsStateWithLifecycle()
    val localSide by viewModel.localSide.collectAsStateWithLifecycle()
    val onlineError by viewModel.onlineError.collectAsStateWithLifecycle()

    var showMoveListFor by remember { mutableStateOf<FighterDefinition?>(null) }
    var showTrainingSettings by remember { mutableStateOf(false) }

    val world = worldState
    if (world == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            Text("Chargement du combat...", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        return
    }

    val controlledFighter = if (localSide == 1) world.p1 else world.p2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Arcade HUD (HP bars, Timer, Ulti gauges, Exit)
            CombatHudHeader(
                world = world,
                localSide = localSide,
                onExit = onExitBattle
            )

            // Training Mode Quick Bar (Reset HP, Move List, Opponent Switch, Settings)
            if (world.mode == GameMode.TRAINING) {
                TrainingQuickToolbar(
                    training = world.training,
                    p1Name = world.p1.def.name,
                    p2Name = world.p2.def.name,
                    onResetHealth = { viewModel.resetTrainingHealth() },
                    onOpenMoveList = { showMoveListFor = world.p1.def },
                    onOpenSettings = { showTrainingSettings = true },
                    onSelectChallenge = { idx -> viewModel.selectTrainingChallenge(idx) },
                    challengesCount = world.p1.def.comboChallenges.size
                )
            }

            // 2D Fighting Stage Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ArenaCanvas(
                    world = world,
                    timeMs = timeMs,
                    modifier = Modifier.fillMaxSize()
                )

                // Combo counter badge
                val activeCombo = if (localSide == 1) world.p1 else world.p2
                if (activeCombo.comboCount >= 2) {
                    Surface(
                        color = FlameOrange.copy(alpha = 0.90f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(
                                text = "${activeCombo.comboCount} HITS !",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "${activeCombo.comboDamage} DÉGÂTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = UltiGold
                            )
                        }
                    }
                }

                // Training Combo Challenge Completed Toast
                if (world.mode == GameMode.TRAINING && world.training.challengeSuccessBannerMs > 0L) {
                    Surface(
                        color = EmeraldHp,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ObsidianBg)
                            Text(
                                text = "COMBO RÉUSSI !",
                                style = MaterialTheme.typography.titleLarge,
                                color = ObsidianBg
                            )
                        }
                    }
                }

                // Center Round / KO Banner
                if (world.bannerText.isNotEmpty() && world.phase != MatchPhase.MATCH_OVER) {
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, UltiGold),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = world.bannerText,
                            style = MaterialTheme.typography.displayLarge,
                            color = UltiGold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            // Arcade Touch Controls
            ArcadeControls(
                fighter = controlledFighter.def,
                runtime = controlledFighter,
                onMoveLeftChanged = { viewModel.setMoveLeft(it) },
                onMoveRightChanged = { viewModel.setMoveRight(it) },
                onJumpPressed = { viewModel.triggerJump() },
                onBlockChanged = { viewModel.setBlock(it) },
                onClassicPressed = { viewModel.triggerClassic() },
                onBaseAbilityPressed = { viewModel.triggerBaseAbility() },
                onUltiPressed = { viewModel.triggerUlti() }
            )
        }

        // Online Disconnect Error Overlay
        if (onlineError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, CrimsonHp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("CONNEXION SALON", style = MaterialTheme.typography.headlineMedium, color = CrimsonHp)
                        Text(onlineError!!, style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center)
                        Button(onClick = onExitBattle) {
                            Text("RETOUR AU MENU")
                        }
                    }
                }
            }
        }

        // Match Over Overlay
        if (world.phase == MatchPhase.MATCH_OVER) {
            val didWin = world.winnerSide == localSide
            val winnerName = if (world.winnerSide == 1) world.p1.def.name else world.p2.def.name
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(2.dp, if (didWin) UltiGold else CrimsonHp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (didWin) "VICTOIRE !" else "DÉFAITE",
                            style = MaterialTheme.typography.displayLarge,
                            color = if (didWin) UltiGold else CrimsonHp
                        )
                        Text(
                            text = "$winnerName remporte le duel (${world.p1RoundsWon} - ${world.p2RoundsWon})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (world.mode == GameMode.VS_BOT) {
                                Button(
                                    onClick = onRematchLocal,
                                    colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                                    modifier = Modifier.testTag("rematch_button")
                                ) {
                                    Text("REVANCHE")
                                }
                            }
                            OutlinedButton(
                                onClick = onExitBattle,
                                modifier = Modifier.testTag("exit_match_button")
                            ) {
                                Text("MENU", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    showMoveListFor?.let { fighter ->
        MoveListSheet(
            fighter = fighter,
            onDismiss = { showMoveListFor = null }
        )
    }

    if (showTrainingSettings) {
        TrainingSettingsSheet(
            world = world,
            onSelectOpponentMode = { viewModel.setTrainingOpponentMode(it) },
            onSelectBotDifficulty = { viewModel.setTrainingBotDifficulty(it) },
            onToggleAutoRegen = { viewModel.toggleTrainingAutoRegen() },
            onToggleInfiniteUlti = { viewModel.toggleTrainingInfiniteUlti() },
            onSwitchFighters = { p1, p2 -> viewModel.switchTrainingFighters(p1, p2) },
            onDismiss = { showTrainingSettings = false }
        )
    }
}

@Composable
private fun CombatHudHeader(
    world: ArenaWorldState,
    localSide: Int,
    onExit: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // P1 Bar
                FighterHealthAndUlti(
                    fighter = world.p1,
                    roundsWon = world.p1RoundsWon,
                    isLocal = localSide == 1,
                    alignRight = false,
                    modifier = Modifier.weight(1f)
                )

                // Center Clock or Training badge + Exit
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (world.mode == GameMode.TRAINING) {
                        Text(
                            text = "LABO",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldHp
                        )
                    } else {
                        Text(
                            text = "${world.roundTimeRemainingSec}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = UltiGold
                        )
                    }
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("battle_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Quitter le combat",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // P2 Bar
                FighterHealthAndUlti(
                    fighter = world.p2,
                    roundsWon = world.p2RoundsWon,
                    isLocal = localSide == 2,
                    alignRight = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FighterHealthAndUlti(
    fighter: FighterRuntime,
    roundsWon: Int,
    isLocal: Boolean,
    alignRight: Boolean,
    modifier: Modifier = Modifier
) {
    val hpRatio = (fighter.hp.toFloat() / fighter.def.stats.maxHp.toFloat()).coerceIn(0f, 1f)
    val hpColor = when {
        hpRatio > 0.5f -> EmeraldHp
        hpRatio > 0.25f -> UltiGold
        else -> CrimsonHp
    }

    Column(
        modifier = modifier,
        horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!alignRight) {
                Text(
                    text = fighter.def.name + if (isLocal) " (VOUS)" else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                RoundPips(roundsWon)
            } else {
                RoundPips(roundsWon)
                Text(
                    text = fighter.def.name + if (isLocal) " (VOUS)" else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }

        LinearProgressIndicator(
            progress = { hpRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = hpColor,
            trackColor = ObsidianBg
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${fighter.hp}/${fighter.def.stats.maxHp}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = "ULTI ${fighter.ultiMeter}%",
                style = MaterialTheme.typography.labelSmall,
                color = if (fighter.ultiMeter >= 100) UltiGold else ElectricCyan
            )
        }
    }
}

@Composable
private fun RoundPips(roundsWon: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(2) { idx ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (idx < roundsWon) UltiGold else ObsidianOutline)
            )
        }
    }
}

@Composable
private fun TrainingQuickToolbar(
    training: TrainingHudState,
    p1Name: String,
    p2Name: String,
    onResetHealth: () -> Unit,
    onOpenMoveList: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectChallenge: (Int) -> Unit,
    challengesCount: Int
) {
    Surface(
        color = ObsidianSurfaceVariant,
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onResetHealth,
                    color = EmeraldHp.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldHp),
                    modifier = Modifier.testTag("training_reset_hp_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = EmeraldHp, modifier = Modifier.size(16.dp))
                        Text("RESET PV", style = MaterialTheme.typography.labelSmall, color = EmeraldHp)
                    }
                }

                Surface(
                    onClick = onOpenMoveList,
                    color = ElectricCyan.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ElectricCyan),
                    modifier = Modifier.testTag("training_movelist_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Text("COUPS ($p1Name)", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                    }
                }

                Surface(
                    onClick = onOpenSettings,
                    color = FlameOrange.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, FlameOrange),
                    modifier = Modifier.testTag("training_settings_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = FlameOrange, modifier = Modifier.size(16.dp))
                        val oppSummary = if (training.opponentMode == TrainingOpponentMode.AI_BOT) {
                            "${p2Name}: IA ${training.botDifficulty.label}"
                        } else {
                            "${p2Name}: ${training.opponentMode.label}"
                        }
                        Text(oppSummary, style = MaterialTheme.typography.labelSmall, color = FlameOrange)
                    }
                }
            }

            // Guided Combo Practice Bar
            training.activeChallenge?.let { challenge ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "COMBO :",
                            style = MaterialTheme.typography.labelSmall,
                            color = UltiGold
                        )
                        challenge.steps.forEachIndexed { idx, step ->
                            val completed = idx < training.currentStepIndex
                            val isNext = idx == training.currentStepIndex
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            completed -> EmeraldHp
                                            isNext -> UltiGold.copy(alpha = 0.25f)
                                            else -> ObsidianBg
                                        }
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = step.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (completed) ObsidianBg else Color.White
                                )
                            }
                        }
                        Text(
                            text = challenge.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }

                    if (challengesCount > 1) {
                        Surface(
                            onClick = {
                                val next = (training.selectedChallengeIndex + 1) % challengesCount
                                onSelectChallenge(next)
                            },
                            color = ObsidianBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ObsidianOutline),
                            modifier = Modifier.testTag("training_next_combo_button")
                        ) {
                            Text(
                                text = "SUIVANT (${training.selectedChallengeIndex + 1}/$challengesCount)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingSettingsSheet(
    world: ArenaWorldState,
    onSelectOpponentMode: (TrainingOpponentMode) -> Unit,
    onSelectBotDifficulty: (BotDifficulty) -> Unit,
    onToggleAutoRegen: () -> Unit,
    onToggleInfiniteUlti: () -> Unit,
    onSwitchFighters: (p1Id: String, p2Id: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "OPTIONS D'ENTRAÎNEMENT",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Text("COMPORTEMENT DU MANNEQUIN", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainingOpponentMode.entries.forEach { mode ->
                    FilterChip(
                        selected = world.training.opponentMode == mode,
                        onClick = { onSelectOpponentMode(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }

            if (world.training.opponentMode == TrainingOpponentMode.AI_BOT) {
                Text("DIFFICULTÉ DE L'IA", style = MaterialTheme.typography.labelSmall, color = FlameOrange)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = world.training.botDifficulty == diff,
                            onClick = { onSelectBotDifficulty(diff) },
                            label = { Text(diff.label) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Régénération auto des PV", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Switch(checked = world.training.autoRegenHp, onCheckedChange = { onToggleAutoRegen() })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Jauge d'Ultime infinie (100%)", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Switch(checked = world.training.infiniteUlti, onCheckedChange = { onToggleInfiniteUlti() })
            }

            Text("CHANGER VOTRE COMBATTANT (J1)", style = MaterialTheme.typography.labelSmall, color = UltiGold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FighterRoster.fighters.forEach { f ->
                    FilterChip(
                        selected = world.p1.def.id == f.id,
                        onClick = { onSwitchFighters(f.id, world.p2.def.id) },
                        label = { Text(f.name) }
                    )
                }
            }

            Text("CHANGER LE MANNEQUIN (J2)", style = MaterialTheme.typography.labelSmall, color = UltiGold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FighterRoster.fighters.forEach { f ->
                    FilterChip(
                        selected = world.p2.def.id == f.id,
                        onClick = { onSwitchFighters(world.p1.def.id, f.id) },
                        label = { Text(f.name) }
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("REPRENDRE L'ENTRAÎNEMENT")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
