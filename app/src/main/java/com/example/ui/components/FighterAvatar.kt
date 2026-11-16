package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.data.FighterDefinition
import com.example.game.data.FighterVisualStyle

@Composable
fun FighterAvatar(
    fighter: FighterDefinition,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        fighter.primaryColor.copy(alpha = 0.35f),
                        Color(0xFF10131E)
                    )
                )
            )
            .border(1.5.dp, fighter.primaryColor.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w * 0.5f
            val cy = h * 0.48f

            // Subtle energy aura circle
            drawCircle(
                color = fighter.accentColor.copy(alpha = 0.20f),
                radius = w * 0.38f,
                center = Offset(cx, cy)
            )

            // Shoulders / torso silhouette
            val torso = Path().apply {
                moveTo(w * 0.22f, h * 0.96f)
                lineTo(w * 0.32f, h * 0.64f)
                lineTo(w * 0.68f, h * 0.64f)
                lineTo(w * 0.78f, h * 0.96f)
                close()
            }
            drawPath(torso, color = fighter.primaryColor)

            // Head
            drawCircle(
                color = Color(0xFFEBD2BE),
                radius = w * 0.17f,
                center = Offset(cx, h * 0.40f)
            )

            // Style-specific signature gear
            when (fighter.visualStyle) {
                FighterVisualStyle.SHOTO_FLAME -> {
                    // Spiky hair & red headband
                    drawRect(
                        color = fighter.primaryColor,
                        topLeft = Offset(cx - w * 0.18f, h * 0.27f),
                        size = Size(w * 0.36f, h * 0.08f)
                    )
                    drawLine(
                        color = fighter.accentColor,
                        start = Offset(cx + w * 0.15f, h * 0.31f),
                        end = Offset(w * 0.86f, h * 0.24f),
                        strokeWidth = w * 0.06f,
                        cap = StrokeCap.Round
                    )
                }
                FighterVisualStyle.CYBER_NINJA -> {
                    // Glowing visor
                    drawRect(
                        color = Color(0xFF151924),
                        topLeft = Offset(cx - w * 0.18f, h * 0.24f),
                        size = Size(w * 0.36f, h * 0.19f)
                    )
                    drawLine(
                        color = fighter.primaryColor,
                        start = Offset(cx - w * 0.12f, h * 0.38f),
                        end = Offset(cx + w * 0.14f, h * 0.38f),
                        strokeWidth = w * 0.065f,
                        cap = StrokeCap.Round
                    )
                }
                FighterVisualStyle.STEEL_COLOSSUS -> {
                    // Heavy steel helmet & pauldrons
                    drawCircle(
                        color = fighter.primaryColor,
                        radius = w * 0.20f,
                        center = Offset(cx, h * 0.38f),
                        style = Stroke(width = w * 0.08f)
                    )
                    drawCircle(
                        color = fighter.accentColor,
                        radius = w * 0.08f,
                        center = Offset(cx, h * 0.77f)
                    )
                }
                FighterVisualStyle.WIND_MAGE -> {
                    // Arcane halo behind head
                    drawCircle(
                        color = fighter.accentColor,
                        radius = w * 0.27f,
                        center = Offset(cx, h * 0.36f),
                        style = Stroke(width = w * 0.04f)
                    )
                    drawArc(
                        color = fighter.primaryColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(cx - w * 0.19f, h * 0.21f),
                        size = Size(w * 0.38f, h * 0.24f)
                    )
                }
                FighterVisualStyle.NEON_RONIN -> {
                    // Conical ronin hat & diagonal katana glint
                    val hat = Path().apply {
                        moveTo(cx, h * 0.13f)
                        lineTo(w * 0.18f, h * 0.34f)
                        lineTo(w * 0.82f, h * 0.34f)
                        close()
                    }
                    drawPath(hat, color = Color(0xFF1D2234))
                    drawPath(hat, color = fighter.accentColor, style = Stroke(width = w * 0.03f))
                    drawLine(
                        color = fighter.primaryColor,
                        start = Offset(w * 0.18f, h * 0.86f),
                        end = Offset(w * 0.84f, h * 0.56f),
                        strokeWidth = w * 0.055f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
