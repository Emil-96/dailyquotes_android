package com.emil.dailyquotes

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Returns the login or signup page.
 *
 * @param modifier A [Modifier] to adjust the content.
 */
@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun LoginPage(
    context: Context,
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager
) {
    var login by remember { mutableStateOf(true) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val finishedEnabled =
        email.isNotEmpty() && password.isNotEmpty() && (login || password == confirmPassword)

    val focusManager = LocalFocusManager.current

    val nameAutofillNode = createAutofillNode(AutofillType.PersonFirstName) { name = it }
    val emailAutofillNode = createAutofillNode(AutofillType.EmailAddress) { email = it }
    val passwordAutofillNode = createAutofillNode(AutofillType.Password) { password = it }
    val newPasswordAutofillNode = createAutofillNode(AutofillType.NewPassword) {
        password = it
        confirmPassword = it
    }

    if (login) {
        LocalAutofillTree.current += emailAutofillNode
        LocalAutofillTree.current += passwordAutofillNode
    } else {
        LocalAutofillTree.current += nameAutofillNode
        LocalAutofillTree.current += emailAutofillNode
        LocalAutofillTree.current += newPasswordAutofillNode
    }

    val finish = {
        if (finishedEnabled) {
            mainActivity?.navigateTo(ROUTE_LOADING)
            if (login) {
                firebaseManager.logIn(
                    email = email,
                    password = password,
                    onSuccess = {
                        mainActivity?.backTo(ROUTE_SETTINGS) },
                    onFailure = {
                        mainActivity?.showMessage("Failed to log in", isError = true)
                        mainActivity?.backTo(ROUTE_LOGIN)
                    }
                )
            } else {
                firebaseManager.register(
                    name = name,
                    email = email,
                    password = password,
                    onSuccess = {
                        mainActivity?.backTo(ROUTE_SETTINGS)
                        registerPassword(context, email, password)
                    },
                    onFailure = {
                        mainActivity?.showMessage("Failed to register", isError = true)
                        mainActivity?.backTo(ROUTE_LOGIN)
                    }
                )
            }
        }
    }

    val onLoginFocus = {
        tryGoogleLogin(
            context = context,
            setEmail = {
                email = it
            },
            setPassword = {
                password = it
            },
            onDone = {
                mainActivity?.navigateTo(ROUTE_LOADING)
                firebaseManager.logIn(
                    email = email,
                    password = password,
                    onSuccess = {
                        mainActivity?.backTo(ROUTE_SETTINGS)
                    }
                )
            }
        )
    }

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

                Column(
                    modifier = modifier.padding(vertical = 16.dp, horizontal = 24.dp)
                ) {
                    AnimatedVisibility(visible = !login) {
                        TextField(
                            label = "Name",
                            text = name,
                            setText = { name = it },
                            capitalization = KeyboardCapitalization.Words,
                            focusManager = focusManager,
                            autofillNode = nameAutofillNode
                        )
                    }
                    TextField(
                        label = "Email",
                        text = email,
                        setText = { email = it },
                        keyboardType = KeyboardType.Email,
                        focusManager = focusManager,
                        autofillNode = emailAutofillNode,
                        onFocus = {
                            if (email == "" && password == "" && login) {
                                onLoginFocus()
                            }
                        }
                    )
                    TextField(
                        label = "Password",
                        text = password,
                        setText = { password = it },
                        isPassword = true,
                        keyboardType = KeyboardType.Password,
                        isFinal = login,
                        focusManager = focusManager,
                        autofillNode = if (login) passwordAutofillNode else newPasswordAutofillNode,
                        onFocus = {
                            if (password == "" && email == "" && login) {
                                onLoginFocus()
                            }
                        },
                        onDone = {
                            finish()
                        }
                    )
                    AnimatedVisibility(visible = !login) {
                        TextField(
                            label = "Confirm password",
                            text = confirmPassword,
                            setText = { confirmPassword = it },
                            isPassword = true,
                            keyboardType = KeyboardType.Password,
                            isFinal = true,
                            focusManager = focusManager,
                            autofillNode = newPasswordAutofillNode,
                            onDone = {
                                finish()
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .animateContentSize()
                        .fillMaxWidth()
                        .padding(end = 24.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {

                    Button(
                        enabled = finishedEnabled,
                        onClick = {
                            finish()
                        }
                    ) {
                        AnimatedContent(targetState = login, label = "change button label") {
                            Text(text = if (it) "Log in" else "Sign up")
                        }
                    }
                }
            }
        }
    }
}

private fun tryGoogleLogin(
    context: Context,
    setEmail: (String) -> Unit,
    setPassword: (String) -> Unit,
    onDone: () -> Unit
) {
    val credentialManager = CredentialManager.create(context)
    val getPasswordOption = GetPasswordOption()

    val getCredentialRequest = GetCredentialRequest(
        listOf(getPasswordOption)
    )

    mainActivity?.lifecycleScope?.launch {
        try {
            val result = credentialManager.getCredential(
                context = context,
                request = getCredentialRequest
            )

            val credential = result.credential
            if (credential is PasswordCredential) {
                setEmail(credential.id)
                setPassword(credential.password)
                onDone()
            }
        } catch (e: Exception) {
            Log.e("Login", "Caught exception: $e")
        }
    }

}

private fun registerPassword(
    context: Context,
    email: String,
    password: String
){
    val credentialManager = CredentialManager.create(context)

    val createPasswordRequest = CreatePasswordRequest(
        id = email, password = password
    )

    mainActivity?.lifecycleScope?.launch {
        try {
            val result = credentialManager.createCredential(
                context,
                createPasswordRequest
            )
        }catch (e: Exception){
            Log.d("Login", "Caught error after registering password with Google: $e")
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun createAutofillNode(
    type: AutofillType,
    onFill: (String) -> Unit
): AutofillNode {
    return AutofillNode(
        autofillTypes = listOf(type),
        onFill = {
            onFill(it)
        }
    )
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TextField(
    modifier: Modifier = Modifier,
    label: String,
    text: String,
    setText: (String) -> Unit,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardOptions.Default.capitalization,
    isFinal: Boolean = false,
    focusManager: FocusManager? = null,
    autofillNode: AutofillNode? = null,
    onFocus: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    val autofill = LocalAutofill.current

    OutlinedTextField(
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                autofillNode?.boundingBox = it.boundsInWindow()
            }
            .onFocusChanged { focusState ->
                /*
                autofill?.run {
                    if (focusState.isFocused) {
                        requestAutofillForNode(autofillNode)
                    } else {
                        cancelAutofillForNode(autofillNode)
                    }
                }
                */

                if (focusState.isFocused) {
                    onFocus()
                }
            },
        value = text,
        onValueChange = { setText(it) },
        label = { Text(text = label) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = if (isFinal) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager?.moveFocus(FocusDirection.Down)
            },
            onDone = {
                onDone()
            }
        ),
    )
}