package com.emil.dailyquotes

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfile(
    modifier: Modifier = Modifier,
    firebaseManager: FirebaseManager,
    selectedImageUri: Uri?
) {
    val showCrop = selectedImageUri?.path?.isNotBlank() ?: false

    var dialogOptions by remember {
        mutableStateOf(DialogOptions())
    }
    var showDialog by remember { mutableStateOf(false) }

    val originalName = "" + firebaseManager.getName().value
    var name by remember {
        mutableStateOf("" + firebaseManager.getName().value)
    }

    if (name != originalName) {
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

    val imageUrl = firebaseManager.getImageUrl().observeAsState("")

    if (showDialog) {
        Dialog(
            dialogOptions = dialogOptions,
            setVisibility = {
                showDialog = false
            }
        )
    }

    var croppedImage: ImageBitmap by remember {
        mutableStateOf(ImageBitmap(1, 1))
    }

    var selectedImage: ImageBitmap? by remember {
        mutableStateOf(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (name == originalName) {
                            mainActivity?.back()
                        } else {
                            showDialog = true
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "close"
                        )
                    }
                },
                title = { Text(text = "Edit profile") },
                actions = {
                    val duration = 250
                    AnimatedVisibility(
                        visible = !showCrop,
                        enter = fadeIn(animationSpec = tween(duration)),
                        exit = fadeOut(animationSpec = tween(duration))
                    ) {
                        TextButton(onClick = {
                            saveProfile(
                                firebaseManager = firebaseManager,
                                originalName = originalName,
                                newName = name,
                                newImage = selectedImage
                            )
                        }) {
                            Text(text = "Save")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            AnimatedContent(
                targetState = selectedImageUri,
                label = "show and hide cropping ui"
            ) { cropping ->
                if (cropping?.path?.isNotBlank() == true) {
                    ImageCrop(
                        image = cropping,
                        hideCrop = { mainActivity?.selectedImage?.setImage(Uri.EMPTY) },
                        setImage = {
                            croppedImage = it
                        },
                        saveImage = {
                            selectedImage = it
                        }
                    )
                } else {
                    EditFields(
                        profileImage = selectedImage,
                        imageUrl = imageUrl.value,
                        name = name,
                        setName = { name = it }
                    )
                }
            }
            /** This is required as otherwise the recording of the cropped image doesn't work (I don't know why) */
            ImagePreview(
                modifier = Modifier
                    .alpha(0f)
                    .height(0.dp),
                image = croppedImage
            )
        }
    }
}

private fun saveProfile(
    firebaseManager: FirebaseManager,
    originalName: String,
    newName: String,
    newImage: ImageBitmap?
) {
    updateName(
        firebaseManager = firebaseManager,
        originalName = originalName,
        newName = newName,
        onSuccess = {
            updateImage(
                firebaseManager = firebaseManager,
                newImage = newImage,
                onSuccess = { mainActivity?.back() },
                onFailure = { updateFailed() }
            )
        },
        onFailure = { updateFailed() }
    )
}

private fun updateName(
    firebaseManager: FirebaseManager,
    originalName: String,
    newName: String,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    if (originalName != newName) {
        firebaseManager.changeName(
            name = newName,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    } else {
        onSuccess()
    }
}

private fun updateImage(
    firebaseManager: FirebaseManager,
    newImage: ImageBitmap?,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    if (newImage != null && !newImage.isZero) {
        firebaseManager.changeProfileImage(
            image = newImage,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    } else {
        onSuccess()
    }
}

private fun updateFailed() {
    mainActivity?.showMessage(
        "Failed to update profile completely",
        true
    )
}

@Composable
private fun ImagePreview(
    modifier: Modifier,
    image: ImageBitmap
) {
    Image(
        modifier = modifier,
        bitmap = image,
        contentDescription = ""
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EditFields(
    profileImage: ImageBitmap?,
    imageUrl: String,
    name: String,
    setName: (String) -> Unit
) {
    Column {
        EditImage(
            profileImage = profileImage,
            imageUrl = imageUrl
        )
        TextField(
            modifier = Modifier.padding(top = 24.dp),
            label = "Name",
            text = "" + name,
            setText = setName,
            capitalization = KeyboardCapitalization.Words
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun EditImage(
    profileImage: ImageBitmap?,
    imageUrl: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (profileImage == null || profileImage.isZero) {
                GlideImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)),
                    model = imageUrl,
                    contentDescription = "profile image",
                    loading = placeholder(R.drawable.ic_person),
                    failure = placeholder(R.drawable.ic_person),
                    colorFilter = if(imageUrl.isNotEmpty()) null else
                        ColorFilter.tint(
                        MaterialTheme.colorScheme.onBackground.copy(
                            alpha = .5f
                        )
                    ),
                )
            } else {
                Image(
                    modifier = Modifier
                        .fillMaxSize(),
                    bitmap = profileImage,
                    contentDescription = "profile image"
                )
            }
        }
        FilledTonalButton(onClick = {
            mainActivity?.pickMedia?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            Log.d("Edit", "Clicked photo pick, selecting image...")
        }) {
            Text(text = "Select new image")
        }
    }
}
