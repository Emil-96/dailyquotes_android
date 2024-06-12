package com.emil.dailyquotes

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.emil.dailyquotes.room.Quote
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun FavoritePager(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager
) {
    val scope = rememberCoroutineScope()

    val favorites = firebaseManager.favorites.observeAsState(initial = listOf())

    scope.launch {
        firebaseManager.getFavorites()
    }

    Column {
        Row(
            modifier = Modifier
                .padding(start = 16.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Favorites")
            Spacer(modifier = Modifier.weight(1f))
            if(favorites.value.isNotEmpty()) {
                TextButton(onClick = {
                    mainActivity?.navigateTo(ROUTE_FAVORITES)
                }) {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "See all")
                        Icon(
                            painter = painterResource(id = R.drawable.ic_forward),
                            contentDescription = ""
                        )
                    }
                }
            }
        }
        AnimatedContent(targetState = favorites.value, label = "") { list ->
            FavPager(list = list, firebaseManager = firebaseManager)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavPager(
    list: List<Quote>,
    firebaseManager: FirebaseManager
){
    val pagerState = rememberPagerState(pageCount = { list.size.coerceAtLeast(1) })

    HorizontalPager(
        modifier = Modifier
            .padding(vertical = 16.dp),
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 36.dp),
        pageSpacing = 0.dp,
        beyondBoundsPageCount = 1
    ) { page ->
        if(list.isNotEmpty()) {
            QuoteCard(
                modifier = Modifier
                    .graphicsLayer {
                        val pageOffset = (
                                (pagerState.currentPage - page) + pagerState
                                    .currentPageOffsetFraction
                                ).absoluteValue
                        val scale = 1f - (pageOffset * .1f)
                        scaleX = scale
                        scaleY = scale
                    },
                quote = list[list.lastIndex - page],
                firebaseManager = firebaseManager
            )
        }else{
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier
                        .padding(24.dp)
                        .alpha(.5f),
                    text = "Starred quotes will appear here"
                )
            }
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun FavoritePage(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager,
    showBackIcon: Boolean = true,
) {
    val scope = rememberCoroutineScope()

    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp }

    val favorites = firebaseManager.favorites.observeAsState(initial = listOf())

    scope.launch {
        firebaseManager.getFavorites()
    }
    val listState = rememberLazyListState()

    Column {
        TopNavBar(title = "Favorites", showBackButton = showBackIcon)
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                state = listState
            ) {
                items(favorites.value.size) { index ->
                    QuoteCard(
                        quote = favorites.value[favorites.value.lastIndex - index],
                        firebaseManager = firebaseManager,
                        startWithActionRow = false
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                state = listState
            ) {
                items(favorites.value.size) { index ->
                    QuoteCard(
                        modifier = Modifier.widthIn(max = screenWidth / 2),
                        quote = favorites.value[favorites.value.lastIndex - index],
                        firebaseManager = firebaseManager,
                        startWithActionRow = true
                    )
                }
            }
        }
    }
}