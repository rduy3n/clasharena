package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.game.data.BotDifficulty
import com.example.game.data.FighterRoster
import com.example.game.data.GameMode
import com.example.game.data.TrainingOpponentMode
import com.example.ui.components.FighterAvatar
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary

@Composable
fun MatchSetupScreen(
    mode: GameMode,
    defaultFighterId: String,
    onStartMatch: (
        p1Id: String,
        p2Id: String,
        stageId: String,
        difficulty: BotDifficulty,
        trainingOpponentMode: TrainingOpponentMode
    ) -> Unit,
    onBack: () -> Unit
) {
    var p1Id by remember { mutableStateOf(defaultFighterId) }
    var p2Id by remember {
        mutableStateOf(
            FighterRoster.fighters.firstOrNull { it.id != defaultFighterId }?.id ?: "kaelith"
        )
    }
    var stageId by remember { mutableStateOf(FighterRoster.stages.first().id) }
    var difficulty by remember { mutableStateOf(BotDifficulty.NORMAL) }
    var trainingOppMode by remember { mutableStateOf(TrainingOpponentMode.STATIONARY) }

    val p1Def = FighterRoster.getFighter(p1Id)
    val p2Def = FighterRoster.getFighter(p2Id)
    val accent = if (mode == GameMode.TRAINING) EmeraldHp else FlameOrange

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("setup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = if (mode == GameMode.TRAINING) "CONFIGURATION ENTRAÎNEMENT" else "CONFIGURATION DU DUEL",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = if (mode == GameMode.TRAINING) "Choisissez votre combattant, le mannequin et le mode" else "Sélectionnez votre combattant, votre adversaire IA et l'arène",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Fighter 1 picker
            Text(
                text = "VOTRE COMBATTANT (J1)",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan
            )
            FighterRowPicker(
                selectedId = p1Id,
                onSelect = { p1Id = it },
                prefixTag = "p1_pick"
            )
            SelectedFighterPreview(label = "J1", fighterId = p1Id)

            // Fighter 2 picker
            Text(
                text = if (mode == GameMode.TRAINING) "MANNEQUIN / ADVERSAIRE (J2)" else "ADVERSAIRE BOT (IA)",
                style = MaterialTheme.typography.labelLarge,
                color = FlameOrange
            )
            FighterRowPicker(
                selectedId = p2Id,
                onSelect = { p2Id = it },
                prefixTag = "p2_pick"
            )
            SelectedFighterPreview(label = "J2", fighterId = p2Id)

            // Training Opponent Mode OR Bot Difficulty
            if (mode == GameMode.TRAINING) {
                Text(
                    text = "COMPORTEMENT DU MANNEQUIN",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldHp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrainingOpponentMode.entries.forEach { oppMode ->
                        FilterChip(
                            selected = trainingOppMode == oppMode,
                            onClick = { trainingOppMode = oppMode },
                            label = { Text(oppMode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldHp.copy(alpha = 0.25f),
                                selectedLabelColor = EmeraldHp
                            ),
                            modifier = Modifier.testTag("training_opp_${oppMode.name.lowercase()}")
                        )
                    }
                }
            }

            if (mode == GameMode.VS_BOT || (mode == GameMode.TRAINING && trainingOppMode == TrainingOpponentMode.AI_BOT)) {
                Text(
                    text = "DIFFICULTÉ DE L'IA",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(diff.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameOrange.copy(alpha = 0.25f),
                                selectedLabelColor = FlameOrange
                            ),
                            modifier = Modifier.testTag("difficulty_${diff.name.lowercase()}")
                        )
                    }
                }
            }

            // Stage picker
            Text(
                text = "ARÈNE DE COMBAT",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FighterRoster.stages.forEach { stage ->
                    val selected = stage.id == stageId
                    Surface(
                        onClick = { stageId = stage.id },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) stage.accentGlow else ObsidianOutline
                        ),
                        color = ObsidianSurface,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stage_pick_${stage.id}")
                    ) {
                        Column {
                            Image(
                                painter = painterResource(id = stage.backgroundRes),
                                contentDescription = stage.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                            )
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = stage.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selected) stage.accentGlow else Color.White
                                )
                                Text(
                                    text = stage.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onStartMatch(p1Def.id, p2Def.id, stageId, difficulty, trainingOppMode)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = if (mode == GameMode.TRAINING) ObsidianBg else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_match_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (mode == GameMode.TRAINING) "LANCER L'ENTRAÎNEMENT" else "LANCER LE COMBAT",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FighterRowPicker(
    selectedId: String,
    onSelect: (String) -> Unit,
    prefixTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FighterRoster.fighters.forEach { fighter ->
            val selected = fighter.id == selectedId
            Surface(
                onClick = { onSelect(fighter.id) },
                color = if (selected) fighter.primaryColor.copy(alpha = 0.22f) else ObsidianSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) fighter.primaryColor else ObsidianOutline
                ),
                modifier = Modifier.testTag("${prefixTag}_${fighter.id}")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FighterAvatar(fighter = fighter, size = 54.dp)
                    Text(
                        text = fighter.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = fighter.archetype,
                        style = MaterialTheme.typography.labelSmall,
                        color = fighter.primaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedFighterPreview(label: String, fighterId: String) {
    val fighter = FighterRoster.getFighter(fighterId)
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$label : ${fighter.name} (${fighter.stats.maxHp} PV)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "${fighter.classicMove.name} • ${fighter.baseAbility.name} • ${fighter.ultimateMove.name}",
                style = MaterialTheme.typography.labelSmall,
                color = fighter.primaryColor
            )
        }
    }
}
