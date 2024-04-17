package com.emil.dailyquotes

import android.content.Intent
import android.icu.util.Calendar
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.shimmer
import com.google.accompanist.placeholder.placeholder

/**
 * Returns the home screen page.
 *
 * @param modifier A [Modifier] to adjust the content.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager
) {

    val quote = preferenceManager?.quote?.observeAsState()
    val name = firebaseManager.getName().observeAsState()

    var favorite by remember{ mutableStateOf(false) }

    val showPlaceholder = quote?.value?.quote?.isEmpty() ?: true

    val transitionDurationMillis = 500

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
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
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
            shape = RoundedCornerShape(24.dp)
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
                Text(
                    text = "" + quote?.value?.quote,
                    style = MaterialTheme.typography.headlineMedium,
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
                ) {
                    FilledIconToggleButton(
                        checked = favorite,
                        onCheckedChange = {favorite = !favorite}
                    ) {
                        Icon(
                            painter = painterResource(id = if(favorite) R.drawable.ic_star_filled else R.drawable.ic_star),
                            contentDescription = "mark as favorite"
                        )
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.type = "text/plain"
                        shareIntent.putExtra(Intent.EXTRA_TEXT, "\"" + quote?.value?.quote + "\"")
                        mainActivity?.startActivity(Intent.createChooser(shareIntent, "Share quote via..."))
                    }) {
                        Icon(
                            // TODO: Icon is too big, needs to be changed
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = "share"
                        )
                    }
                }
            }
        }
    }
}