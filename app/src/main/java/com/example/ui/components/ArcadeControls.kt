package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.game.data.FighterDefinition
import com.example.game.engine.FighterRuntime
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.GuardBlue
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold

@Composable
fun ArcadeControls(
    fighter: FighterDefinition,
    runtime: FighterRuntime,
    onMoveLeftChanged: (Boolean) -> Unit,
    onMoveRightChanged: (Boolean) -> Unit,
    onJumpPressed: () -> Unit,
    onBlockChanged: (Boolean) -> Unit,
    onClassicPressed: () -> Unit,
    onBaseAbilityPressed: () -> Unit,
    onUltiPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ultiReady = runtime.ultiMeter >= fighter.ultimateMove.ultiCost
    val baseCdSec = runtime.baseCooldownMs / 1000f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Pad: Movement + Jump + Guard
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HoldActionPadButton(
                    icon = Icons.Default.ArrowUpward,
                    label = "SAUT",
                    tint = Color.White,
                    testTag = "control_jump",
                    onHoldChanged = { pressed ->
                        if (pressed) onJumpPressed()
                    }
                )
                HoldActionPadButton(
                    icon = Icons.Default.Shield,
                    label = "GARDE",
                    tint = GuardBlue,
                    testTag = "control_block",
                    onHoldChanged = onBlockChanged
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HoldActionPadButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    label = "GAUCHE",
                    tint = Color.White,
                    testTag = "control_left",
                    onHoldChanged = onMoveLeftChanged
                )
                HoldActionPadButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    label = "DROITE",
                    tint = Color.White,
                    testTag = "control_right",
                    onHoldChanged = onMoveRightChanged
                )
            }
        }

        // Right Pad: Classic, Base Ability, Ultimate
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ultimate button
            AttackPadButton(
                title = "ULTIME",
                subtitle = if (ultiReady) fighter.ultimateMove.name else "${runtime.ultiMeter}% / 100%",
                icon = Icons.Default.AutoAwesome,
                accentColor = if (ultiReady) UltiGold else TextSecondary,
                enabled = ultiReady,
                cooldownText = if (ultiReady) "PRÊT !" else "${runtime.ultiMeter}%",
                testTag = "control_ulti",
                onClick = onUltiPressed,
                modifier = Modifier.size(width = 166.dp, height = 56.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttackPadButton(
                    title = "CLASSIQUE",
                    subtitle = fighter.classicMove.name,
                    icon = Icons.Default.SportsMma,
                    accentColor = ElectricCyan,
                    enabled = runtime.classicCooldownMs <= 0L,
                    cooldownText = if (runtime.classicCooldownMs > 0L) "..." else null,
                    testTag = "control_classic",
                    onClick = onClassicPressed,
                    modifier = Modifier.size(width = 96.dp, height = 66.dp)
                )
                AttackPadButton(
                    title = "CAPACITÉ",
                    subtitle = fighter.baseAbility.name,
                    icon = Icons.Default.Bolt,
                    accentColor = FlameOrange,
                    enabled = runtime.baseCooldownMs <= 0L,
                    cooldownText = if (runtime.baseCooldownMs > 0L) String.format("%.1fs", baseCdSec) else null,
                    testTag = "control_base",
                    onClick = onBaseAbilityPressed,
                    modifier = Modifier.size(width = 96.dp, height = 66.dp)
                )
            }
        }
    }
}

@Composable
private fun HoldActionPadButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onHoldChanged: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        color = if (isPressed) tint.copy(alpha = 0.28f) else ObsidianSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, if (isPressed) tint else ObsidianOutline),
        modifier = Modifier
            .size(width = 66.dp, height = 56.dp)
            .testTag(testTag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onHoldChanged(true)
                    waitForUpOrCancellation()
                    isPressed = false
                    onHoldChanged(false)
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun AttackPadButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    cooldownText: String?,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (enabled) accentColor.copy(alpha = 0.18f) else ObsidianSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (enabled) 1.8.dp else 1.dp,
            color = if (enabled) accentColor else ObsidianOutline
        ),
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (enabled) 0.28f else 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) accentColor else TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cooldownText ?: title,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) Color.White else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) accentColor else TextSecondary.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
