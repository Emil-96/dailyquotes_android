package com.emil.dailyquotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import java.util.Date

@Composable
fun HomeScreen(modifier: Modifier = Modifier){

    val name = firebaseManager?.getName()?.observeAsState()

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
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Your quote for the day:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.alpha(.5f)
                )
                Text(
                    text = "This is an example of a motivational quote.",
                    style = MaterialTheme.typography.displaySmall,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}