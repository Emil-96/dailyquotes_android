package com.emil.dailyquotes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
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
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.emil.dailyquotes.room.QuoteDatabase
import com.emil.dailyquotes.ui.theme.DailyQuotesTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.absoluteValue
import kotlin.math.pow

/**
 * This is the main entry point for the application.
 */

private val USE_PAGER_EXPERIMENTAL = false

private val DIRECTION_LEFT = -1
private val DIRECTION_RIGHT = 1

var mainActivity: MainActivity? = null
var firebaseManager: FirebaseManager? = null
var preferenceManager: PreferenceManager? = null
var quoteDatabase: QuoteDatabase? = null

var csvImportLauncher: ActivityResultLauncher<Intent>? = null

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Gets created when the app gets executed and handles all further action.
 */
class MainActivity : ComponentActivity() {

    private lateinit var pageNavController: NavHostController
    private var currentPage: String? = null

    private var pages = mutableStateListOf("home")
    @OptIn(ExperimentalFoundationApi::class)
    private lateinit var pagerState: PagerState
    private var composableCoroutineScope: CoroutineScope? = null
    private lateinit var callback: OnBackPressedCallback

    private val dbManager = DBManager()

    /**
     * Gets called when the UI gets created.
     * It is the entry point for all UI action.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        mainActivity = this
        firebaseManager = FirebaseManager(this)
        preferenceManager = PreferenceManager()
        quoteDatabase = Room.databaseBuilder(
            applicationContext,
            QuoteDatabase::class.java, "quotes-database"
        )
            .allowMainThreadQueries()
            .build()

        registerCsvLauncher()

        setContent {

            window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

            DailyQuotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    if(USE_PAGER_EXPERIMENTAL){
                        NavigationPager()
                    }else{
                        NavigationHost()
                    }

                }
            }
        }
    }

    /**
     * Returns a [HorizontalPager] element which contains multiple [pages] and handles all the navigation happening within the application.
     *
     * In [onCreate] the constant [USE_PAGER_EXPERIMENTAL] determines if this or [NavigationHost] gets used.
     * Using the [HorizontalPager] has led to some issues so please only use this when you are sure what you are doing.
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun NavigationPager(){

        val pagerPages = remember { pages }

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

        /**
         * This is the [HorizontalPager] element that gets returned.
         * It contains all the possible navigation destinations.
         */
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

    /**
     * Returns a [NavHost] element which is used with the [pageNavController] takes care of all navigation destinations and the animations when transitioning between them.
     *
     * In [onCreate] the constant [USE_PAGER_EXPERIMENTAL] determines if this or the [HorizontalPager] in [NavigationPager] gets used.
     */
    @Composable
    fun NavigationHost(){

        pageNavController = rememberNavController()
        currentPage = currentRoute(navController = pageNavController)
        val orientation = LocalConfiguration.current.orientation

        NavHost(
            navController = pageNavController,
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
                content = { DBManagerPage(dbManager) }
            )
        }
    }

    /**
     * The direction in which the navigation flow is going and in which direction the page should be animated when entering.
     *
     * Returns either [DIRECTION_LEFT] or [DIRECTION_RIGHT].
     *
     * It is a supporting method used in [NavigationHost] to adjust the page transition accordingly.
     *
     * @param initialDestination The [NavDestination] from which the transition started.
     */
    private fun getNavEnterDirection(initialDestination: NavDestination): Int{

        return if(pageNavController.previousBackStackEntry?.destination?.route == initialDestination.route){
            DIRECTION_RIGHT
        }else{
            DIRECTION_LEFT
        }
    }

    /**
     * The direction in which the navigation flow is going and in which direction the page should be animated when exiting.
     *
     * Returns either [DIRECTION_LEFT] or [DIRECTION_RIGHT].
     *
     * It is a supporting method used in [NavigationHost] to adjust the page transition accordingly.
     *
     * @param initialDestination The [NavDestination] from which the transition started.
     */
    private fun getNavExitDirection(initialDestination: NavDestination): Int{

        val currentBackStack = pageNavController.currentBackStack.value

        return if(currentBackStack[currentBackStack.size - 2].destination.route == initialDestination.route){
            DIRECTION_LEFT
        }else{
            DIRECTION_RIGHT
        }
    }

    /**
     * Public method to navigate to a desired destination.
     *
     * @param route The desired destination. It has to exist in [pageNavController]
     */
    @OptIn(ExperimentalFoundationApi::class)
    fun navigateTo(route: String){
        if(USE_PAGER_EXPERIMENTAL) {
            pages.add(route)
            composableCoroutineScope?.launch {
                pagerState.animateScrollToPage(pages.lastIndex)
            }
        }else{
            pageNavController.let{ controller ->
                if(currentPage != route){
                    controller.navigate(route)
                }
            }
        }
    }

    /**
     * Public method to quickly navigate back to the previous destination.
     */
    @OptIn(ExperimentalFoundationApi::class)
    fun back(){
        composableCoroutineScope?.launch {
            pagerState.animateScrollToPage(pages.lastIndex - 1)
        }
    }

    /**
     * Register the activity for the result when trying to import data from a CSV file.
     *
     * This method is supposed to only be used by administrators.
     */
    private fun registerCsvLauncher(){
        csvImportLauncher = mainActivity?.registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){
                val data: Intent? = result.data
                data?.data?.let { uri ->
                    parseCsvUri(uri, dbManager)
                } ?: Log.e("FileImport", "Failed to retrieve URI from Intent")
            }
        }
    }
}

/**
 * Returns the home screen that you see when first opening the app. It describes only the scaffolding and the bottom navigation bar.
 *
 * **Not to be confused with [HomeScreen].**
 *
 * @param modifier A [Modifier] to adjust the content.
 */
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

/**
 * Returns the current back stack destination.
 *
 * @param navController The [NavController] from which the current back stack destination should be retrieved.
 */
@Composable
fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}