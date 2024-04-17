package com.emil.dailyquotes

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoginPage(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager
) {

    var login by remember { mutableStateOf(true) }

    val imePadding = WindowInsets.imeAnimationTarget.asPaddingValues()

    Log.d("Login", "IME Target as padding: ${imePadding.calculateBottomPadding()}")
    Log.d(
        "Login",
        "IME Target and zero padding are the same: ${imePadding.calculateBottomPadding() == (0.dp)}"
    )

    Box(
        modifier = modifier
            .imePadding()
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryTabRow(
                modifier = Modifier
                    .padding(horizontal = 24.dp),
                selectedTabIndex = true.compareTo(login),
                divider = {}
            ) {
                TabTitle(text = "Log in") {
                    login = true
                }
                TabTitle(text = "Register") {
                    login = false
                }
            }

            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(),
                modifier = Modifier
                    .focusable(true)
            ) {

                var name by remember { mutableStateOf("") }
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var confirmPassword by remember { mutableStateOf("") }

                Column(
                    modifier = modifier.padding(vertical = 16.dp, horizontal = 24.dp)
                ) {
                    AnimatedVisibility(visible = !login) {
                        NameField(name = name, setName = { name = it })
                    }
                    EmailField(email = email, setEmail = { email = it })
                    PasswordField(password = password, setPassword = { password = it })
                    AnimatedVisibility(visible = !login) {
                        PasswordField(
                            isConfirm = true,
                            password = confirmPassword,
                            setPassword = { confirmPassword = it })
                    }
                }

                Row(
                    modifier = Modifier
                        .animateContentSize()
                        .fillMaxWidth()
                        .padding(end = 24.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {

                    var clicked by remember { mutableStateOf(false) }

                    Button(
                        enabled = email.isNotEmpty() && password.isNotEmpty() && (login || password == confirmPassword),
                        onClick = {
                            clicked = true
                            if (login) {
                                firebaseManager.logIn(
                                    email = email,
                                    password = password,
                                    onSuccess = { mainActivity?.back() }
                                )
                            } else {
                                firebaseManager.register(
                                    name = name,
                                    email = email,
                                    password = password,
                                    onSuccess = { mainActivity?.back() }
                                )
                            }
                        }
                    ) {
                        if (!clicked) {
                            Text(text = if (login) "Log in" else "Sign up")
                        } else {
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

@Composable
private fun TabTitle(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember {
        MutableInteractionSource()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = interactionSource
            ) { onClick() }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Returns the field to enter the name.
 *
 * @param name The name of the user.
 * @param setName The method to be called when the name text changes.
 */
@Composable
fun NameField(
    name: String,
    setName: (String) -> Unit
) {
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
@Composable
fun EmailField(
    email: String,
    setEmail: (String) -> Unit
) {
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
@Composable
fun PasswordField(
    isConfirm: Boolean = false,
    password: String,
    setPassword: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = password,
        onValueChange = { setPassword(it) },
        label = { Text(text = if (!isConfirm) "Password" else "Confirm password") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password
        )
    )
}