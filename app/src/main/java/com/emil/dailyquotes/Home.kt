package com.emil.dailyquotes

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.icu.util.Calendar
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.emil.dailyquotes.room.Quote
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.shimmer
import com.google.accompanist.placeholder.placeholder

/**
 * Returns the home screen page.
 *
 * @param modifier A [Modifier] to adjust the content.
 */
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager,
    preferenceManager: PreferenceManager
) {
    val orientation = LocalConfiguration.current.orientation
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        DailyQuote(firebaseManager = firebaseManager, preferenceManager = preferenceManager)
        if (orientation == Configuration.ORIENTATION_PORTRAIT && firebaseManager.isSignedIn()) {
            FavoritePager(firebaseManager = firebaseManager)
        }
    }
}

@Composable
private fun DailyQuote(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager,
    preferenceManager: PreferenceManager
) {
    val quote = preferenceManager.quote.observeAsState()
    val name = firebaseManager.getName().observeAsState()

    val currentHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    val greeting = if (currentHours <= 11) {
        "Good morning ${name.value}"
    } else if (currentHours >= 16) {
        "Good evening ${name.value}"
    } else {
        "Hello ${name.value}"
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        QuoteCard(
            quote = quote.value,
            title = "Your quote for the day",
            firebaseManager = firebaseManager,
            startWithActionRow = true
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun QuoteCard(
    modifier: Modifier = Modifier,
    quote: Quote?,
    title: String = "",
    firebaseManager: FirebaseManager,
    startWithActionRow: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    showProfile: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val vibrator = LocalHapticFeedback.current

    val showPlaceholder = quote?.quote?.isEmpty() ?: true

    var showActionRow by remember {
        mutableStateOf(startWithActionRow)
    }

    val transitionDurationMillis = 500

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .animateContentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    vibrator.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showActionRow = !showActionRow
                }),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 24.dp),
            //verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                showProfile,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ){
                val imageUrl = firebaseManager.getImageUrl().value
                val name = firebaseManager.getName().value
                Row (
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ){
                    GlideImage(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                        model = imageUrl,
                        contentDescription = "profile image"
                    )
                    Text(
                        modifier = Modifier.alpha(.8f),
                        text = "$name's quote for the day"
                    )
                }
            }
            if (title != "") {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .alpha(.5f)
                        .padding(bottom = 16.dp)
                )
            }
            Text(
                text = "" + quote?.quote,
                style = textStyle,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .alpha(if (showPlaceholder) .2f else 1f)
                    .clip(RoundedCornerShape(8.dp))
                    .placeholder(
                        visible = showPlaceholder,
                        color = Color.Transparent,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                    .defaultMinSize(minHeight = if (showPlaceholder) 86.dp else 0.dp)
                    .fillMaxWidth()
            )
            firebaseManager?.let {
                AnimatedVisibility(
                    visible = showActionRow && !showPlaceholder,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = transitionDurationMillis)
                    ) + fadeIn(animationSpec = tween(durationMillis = transitionDurationMillis))
                ) {
                    quote?.let {
                        ActionRow(
                            modifier = Modifier.padding(top = 16.dp),
                            firebaseManager = firebaseManager,
                            quote = it
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager,
    quote: Quote,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
    ) {
        FavoriteButton(
            firebaseManager = firebaseManager,
            quote = quote,
        )
        IconButton(onClick = {
            mainActivity?.share(quote = quote)
        }) {
            Icon(
                // TODO: Icon is too big, needs to be changed
                painter = painterResource(id = R.drawable.ic_share),
                contentDescription = "share"
            )
        }
    }
}

@Composable
private fun FavoriteButton(
    firebaseManager: FirebaseManager,
    quote: Quote,
) {

    val favorites = firebaseManager.favorites.observeAsState(initial = listOf())

    val isFavorite = favorites.value.contains(quote)

    var isLoading by remember {
        mutableStateOf(false)
    }

    FilledIconToggleButton(
        checked = isFavorite,
        onCheckedChange = { checked ->
            if (!firebaseManager.isSignedIn()) {
                mainActivity?.navigateTo(ROUTE_LOGIN)
            } else if (checked) {
                isLoading = true
                firebaseManager.addToFavorites(
                    quote,
                    onSuccess = {
                        isLoading = false
                    },
                    onFailure = {
                        isLoading = false
                        mainActivity?.showMessage(
                            message = "Couldn't save favorite",
                            isError = true
                        )
                    }
                )
            } else {
                isLoading = true
                firebaseManager.removeFromFavorites(
                    quote,
                    onSuccess = {
                        isLoading = false
                    },
                    onFailure = {
                        isLoading = false
                        mainActivity?.showMessage(
                            "Failed to remove favorite",
                            true
                        )
                    }
                )
            }
        }
    ) {
        AnimatedContent(targetState = isLoading, label = "") { loading ->
            if (!loading) {
                Icon(
                    painter = painterResource(id = if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star),
                    contentDescription = "mark as favorite"
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp),
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}