package com.example.game.data

enum class BotDifficulty(
    val label: String,
    val reactionIntervalMs: Long,
    val aggression: Float,
    val blockChance: Float
) {
    EASY("Facile", 540L, 0.42f, 0.15f),
    NORMAL("Normal", 300L, 0.68f, 0.36f),
    HARD("Difficile", 160L, 0.88f, 0.62f)
}

enum class TrainingOpponentMode(val label: String) {
    STATIONARY("Stationnaire"),
    AUTO_BLOCK("Garde Auto"),
    AI_BOT("Bot IA")
}

enum class GameMode {
    VS_BOT,
    TRAINING,
    ONLINE
}
