package com.emil.dailyquotes

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Picture
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.EaseInCirc
import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.MutableLiveData
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
import com.emil.dailyquotes.room.Quote
import com.emil.dailyquotes.room.QuoteDatabase
import com.emil.dailyquotes.ui.theme.DailyQuotesTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.absoluteValue


/**
 * This is the main entry point for the application.
 */

private const val USE_PAGER_EXPERIMENTAL = false
private const val USE_BACK_PROGRESS_EXPERIMENTAL = false

enum class Direction {
    DEFAULT, LEFT, RIGHT, BOTTOM, TOP, NONE
}

const val ROUTE_HOME = "home"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_PROFILE = "profile"
const val ROUTE_LOGIN = "login"
const val ROUTE_LOADING = "loading"
const val ROUTE_ACCOUNT = "account"
const val ROUTE_DBMANAGER = "db_manager"
const val ROUTE_FAVORITES = "favorites"
const val ROUTE_EDIT_PROFILE = "edit_profile"

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

    private var sharedQuote: Quote? = null

    private var showShareSheet: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
     * Gets called when the UI gets created.
     * It is the entry point for all UI action.
     */
    @OptIn(ExperimentalMaterial3Api::class)
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

        val preferenceManager = PreferenceManager {}
        val firebaseManager = FirebaseManager(this, preferenceManager, quoteDatabase) {}
        Log.d("MainActivity", "Trying to load daily quote")

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

            callback = object : OnBackPressedCallback(enabled = false) {
                override fun handleOnBackPressed() {
                    onBack?.let {
                        it()
                    }
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
                    val showSheet = showShareSheet.observeAsState()

                    if (showSheet.value == true) {
                        sharedQuote?.let { quote ->
                            ShareSheet(
                                context = this,
                                quote = quote,
                                onDismiss = { showShareSheet.postValue(false) }
                            )
                        }
                    }

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

                ROUTE_FAVORITES -> {
                    FavoritePage(firebaseManager = firebaseManager)
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

        NavHost(
            navController = pageNavController,
            startDestination = ROUTE_HOME,
        ) {
            composable(
                route = ROUTE_HOME,
                enterTransition = {
                    navEnterTransition(
                        direction = if (initialState.destination.route == ROUTE_EDIT_PROFILE) Direction.NONE else Direction.LEFT,
                    )
                },
                exitTransition = {
                    navExitTransition(
                        direction = if (targetState.destination.route == ROUTE_EDIT_PROFILE) Direction.NONE else Direction.LEFT,
                    )
                },
                content = {
                    HomePage(
                        firebaseManager = firebaseManager,
                        preferenceManager = preferenceManager,
                    )
                }
            )
            getNavDestination(
                this,
                ROUTE_SETTINGS
            ) { SettingsPage(firebaseManager = firebaseManager) }
            getNavDestination(
                this,
                ROUTE_LOGIN
            ) { LoginPage(firebaseManager = firebaseManager, context = this@MainActivity) }
            getNavDestination(
                this,
                ROUTE_ACCOUNT
            ) { AccountPage(firebaseManager = firebaseManager) }
            getNavDestination(
                this,
                ROUTE_DBMANAGER
            ) { DBManagerPage(dbManager = dbManager) }
            getNavDestination(this, ROUTE_LOADING) { LoadingScreen() }
            getNavDestination(
                this,
                ROUTE_FAVORITES
            ) { FavoritePage(firebaseManager = firebaseManager) }
            getNavDestination(
                this,
                ROUTE_EDIT_PROFILE,
                slideDirection = Direction.BOTTOM,
                enterEasing = EaseOutCirc,
            ) { EditProfile(firebaseManager = firebaseManager) }
        }
    }

    private fun getNavDestination(
        navGraphBuilder: NavGraphBuilder,
        route: String,
        slideDirection: Direction = Direction.DEFAULT,
        enterEasing: Easing = EASING_ENTER,
        exitEasing: Easing = EASING_EXIT,
        transitionDurationMillis: Int = 350,
        destination: @Composable() () -> Unit,
    ): Unit {
        return navGraphBuilder.composable(
            route = route,
            enterTransition = {
                navEnterTransition(
                    direction = if (slideDirection == Direction.DEFAULT) getNavEnterDirection(
                        initialState.destination
                    ) else slideDirection,
                    easing = enterEasing,
                    durationMillis = transitionDurationMillis
                )
            },
            exitTransition = {
                navExitTransition(
                    direction = if (slideDirection == Direction.DEFAULT) getNavExitDirection(
                        initialState.destination
                    ) else slideDirection,
                    easing = exitEasing,
                    durationMillis = (transitionDurationMillis * .6).toInt()
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
     * @return The direction in which the page should be animated. Either [Direction.LEFT] or [Direction.RIGHT].
     */
    private fun getNavEnterDirection(initialDestination: NavDestination): Direction {

        return if (pageNavController.previousBackStackEntry?.destination?.route == initialDestination.route) {
            Direction.RIGHT
        } else {
            Direction.LEFT
        }
    }

    /**
     * The direction in which the navigation flow is going and in which direction the page should be animated when exiting.
     *
     * It is a supporting method used in [NavigationHost] to adjust the page transition accordingly.
     *
     * @param initialDestination The [NavDestination] from which the transition started.
     *
     * @return The direction in which the page should be animated.
     */
    @SuppressLint("RestrictedApi")
    private fun getNavExitDirection(initialDestination: NavDestination): Direction {

        val currentBackStack = pageNavController.currentBackStack.value

        return if (currentBackStack[currentBackStack.size - 2].destination.route == initialDestination.route) {
            Direction.LEFT
        } else {
            Direction.RIGHT
        }
    }

    /**
     * Public method to navigate to a desired destination.
     *
     * @param route The desired destination. It has to exist in [pageNavController]
     */
    @OptIn(ExperimentalFoundationApi::class)
    fun navigateTo(route: String) {
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
        //callback.handleOnBackPressed()
        /*composableCoroutineScope?.launch {
            pagerState.animateScrollToPage(pages.lastIndex - 1)
        }*/
    }

    fun backTo(route: String) {
        pageNavController.popBackStack(route, inclusive = false)
    }

    /*
    fun setOnBack(function: () -> Unit) {
        onBack = function
        callback.isEnabled = true
        pageNavController.enableOnBackPressed(false)
    }

    fun removeOnBack(){
        onBack = null
        callback.isEnabled = true
        pageNavController.enableOnBackPressed(false)
    }
    */

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

    fun share(quote: Quote) {
        sharedQuote = quote
        showShareSheet.postValue(true)
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
) {
    val localConfig = LocalConfiguration.current
    val orientation = localConfig.orientation
    val screenWidth = with(LocalDensity.current) { localConfig.screenWidthDp.dp.toPx() }
    val screenHeight = with(LocalDensity.current) { localConfig.screenHeightDp.dp.toPx() }

    val navigationItems = getNavigationDestinations(firebaseManager, preferenceManager, orientation)

    val pagerState = rememberPagerState(pageCount = { navigationItems.size })

    val navController = rememberNavController()
    val currentRoute = currentRoute(navController = navController)

    val scope = rememberCoroutineScope()

    if (pagerState.currentPage != 0) {
        BackHandler {
            scope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    var position by remember {
        mutableIntStateOf(0)
    }

    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        Scaffold(
            bottomBar = {
                BottomBar(
                    pagerState = pagerState,
                    //position = (position-1).coerceIn(0,1),
                    setPosition = { position = it * 2 },
                    navigationItems = navigationItems,
                    currentRoute = currentRoute,
                    screenWidth = screenWidth
                )
            }
        ) { paddingValues ->
            PortraitPager(
                modifier = Modifier.padding(paddingValues),
                pagerState, navigationItems, screenWidth
            )
        }
    } else {
        Row(
            modifier = Modifier.safeContentPadding()
        ) {
            SideRail(
                pagerState = pagerState,
                setPosition = { position = it },
                navigationItems = navigationItems,
                currentRoute = currentRoute,
                screenWidth = screenWidth
            )
            LandscapePager(
                pagerState = pagerState,
                navigationItems = navigationItems,
                screenHeight = screenHeight
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PortraitPager(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    navigationItems: List<NavigationDestination>,
    screenWidth: Float
) {
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        beyondBoundsPageCount = 1
    ) { page ->
        navigationItems[page].getContent(
            modifier = Modifier
                .graphicsLayer {
                    val pageOffset = (
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            ).absoluteValue
                    alpha = (1f - 2 * pageOffset).coerceIn(0f, 1f)

                    val direction = if (pagerState.currentPageOffsetFraction >= 0) 1f else -1f
                    val offsetDerivedFromProgress =
                        EaseInCirc.transform(pageOffset) * screenWidth
                    val completePageOffset = screenWidth * direction * pageOffset
                    translationX =
                            //(backProgress * screenWidth / 4) -
                        (completePageOffset - direction * offsetDerivedFromProgress)
                }
        )()
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LandscapePager(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    navigationItems: List<NavigationDestination>,
    screenHeight: Float
) {
    VerticalPager(
        state = pagerState,
        beyondBoundsPageCount = 1
    ) { page ->
        navigationItems[page].getContent(
            modifier = Modifier
                .graphicsLayer {
                    val pageOffset = (
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            ).absoluteValue
                    alpha = (1f - 2 * pageOffset).coerceIn(0f, 1f)

                    val direction = if (pagerState.currentPageOffsetFraction >= 0) 1f else -1f
                    val offsetDerivedFromProgress =
                        EaseInCirc.transform(pageOffset) * screenHeight
                    val completePageOffset = screenHeight * direction * pageOffset
                    translationY = completePageOffset - direction * offsetDerivedFromProgress
                }
        )()
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomBar(
    pagerState: PagerState,
    setPosition: (Int) -> Unit,
    navigationItems: List<NavigationDestination>,
    currentRoute: String,
    screenWidth: Float
) {
    val scope = rememberCoroutineScope()

    NavigationBar {
        navigationItems.forEachIndexed { index, item ->
            val navItem = item.getNavigationItem()
            val selected = pagerState.currentPage == index
            if (selected) {
                setPosition(index)
            }
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != navItem.route) {
                        scope.launch {
                            pagerState.animateScrollToPage(page = index)
                            /*
                            pagerState.animateScrollBy(
                                value = screenWidth * (index - pagerState.currentPage),
                                animationSpec = tween(durationMillis = 350)
                            )
                            */
                        }
                    }
                },
                icon = { Icon(painterResource(id = navItem.icon), navItem.label) },
                label = { Text(text = navItem.label) }
            )
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SideRail(
    pagerState: PagerState,
    setPosition: (Int) -> Unit,
    navigationItems: List<NavigationDestination>,
    currentRoute: String,
    screenWidth: Float
) {
    val scope = rememberCoroutineScope()

    NavigationRail {
        Spacer(modifier = Modifier.weight(1f))
        navigationItems.forEachIndexed { index, item ->
            val navItem = item.getNavigationItem()
            val selected = pagerState.currentPage == index
            if (selected) {
                setPosition(index)
            }
            NavigationRailItem(
                selected = pagerState.currentPage == index,
                onClick = {
                    if (currentRoute != navItem.route) {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = index
                            )
                            /*
                            pagerState.animateScrollBy(
                                value = screenWidth * (index - pagerState.currentPage),
                                animationSpec = tween(durationMillis = 350)
                            )
                             */
                        }
                    }
                },
                icon = { Icon(painterResource(id = navItem.icon), navItem.label) },
                label = { Text(text = navItem.label) }
            )
            Spacer(modifier = Modifier.weight(1f))
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
fun currentRoute(navController: NavController): String {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return "" + navBackStackEntry?.destination?.route
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(context: Context, quote: Quote, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val picture = remember { Picture() }

    val backgroundColor = CardDefaults.elevatedCardColors().contentColor

    ModalBottomSheet(
        windowInsets = WindowInsets(0.dp),
        sheetState = sheetState,
        onDismissRequest = { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .drawWithCache {
                        val width = this.size.width.toInt()
                        val height = this.size.height.toInt()
                        onDrawWithContent {
                            val pictureCanvas = Canvas(
                                picture.beginRecording(
                                    width, height
                                )
                            )
                            draw(this, this.layoutDirection, pictureCanvas, this.size) {
                                this@onDrawWithContent.drawContent()
                            }
                            picture.endRecording()

                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawPicture(picture)
                            }
                        }
                    },
                colors = CardDefaults.elevatedCardColors()
                    .copy(containerColor = backgroundColor)
            ) {
                QuoteCard(
                    modifier = Modifier.padding(24.dp),
                    quote = quote
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val imageUri = getUriForBitmap(
                                context = context,
                                bitmap = pictureToBitmap(picture, backgroundColor.toArgb())
                            )
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "image/png"
                            clipData = ClipData.newRawUri(null, imageUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            putExtra(
                            Intent.EXTRA_STREAM,
                            imageUri
                            )
                        }
                        shareIntent.clipData = ClipData.newRawUri("quote", imageUri)
                        mainActivity?.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                "Share quote via..."
                            )
                        )
                    }
                ) {
                    Text(text = "Share Image")
                }
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDismiss()
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(
                            Intent.EXTRA_TEXT,
                            "\"" + quote.quote.trim() + "\""
                        )
                        mainActivity?.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                "Share quote via..."
                            )
                        )
                    }
                ) {
                    Text(text = "Share Text")
                }
            }
            Spacer(modifier = Modifier.height(0.dp))
        }
    }
}

private fun pictureToBitmap(picture: Picture, backgroundColor: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(
        picture.width,
        picture.height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(backgroundColor)
    canvas.drawPicture(picture)
    return bitmap
}

private fun getUriForBitmap(context: Context, bitmap: Bitmap): Uri? {
    val image = File(context.cacheDir, "shared_image.png")
    var uri: Uri? = null
    try {
        //imageFolder.mkdirs()
        //val imageFile = File(imageFolder, "shared_image.png")
        val fileOutputStream = FileOutputStream(image)
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()
        uri = FileProvider.getUriForFile(context, context.packageName + ".provider", image)
    } catch (e: Exception) {
        Log.e("MainActivity", "Error preparing uri for file sharing: $e")
    }
    return uri
}