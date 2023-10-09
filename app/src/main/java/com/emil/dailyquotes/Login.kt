package com.emil.dailyquotes

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginPage(
    modifier: Modifier = Modifier
){

    var login by remember { mutableStateOf(true) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column (
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            //Text(text = "Login", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                if(login) {
                    Button(
                        onClick = {  },
                    ) {
                        Text(text = "Log in")
                    }
                    OutlinedButton(
                        onClick = { login = false },
                    ) {
                        Text(text = "Sign up")
                    }
                }else{
                    OutlinedButton(
                        onClick = { login = true },
                    ) {
                        Text(text = "Log in")
                    }
                    Button(
                        onClick = {  },
                    ) {
                        Text(text = "Sign up")
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier
                    .animateContentSize()
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp),
                elevation = CardDefaults.elevatedCardElevation()
            ){

                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }

                if(login){
                    //password = ""
                    LoginFields(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
                        email = email,
                        setEmail = { email = it },
                        password = password,
                        setPassword = { password = it }
                    )
                }else{
                    //password = ""
                    SignUpFields(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
                        email = email,
                        setEmail = { email = it },
                        password = password,
                        setPassword = { password = it }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 24.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = { /*TODO*/ }) {
                        Text(text = if (login) "Log in" else "Sign up")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginFields(
    modifier: Modifier = Modifier,
    email: String = "",
    setEmail: (String) -> Unit = {},
    password: String = "",
    setPassword: (String) -> Unit = {}
){
    Column(modifier = modifier) {

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = email,
            onValueChange = { setEmail(it) },
            label = { Text(text = "E-Mail") }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = password,
            onValueChange = { setPassword(it) },
            label = { Text(text = "Password") },
            visualTransformation = PasswordVisualTransformation()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpFields(
    modifier: Modifier = Modifier,
    email: String = "",
    setEmail: (String) -> Unit = {},
    password: String = "",
    setPassword: (String) -> Unit = {}
){
    Column(modifier = modifier) {

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = email,
            onValueChange = { setEmail(it) },
            label = { Text(text = "E-Mail") }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = password,
            onValueChange = { setPassword(it) },
            label = { Text(text = "Password") },
            visualTransformation = PasswordVisualTransformation()
        )
    }
}