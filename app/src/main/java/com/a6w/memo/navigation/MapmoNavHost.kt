package com.a6w.memo.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.a6w.memo.route.home.ui.HomeScreen
import com.a6w.memo.route.mapmo.ui.MapmoScreen
import com.a6w.memo.route.setting.SettingScreen

/**
 * Mapmo Nav Host
 * - Manage navigation for each screen
 * - Each routes are defined at [MapmoNavRoute] as data class
 */


// ——— Constants ———————————————————————————————————————————

private const val TRANSITION_DURATION_MS = 220
private const val TRANSITION_EXIT_DURATION_MS = 90
private const val TRANSITION_DELAY_MS = 90

// ——— Transitions —————————————————————————————————————————

private val navEnterTransition = fadeIn(tween(TRANSITION_DURATION_MS, delayMillis = TRANSITION_DELAY_MS))
private val navExitTransition = fadeOut(tween(TRANSITION_EXIT_DURATION_MS))

// ——— NavHost —————————————————————————————————————————————
@Composable
fun MapmoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = MapmoNavRoute.Home,
        enterTransition = { navEnterTransition },
        exitTransition = { navExitTransition },
        popEnterTransition = { navEnterTransition },
        popExitTransition = { navExitTransition },
    ) {
        // Home
        composable<MapmoNavRoute.Home> {
            HomeScreenNav(
                modifier = Modifier,
                navigateToMapmo = { mapmoID: String? ->
                    val route = MapmoNavRoute.Mapmo(
                        mapmoID = mapmoID
                    )
                    navController.navigate(route)
                },
                navigateToSetting = { navController.navigate(MapmoNavRoute.Setting) },
            )
        }

        // Mapmo
        composable<MapmoNavRoute.Mapmo> {
            MapmoScreenNav(
                modifier = Modifier,
                navigationPop = { navController.popBackStack() },
            )
        }

        // Setting
        composable<MapmoNavRoute.Setting> {
            SettingScreenNav(
                modifier = Modifier,
                navigationPop = { navController.popBackStack() },
            )
        }
    }
}

// Home Screen Nav
@Composable
private fun HomeScreenNav(
    modifier: Modifier = Modifier,
    navigateToMapmo: (mapmoID: String?) -> Unit,
    navigateToSetting: () -> Unit,
) {
    // Home Screen
    HomeScreen(
        modifier = modifier
            .fillMaxSize(),
        navigateToMapmo = navigateToMapmo,
        navigateToSetting = navigateToSetting,
    )
}

// Mapmo Screen Nav
@Composable
private fun MapmoScreenNav(
    modifier: Modifier = Modifier,
    navigationPop: () -> Unit,
) {
    // Mapmo Screen
    MapmoScreen(
        modifier = modifier
            .fillMaxSize(),
        navigationPop = navigationPop,
    )
}

// Setting Screen Nav
@Composable
private fun SettingScreenNav(
    modifier: Modifier = Modifier,
    navigationPop: () -> Unit,
) {
    // Setting Screen
    SettingScreen(
        modifier = modifier
            .fillMaxSize(),
        navigationPop = navigationPop,
    )
}