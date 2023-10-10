package com.emil.dailyquotes

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emil.dailyquotes.ui.theme.DailyQuotesTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.pow

private val USE_PAGER_EXPERIMENTAL = false

private val DIRECTION_LEFT = -1
private val DIRECTION_RIGHT = 1

var mainActivity: MainActivity? = null
var firebaseManager: FirebaseManager? = null

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    private lateinit var pageNavController: NavHostController
    private var currentPage: String? = null

    private var pages = mutableStateListOf("home")
    @OptIn(ExperimentalFoundationApi::class)
    private lateinit var pagerState: PagerState
    private lateinit var composableCoroutineScope: CoroutineScope
    private lateinit var callback: OnBackPressedCallback

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        mainActivity = this
        firebaseManager = FirebaseManager(this)

        setContent {

            window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

            DailyQuotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    if(USE_PAGER_EXPERIMENTAL){
                        getNavPager()
                    }else{
                        getNavHost()
                    }

                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun getNavPager(){

        var pagerPages = remember { pages }

        var userInputEnabled by remember { mutableStateOf(true) }

        var progress by remember { mutableFloatStateOf(0f) }
        val offset: Float by animateFloatAsState(targetValue = (100 * progress), label = "")

        callback = object : OnBackPressedCallback(enabled = false){

            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                super.handleOnBackStarted(backEvent)
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                if(pages.size == 1){
                    super.handleOnBackProgressed(backEvent)
                }else {
                    progress = backEvent.progress.toDouble().pow(.2).toFloat()
                }
            }

            override fun handleOnBackPressed() {
                if(pages.size == 1){
                }else {
                    progress = 0f
                    userInputEnabled = false
                    back()
                }
            }

            override fun handleOnBackCancelled() {
                if(pages.size == 1){
                    super.handleOnBackCancelled()
                }else {
                    progress = 0f
                }
            }

        }

        onBackPressedDispatcher.addCallback(callback)
        callback.isEnabled = true

        //val pagerPages = pages.observeAsState()

        pagerState = rememberPagerState(
            pageCount = { pagerPages.size }
        )
        LaunchedEffect(pagerState){
            snapshotFlow { pagerState.currentPageOffsetFraction }.collect{ currentOffset ->
                val currentPage = pagerState.currentPage
                val previousPage = pagerState.settledPage
                if(currentOffset < 0.001 && currentPage < previousPage && pages.lastIndex > currentPage){
                    while(pages.size > currentPage + 1){
                        pages.removeAt(currentPage + 1)
                    }
                    userInputEnabled = true
                }
                if(currentPage == 0 && previousPage != 0){
                    callback.isEnabled = false
                }
            }
        }

        composableCoroutineScope = rememberCoroutineScope()

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false
        ) { page ->
            val pageModifier = Modifier
                .graphicsLayer {
                    val pageOffset = (
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            ).absoluteValue
                    alpha = 1f - pageOffset.coerceIn(0f, 1f)
                }
                .offset(x = offset.dp)
            when(pagerPages[page]){
                "home" -> {
                    HomePage(modifier = pageModifier)
                }
                "settings" -> {
                    SettingsPage(modifier = pageModifier)
                }
                "account" -> {
                    AccountPage(modifier = pageModifier)
                }
                "login" -> {
                    LoginPage(modifier = pageModifier)
                }
            }
        }

        if(!userInputEnabled){
            Box(modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {})
        }

    }

    @Composable
    fun getNavHost(){

        pageNavController = rememberNavController()
        currentPage = currentRoute(navController = pageNavController!!)
        val orientation = LocalConfiguration.current.orientation

        NavHost(
            navController = pageNavController!!,
            startDestination = "home",
        ){
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
            composable(
                route = "db_manager",
                enterTransition = { navEnterTransition(
                    direction = getNavEnterDirection(initialState.destination),
                    orientation = orientation) },
                exitTransition = { navExitTransition(
                    direction = getNavExitDirection(initialState.destination),
                    orientation = orientation) },
                content = { DBManagerPage() }
            )
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

    @OptIn(ExperimentalFoundationApi::class)
    fun navigateTo(route: String){
        if(USE_PAGER_EXPERIMENTAL) {
            pages.add(route)
            composableCoroutineScope.launch {
                pagerState.animateScrollToPage(pages.lastIndex)
            }
        }else{
            pageNavController?.let{ controller ->
                if(currentPage != route){
                    controller.navigate(route)
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun back(){
        composableCoroutineScope.launch {
            pagerState.animateScrollToPage(pages.lastIndex - 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavPager(){
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