package app.termosh.feature.terminal.startup

import app.termosh.domain.model.Server
import app.termosh.domain.repository.SecretCodec
import app.termosh.feature.terminal.input.InputComposer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Превращает Server.envVars / envSecrets / startupCommands
 * в последовательность шагов с задержками. Сама ничего не пишет в PTY.
 */
@Singleton
class StartupScriptRunner @Inject constructor(
    private val codec: SecretCodec,
) {

    data class Step(val text: String, val delayMs: Long)

    suspend fun buildSteps(server: Server): List<Step> {
        val hasAny = server.envVars.isNotEmpty() ||
            server.envSecrets.isNotEmpty() ||
            server.startupCommands.isNotEmpty()
        if (!hasAny) return emptyList()

        val steps = mutableListOf<Step>()

        server.envVars.forEach { (k, v) ->
            steps += Step("export $k=" + InputComposer.escapeShellValue(v) + "\n", 80L)
        }

        server.envSecrets.forEach { (k, ref) ->
            val plain = runCatching { codec.decryptString(ref) }.getOrNull()
            if (plain != null) {
                steps += Step("export $k=" + InputComposer.escapeShellValue(plain) + "\n", 80L)
            }
        }

        server.startupCommands.forEach { cmd ->
            steps += Step(cmd + "\n", 200L)
        }

        return steps
    }
}
