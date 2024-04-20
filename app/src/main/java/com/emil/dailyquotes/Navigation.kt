package com.emil.dailyquotes

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

val EASING_ENTER = FastOutSlowInEasing
val EASING_EXIT = LinearEasing

fun getNavigationDestinations(firebaseManager: FirebaseManager, preferenceManager: PreferenceManager): List<NavigationDestination>{
    return listOf(
        NavigationDestination(
            navItem = BottomNavigationItem.HomeScreenItem,
            content = { HomeScreen(firebaseManager = firebaseManager, preferenceManager = preferenceManager) }
        ),
        NavigationDestination(
            navItem = BottomNavigationItem.ProfileScreenItem,
            content = { ProfilePage(firebaseManager = firebaseManager) }
        )
    )
}

class NavigationDestination(
    private val navItem: BottomNavigationItem,
    private val content: @Composable () -> Unit
){

    fun getNavigationItem(): BottomNavigationItem{
        return navItem
    }

    fun getContent(modifier: Modifier = Modifier): @Composable () -> Unit{
        return {
            Box(
                modifier = modifier
            ){
                content()
            }
        }
    }
}

/**
 * Describes different items to be used in the bottom navigation bar on the home screen.
 */
sealed class BottomNavigationItem(val route: String, val label: String, val icon: Int){

    /**
     * The item leading to the home page.
     */
    object HomeScreenItem: BottomNavigationItem(
        route = "home",
        label = "Home",
        icon = R.drawable.ic_quote
    )

    /**
     * The item leading to the profile page.
     */
    object ProfileScreenItem: BottomNavigationItem(
        route = "profile",
        label = "Profile",
        icon = R.drawable.ic_person
    )
}

/**
 * Returns the [EnterTransition] to be used when navigating between different screens.
 *
 * @param durationMillis The duration of the transition animation.
 * @param direction The direction the page should move.
 *
 * @return The desired [EnterTransition] that can directly be applied to a destination.
 */
fun navEnterTransition(durationMillis: Int = 350, direction: Direction, easing: Easing = EASING_ENTER) : EnterTransition {
    val directionMultiplier = if(direction == Direction.NONE) 0 else if(direction == Direction.LEFT || direction == Direction.TOP) -1 else 1
    val slideHorizontally = direction == Direction.LEFT || direction == Direction.RIGHT
    if(slideHorizontally) {
        return slideInHorizontally(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = easing
            )
        ) { fullWidth ->
            directionMultiplier * fullWidth / 20
        } + fadeIn(animationSpec = tween(durationMillis = (durationMillis * .5).toInt()))
    }else{
        return slideInVertically(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = easing
            )
        ) { fullHeight ->
            directionMultiplier * fullHeight / 10
        } + fadeIn(animationSpec = tween(durationMillis = (durationMillis * .5).toInt()))
    }
}

/**
 * Returns the [ExitTransition] to be used when navigating between different screens.
 *
 * @param durationMillis The duration of the transition animation.
 * @param direction The direction the page should move.
 *
 * @return The desired [ExitTransition] that can directly be applied to a destination.
 */
fun navExitTransition(durationMillis: Int = 200, direction: Direction, easing: Easing = EASING_EXIT) : ExitTransition {
    val directionMultiplier = if(direction == Direction.NONE) 0 else if(direction == Direction.LEFT || direction == Direction.TOP) -1 else 1
    val slideHorizontally = direction == Direction.LEFT || direction == Direction.RIGHT
    if(slideHorizontally) {
        return slideOutHorizontally(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = easing
            )
        ) { fullWidth ->
            directionMultiplier * fullWidth / 20
        } + fadeOut(animationSpec = tween(durationMillis = (durationMillis * .5).toInt()))
    }else{
        return slideOutVertically(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = easing
            )
        ) { fullHeight ->
            directionMultiplier * fullHeight / 10
        } + fadeOut(animationSpec = tween(durationMillis = (durationMillis * .5).toInt()))
    }
}

/**
 * Returns a generic [TopAppBar] with a title and a back button.
 *
 * @param title The title to be displayed on the [TopAppBar]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavBar(title: String){
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(
                onClick = {
                    Log.d("TopNavBar", "tapped on back icon")
                    mainActivity?.back()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "back"
                )
            }
        })
}