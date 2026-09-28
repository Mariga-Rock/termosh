package app.termosh.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.termosh.feature.settings.backup.BackupScreen
import app.termosh.feature.settings.exports.ExportScreen
import app.termosh.feature.settings.logs.LogsScreen

object SettingsRoutes {
    const val ROUTE = "settings"
    const val LICENSE = "settings/license"
    const val LOGS = "settings/logs"
    const val BACKUP = "settings/backup"
    const val EXPORTS = "settings/exports"
}

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable(SettingsRoutes.ROUTE) {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onOpenLicense = { navController.navigate(SettingsRoutes.LICENSE) },
            onOpenLogs = { navController.navigate(SettingsRoutes.LOGS) },
            onOpenBackup = { navController.navigate(SettingsRoutes.BACKUP) },
            onOpenExports = { navController.navigate(SettingsRoutes.EXPORTS) },
        )
    }
    composable(SettingsRoutes.LICENSE) {
        LicenseScreen(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.LOGS) {
        LogsScreen(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.BACKUP) {
        BackupScreen(onBack = { navController.popBackStack() })
    }
    composable(SettingsRoutes.EXPORTS) {
        ExportScreen(onBack = { navController.popBackStack() })
    }
}
