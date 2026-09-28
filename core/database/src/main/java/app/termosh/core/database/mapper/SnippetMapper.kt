package app.termosh.core.database.mapper

import app.termosh.core.database.entity.SnippetEntity
import app.termosh.domain.model.Snippet

object SnippetMapper {

    fun toDomain(entity: SnippetEntity): Snippet = Snippet(
        id = entity.id,
        name = entity.name,
        command = entity.command,
        sortOrder = entity.sortOrder,
        createdAt = entity.createdAt,
    )

    fun toEntity(domain: Snippet): SnippetEntity = SnippetEntity(
        id = domain.id,
        name = domain.name,
        command = domain.command,
        sortOrder = domain.sortOrder,
        createdAt = domain.createdAt,
    )
}
