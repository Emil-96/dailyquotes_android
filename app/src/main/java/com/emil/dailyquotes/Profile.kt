package com.emil.dailyquotes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Returns the profile page.
 *
 * @param modifier A [Modifier] to adjust the content.
 * @param firebaseManager A [FirebaseManager] to retrieve information about the currently signed in user.
 */
@Composable
fun ProfilePage(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager
){
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.End),
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
        if(firebaseManager.isSignedIn()){
            ProfileView(firebaseManager = firebaseManager)
        }else {
            NotSignedIn()
        }
    }
}

@Composable
private fun ProfileView(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager
){

    val name = firebaseManager.getName()
    val email = firebaseManager.getEmail()
    val profileImage = null

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = modifier.padding(top = 56.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)),
                painter = painterResource(
                    id = R.drawable.ic_person
                ),
                contentDescription = "profile image",
                colorFilter = if(profileImage == null) ColorFilter.tint(MaterialTheme.colorScheme.onBackground.copy(alpha = .5f)) else null,
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 72.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LabelText(label = "Name")
                FieldText(text = name.value ?: "")
                LabelText(label = "E-Mail")
                FieldText(text = email.value ?: "")
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = { /*TODO*/ }
            ) {
                Text(text = "Edit profile")
            }
        }
    }
}

@Composable
private fun LabelText(label: String){
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.alpha(.5f)
    )
}

@Composable
private fun FieldText(text: String){
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
    )
}

/**
 * Returns the field that will be displayed when the user is not signed in.
 *
 * @param modifier A [Modifier] to adjust the content.
 */
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