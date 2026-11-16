package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.game.data.FighterVisualStyle
import com.example.game.data.MoveEffectType
import com.example.game.engine.ArenaWorldState
import com.example.game.engine.CombatAction
import com.example.game.engine.FighterRuntime
import com.example.ui.theme.GuardBlue
import com.example.ui.theme.RussoOneFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArenaCanvas(
    world: ArenaWorldState,
    timeMs: Long,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier) {
        // Stage artwork backdrop
        Image(
            painter = painterResource(id = world.stage.backgroundRes),
            contentDescription = world.stage.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Subtle vignette so fighters & effects pop with maximum contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x660B0D14),
                            Color(0x220B0D14),
                            Color(0xAA0B0D14)
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val groundY = h * 0.84f

            // Arena floor & neon perspective line
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        world.stage.floorColor.copy(alpha = 0.88f),
                        Color(0xFF07090F)
                    ),
                    startY = groundY,
                    endY = h
                ),
                topLeft = Offset(0f, groundY),
                size = Size(w, h - groundY)
            )
            drawLine(
                color = world.stage.accentGlow.copy(alpha = 0.85f),
                start = Offset(0f, groundY),
                end = Offset(w, groundY),
                strokeWidth = 4f
            )

            // Ulti cinematic screen flash
            if (world.ultiFlashMs > 0L) {
                val alpha = (world.ultiFlashMs / 340f).coerceIn(0f, 1f) * 0.36f
                drawRect(color = world.ultiFlashColor.copy(alpha = alpha))
            }

            // Slash / Special / Ulti effects behind/around fighters
            world.slashEffects.forEach { fx ->
                val progress = (fx.ageMs.toFloat() / fx.maxAgeMs.toFloat()).coerceIn(0f, 1f)
                val fxX = fx.x * w
                val fxY = groundY - fx.y * (h * 0.65f) - h * 0.14f
                val reachPx = fx.range * w

                when (fx.effectType) {
                    MoveEffectType.ULTI_PILLAR -> {
                        val pillarX = (fx.x + fx.facing * 0.25f) * w
                        val pillarW = w * 0.18f * (1f - progress * 0.35f)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, fx.color.copy(alpha = 0.85f * (1f - progress)), Color.Transparent),
                                startX = pillarX - pillarW,
                                endX = pillarX + pillarW
                            ),
                            topLeft = Offset(pillarX - pillarW, 0f),
                            size = Size(pillarW * 2f, groundY)
                        )
                    }
                    MoveEffectType.ULTI_SLASH -> {
                        drawLine(
                            color = fx.color.copy(alpha = 1f - progress),
                            start = Offset(fxX, fxY),
                            end = Offset(fxX + fx.facing * reachPx, fxY - h * 0.04f),
                            strokeWidth = (1f - progress) * 28f,
                            cap = StrokeCap.Round
                        )
                    }
                    MoveEffectType.SHOCKWAVE, MoveEffectType.ULTI_SLAM -> {
                        val r = reachPx * (0.25f + progress * 0.85f)
                        drawOval(
                            color = fx.color.copy(alpha = (1f - progress) * 0.85f),
                            topLeft = Offset(fxX - r, groundY - r * 0.24f),
                            size = Size(r * 2f, r * 0.45f),
                            style = Stroke(width = 9f * (1f - progress) + 2f)
                        )
                    }
                    else -> {
                        val arcOffset = fx.facing * reachPx * 0.55f
                        drawArc(
                            color = fx.color.copy(alpha = (1f - progress) * 0.9f),
                            startAngle = if (fx.facing > 0) -65f else 115f,
                            sweepAngle = 130f,
                            useCenter = false,
                            topLeft = Offset(fxX + arcOffset - h * 0.12f, fxY - h * 0.12f),
                            size = Size(h * 0.24f, h * 0.24f),
                            style = Stroke(width = 14f * (1f - progress) + 3f, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Draw both fighters
            drawFighter(
                f = world.p1,
                arenaW = w,
                arenaH = h,
                groundY = groundY,
                timeMs = timeMs
            )
            drawFighter(
                f = world.p2,
                arenaW = w,
                arenaH = h,
                groundY = groundY,
                timeMs = timeMs
            )

            // Draw projectiles (fireballs / tornados)
            world.projectiles.forEach { proj ->
                val px = proj.x * w
                val py = groundY - proj.y * (h * 0.65f) - h * 0.09f
                val r = h * 0.065f
                if (proj.isTornado) {
                    for (ring in 0..3) {
                        val ry = py - r * 1.1f + ring * (r * 0.6f)
                        val rw = r * (1.3f - ring * 0.2f)
                        drawOval(
                            color = proj.color.copy(alpha = 0.82f),
                            topLeft = Offset(px - rw, ry - r * 0.25f),
                            size = Size(rw * 2f, r * 0.5f),
                            style = Stroke(width = 5f)
                        )
                    }
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, proj.accentColor, proj.color, Color.Transparent),
                            center = Offset(px, py),
                            radius = r * 1.7f
                        ),
                        radius = r * 1.7f,
                        center = Offset(px, py)
                    )
                }
            }

            // Hit sparks
            world.sparks.forEach { spark ->
                val p = (spark.ageMs.toFloat() / spark.maxAgeMs.toFloat()).coerceIn(0f, 1f)
                val sx = spark.x * w
                val sy = groundY - spark.y * (h * 0.65f)
                val maxR = if (spark.isUlti) h * 0.20f else h * 0.11f
                val count = if (spark.isUlti) 10 else 6
                for (i in 0 until count) {
                    val angle = (i.toFloat() / count) * (2f * PI.toFloat())
                    val dist = maxR * p
                    drawLine(
                        color = spark.color.copy(alpha = 1f - p),
                        start = Offset(sx + cos(angle) * dist * 0.3f, sy + sin(angle) * dist * 0.3f),
                        end = Offset(sx + cos(angle) * dist, sy + sin(angle) * dist),
                        strokeWidth = if (spark.isUlti) 6f else 4f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Floating damage / status popups
            world.popups.forEach { popup ->
                val alpha = (1f - popup.ageMs.toFloat() / popup.maxAgeMs.toFloat()).coerceIn(0f, 1f)
                drawPopupText(
                    textMeasurer = textMeasurer,
                    text = popup.text,
                    x = popup.x * w,
                    y = (groundY - popup.y * (h * 0.65f) - h * 0.24f).coerceAtLeast(18f),
                    color = popup.color.copy(alpha = alpha),
                    isBig = popup.isCritical
                )
            }
        }
    }
}

private fun DrawScope.drawFighter(
    f: FighterRuntime,
    arenaW: Float,
    arenaH: Float,
    groundY: Float,
    timeMs: Long
) {
    val cx = f.x * arenaW
    val jumpOffset = f.y * (arenaH * 0.65f)
    val bob = if (f.action == CombatAction.IDLE) sin(timeMs / 180.0).toFloat() * (arenaH * 0.008f) else 0f
    val footY = groundY - jumpOffset

    val scale = if (f.def.visualStyle == FighterVisualStyle.STEEL_COLOSSUS) 1.15f else 1.0f
    val bodyH = arenaH * 0.25f * scale
    val bodyW = arenaH * 0.095f * scale

    // Ground shadow
    val shadowScale = (1f - f.y * 1.4f).coerceIn(0.35f, 1f)
    drawOval(
        color = Color.Black.copy(alpha = 0.45f * shadowScale),
        topLeft = Offset(cx - bodyW * 0.85f * shadowScale, groundY - arenaH * 0.015f),
        size = Size(bodyW * 1.7f * shadowScale, arenaH * 0.03f)
    )

    val isHit = f.action == CombatAction.HIT_STUN
    val isKo = f.action == CombatAction.KO
    val torsoTint = if (isHit && (timeMs / 60L) % 2L == 0L) Color.White else f.def.primaryColor

    val baseTopY = if (isKo) groundY - bodyW * 0.6f else footY - bodyH + bob
    val lean = when (f.action) {
        CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI -> f.facing * bodyW * 0.28f
        CombatAction.HIT_STUN -> -f.facing * bodyW * 0.22f
        else -> 0f
    }

    if (isKo) {
        // Knocked out horizontal silhouette
        drawRoundRect(
            color = f.def.primaryColor.copy(alpha = 0.8f),
            topLeft = Offset(cx - bodyH * 0.4f, groundY - bodyW * 0.65f),
            size = Size(bodyH * 0.8f, bodyW * 0.55f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        return
    }

    // Walk stride animation
    val stride = if (f.action == CombatAction.WALK) sin(timeMs / 85.0).toFloat() * bodyW * 0.38f else bodyW * 0.25f

    // Legs
    drawLine(
        color = Color(0xFF1F2437),
        start = Offset(cx - bodyW * 0.22f, baseTopY + bodyH * 0.58f),
        end = Offset(cx - stride, footY),
        strokeWidth = bodyW * 0.26f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF1A1E2E),
        start = Offset(cx + bodyW * 0.22f, baseTopY + bodyH * 0.58f),
        end = Offset(cx + stride, footY),
        strokeWidth = bodyW * 0.26f,
        cap = StrokeCap.Round
    )

    // Torso
    drawRoundRect(
        color = torsoTint,
        topLeft = Offset(cx + lean - bodyW * 0.45f, baseTopY + bodyH * 0.24f),
        size = Size(bodyW * 0.90f, bodyH * 0.40f),
        cornerRadius = CornerRadius(10f, 10f)
    )

    // Head
    val headCenter = Offset(cx + lean, baseTopY + bodyH * 0.13f)
    drawCircle(
        color = Color(0xFFEBD2BE),
        radius = bodyW * 0.34f,
        center = headCenter
    )

    // Fighter visual distinction
    when (f.def.visualStyle) {
        FighterVisualStyle.SHOTO_FLAME -> {
            drawRect(
                color = f.def.primaryColor,
                topLeft = Offset(headCenter.x - bodyW * 0.36f, headCenter.y - bodyW * 0.18f),
                size = Size(bodyW * 0.72f, bodyW * 0.14f)
            )
            drawLine(
                color = f.def.accentColor,
                start = Offset(headCenter.x - f.facing * bodyW * 0.32f, headCenter.y - bodyW * 0.12f),
                end = Offset(headCenter.x - f.facing * bodyW * 0.75f, headCenter.y + bodyW * 0.08f),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }
        FighterVisualStyle.CYBER_NINJA -> {
            drawLine(
                color = f.def.primaryColor,
                start = Offset(headCenter.x, headCenter.y - bodyW * 0.04f),
                end = Offset(headCenter.x + f.facing * bodyW * 0.34f, headCenter.y - bodyW * 0.04f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
        FighterVisualStyle.STEEL_COLOSSUS -> {
            drawCircle(
                color = f.def.accentColor,
                radius = bodyW * 0.15f,
                center = Offset(cx + lean, baseTopY + bodyH * 0.40f)
            )
        }
        FighterVisualStyle.WIND_MAGE -> {
            drawCircle(
                color = f.def.accentColor.copy(alpha = 0.7f),
                radius = bodyW * 0.48f,
                center = headCenter,
                style = Stroke(width = 3.5f)
            )
        }
        FighterVisualStyle.NEON_RONIN -> {
            val hat = Path().apply {
                moveTo(headCenter.x, headCenter.y - bodyW * 0.55f)
                lineTo(headCenter.x - bodyW * 0.56f, headCenter.y - bodyW * 0.10f)
                lineTo(headCenter.x + bodyW * 0.56f, headCenter.y - bodyW * 0.10f)
                close()
            }
            drawPath(hat, color = Color(0xFF1D2234))
            drawPath(hat, color = f.def.accentColor, style = Stroke(width = 2.5f))
        }
    }

    // Attacking / Guarding arm + weapon
    val armStart = Offset(cx + lean + f.facing * bodyW * 0.25f, baseTopY + bodyH * 0.34f)
    val armEnd = when (f.action) {
        CombatAction.CLASSIC -> Offset(armStart.x + f.facing * bodyW * 0.95f, armStart.y - bodyH * 0.02f)
        CombatAction.BASE_ABILITY -> Offset(armStart.x + f.facing * bodyW * 1.15f, armStart.y - bodyH * 0.05f)
        CombatAction.ULTI -> Offset(armStart.x + f.facing * bodyW * 1.25f, armStart.y - bodyH * 0.18f)
        CombatAction.BLOCK -> Offset(armStart.x + f.facing * bodyW * 0.35f, armStart.y - bodyH * 0.14f)
        else -> Offset(armStart.x + f.facing * bodyW * 0.45f, armStart.y + bodyH * 0.10f)
    }

    drawLine(
        color = f.def.accentColor,
        start = armStart,
        end = armEnd,
        strokeWidth = bodyW * 0.22f,
        cap = StrokeCap.Round
    )

    // Ronin katana or Ninja blade when attacking
    if (f.def.visualStyle == FighterVisualStyle.NEON_RONIN || f.def.visualStyle == FighterVisualStyle.CYBER_NINJA) {
        val bladeLen = if (f.action in setOf(CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI)) bodyW * 1.15f else bodyW * 0.65f
        drawLine(
            color = f.def.accentColor,
            start = armEnd,
            end = Offset(armEnd.x + f.facing * bladeLen, armEnd.y - bodyH * 0.06f),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
    }

    // Guard energy shield
    if (f.isBlocking) {
        drawArc(
            color = GuardBlue.copy(alpha = 0.85f),
            startAngle = if (f.facing > 0) -70f else 110f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - bodyW * 0.9f, baseTopY + bodyH * 0.05f),
            size = Size(bodyW * 1.8f, bodyH * 0.8f),
            style = Stroke(width = 7f, cap = StrokeCap.Round)
        )
    }

    // Ulti ready aura ring at feet
    if (f.ultiMeter >= 100) {
        val pulse = 0.55f + 0.35f * sin(timeMs / 130.0).toFloat()
        drawOval(
            color = f.def.accentColor.copy(alpha = pulse),
            topLeft = Offset(cx - bodyW * 0.95f, groundY - arenaH * 0.02f),
            size = Size(bodyW * 1.9f, arenaH * 0.04f),
            style = Stroke(width = 4f)
        )
    }
}

private fun DrawScope.drawPopupText(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    color: Color,
    isBig: Boolean
) {
    val style = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.Bold,
        fontSize = if (isBig) 18.sp else 14.sp,
        color = color
    )
    val result = textMeasurer.measure(text = text, style = style)
    val left = (x - result.size.width / 2f).coerceIn(8f, size.width - result.size.width - 8f)
    drawText(
        textLayoutResult = result,
        topLeft = Offset(left, y)
    )
}
