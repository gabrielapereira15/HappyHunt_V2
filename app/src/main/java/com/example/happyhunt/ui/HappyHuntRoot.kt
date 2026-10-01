package com.example.happyhunt.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.happyhunt.AppContainer
import com.example.happyhunt.R
import com.example.happyhunt.data.Settings
import com.example.happyhunt.ui.explore.ExploreScreen
import com.example.happyhunt.ui.explore.ExploreViewModel
import com.example.happyhunt.ui.place.PlaceScreen
import com.example.happyhunt.ui.place.PlaceViewModel
import com.example.happyhunt.ui.saved.SavedScreen
import com.example.happyhunt.ui.saved.SavedViewModel
import com.example.happyhunt.ui.search.AreaSearchScreen
import com.example.happyhunt.ui.search.AreaSearchViewModel
import com.example.happyhunt.ui.settings.SettingsScreen
import com.example.happyhunt.ui.settings.SettingsViewModel
import com.example.happyhunt.ui.theme.Hunt
import com.example.happyhunt.ui.theme.HuntIcons
import com.example.happyhunt.ui.welcome.WelcomeScreen
import kotlinx.serialization.Serializable

@Serializable data object WelcomeRoute

@Serializable data class AreaSearchRoute(val fromWelcome: Boolean = false)

@Serializable data object ExploreRoute

@Serializable data object SavedRoute

@Serializable data object SettingsRoute

@Serializable data class PlaceRoute(val id: String)

private data class Tab(val route: Any, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(ExploreRoute, R.string.tab_explore, HuntIcons.Explore),
    Tab(SavedRoute, R.string.tab_saved, HuntIcons.Heart),
    Tab(SettingsRoute, R.string.tab_settings, HuntIcons.Settings),
)

@Composable
fun HappyHuntRoot(container: AppContainer, settings: Settings) {
    val nav = rememberNavController()
    val start: Any = remember { if (settings.origin == null) WelcomeRoute else ExploreRoute }
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val onTab = destination != null && tabs.any { tab -> destination.hierarchy.any { it.hasRoute(tab.route::class) } }

    Scaffold(
        containerColor = Hunt.colors.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(visible = onTab, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    HorizontalDivider(color = Hunt.colors.line)
                    NavigationBar(containerColor = Hunt.colors.surface, tonalElevation = 0.dp) {
                        tabs.forEach { tab ->
                            val selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { nav.openTab(tab.route) },
                                icon = { Icon(if (selected && tab.route == SavedRoute) HuntIcons.HeartFilled else tab.icon, contentDescription = null) },
                                label = { Text(stringResource(tab.label)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Hunt.colors.primary,
                                    selectedTextColor = Hunt.colors.primary,
                                    indicatorColor = Hunt.colors.primarySoft,
                                    unselectedIconColor = Hunt.colors.inkMuted,
                                    unselectedTextColor = Hunt.colors.inkMuted,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
        ) {
            composable<WelcomeRoute> {
                WelcomeScreen(
                    container = container,
                    onReady = { nav.navigate(ExploreRoute) { popUpTo<WelcomeRoute> { inclusive = true } } },
                    onChooseArea = { nav.navigate(AreaSearchRoute(fromWelcome = true)) },
                )
            }
            composable<AreaSearchRoute>(
                enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
                exitTransition = { ExitTransition.None },
                popExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() },
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<AreaSearchRoute>()
                AreaSearchScreen(
                    viewModel = viewModel { AreaSearchViewModel(container) },
                    container = container,
                    onBack = { nav.popBackStack() },
                    onDone = {
                        if (route.fromWelcome) nav.navigate(ExploreRoute) { popUpTo<WelcomeRoute> { inclusive = true } }
                        else nav.popBackStack()
                    },
                )
            }
            composable<ExploreRoute> {
                ExploreScreen(
                    viewModel = viewModel { ExploreViewModel(container) },
                    onOpenPlace = { nav.navigate(PlaceRoute(it)) },
                    onChangeArea = { nav.navigate(AreaSearchRoute()) },
                )
            }
            composable<SavedRoute> {
                SavedScreen(
                    viewModel = viewModel { SavedViewModel(container) },
                    onOpenPlace = { nav.navigate(PlaceRoute(it)) },
                    onExplore = { nav.openTab(ExploreRoute) },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(viewModel = viewModel { SettingsViewModel(container) })
            }
            composable<PlaceRoute>(
                enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() },
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<PlaceRoute>()
                PlaceScreen(
                    viewModel = viewModel(key = route.id) { PlaceViewModel(container, route.id) },
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.openTab(route: Any) {
    navigate(route) {
        popUpTo<ExploreRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
