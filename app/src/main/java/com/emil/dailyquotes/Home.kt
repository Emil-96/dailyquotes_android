package com.emil.dailyquotes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.emil.dailyquotes.room.Quote
import com.google.accompanist.placeholder.PlaceholderDefaults
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.color
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.shimmer
import com.google.accompanist.placeholder.placeholder
import java.util.Date

@Composable
fun HomeScreen(modifier: Modifier = Modifier){

    preferenceManager?.loadDailyQuote()

    val quote : State<Quote?>? = preferenceManager?.quote?.observeAsState()
    val name = firebaseManager?.getName()?.observeAsState()

    val showPlaceholder = quote?.value?.quote?.isEmpty() ?: true

    val transitionDurationMillis = 500

    val currentHours = Date().hours

    val greeting = if(currentHours <= 11) {
        "Good morning ${name?.value}"
    }else if(currentHours >= 16) {
        "Good evening ${name?.value}"
    }else{
        "Hello ${name?.value}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ){
        Text(
            text = greeting,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = tween(transitionDurationMillis)
                ),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                    //.animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Your quote for the day:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.alpha(.5f)
                )
                AnimatedVisibility(
                    visible = true,//quote?.value != null,
                    enter = fadeIn(animationSpec = tween(transitionDurationMillis), initialAlpha = 0f)
                ) {
                    Text(
                        text = "" + quote?.value?.quote,
                        style = MaterialTheme.typography.headlineMedium,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .alpha(if(showPlaceholder) .2f else 1f)
                            .clip(RoundedCornerShape(8.dp))
                            .placeholder(
                                visible = showPlaceholder,
                                color = Color.Transparent,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                            .defaultMinSize(minHeight = if(showPlaceholder) 86.dp else 0.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}