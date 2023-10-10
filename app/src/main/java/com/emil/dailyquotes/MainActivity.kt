package com.emil.dailyquotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emil.dailyquotes.ui.theme.DailyQuotesTheme

private val DIRECTION_LEFT = -1
private val DIRECTION_RIGHT = 1

var mainActivity: MainActivity? = null

class MainActivity : ComponentActivity() {

    private var pageNavController: NavHostController? = null
    private var currentPage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        mainActivity = this

        setContent {
            DailyQuotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    pageNavController = rememberNavController()
                    currentPage = currentRoute(navController = pageNavController!!)
                    val orientation = LocalConfiguration.current.orientation

                    NavHost(navController = pageNavController!!, startDestination = "home"){
                        composable(
                            route = "home",
                            enterTransition = { navEnterTransition(direction = DIRECTION_LEFT, orientation = orientation) },
                            exitTransition = { navExitTransition(direction = DIRECTION_LEFT, orientation = orientation) },
                            content = { HomePage() }
                        )
                        composable(
                            route = "settings",
                            enterTransition = { navEnterTransition(
                                direction = getNavEnterDirection(initialState.destination),
                                orientation = orientation) },
                            exitTransition = { navExitTransition(
                                direction = getNavExitDirection(initialState.destination),
                                orientation = orientation) },
                            content = { SettingsPage() }
                        )
                        composable(
                            route = "login",
                            enterTransition = { navEnterTransition(
                                direction = getNavEnterDirection(initialState.destination),
                                orientation = orientation) },
                            exitTransition = { navExitTransition(
                                direction = getNavExitDirection(initialState.destination),
                                orientation = orientation) },
                            content = { LoginPage() }
                        )
                        composable(
                            route = "account",
                            enterTransition = { navEnterTransition(
                                direction = getNavEnterDirection(initialState.destination),
                                orientation = orientation) },
                            exitTransition = { navExitTransition(
                                direction = getNavExitDirection(initialState.destination),
                                orientation = orientation) },
                            content = { AccountPage() }
                        )
                    }
                }
            }
        }
    }

    private fun getNavEnterDirection(initialDestination: NavDestination): Int{

        return if(pageNavController?.previousBackStackEntry?.destination?.route == initialDestination.route){
            DIRECTION_RIGHT
        }else{
            DIRECTION_LEFT
        }
    }

    private fun getNavExitDirection(initialDestination: NavDestination): Int{

        val currentBackStack = pageNavController?.currentBackStack?.value

        return if(currentBackStack?.get(currentBackStack.size - 2)?.destination?.route == initialDestination.route){
            DIRECTION_LEFT
        }else{
            DIRECTION_RIGHT
        }
    }

    fun navigateTo(route: String){
        pageNavController?.let{ controller ->
            if(currentPage != route){
                controller.navigate(route)
            }
        }
    }

    fun back(){
        pageNavController?.popBackStack()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(modifier: Modifier = Modifier){

    val navController = rememberNavController()
    val currentRoute = currentRoute(navController = navController)
    val orientation = LocalConfiguration.current.orientation

    Scaffold(
        modifier = modifier,
        bottomBar = {

            val navigationItems = listOf(BottomNavigationItem.HomeScreenItem, BottomNavigationItem.ProfileScreenItem)

            NavigationBar {
                navigationItems.forEachIndexed{ index, item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if(currentRoute != item.route){
                                navController.navigate(item.route){
                                    launchSingleTop = true
                                    popUpTo("home")
                                }
                        }},
                        icon = { Icon(painterResource(id = item.icon), item.label) },
                        label = { Text(text = item.label) }
                    )
                }
            }
        }
    ){ paddingValues ->
        NavHost(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            startDestination = "home"
        ){
            composable(
                route = "home",
                enterTransition = { navEnterTransition(direction = DIRECTION_LEFT, orientation = orientation) },
                exitTransition = { navExitTransition(direction = DIRECTION_LEFT, orientation = orientation) },
                content = { HomeScreen() }
            )
            composable(
                route = "profile",
                enterTransition = { navEnterTransition(direction = DIRECTION_RIGHT, orientation = orientation) },
                exitTransition = { navExitTransition(direction = DIRECTION_RIGHT, orientation = orientation) },
                content = { ProfilePage() }
            )
        }
    }
}

@Composable
fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}