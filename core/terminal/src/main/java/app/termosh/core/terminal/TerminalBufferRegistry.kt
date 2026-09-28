package app.termosh.core.terminal

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TerminalBufferRegistry @Inject constructor() {

    private val screens = mutableMapOf<String, TermScreen>()

    @Synchronized
    fun get(id: String): TermScreen = screens.getOrPut(id) { TermScreen() }

    @Synchronized
    fun remove(id: String) { screens.remove(id) }

    @Synchronized
    fun clear() { screens.clear() }
}
