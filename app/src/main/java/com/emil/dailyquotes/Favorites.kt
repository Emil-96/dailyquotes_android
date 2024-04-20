package com.emil.dailyquotes

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.emil.dailyquotes.room.Quote
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritePager(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager
) {
    val scope = rememberCoroutineScope()

    val emptyQuoteList: List<Quote> = listOf()
    var favorites by remember { mutableStateOf(emptyQuoteList) }

    scope.launch {
        favorites = firebaseManager.getFavorites()
    }

    val pagerState = rememberPagerState(pageCount = { favorites.size })

    Column {
        Row(
            modifier = Modifier
                .padding(start = 16.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Favorites")
            Spacer(modifier = Modifier.weight(1f))
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
        HorizontalPager(
            modifier = Modifier
                .padding(vertical = 16.dp),
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 36.dp),
            pageSpacing = 0.dp,
            beyondBoundsPageCount = 1
        ) { page ->
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
                quote = favorites[page],
                firebaseManager = firebaseManager
            )
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

    val emptyQuoteList: List<Quote> = listOf()
    var favorites by remember { mutableStateOf(emptyQuoteList) }

    scope.launch {
        favorites = firebaseManager.getFavorites()
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
                items(favorites.size) { index ->
                    QuoteCard(
                        quote = favorites[index],
                        firebaseManager = firebaseManager,
                        startWithActionRow = true
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                state = listState
            ) {
                items(favorites.size) { index ->
                    QuoteCard(
                        modifier = Modifier.widthIn(max = screenWidth / 2),
                        quote = favorites[index],
                        firebaseManager = firebaseManager,
                        startWithActionRow = true
                    )
                }
            }
        }
    }
}