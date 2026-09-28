package app.termosh.core.common.region

data class ScoreResult(
    val score: Int,
    val signals: Map<String, Int>,
    val granted: Boolean,
    val reason: String,
)
