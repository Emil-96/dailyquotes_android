package com.emil.dailyquotes

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

const val DIALOG_HIDDEN = 0
const val DIALOG_TEXT = 1
const val DIALOG_OPTIONS = 2
const val DIALOG_CONTENT = 3

/**
 * Returns the settings page.
 *
 * @param modifier A [Modifier] to adjust the content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(modifier: Modifier = Modifier){

    var dialogOptions by remember{ mutableStateOf(DialogOptions()) }
    var dialogVisibility by remember { mutableIntStateOf(DIALOG_HIDDEN) }

    if(dialogVisibility != DIALOG_HIDDEN) {
        Dialog(
            dialogOptions = dialogOptions,
            setVisibility = { dialogVisibility = it }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopNavBar(title = "Settings")
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            if(firebaseManager?.getCurrentUser() == null) {
                Setting(
                    title = "Account",
                    description = "Log in to synchronise your content",
                    hasSwitch = false,
                    onCheckedChange = {
                        mainActivity?.navigateTo("login")
                    }
                )
            }else{
                Setting(
                    title = "Account",
                    description = "Manage your account",
                    hasSwitch = false,
                    onCheckedChange = {
                        mainActivity?.navigateTo("account")
                    }
                )
            }
            if(firebaseManager?.isAdmin() == true){
                Setting(
                    title = "Manage Data",
                    description = "Add and remove entries in the database",
                    hasSwitch = false,
                    onCheckedChange = {
                        mainActivity?.navigateTo("db_manager")
                    }
                )
            }
        }

    }
}

/*
@Composable
fun LoginView(
    modifier: Modifier = Modifier
){
    Column(
        modifier = modifier.background(Color.Yellow)
    ) {
        Text(text = "Log in view")
    }
}
*/

/**
 * Returns a setting that can lead to a different page or has a switch to change a setting directly.
 * Both will be handled via the [onCheckedChange] method as it gets executed when the user taps on the setting
 * no matter if it has a switch or not.
 *
 * @param modifier A [Modifier] to adjust the content.
 * @param title The short big text indicating what the setting is about or where it leads.
 * @param description A smaller, more detailed text below the title.
 * @param isChecked The state of the switch.
 * @param onCheckedChange The method to be executed when the state of the switch changes.
 * @param hasSwitch Whether the setting has a switch or not.
 */
@Composable
private fun Setting(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    isChecked: LiveData<Boolean> = MutableLiveData(false),
    onCheckedChange: (Boolean) -> Unit,
    hasSwitch: Boolean = true){

    val checked by isChecked.observeAsState(isChecked.value == true)

    Box(modifier = modifier
        .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp, end = 8.dp, bottom = 4.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                description?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            }
            if(hasSwitch) {
                Switch(
                    checked = checked,
                    onCheckedChange = {
                        onCheckedChange(!checked)
                    })
            }
        }
    }
}

/**
 * Returns a dialog.
 *
 * @param dialogOptions The customization options of the dialog.
 * @param setVisibility The is the method that gets called when the visibility changes.
 * It could handle [DIALOG_HIDDEN], [DIALOG_CONTENT], [DIALOG_OPTIONS] and [DIALOG_TEXT] but currently is only required to handle [DIALOG_HIDDEN].
 */
@Composable
private fun Dialog(
    //modifier: Modifier = Modifier,
    dialogOptions: DialogOptions,
    setVisibility: (Int) -> Unit
){

    Log.d("Settings", "Starting with selection ${dialogOptions.itemSelected}")

    var selected by remember{ mutableIntStateOf(dialogOptions.itemSelected) }

    with(dialogOptions) {
        AlertDialog(
            onDismissRequest = { setVisibility(DIALOG_HIDDEN) },
            title = { Text(text = title) },
            text = {
                when (visibilityMode) {
                    DIALOG_TEXT -> {
                        Text(text = text)
                    }
                    DIALOG_OPTIONS -> {
                        Column() {
                            for(option in options){
                                MultipleChoiceOption(
                                    title = option.value.title,
                                    description = option.value.description,
                                    selected = (selected == option.key),
                                    onSelect = { selected = option.key }
                                )
                            }
                        }
                    }
                    DIALOG_CONTENT -> {
                        content()
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { setVisibility(DIALOG_HIDDEN) }) {
                    Text(text = "Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dialogConfirm(selected)
                    setVisibility(DIALOG_HIDDEN)
                }) {
                    Text(text = "Confirm")
                }
            }
        )
    }
}

/**
 * Returns an option field that can be used in a dialog.
 *
 * @param title The title of the option. It should be short and descriptive.
 * @param description A more detailed description of what the option does.
 * @param selected Whether the option is selected or not.
 * @param onSelect The code to be executed when the selection state changes.
 */
@Composable
private fun MultipleChoiceOption(
    title: String,
    description: String?,
    selected: Boolean,
    onSelect: () -> Unit
){
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = true, onClick = onSelect)
            .padding(vertical = 4.dp),
    ) {
        RadioButton(selected = selected, onClick = { onSelect() })
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge
            )
            description?.let { Text(text = description) }
        }
    }
}

/**
 * Represents all possible dialog customization options.
 *
 * @param title The title of the dialog.
 * @param text The text that will be displayed in the dialog if [visibilityMode] is [DIALOG_TEXT].
 * @param options The map of identifiers and [DialogItem] elements that will be displayed in the dialog if [visibilityMode] is [DIALOG_OPTIONS].
 * @param itemSelected The currently selected item. This only has an impact if there are options set and [visibilityMode] is [DIALOG_OPTIONS].
 * @param dialogConfirm The code to be executed when the primary button of the dialog is pressed.
 * @param visibilityMode Determines the visibility and the content of the dialog. Can be one of [DIALOG_HIDDEN], [DIALOG_TEXT], [DIALOG_OPTIONS] or [DIALOG_CONTENT].
 * @param content The freely describable content of the dialog. This only has an impact if [visibilityMode] is [DIALOG_CONTENT].
 */
class DialogOptions(
    var title: String = "",
    var text: String = "",
    var options: Map<Int, DialogItem> = mapOf(),
    var itemSelected: Int = 0,
    var dialogConfirm: (Int?) -> Unit = {},
    var visibilityMode: Int = DIALOG_HIDDEN,
    var content: @Composable () -> Unit = {  }
)

/**
 * Represents a dialog option and is used in [Dialog] to interpret the [DialogOptions] as [MultipleChoiceOption] elements.
 *
 * @param title The title of the option. Should be short and indicative of the function.
 * @param description A more detailed description of what the option does.
 */
class DialogItem(
    var title: String = "",
    var description: String? = null
)