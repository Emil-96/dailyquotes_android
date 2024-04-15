package com.emil.dailyquotes

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.emil.dailyquotes.room.Quote
import com.google.firebase.firestore.DocumentSnapshot
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Used for all interaction when importing and uploading data from a CSV file.
 *
 * Should only be used by administrators.
 */
class DBManager(
    private val firebaseManager: FirebaseManager
): ViewModel(){

    private val _importedLines: MutableLiveData<List<Quote>> = MutableLiveData()
    val importedLines: LiveData<List<Quote>> = _importedLines

    /**
     * The constructor.
     */
    init {
        // Initialize LiveData objects with empty data or your default values
        _importedLines.value = listOf()
    }

    /**
     * Add all elements to the temporary saved quotes to be used later.
     *
     * @param elements The [Quote] elements that should be saved.
     */
    fun addElements(elements: ArrayList<Quote>){
        _importedLines.postValue(elements.toList())
        Log.d("CSV", "Added ${elements.size} lines")
    }

    /**
     * Clear all temporary saved quotes.
     */
    fun clearElements(){
        _importedLines.postValue(emptyList())
    }

    /**
     * Start uploading all temporary saved quotes to the remote backend.
     *
     * @param onSuccess Code that will be executed when the upload succeeds.
     */
    fun uploadCsvElements(onSuccess: () -> Unit){
        importedLines.value?.let {
            firebaseManager.uploadCsvElements(
                elements = it,
                onSuccess = onSuccess
            )
        }
    }

}

/**
 * Returns the database management page.
 *
 * @param dbManager The database manager to act as an interface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DBManagerPage(dbManager: DBManager){

    val csvItems = dbManager.importedLines.observeAsState(emptyList())

    Scaffold(
        topBar = { TopNavBar(title = "Add data") },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {

                var uploading by remember { mutableStateOf(false) }

                if(csvItems.value.isNotEmpty()) {
                    if(!uploading) {
                        ExtendedFloatingActionButton(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            //modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                            onClick = {
                                dbManager.clearElements()
                            }
                        ) {
                            Icon(
                                modifier = Modifier.padding(end = 8.dp),
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = "reset"
                            )
                            Text(text = "Reset")
                        }
                    }
                    ExtendedFloatingActionButton(
                        //modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        onClick = {
                            if(!uploading) {
                                dbManager.uploadCsvElements {
                                    mainActivity?.back()
                                }
                                uploading = true
                            }
                        }
                    ) {
                        if(uploading){
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                strokeWidth = 4.dp
                            )
                        }else {
                            Icon(
                                modifier = Modifier.padding(end = 8.dp),
                                painter = painterResource(id = R.drawable.ic_upload),
                                contentDescription = "upload"
                            )
                            Text(text = "Upload")
                        }
                    }
                }else {
                    ExtendedFloatingActionButton(
                        //modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        onClick = {
                            openCsv()
                        }
                    ) {
                        Icon(
                            modifier = Modifier.padding(end = 8.dp),
                            painter = painterResource(id = R.drawable.ic_upload),
                            contentDescription = "import"
                        )
                        Text(text = "Import from CSV")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedCard{
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    text = "Imported ${csvItems.value.size} items"
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ){
                items(csvItems.value){ element ->
                    CsvListItem(element = element)
                }
            }
        }
    }
}

/**
 * Returns a list item for the preview of the temporary saved quotes.
 *
 * @param element The quote that should be displayed.
 */
@Composable
private fun CsvListItem(element: Quote){
    Row (
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ){
        CsvListItemText(modifier = Modifier.width(86.dp), string = element.category)
        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .padding(vertical = 2.dp)
        )
        CsvListItemText(string = element.quote)
    }
}

/**
 * Returns the text of list item.
 *
 * To be used in [CsvListItem].
 *
 * @param modifier A [Modifier] to adjust the content.
 * @param string The text to be displayed.
 */
@Composable
private fun CsvListItemText(
    modifier: Modifier = Modifier,
    string: String
){
    Text(
        modifier = modifier,
        text = string,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Launch the system file chooser to select a CSV file from which the data should be imported.
 */
private fun openCsv(){

    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
    //intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "*/*"

    csvImportLauncher?.launch(intent)
}

/**
 * Parse the selected CSV file and save the content to the temporary saved quotes for further processing.
 *
 * @param uri The [Uri] pointing to the selected CSV file.
 * @param dbManager The database manager to save the content of the CSV file to.
 */
fun parseCsvUri(uri: Uri, dbManager: DBManager){
    try {

        val csvList = arrayListOf<Quote>()

        val inputStream = mainActivity?.contentResolver?.openInputStream(uri)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        var line: String? = bufferedReader.readLine()
        while (line != null) {
            //Log.d("CSV", line)
            val newElement = parseQuoteFromCsv(line)
            newElement?.let { csvList.add(it) }
            line = bufferedReader.readLine()
        }
        dbManager.addElements(csvList)
        bufferedReader.close()
        inputStream?.close()
    } catch(error: IOException){
        Log.e("FileImport", "Error reading file")
    }
}

/**
 * Takes in a line from the CSV file and parses it to a [Quote].
 *
 * @param csvLine The line form the CSV file.
 *
 * @return The parsed [Quote] element.
 */
private fun parseQuoteFromCsv(csvLine: String): Quote? {
    val parts = csvLine.split("\t")
    return if(parts.size == 5 && parts[0].isNotEmpty()){
        //Log.d("CSV", "adding ${parts[2]}")
        var quote = parts[2]
        if(quote[0] == '"' && quote[quote.lastIndex] == '"'){
            quote = quote.substring(1, quote.lastIndex - 1)
        }
        Quote(
            id = "0",
            category = parts[1],
            quote = quote,
            imageUrl = parts[3],
            quoteUrl = parts[4]
        )
    }else{
        null
    }
}

/**
 * Takes a JSON String and parses it to a [Quote].
 *
 * @param jsonQuote The JSON String representing a quote.
 *
 * @return The parsed [Quote] element.
 */
fun parseQuote(jsonQuote: String): Quote? {
    return Gson().fromJson(jsonQuote, Quote::class.java)
}

/**
 * Takes a [Quote] element and parses it to a JSON String.
 *
 * @param quote The [Quote] element to be parsed.
 *
 * @return The parsed JSON String.
 */
fun parseQuoteToJson(quote: Quote): String{
    return Gson().toJson(quote).toString()
}