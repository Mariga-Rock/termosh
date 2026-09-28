package app.termosh.domain.model

data class Snippet(
    val id: String,
    val name: String,
    val command: String,
    val sortOrder: Int,
    val createdAt: Long,
)
