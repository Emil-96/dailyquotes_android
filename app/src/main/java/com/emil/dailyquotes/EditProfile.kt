package com.emil.dailyquotes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EditProfile(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager
){
    val originalName = firebaseManager.getName().value

    var name by remember {
      mutableStateOf(firebaseManager.getName().value)
    }

    var dialogOptions by remember {
        mutableStateOf(DialogOptions())
    }
    var showDialog by remember { mutableStateOf(false) }

    if(name != originalName){
        dialogOptions = DialogOptions(
            title = "Discard changes?",
            text = "You have unsaved changes that will be discarded",
            visibilityMode = DIALOG_TEXT,
            dialogConfirm = {
                mainActivity?.back()
            }
        )
        BackHandler {
            showDialog = !showDialog
        }
    }

    if(showDialog){
        Dialog(
            dialogOptions = dialogOptions,
            setVisibility = {
                showDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = {
                    if(name == originalName) {
                        mainActivity?.back()
                    }else{
                        showDialog = true
                    }
                }) {
                    Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = "close")
                } },
                title = { Text(text = "Edit profile") },
                actions = {
                    TextButton(onClick = { /*TODO*/ }) {
                        Text(text = "Save")
                    }
                }
            )
        }
    ){ paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(horizontal = 24.dp)
        ){
            TextField(
                modifier = Modifier.padding(top = 24.dp),
                label = "Name",
                text = "" + name,
                setText = { name = it }
            )
        }
    }
}
