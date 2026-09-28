package app.termosh.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val command: String,
    val sortOrder: Int = 0,
    val createdAt: Long,
)
