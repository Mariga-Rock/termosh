package app.termosh.core.common

/**
 * Runtime-флаги сборки. Устанавливаются в TermoshApplication.onCreate()
 * из ресурса R.bool.is_personal, который различается между flavor'ами
 * public и personal.
 *
 * Не использовать в местах, где значение нужно ДО onCreate Application.
 */
object BuildFlags {
    @Volatile
    var isPersonal: Boolean = false
}
