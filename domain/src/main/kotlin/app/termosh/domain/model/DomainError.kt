package app.termosh.domain.model

/**
 * Доменные ошибки. Инфраструктура маппит свои исключения в эти типы.
 */
sealed class DomainError(message: String) : Exception(message) {

    class NotFound(what: String) : DomainError("Not found: $what")

    class InvalidInput(reason: String) : DomainError("Invalid input: $reason")

    class Security(reason: String) : DomainError("Security: $reason")

    class Connection(reason: String) : DomainError("Connection: $reason")

    class Unknown(cause: Throwable) : DomainError(cause.message ?: "Unknown error")
}
