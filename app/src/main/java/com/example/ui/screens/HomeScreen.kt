package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.firebase.PlayerProfile
import com.example.game.data.FighterRoster
import com.example.ui.components.FighterAvatar
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold

@Composable
fun HomeScreen(
    profile: PlayerProfile?,
    fallbackName: String,
    onPlayVsBot: () -> Unit,
    onPlayTraining: () -> Unit,
    onPlayOnline: () -> Unit,
    onOpenRoster: () -> Unit,
    onSignOut: () -> Unit
) {
    val favFighter = FighterRoster.getFighter(profile?.favoriteFighterId ?: "ryuji")
    val playerName = profile?.displayName ?: fallbackName
    val wins = profile?.wins ?: 0L
    val losses = profile?.losses ?: 0L

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
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header with Player Profile & Sign Out
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FighterAvatar(fighter = favFighter, size = 48.dp)
                    Column {
                        Text(
                            text = playerName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "$wins V • $losses D • Favori : ${favFighter.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                    }
                }
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Se déconnecter",
                        tint = TextSecondary
                    )
                }
            }

            // Hero Arena Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_arena),
                    contentDescription = "Arène Clash Arena",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    ObsidianBg.copy(alpha = 0.92f),
                                    ObsidianBg.copy(alpha = 0.35f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(FlameOrange)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SAISON 1 • ÉQUILIBRÉE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "CLASH ARENA",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Text(
                        text = "Coup Classique • Capacité • Ultime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan
                    )
                }
            }

            // Primary Game Mode Cards
            ModeActionCard(
                title = "DUEL CONTRE BOT",
                subtitle = "Affrontez l'IA en 2 rounds gagnants • Difficulté réglable",
                badge = "ARCADE 1V1",
                accentColor = FlameOrange,
                icon = Icons.Default.SmartToy,
                testTag = "mode_vs_bot_card",
                onClick = onPlayVsBot
            )

            ModeActionCard(
                title = "MODE ENTRAÎNEMENT",
                subtitle = "Dummy fixe ou IA • Reset PV • Défis Combos • Liste des coups",
                badge = "DOJO LAB",
                accentColor = EmeraldHp,
                icon = Icons.Default.FitnessCenter,
                testTag = "mode_training_card",
                onClick = onPlayTraining
            )

            ModeActionCard(
                title = "DUEL EN LIGNE (AMIS)",
                subtitle = "Créez ou rejoignez un salon privé avec un code à 6 caractères",
                badge = "MULTIJOUEUR",
                accentColor = ElectricCyan,
                icon = Icons.Default.Public,
                testTag = "mode_online_card",
                onClick = onPlayOnline
            )

            ModeActionCard(
                title = "COMBATTANTS & STATS",
                subtitle = "Comparez les 5 personnages équilibrés et leurs coups spéciaux",
                badge = "ROSTER (5)",
                accentColor = UltiGold,
                icon = Icons.Default.Groups,
                testTag = "mode_roster_card",
                onClick = onOpenRoster
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ModeActionCard(
    title: String,
    subtitle: String,
    badge: String,
    accentColor: Color,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = ObsidianSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
