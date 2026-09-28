package app.termosh.domain.usecase.knownhost

import app.termosh.domain.model.KnownHost
import app.termosh.domain.repository.KnownHostRepository

/**
 * Реализует логику TOFU на доменном уровне.
 *
 * Возвращает:
 *  - AcceptFirstTime — ключ увиден впервые, сохранён.
 *  - Accept — отпечаток совпал с сохранённым.
 *  - Reject — отпечаток не совпал, потенциальная MITM-атака.
 */
class VerifyHostKeyUseCase(
    private val repository: KnownHostRepository,
) {
    sealed interface Result {
        data object AcceptFirstTime : Result
        data object Accept : Result
        data class Reject(val expected: String, val actual: String) : Result
    }

    suspend operator fun invoke(host: String, port: Int, fingerprintSha256: String): Result {
        val existing = repository.get(host, port)
        return when {
            existing == null -> {
                repository.save(
                    KnownHost(
                        host = host,
                        port = port,
                        fingerprintSha256 = fingerprintSha256,
                        addedAt = System.currentTimeMillis(),
                    ),
                )
                Result.AcceptFirstTime
            }
            existing.fingerprintSha256 == fingerprintSha256 -> Result.Accept
            else -> Result.Reject(
                expected = existing.fingerprintSha256,
                actual = fingerprintSha256,
            )
        }
    }
}
