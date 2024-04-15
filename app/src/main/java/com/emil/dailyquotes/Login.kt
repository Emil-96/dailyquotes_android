package com.emil.dailyquotes

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Returns the login or signup page.
 *
 * @param modifier A [Modifier] to adjust the content.
 */
@Composable
fun LoginPage(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager
){

    var login by remember { mutableStateOf(true) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column (
            horizontalAlignment = Alignment.CenterHorizontally
        ){
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
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp),
                elevation = CardDefaults.elevatedCardElevation()
            ){

                var name by remember { mutableStateOf("") }
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var confirmPassword by remember { mutableStateOf("") }

                Box(
                    modifier = Modifier
                        .animateContentSize()
                        .padding(vertical = 16.dp, horizontal = 24.dp),
                ) {
                    if (login) {
                        LoginFields(
                            email = email,
                            setEmail = { email = it },
                            password = password,
                            setPassword = { password = it }
                        )
                    } else {
                        SignUpFields(
                            name = name,
                            setName = { name = it },
                            email = email,
                            setEmail = { email = it },
                            password = password,
                            setPassword = { password = it },
                            confirmPassword = confirmPassword,
                            setConfirmPassword = { confirmPassword = it }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 24.dp, bottom = 16.dp)
                        .animateContentSize(),
                    horizontalArrangement = Arrangement.End
                ) {

                    var clicked by remember { mutableStateOf(false) }

                    Button(
                        enabled = email.isNotEmpty() && password.isNotEmpty() && (login || password == confirmPassword),
                        onClick = {
                            clicked = true
                            if(login){
                                firebaseManager.logIn(
                                    email = email,
                                    password = password,
                                    onSuccess = { mainActivity?.back() }
                                )
                            }else{
                                firebaseManager.register(
                                    name = name,
                                    email = email,
                                    password = password,
                                    onSuccess = { mainActivity?.back() }
                                )
                            }
                        }
                    ) {
                        if(!clicked) {
                            Text(text = if (login) "Log in" else "Sign up")
                        }else{
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 4.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns the fields required for logging in.
 *
 * @param modifier A [Modifier] to adjust the content.
 * @param email The email address of the user.
 * @param setEmail The method to be called when the email text changes.
 * @param password The password of the user.
 * @param setPassword The method to be called when the password text changes.
 */
@Composable
fun LoginFields(
    modifier: Modifier = Modifier,
    email: String = "",
    setEmail: (String) -> Unit = {},
    password: String = "",
    setPassword: (String) -> Unit = {}
){
    Column(modifier = modifier) {
        EmailField(email = email, setEmail = setEmail)
        PasswordField(password = password, setPassword = setPassword)
    }
}

/**
 * Returns the fields required for signing up.
 *
 * @param modifier A [Modifier] to adjust the content.
 * @param name The name of the user.
 * @param setName The method to be called when the name text changes.
 * @param email The email address of the user.
 * @param setEmail The method to be called when the email text changes.
 * @param password The password of the user.
 * @param setPassword The method to be called when the password text changes.
 * @param confirmPassword The confirmation of the user's password.
 * @param setConfirmPassword The method to be called when the confirmation text of the user's password changes.
 */
@Composable
fun SignUpFields(
    modifier: Modifier = Modifier,
    name: String = "",
    setName: (String) -> Unit = {},
    email: String = "",
    setEmail: (String) -> Unit = {},
    password: String = "",
    setPassword: (String) -> Unit = {},
    confirmPassword: String = "",
    setConfirmPassword: (String) -> Unit = {}
){
    Column(modifier = modifier) {
        NameField(name = name, setName = setName)
        EmailField(email = email, setEmail = setEmail)
        PasswordField(password = password, setPassword = setPassword)
        PasswordField(isConfirm = true, password = confirmPassword, setPassword = setConfirmPassword)
    }
}

/**
 * Returns the field to enter the name.
 *
 * @param name The name of the user.
 * @param setName The method to be called when the name text changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameField(
    name: String,
    setName: (String) -> Unit
){
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = name,
        onValueChange = { setName(it) },
        label = { Text(text = "Display name") },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words
        )
    )
}

/**
 * Returns the field to enter the email address.
 *
 * @param email The email address of the user.
 * @param setEmail The method to be called when the email text changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailField(
    email: String,
    setEmail: (String) -> Unit
){
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = email,
        onValueChange = { setEmail(it) },
        label = { Text(text = "E-Mail") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email
        )
    )
}

/**
 * Returns the field to enter the password.
 *
 * @param isConfirm Whether the password field is describing the confirmation password.
 * @param password The password of the user.
 * @param setPassword The method to be called when the password text changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordField(
    isConfirm: Boolean = false,
    password: String,
    setPassword: (String) -> Unit
){
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = password,
        onValueChange = { setPassword(it) },
        label = { Text(text = if(isConfirm) "Password" else "Confirm password") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password
        )
    )
}