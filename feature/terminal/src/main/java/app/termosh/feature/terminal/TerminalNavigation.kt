package app.termosh.feature.terminal

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

object TerminalRoutes {
    const val ARG_SERVER_ID = "serverId"
    const val ROUTE = "terminal/{$ARG_SERVER_ID}"
    fun route(serverId: String): String = "terminal/$serverId"
}

fun NavGraphBuilder.terminalGraph(navController: NavController) {
    composable(
        route = TerminalRoutes.ROUTE,
        arguments = listOf(
            navArgument(TerminalRoutes.ARG_SERVER_ID) { type = NavType.StringType },
        ),
    ) {
        TerminalScreen(onBack = { navController.popBackStack() })
    }
}
