package app.termosh.feature.servers.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.termosh.feature.servers.edit.ServerEditScreen
import app.termosh.feature.servers.forwards.ForwardsScreen
import app.termosh.feature.servers.imports.ExportScreen
import app.termosh.feature.servers.imports.ImportScreen
import app.termosh.feature.servers.keys.KeysListScreen
import app.termosh.feature.servers.list.ServersScreen
import app.termosh.feature.servers.sftp.SftpScreen
import app.termosh.feature.servers.snippets.SnippetsListScreen
import app.termosh.feature.servers.totp.TotpScreen

object ServersRoutes {
    const val LIST = "servers"
    const val EDIT_NEW = "servers/edit"
    const val EDIT_ARG_ID = "serverId"
    const val EDIT_EXISTING = "servers/edit/{$EDIT_ARG_ID}"
    const val KEYS = "servers/keys"
    const val SNIPPETS = "servers/snippets"
    const val TOTP = "servers/totp"
    const val IMPORT = "servers/import"
    const val EXPORT = "servers/export"

    const val FORWARDS_ARG_ID = "serverId"
    const val FORWARDS = "servers/forwards/{$FORWARDS_ARG_ID}"
    fun forwards(serverId: String): String = "servers/forwards/$serverId"

    const val SFTP_ARG_ID = "serverId"
    const val SFTP = "servers/sftp/{$SFTP_ARG_ID}"
    fun sftp(serverId: String): String = "servers/sftp/$serverId"

    fun editNew(): String = EDIT_NEW
    fun editExisting(serverId: String): String = "servers/edit/$serverId"
}

fun NavGraphBuilder.serversGraph(
    navController: NavController,
    onConnectServer: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLicense: () -> Unit,
) {
    composable(ServersRoutes.LIST) {
        ServersScreen(
            onAddServer = { navController.navigate(ServersRoutes.editNew()) },
            onEditServer = { id -> navController.navigate(ServersRoutes.editExisting(id)) },
            onConnectServer = onConnectServer,
            onOpenKeys = { navController.navigate(ServersRoutes.KEYS) },
            onOpenSnippets = { navController.navigate(ServersRoutes.SNIPPETS) },
            onOpenForwards = { id -> navController.navigate(ServersRoutes.forwards(id)) },
            onOpenSettings = onOpenSettings,
            onOpenLicense = onOpenLicense,
            onOpenTotp = { navController.navigate(ServersRoutes.TOTP) },
            onOpenImport = { navController.navigate(ServersRoutes.IMPORT) },
            onOpenExport = { navController.navigate(ServersRoutes.EXPORT) },
            onOpenSftp = { id -> navController.navigate(ServersRoutes.sftp(id)) },
        )
    }
    composable(ServersRoutes.EDIT_NEW) {
        ServerEditScreen(onBack = { navController.popBackStack() })
    }
    composable(
        route = ServersRoutes.EDIT_EXISTING,
        arguments = listOf(navArgument(ServersRoutes.EDIT_ARG_ID) { type = NavType.StringType }),
    ) {
        ServerEditScreen(onBack = { navController.popBackStack() })
    }
    composable(ServersRoutes.KEYS) {
        KeysListScreen(onBack = { navController.popBackStack() })
    }
    composable(ServersRoutes.SNIPPETS) {
        SnippetsListScreen(onBack = { navController.popBackStack() })
    }
    composable(ServersRoutes.TOTP) {
        TotpScreen(onBack = { navController.popBackStack() })
    }
    composable(ServersRoutes.IMPORT) {
        ImportScreen(onBack = { navController.popBackStack() })
    }
    composable(ServersRoutes.EXPORT) {
        ExportScreen(onBack = { navController.popBackStack() })
    }
    composable(
        route = ServersRoutes.FORWARDS,
        arguments = listOf(navArgument(ServersRoutes.FORWARDS_ARG_ID) { type = NavType.StringType }),
    ) {
        ForwardsScreen(onBack = { navController.popBackStack() })
    }
    composable(
        route = ServersRoutes.SFTP,
        arguments = listOf(navArgument(ServersRoutes.SFTP_ARG_ID) { type = NavType.StringType }),
    ) {
        SftpScreen(onBack = { navController.popBackStack() })
    }
}
