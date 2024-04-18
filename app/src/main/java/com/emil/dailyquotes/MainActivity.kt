package com.emil.dailyquotes

import android.annotation.SuppressLint
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.EaseInCirc
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.emil.dailyquotes.room.Migration1to2
import com.emil.dailyquotes.room.QuoteDatabase
import com.emil.dailyquotes.ui.theme.DailyQuotesTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * This is the main entry point for the application.
 */

private const val USE_PAGER_EXPERIMENTAL = false
private const val USE_BACK_PROGRESS_EXPERIMENTAL = false

private const val DIRECTION_LEFT = -1
private const val DIRECTION_RIGHT = 1

const val ROUTE_HOME = "home"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_LOGIN = "login"
const val ROUTE_LOADING = "loading"
const val ROUTE_ACCOUNT = "account"
const val ROUTE_DBMANAGER = "db_manager"

var mainActivity: MainActivity? = null

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

    private var onBack: (() -> Unit)? = null

    /**
     * Gets called when the UI gets created.
     * It is the entry point for all UI action.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        mainActivity = this
        val quoteDatabase = Room.databaseBuilder(
            applicationContext,
            QuoteDatabase::class.java, "quotes-database"
        )
            .addMigrations(Migration1to2)
            .allowMainThreadQueries()
            .build()

        val firebaseManager = FirebaseManager(this, quoteDatabase) {}
        val preferenceManager = PreferenceManager {}
        Log.d("MainActivity", "Trying to load daily quote")
        preferenceManager.loadDailyQuote(firebaseManager, quoteDatabase.quoteDao())

        val dbManager = DBManager(firebaseManager)

        registerCsvLauncher(dbManager)

        setContent {

            window.decorView.setBackgroundColor(MaterialTheme.colorScheme.background.toArgb())

            /*
            var backProgress by remember { mutableFloatStateOf(0f) }

            val backProgressAnimated: Float by animateFloatAsState(
                targetValue = backProgress,
                label = ""
            )

            val backProgressOption = if (USE_BACK_PROGRESS_EXPERIMENTAL) {
                backProgressAnimated
            } else {
                0f
            }
             */

            callback = object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    onBack?.let { it() }
                    //backProgress = 0f
                }

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    //backProgress = backEvent.progress
                }

                override fun handleOnBackCancelled() {
                    //backProgress = 0f
                }
            }

            onBackPressedDispatcher.addCallback(callback)

            DailyQuotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    if (USE_PAGER_EXPERIMENTAL) {
                        NavigationPager(firebaseManager, preferenceManager)
                    } else {
                        NavigationHost(
                            firebaseManager,
                            preferenceManager,
                            dbManager,
                        )
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
    fun NavigationPager(
        firebaseManager: FirebaseManager,
        preferenceManager: PreferenceManager
    ) {
        val pagerPages = remember { pages }

        var userInputEnabled by remember { mutableStateOf(true) }

        var progress by remember { mutableFloatStateOf(0f) }
        val offset: Float by animateFloatAsState(targetValue = (100 * progress), label = "")

        /*
        callback = object : OnBackPressedCallback(enabled = false) {

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                if (pages.size == 1) {
                    super.handleOnBackProgressed(backEvent)
                } else {
                    progress = backEvent.progress.toDouble().pow(.2).toFloat()
                }
            }

            override fun handleOnBackPressed() {
                if (pages.size >= 1) {
                    progress = 0f
                    userInputEnabled = false
                    back()
                }
            }

            override fun handleOnBackCancelled() {
                if (pages.size == 1) {
                    super.handleOnBackCancelled()
                } else {
                    progress = 0f
                }
            }

        }

        onBackPressedDispatcher.addCallback(callback)
        callback.isEnabled = true
        */

        pagerState = rememberPagerState(
            pageCount = { pagerPages.size }
        )
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPageOffsetFraction }.collect { currentOffset ->
                val currentPage = pagerState.currentPage
                val previousPage = pagerState.settledPage
                if (currentOffset < 0.001 && currentPage < previousPage && pages.lastIndex > currentPage) {
                    while (pages.size > currentPage + 1) {
                        pages.removeAt(currentPage + 1)
                    }
                    userInputEnabled = true
                }
                if (currentPage == 0 && previousPage != 0) {
                    //callback.isEnabled = false
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
            when (pagerPages[page]) {
                ROUTE_HOME -> {
                    HomePage(
                        modifier = pageModifier,
                        firebaseManager = firebaseManager,
                        preferenceManager = preferenceManager
                    )
                }

                ROUTE_SETTINGS -> {
                    SettingsPage(
                        modifier = pageModifier,
                        firebaseManager = firebaseManager
                    )
                }

                ROUTE_ACCOUNT -> {
                    AccountPage(
                        modifier = pageModifier,
                        firebaseManager = firebaseManager
                    )
                }

                ROUTE_LOGIN -> {
                    LoginPage(
                        modifier = pageModifier,
                        firebaseManager = firebaseManager,
                        context = this@MainActivity
                    )
                }

                ROUTE_LOADING -> {
                    LoadingScreen()
                }
            }
        }

        if (!userInputEnabled) {
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
    fun NavigationHost(
        firebaseManager: FirebaseManager,
        preferenceManager: PreferenceManager,
        dbManager: DBManager,
        backProgress: Float = 0f
    ) {

        pageNavController = rememberNavController()
        currentPage = currentRoute(navController = pageNavController)
        val orientation = LocalConfiguration.current.orientation

        NavHost(
            navController = pageNavController,
            startDestination = ROUTE_HOME,
        ) {
            composable(
                route = ROUTE_HOME,
                enterTransition = {
                    navEnterTransition(
                        direction = DIRECTION_LEFT,
                        orientation = orientation
                    )
                },
                exitTransition = {
                    navExitTransition(
                        direction = DIRECTION_LEFT,
                        orientation = orientation
                    )
                },
                content = {
                    HomePage(
                        firebaseManager = firebaseManager,
                        preferenceManager = preferenceManager,
                        backProgress = backProgress
                    )
                }
            )
            getNavDestination(
                this,
                orientation,
                ROUTE_SETTINGS
            ) { SettingsPage(firebaseManager = firebaseManager) }
            getNavDestination(
                this,
                orientation,
                ROUTE_LOGIN
            ) { LoginPage(firebaseManager = firebaseManager, context = this@MainActivity) }
            getNavDestination(
                this,
                orientation,
                ROUTE_ACCOUNT
            ) { AccountPage(firebaseManager = firebaseManager) }
            getNavDestination(
                this,
                orientation,
                ROUTE_DBMANAGER
            ) { DBManagerPage(dbManager = dbManager) }
            getNavDestination(this, orientation, ROUTE_LOADING) { LoadingScreen() }
        }
    }

    private fun getNavDestination(
        navGraphBuilder: NavGraphBuilder,
        orientation: Int,
        route: String,
        destination: @Composable() () -> Unit
    ): Unit {
        return navGraphBuilder.composable(
            route = route,
            enterTransition = {
                navEnterTransition(
                    direction = getNavEnterDirection(initialState.destination),
                    orientation = orientation
                )
            },
            exitTransition = {
                navExitTransition(
                    direction = getNavExitDirection(initialState.destination),
                    orientation = orientation
                )
            },
            content = { destination() }
        )
    }

    /**
     * The direction in which the navigation flow is going and in which direction the page should be animated when entering.
     *
     * It is a supporting method used in [NavigationHost] to adjust the page transition accordingly.
     *
     * @param initialDestination The [NavDestination] from which the transition started.
     *
     * @return The direction in which the page should be animated. Either [DIRECTION_LEFT] or [DIRECTION_RIGHT].
     */
    private fun getNavEnterDirection(initialDestination: NavDestination): Int {

        return if (pageNavController.previousBackStackEntry?.destination?.route == initialDestination.route) {
            DIRECTION_RIGHT
        } else {
            DIRECTION_LEFT
        }
    }

    /**
     * The direction in which the navigation flow is going and in which direction the page should be animated when exiting.
     *
     * It is a supporting method used in [NavigationHost] to adjust the page transition accordingly.
     *
     * @param initialDestination The [NavDestination] from which the transition started.
     *
     * @return The direction in which the page should be animated. Either [DIRECTION_LEFT] or [DIRECTION_RIGHT].
     */
    @SuppressLint("RestrictedApi")
    private fun getNavExitDirection(initialDestination: NavDestination): Int {

        val currentBackStack = pageNavController.currentBackStack.value

        return if (currentBackStack[currentBackStack.size - 2].destination.route == initialDestination.route) {
            DIRECTION_LEFT
        } else {
            DIRECTION_RIGHT
        }
    }

    /**
     * Public method to navigate to a desired destination.
     *
     * @param route The desired destination. It has to exist in [pageNavController]
     */
    @OptIn(ExperimentalFoundationApi::class)
    fun navigateTo(route: String) {
        onBack = null
        if (USE_PAGER_EXPERIMENTAL) {
            pages.add(route)
            composableCoroutineScope?.launch {
                pagerState.animateScrollToPage(pages.lastIndex)
            }
        } else {
            pageNavController.let { controller ->
                if (currentPage != route) {
                    controller.navigate(route)
                }
            }
        }
    }

    /**
     * Public method to quickly navigate back to the previous destination.
     */
    fun back() {
        Log.d("MainActivity", "going back")
        pageNavController.popBackStack()
        onBack = null
        //callback.handleOnBackPressed()
        /*composableCoroutineScope?.launch {
            pagerState.animateScrollToPage(pages.lastIndex - 1)
        }*/
    }

    fun backTo(route: String) {
        pageNavController.popBackStack(route, inclusive = false)
        onBack = null
    }

    fun setOnBack(function: () -> Unit) {
        onBack = function
    }

    fun setCustomBackEnabled(enabled: Boolean) {
        callback.isEnabled = enabled
    }

    /**
     * Register the activity for the result when trying to import data from a CSV file.
     *
     * This method is supposed to only be used by administrators.
     */
    private fun registerCsvLauncher(dbManager: DBManager) {
        csvImportLauncher =
            mainActivity?.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    data?.data?.let { uri ->
                        parseCsvUri(uri, dbManager)
                    } ?: Log.e("FileImport", "Failed to retrieve URI from Intent")
                }
            }
    }

    fun showMessage(
        message: String,
        isError: Boolean = false
    ) {
        // TODO: Show a toast to the user with the error message.
    }
}

/**
 * Returns the home screen that you see when first opening the app. It describes only the scaffolding and the bottom navigation bar.
 *
 * **Not to be confused with [HomeScreen].**
 *
 * @param modifier A [Modifier] to adjust the content.
 */
@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager,
    preferenceManager: PreferenceManager,
    backProgress: Float = 0f
) {
    val navigationItems = getNavigationDestinations(firebaseManager, preferenceManager)

    val usePager = true

    val pagerState = rememberPagerState(pageCount = { navigationItems.size })

    val navController = rememberNavController()
    val currentRoute = currentRoute(navController = navController)
    val localConfig = LocalConfiguration.current
    val orientation = localConfig.orientation
    val screenWidth = with(LocalDensity.current) { localConfig.screenWidthDp.dp.toPx() }

    val scope = rememberCoroutineScope()

    Log.d("HomePage", "Received back progress: $backProgress")

    mainActivity?.setOnBack {
        scope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    mainActivity?.setCustomBackEnabled(pagerState.currentPage != 0)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                navigationItems.forEachIndexed { index, item ->
                    val navItem = item.getNavigationItem()
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            if (currentRoute != navItem.route) {
                                if (usePager) {
                                    scope.launch {
                                        pagerState.animateScrollBy(
                                            value = screenWidth * (index - pagerState.currentPage),
                                            animationSpec = tween(durationMillis = 350)
                                        )
                                    }
                                } else {
                                    navController.navigate(navItem.route) {
                                        launchSingleTop = true
                                        popUpTo("home")
                                    }
                                }
                            }
                        },
                        icon = { Icon(painterResource(id = navItem.icon), navItem.label) },
                        label = { Text(text = navItem.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (usePager) {
            HorizontalPager(
                modifier = Modifier
                    .padding(paddingValues),
                state = pagerState,
                beyondBoundsPageCount = 1
            ) { page ->
                navigationItems[page].getContent(
                    modifier = Modifier
                        .graphicsLayer {
                            val back = 1f - backProgress
                            val pageOffset = (
                                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                    ).absoluteValue
                            alpha = back * (1f - 2 * pageOffset).coerceIn(0f, 1f)

                            val direction = (if (page % 2 == 0) 1f else -1f)
                            val offsetDerivedFromProgress =
                                EaseInCirc.transform(pageOffset) * screenWidth
                            val completePageOffset = screenWidth * direction * pageOffset
                            translationX =
                                    //(backProgress * screenWidth / 4) -
                                (completePageOffset - direction * offsetDerivedFromProgress)
                        }
                )()
            }
        } else {
            NavHost(
                navController = navController,
                modifier = Modifier.padding(paddingValues),
                startDestination = "home"
            ) {
                composable(
                    route = "home",
                    enterTransition = {
                        navEnterTransition(
                            direction = DIRECTION_LEFT,
                            orientation = orientation
                        )
                    },
                    exitTransition = {
                        navExitTransition(
                            direction = DIRECTION_LEFT,
                            orientation = orientation
                        )
                    },
                    content = {
                        HomeScreen(
                            firebaseManager = firebaseManager,
                            preferenceManager = preferenceManager
                        )
                    }
                )
                composable(
                    route = "profile",
                    enterTransition = {
                        navEnterTransition(
                            direction = DIRECTION_RIGHT,
                            orientation = orientation
                        )
                    },
                    exitTransition = {
                        navExitTransition(
                            direction = DIRECTION_RIGHT,
                            orientation = orientation
                        )
                    },
                    content = { ProfilePage(firebaseManager = firebaseManager) }
                )
            }
        }
    }
}

/**
 * Returns the current back stack destination.
 *
 * @param navController The [NavController] from which the current back stack destination should be retrieved.
 *
 * @return The current back stack destination.
 */
@Composable
fun currentRoute(navController: NavController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}