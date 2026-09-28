package app.termosh.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграции схемы БД.
 *
 * История версий:
 *  - 1: servers, ssh_keys, known_hosts, snippets, totp_secrets
 *  - 2: + port_forwards (внутренняя alpha)
 *  - 3: + расширенные поля servers (useMosh, totpSecretId, startupCommands,
 *        envVars, envSecrets, useJumpCredentials) (внутренняя alpha)
 *  - 4: + known_hosts_hashed
 *  - 5: + servers.useTmux (постоянные сессии)
 *
 * Версии 1–3 — pre-release alpha, никогда не публиковались. Для них
 * разрешён destructive fallback (см. fallbackToDestructiveMigrationFrom).
 * Миграции 3→4 сохраняют данные.
 */
object Migrations {

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `known_hosts_hashed` (
                    `salt` TEXT NOT NULL,
                    `hash` TEXT NOT NULL,
                    `keyType` TEXT NOT NULL,
                    `keyBase64` TEXT NOT NULL,
                    `fingerprintSha256` TEXT NOT NULL,
                    `addedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`salt`, `hash`, `keyType`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `servers` ADD COLUMN `useTmux` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL = arrayOf(MIGRATION_3_4, MIGRATION_4_5)
}
