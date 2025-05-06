package com.example.moodmelody.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.moodmelody.ui.screens.home.HomeScreen
import com.example.moodmelody.ui.screens.search.SearchScreen
import com.example.moodmelody.ui.screens.stats.StatsScreen
import com.example.moodmelody.ui.screens.test.TestScreen
import com.example.moodmelody.viewmodel.MusicViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Stats : Screen("stats")
    object Test : Screen("test")
}

@Composable
fun Navigation(
    navController: NavHostController,
    musicViewModel: MusicViewModel,
    padding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                viewModel = musicViewModel,
                paddingValues = padding
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                navController = navController,
                viewModel = musicViewModel,
                paddingValues = padding
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(
                navController = navController,
                paddingValues = padding
            )
        }
        composable(Screen.Test.route) {
            TestScreen(
                navController = navController
            )
        }
    }
} 