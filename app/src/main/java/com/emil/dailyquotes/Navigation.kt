package com.emil.dailyquotes

import android.content.res.Configuration
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

sealed class BottomNavigationItem(val route: String, val label: String, val icon: Int){
    object HomeScreenItem: BottomNavigationItem(
        route = "home",
        label = "Home",
        icon = R.drawable.ic_quote
    )
    object ProfileScreenItem: BottomNavigationItem(
        route = "profile",
        label = "Profile",
        icon = R.drawable.ic_person
    )
}

fun navEnterTransition(durationMillis: Int = 350, direction: Int, orientation: Int) : EnterTransition {
    if(orientation == Configuration.ORIENTATION_PORTRAIT) {
        return slideInHorizontally(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            )
        ) { fullWidth ->
            direction * fullWidth / 20
        } + fadeIn(animationSpec = tween(durationMillis = (durationMillis * .8).toInt()))
    }else{
        return slideInVertically(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            )
        ) { fullHeight ->
            direction * fullHeight / 20
        } + fadeIn(animationSpec = tween(durationMillis = (durationMillis * .8).toInt()))
    }
}

fun navExitTransition(durationMillis: Int = 200, direction: Int, orientation: Int) : ExitTransition {
    if(orientation == Configuration.ORIENTATION_PORTRAIT) {
        return slideOutHorizontally(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            )
        ) { fullWidth ->
            direction * fullWidth / 20
        } + fadeOut(animationSpec = tween(durationMillis = (durationMillis * .8).toInt()))
    }else{
        return slideOutVertically(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            )
        ) { fullHeight ->
            direction * fullHeight / 20
        } + fadeOut(animationSpec = tween(durationMillis = (durationMillis * .8).toInt()))
    }
}