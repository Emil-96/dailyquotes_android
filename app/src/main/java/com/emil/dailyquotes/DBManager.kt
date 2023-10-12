package com.emil.dailyquotes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.service.credentials.Action
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

val csvElementList: ArrayList<CsvElement>? = null

class DBManager: ViewModel(){

    private val _importedLines: MutableLiveData<List<CsvElement>> = MutableLiveData()
    val importedLines: LiveData<List<CsvElement>> = _importedLines

    init {
        // Initialize LiveData objects with empty data or your default values
        _importedLines.value = listOf()
    }

    fun addElement(element: CsvElement){
        val currentList = _importedLines.value.orEmpty().toMutableList()
        currentList.add(element)
        _importedLines.postValue(currentList)
        Log.d("CSV", "Line count: ${currentList.size} - imported: ${element.quote}")
    }

    fun addElements(elements: ArrayList<CsvElement>){
        _importedLines.postValue(elements.toList())
        Log.d("CSV", "Added ${elements.size} lines")
    }

    fun clearElements(){
        _importedLines.postValue(emptyList())
    }

    fun uploadCsvElements(onSuccess: () -> Unit){
        importedLines.value?.let {
            firebaseManager?.uploadCsvElements(
                elements = it,
                onSuccess = onSuccess
            )
        }
    }

}

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

@Composable
fun CsvListItem(element: CsvElement){
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

@Composable
fun CsvListItemText(
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

fun openCsv(){

    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
    //intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "*/*"

    csvImportLauncher?.launch(intent)
}

fun parseCsvUri(uri: Uri, dbManager: DBManager){
    try {

        var csvList = arrayListOf<CsvElement>()

        val inputStream = mainActivity?.contentResolver?.openInputStream(uri)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        var line: String? = bufferedReader.readLine()
        while (line != null) {
            //Log.d("CSV", line)
            val newElement = parseCsvElement(line, dbManager)
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

fun parseCsvElement(csvLine: String, dbManager: DBManager): CsvElement? {
    val parts = csvLine.split("\t")
    return if(parts.size == 5 && parts[0].isNotEmpty()){
        //Log.d("CSV", "adding ${parts[2]}")
        var quote = parts[2]
        if(quote[0] == '"' && quote[quote.lastIndex] == '"'){
            quote = quote.substring(1, quote.lastIndex - 1)
        }
        CsvElement(
            category = parts[1],
            quote = quote,
            imageLink = parts[3],
            quoteUrl = parts[4]
        )
    }else{
        null
    }
}

data class CsvElement(
    val category: String,
    val quote: String,
    val imageLink: String,
    val quoteUrl: String
)