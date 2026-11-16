package com.example.game.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

/**
 * Type d'effet d'attaque utilisé par le moteur de combat 2D.
 */
enum class MoveEffectType {
    MELEE,
    PROJECTILE,
    DASH_STRIKE,
    SHOCKWAVE,
    COUNTER_WAVE,
    ULTI_UPPERCUT,
    ULTI_RUSH,
    ULTI_SLAM,
    ULTI_PILLAR,
    ULTI_SLASH
}

enum class MoveSlot(val label: String) {
    CLASSIC("Coup Classique"),
    BASE_ABILITY("Capacité de Base"),
    ULTI("Ultime")
}

/**
 * Définition complète d'une attaque (facile à ajuster pour l'équilibrage).
 */
data class MoveSpec(
    val slot: MoveSlot,
    val name: String,
    val description: String,
    val damage: Int,
    val cooldownMs: Long,
    val range: Float,          // Portée normalisée dans l'arène (0.0 à 1.0)
    val startupMs: Long,       // Temps avant l'impact
    val activeMs: Long,        // Durée de l'animation active
    val knockback: Float,      // Recul infligé à l'adversaire (0.0 à 0.30)
    val ultiGainOnHit: Int,    // Charge d'ulti gagnée si le coup touche
    val ultiCost: Int = 0,     // 100 pour l'ulti
    val effectType: MoveEffectType
)

/**
 * Statistiques de base du combattant.
 * Tous les personnages sont équilibrés autour d'un budget total de ~300 points.
 */
data class FighterStats(
    val maxHp: Int,              // Points de vie (ex: 900 - 1250)
    val moveSpeed: Float,        // Vitesse horizontale normalisée / sec (ex: 0.38 - 0.62)
    val jumpPower: Float,        // Impulsion de saut (ex: 1.30 - 1.55)
    val defensePercent: Int,     // Réduction passive de dégâts en % (ex: 5 - 22)
    // Notes d'affichage (sur 100) pour barres visuelles équilibrées
    val ratingHp: Int,
    val ratingAttack: Int,
    val ratingSpeed: Int,
    val ratingDefense: Int,
    val ratingRange: Int
)

enum class ComboInputStep(val label: String, val badge: String) {
    CLASSIC("Classique", "C"),
    BASE_ABILITY("Capacité", "CAP"),
    ULTI("Ultime", "ULTI"),
    JUMP_CLASSIC("Saut + Classique", "↑+C")
}

data class ComboChallenge(
    val id: String,
    val title: String,
    val subtitle: String,
    val steps: List<ComboInputStep>
)

enum class FighterVisualStyle {
    SHOTO_FLAME,
    CYBER_NINJA,
    STEEL_COLOSSUS,
    WIND_MAGE,
    NEON_RONIN
}

/**
 * Modèle centralisé d'un combattant.
 * Pour modifier les stats ou dégâts d'un personnage, il suffit d'éditer son entrée dans [FighterRoster].
 */
data class FighterDefinition(
    val id: String,
    val name: String,
    val title: String,
    val archetype: String,
    val lore: String,
    val primaryColor: Color,
    val accentColor: Color,
    val visualStyle: FighterVisualStyle,
    val stats: FighterStats,
    val classicMove: MoveSpec,
    val baseAbility: MoveSpec,
    val ultimateMove: MoveSpec,
    val comboChallenges: List<ComboChallenge>
)

data class ArenaStage(
    val id: String,
    val name: String,
    val subtitle: String,
    @DrawableRes val backgroundRes: Int,
    val floorColor: Color,
    val accentGlow: Color
)

/**
 * Registre officiel et équilibré des personnages et arènes de Clash Arena.
 * Somme des ratings (PV + ATK + VIT + DEF + PORTÉE) = 300 points pour chaque combattant.
 */
object FighterRoster {

    val stages: List<ArenaStage> = listOf(
        ArenaStage(
            id = "cyber_street",
            name = "Rue Néo-Dojo",
            subtitle = "Asphalte humide & néons d'arcade",
            backgroundRes = R.drawable.img_hero_arena,
            floorColor = Color(0xFF141B2D),
            accentGlow = Color(0xFF00E5FF)
        ),
        ArenaStage(
            id = "sunset_dojo",
            name = "Temple du Crépuscule",
            subtitle = "Toit sacré & cerisiers en fleurs",
            backgroundRes = R.drawable.img_arena_dojo,
            floorColor = Color(0xFF28191E),
            accentGlow = Color(0xFFFF6D00)
        )
    )

    val fighters: List<FighterDefinition> = listOf(
        // 1. RYUJI - Équilibré (Shoto classique) -> Budget = 60+60+60+60+60 = 300
        FighterDefinition(
            id = "ryuji",
            name = "Ryuji",
            title = "Flamme Rouge",
            archetype = "Équilibré",
            lore = "Maître des arts martiaux de rue, redoutable à toute distance grâce à sa boule de feu.",
            primaryColor = Color(0xFFFF5722),
            accentColor = Color(0xFFFFD54F),
            visualStyle = FighterVisualStyle.SHOTO_FLAME,
            stats = FighterStats(
                maxHp = 1050,
                moveSpeed = 0.48f,
                jumpPower = 1.42f,
                defensePercent = 10,
                ratingHp = 60,
                ratingAttack = 60,
                ratingSpeed = 60,
                ratingDefense = 60,
                ratingRange = 60
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Poing Éclair",
                description = "Direct rapide enflammé qui enchaîne facilement avec la capacité.",
                damage = 68,
                cooldownMs = 420L,
                range = 0.17f,
                startupMs = 90L,
                activeMs = 220L,
                knockback = 0.04f,
                ultiGainOnHit = 16,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Hadou-Flamme",
                description = "Projette une sphère d'énergie brûlante à travers toute l'arène.",
                damage = 115,
                cooldownMs = 2400L,
                range = 0.85f,
                startupMs = 160L,
                activeMs = 360L,
                knockback = 0.09f,
                ultiGainOnHit = 22,
                effectType = MoveEffectType.PROJECTILE
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Dragon Solaire",
                description = "Uppercut ascendant dévastateur suivi d'une explosion de flammes.",
                damage = 285,
                cooldownMs = 1000L,
                range = 0.25f,
                startupMs = 150L,
                activeMs = 580L,
                knockback = 0.18f,
                ultiGainOnHit = 0,
                ultiCost = 100,
                effectType = MoveEffectType.ULTI_UPPERCUT
            ),
            comboChallenges = listOf(
                ComboChallenge(
                    id = "ryuji_c1",
                    title = "Enchaînement Hadou",
                    subtitle = "Coup Classique puis Hadou-Flamme à l'impact",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY)
                ),
                ComboChallenge(
                    id = "ryuji_c2",
                    title = "Éveil du Dragon",
                    subtitle = "Saut offensif, Classique, puis Dragon Solaire",
                    steps = listOf(ComboInputStep.JUMP_CLASSIC, ComboInputStep.CLASSIC, ComboInputStep.ULTI)
                )
            )
        ),

        // 2. KAELITH - Assassin rapide -> Budget = 45+65+92+43+55 = 300
        FighterDefinition(
            id = "kaelith",
            name = "Kaelith",
            title = "Ombre Cyber",
            archetype = "Vitesse",
            lore = "Ninja cybernétique ultra-mobile capable de punir la moindre ouverture.",
            primaryColor = Color(0xFF00E5FF),
            accentColor = Color(0xFFE040FB),
            visualStyle = FighterVisualStyle.CYBER_NINJA,
            stats = FighterStats(
                maxHp = 920,
                moveSpeed = 0.62f,
                jumpPower = 1.54f,
                defensePercent = 5,
                ratingHp = 45,
                ratingAttack = 65,
                ratingSpeed = 92,
                ratingDefense = 43,
                ratingRange = 55
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Lame Furtive",
                description = "Frappe double très rapide avec un temps de récupération minimal.",
                damage = 58,
                cooldownMs = 310L,
                range = 0.16f,
                startupMs = 60L,
                activeMs = 170L,
                knockback = 0.03f,
                ultiGainOnHit = 15,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Pas Fantôme",
                description = "Ruée fulgurante vers l'avant qui tranche l'adversaire.",
                damage = 110,
                cooldownMs = 2100L,
                range = 0.34f,
                startupMs = 90L,
                activeMs = 280L,
                knockback = 0.07f,
                ultiGainOnHit = 22,
                effectType = MoveEffectType.DASH_STRIKE
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Tempête de Kunais",
                description = "Assaut éclair en chaîne infligeant une rafale de coups critiques.",
                damage = 275,
                cooldownMs = 1000L,
                range = 0.40f,
                startupMs = 110L,
                activeMs = 540L,
                knockback = 0.16f,
                ultiGainOnHit = 0,
                ultiCost = 100,
                effectType = MoveEffectType.ULTI_RUSH
            ),
            comboChallenges = listOf(
                ComboChallenge(
                    id = "kaelith_c1",
                    title = "Frappe Fantôme",
                    subtitle = "Classique rapide enchaîné avec Pas Fantôme",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY)
                ),
                ComboChallenge(
                    id = "kaelith_c2",
                    title = "Exécution Cyber",
                    subtitle = "Pas Fantôme suivi d'un Classique et de l'Ultime",
                    steps = listOf(ComboInputStep.BASE_ABILITY, ComboInputStep.CLASSIC, ComboInputStep.ULTI)
                )
            )
        ),

        // 3. TITAN - Tank / Colosse -> Budget = 92+75+33+75+25 = 300
        FighterDefinition(
            id = "titan",
            name = "Titan",
            title = "Colosse d'Acier",
            archetype = "Tank",
            lore = "Goliath blindé au cœur d'acier. Lent, mais chaque coup fait trembler le sol.",
            primaryColor = Color(0xFFFFB300),
            accentColor = Color(0xFFFF3D00),
            visualStyle = FighterVisualStyle.STEEL_COLOSSUS,
            stats = FighterStats(
                maxHp = 1280,
                moveSpeed = 0.36f,
                jumpPower = 1.28f,
                defensePercent = 20,
                ratingHp = 92,
                ratingAttack = 75,
                ratingSpeed = 33,
                ratingDefense = 75,
                ratingRange = 25
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Marteau-Poing",
                description = "Coup lourd dévastateur à fort recul.",
                damage = 90,
                cooldownMs = 560L,
                range = 0.18f,
                startupMs = 130L,
                activeMs = 280L,
                knockback = 0.08f,
                ultiGainOnHit = 19,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Onde Sismique",
                description = "Frappe le sol et génère une secousse qui projette l'ennemi.",
                damage = 138,
                cooldownMs = 2800L,
                range = 0.30f,
                startupMs = 190L,
                activeMs = 380L,
                knockback = 0.13f,
                ultiGainOnHit = 24,
                effectType = MoveEffectType.SHOCKWAVE
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Météore Titanesque",
                description = "Bond écrasant libérant une déflagration sismique colossale.",
                damage = 320,
                cooldownMs = 1000L,
                range = 0.34f,
                startupMs = 190L,
                activeMs = 620L,
                knockback = 0.22f,
                ultiGainOnHit = 0,
                ultiCost = 100,
                effectType = MoveEffectType.ULTI_SLAM
            ),
            comboChallenges = listOf(
                ComboChallenge(
                    id = "titan_c1",
                    title = "Impact Sismique",
                    subtitle = "Marteau-Poing puis Onde Sismique",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY)
                ),
                ComboChallenge(
                    id = "titan_c2",
                    title = "Chute du Colosse",
                    subtitle = "Onde Sismique puis Météore Titanesque",
                    steps = listOf(ComboInputStep.BASE_ABILITY, ComboInputStep.ULTI)
                )
            )
        ),

        // 4. SORA - Zoneuse / Contrôle -> Budget = 48+58+62+42+90 = 300
        FighterDefinition(
            id = "sora",
            name = "Sora",
            title = "Tempête Azur",
            archetype = "Zoneur",
            lore = "Maîtresse des vents et de la foudre, elle contrôle l'espace à longue portée.",
            primaryColor = Color(0xFF40C4FF),
            accentColor = Color(0xFF7C4DFF),
            visualStyle = FighterVisualStyle.WIND_MAGE,
            stats = FighterStats(
                maxHp = 940,
                moveSpeed = 0.50f,
                jumpPower = 1.48f,
                defensePercent = 6,
                ratingHp = 48,
                ratingAttack = 58,
                ratingSpeed = 62,
                ratingDefense = 42,
                ratingRange = 90
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Éventail Rasoir",
                description = "Fente venteuse de moyenne portée pour garder l'adversaire à distance.",
                damage = 64,
                cooldownMs = 430L,
                range = 0.21f,
                startupMs = 95L,
                activeMs = 230L,
                knockback = 0.05f,
                ultiGainOnHit = 16,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Tornade d'Azur",
                description = "Lance un tourbillon tranchant qui repousse l'ennemi.",
                damage = 112,
                cooldownMs = 2300L,
                range = 0.88f,
                startupMs = 150L,
                activeMs = 340L,
                knockback = 0.12f,
                ultiGainOnHit = 22,
                effectType = MoveEffectType.PROJECTILE
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Foudre Céleste",
                description = "Invoque un pilier d'éclairs surpuissant devant elle à grande portée.",
                damage = 280,
                cooldownMs = 1000L,
                range = 0.52f,
                startupMs = 160L,
                activeMs = 580L,
                knockback = 0.16f,
                ultiGainOnHit = 0,
                ultiCost = 100,
                effectType = MoveEffectType.ULTI_PILLAR
            ),
            comboChallenges = listOf(
                ComboChallenge(
                    id = "sora_c1",
                    title = "Bourrasque",
                    subtitle = "Éventail Rasoir suivi de Tornade d'Azur",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY)
                ),
                ComboChallenge(
                    id = "sora_c2",
                    title = "Orage Divin",
                    subtitle = "Classique, Tornade, puis Foudre Céleste",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY, ComboInputStep.ULTI)
                )
            )
        ),

        // 5. VEX - Dueliste / Contre -> Budget = 58+68+64+52+58 = 300
        FighterDefinition(
            id = "vex",
            name = "Vex",
            title = "Ronin Néon",
            archetype = "Dueliste",
            lore = "Sabreur solitaire armé d'un katana plasma, expert en percées précises.",
            primaryColor = Color(0xFFE040FB),
            accentColor = Color(0xFF00E5FF),
            visualStyle = FighterVisualStyle.NEON_RONIN,
            stats = FighterStats(
                maxHp = 1020,
                moveSpeed = 0.52f,
                jumpPower = 1.44f,
                defensePercent = 11,
                ratingHp = 58,
                ratingAttack = 68,
                ratingSpeed = 64,
                ratingDefense = 52,
                ratingRange = 58
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Estocade Néon",
                description = "Coup de katana vif avec une excellente allonge.",
                damage = 72,
                cooldownMs = 430L,
                range = 0.20f,
                startupMs = 85L,
                activeMs = 210L,
                knockback = 0.04f,
                ultiGainOnHit = 16,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Entaille Éclair",
                description = "Arc tranchant d'énergie plasma qui perce la garde partiellement.",
                damage = 122,
                cooldownMs = 2500L,
                range = 0.32f,
                startupMs = 120L,
                activeMs = 300L,
                knockback = 0.08f,
                ultiGainOnHit = 23,
                effectType = MoveEffectType.COUNTER_WAVE
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Jugement Astral",
                description = "Iaido foudroyant traversant l'arène en une ligne dimensionnelle.",
                damage = 290,
                cooldownMs = 1000L,
                range = 0.62f,
                startupMs = 130L,
                activeMs = 560L,
                knockback = 0.17f,
                ultiGainOnHit = 0,
                ultiCost = 100,
                effectType = MoveEffectType.ULTI_SLASH
            ),
            comboChallenges = listOf(
                ComboChallenge(
                    id = "vex_c1",
                    title = "Lame Plasma",
                    subtitle = "Estocade Néon puis Entaille Éclair",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY)
                ),
                ComboChallenge(
                    id = "vex_c2",
                    title = "Iaido Final",
                    subtitle = "Classique, Entaille Éclair, puis Jugement Astral",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY, ComboInputStep.ULTI)
                )
            )
        )
    )

    fun getFighter(id: String): FighterDefinition =
        fighters.firstOrNull { it.id == id } ?: fighters.first()

    fun getStage(id: String): ArenaStage =
        stages.firstOrNull { it.id == id } ?: stages.first()
}
