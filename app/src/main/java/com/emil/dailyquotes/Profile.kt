package com.emil.dailyquotes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ProfilePage(modifier: Modifier = Modifier){
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.End),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = { mainActivity?.navigateTo("settings") },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Settings",
                    )
                }
            }
        }
        NotSignedIn()
    }
}

@Composable
fun NotSignedIn(modifier: Modifier = Modifier){
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Text(
            text = "You are not signed in.\nGo into the settings on the top right to sign in.",
            textAlign = TextAlign.Center
        )
    }
}