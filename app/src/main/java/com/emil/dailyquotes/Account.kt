package com.emil.dailyquotes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Returns the account page.
 *
 * @param modifier A [Modifier] to adjust the content.
 */
@Composable
fun AccountPage(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager
){
    Scaffold(
        modifier = modifier,
        topBar = { TopNavBar(title = "Account") }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                onClick = {
                    firebaseManager.logOut(onSuccess = { mainActivity?.back()})
                }
            ) {
                Text(text = "Log out")
            }
        }
    }
}