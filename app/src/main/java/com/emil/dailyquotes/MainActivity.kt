package com.emil.dailyquotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
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
                // A surface container using the 'background' color from the theme
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
                                direction = if(initialState.destination.route == "home") DIRECTION_RIGHT else DIRECTION_LEFT,
                                orientation = orientation) },
                            exitTransition = { navExitTransition(
                                direction = if(initialState.destination.route == "home") DIRECTION_RIGHT else DIRECTION_LEFT,
                                orientation = orientation) },
                            content = { SettingsPage() }
                        )
                    }
                }
            }
        }
    }

    fun navigateTo(route: String){
        pageNavController?.let{ controller ->
            if(currentPage != route){
                controller.navigate(route)
            }
        }
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Text(
            text = "This is the base Daily Quotes app.\nWe'll start from here.",
            modifier = modifier,
            textAlign = TextAlign.Center
        )
    }
}