package app.termosh.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import app.termosh.core.service.SessionControl
import app.termosh.feature.servers.navigation.ServersRoutes
import app.termosh.feature.servers.navigation.serversGraph
import app.termosh.feature.settings.SettingsRoutes
import app.termosh.feature.settings.settingsGraph
import app.termosh.feature.terminal.TerminalRoutes
import app.termosh.feature.terminal.terminalGraph

@Composable
fun TermoshNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = ServersRoutes.LIST,
    ) {
        serversGraph(
            navController = navController,
            onConnectServer = { id -> navController.navigate(TerminalRoutes.route(id)) },
            onOpenSettings = { navController.navigate(SettingsRoutes.ROUTE) },
            onOpenLicense = { navController.navigate(SettingsRoutes.LICENSE) },
        )
        terminalGraph(navController)
        settingsGraph(navController)
    }
}
