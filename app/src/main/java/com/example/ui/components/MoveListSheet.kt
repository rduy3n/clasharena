package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.data.FighterDefinition
import com.example.game.data.MoveSpec
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.GuardBlue
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveListSheet(
    fighter: FighterDefinition,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FighterAvatar(fighter = fighter, size = 52.dp)
                    Column {
                        Text(
                            text = "${fighter.name.uppercase()} • LISTE DES COUPS",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "${fighter.title} (${fighter.archetype}) • ${fighter.stats.maxHp} PV • Déf. ${fighter.stats.defensePercent}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = fighter.primaryColor
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_movelist_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer la liste des coups")
                }
            }

            HorizontalDivider(color = ObsidianOutline)

            MoveDetailCard(
                badge = "CLASSIQUE",
                badgeColor = ElectricCyan,
                move = fighter.classicMove
            )
            MoveDetailCard(
                badge = "CAPACITÉ DE BASE",
                badgeColor = FlameOrange,
                move = fighter.baseAbility
            )
            MoveDetailCard(
                badge = "ULTIME (100%)",
                badgeColor = UltiGold,
                move = fighter.ultimateMove
            )

            // Guard & Movement tips
            Surface(
                color = ObsidianSurfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GuardBlue.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "GARDE & MOBILITÉ",
                        style = MaterialTheme.typography.labelLarge,
                        color = GuardBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Maintenez GARDE pour réduire les dégâts classiques/capacités de 75% et annuler le recul. Les Ultimes percent partiellement la garde (45% réduits).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Combo list
            Text(
                text = "ENCHAÎNEMENTS RECOMMANDÉS",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            fighter.comboChallenges.forEach { combo ->
                Surface(
                    color = ObsidianBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ObsidianOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = combo.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = combo.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            combo.steps.forEachIndexed { idx, step ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(fighter.primaryColor.copy(alpha = 0.22f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${step.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun MoveDetailCard(
    badge: String,
    badgeColor: Color,
    move: MoveSpec
) {
    Surface(
        color = ObsidianBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = move.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.22f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor
                    )
                }
            }
            Text(
                text = move.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip("Dégâts", "${move.damage}")
                StatChip(
                    label = if (move.ultiCost > 0) "Coût" else "Recharge",
                    value = if (move.ultiCost > 0) "${move.ultiCost}% Ulti" else "${String.format("%.1f", move.cooldownMs / 1000f)}s"
                )
                StatChip("Portée", "${(move.range * 100).toInt()}")
                if (move.ultiGainOnHit > 0) {
                    StatChip("Gain Ulti", "+${move.ultiGainOnHit}%")
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}
