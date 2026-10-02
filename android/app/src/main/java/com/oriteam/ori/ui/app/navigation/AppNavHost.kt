package com.oriteam.ori.ui.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oriteam.ori.ui.app.screens.home.HomeRoute
import com.oriteam.ori.ui.app.screens.register.RegisterRoute


@Composable
fun AppNavHost (
    navController: NavHostController = rememberNavController()
){
    NavHost(navController = navController, startDestination = Home){
        composable<Home> {
            HomeRoute(
                onNavigateToRegister = {navController.navigate(Register)}
            )
        }
        composable<Register> {
            RegisterRoute(
                onRegisterSuccess = {
                    navController.navigate(Home) {
                        popUpTo(Register) { inclusive = true } // remove o Register do back stack
                    }
                }
            )
        }
    }
}