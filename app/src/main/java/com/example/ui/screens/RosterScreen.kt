package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.data.FighterDefinition
import com.example.game.data.FighterRoster
import com.example.ui.components.FighterAvatar
import com.example.ui.components.MoveListSheet
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold

@Composable
fun RosterScreen(
    favoriteFighterId: String,
    onSetFavorite: (String) -> Unit,
    onBack: () -> Unit
) {
    var moveListFighter by remember { mutableStateOf<FighterDefinition?>(null) }

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
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("roster_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = "COMBATTANTS & STATS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Tous équilibrés sur un budget total de 300 points",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(FighterRoster.fighters, key = { it.id }) { fighter ->
                    FighterRosterCard(
                        fighter = fighter,
                        isFavorite = fighter.id == favoriteFighterId,
                        onToggleFavorite = { onSetFavorite(fighter.id) },
                        onOpenMoveList = { moveListFighter = fighter }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    moveListFighter?.let { fighter ->
        MoveListSheet(
            fighter = fighter,
            onDismiss = { moveListFighter = null }
        )
    }
}

@Composable
private fun FighterRosterCard(
    fighter: FighterDefinition,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenMoveList: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, fighter.primaryColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    FighterAvatar(fighter = fighter, size = 56.dp)
                    Column {
                        Text(
                            text = "${fighter.name} • ${fighter.title}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "${fighter.archetype} • ${fighter.stats.maxHp} PV • Défense ${fighter.stats.defensePercent}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = fighter.primaryColor
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("favorite_fighter_${fighter.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Définir comme favori",
                        tint = if (isFavorite) UltiGold else TextSecondary
                    )
                }
            }

            Text(
                text = fighter.lore,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatBarRow("PV", fighter.stats.ratingHp, EmeraldHp)
                StatBarRow("ATK", fighter.stats.ratingAttack, FlameOrange)
                StatBarRow("VIT", fighter.stats.ratingSpeed, ElectricCyan)
                StatBarRow("DÉF", fighter.stats.ratingDefense, Color(0xFF448AFF))
                StatBarRow("PORTÉE", fighter.stats.ratingRange, UltiGold)
            }

            OutlinedButton(
                onClick = onOpenMoveList,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, fighter.primaryColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("movelist_button_${fighter.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = fighter.primaryColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VOIR COUPS & COMBOS",
                    style = MaterialTheme.typography.labelLarge,
                    color = fighter.primaryColor
                )
            }
        }
    }
}

@Composable
fun StatBarRow(label: String, rating: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.width(56.dp)
        )
        LinearProgressIndicator(
            progress = { (rating / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = ObsidianBg
        )
        Text(
            text = "$rating",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.width(26.dp)
        )
    }
}
