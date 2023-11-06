package com.emil.dailyquotes

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.emil.dailyquotes.room.Quote
import com.emil.dailyquotes.room.QuoteDao
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlin.random.Random

private val NAME_KEY = "firebase_display_name"

class FirebaseManager(private val context: Context){
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private var _name: MutableLiveData<String> = MutableLiveData("")
    private val name: LiveData<String> = _name

    private var isAdmin = false

    private var quoteDao: QuoteDao? = quoteDatabase?.quoteDao()
    private var localDatabaseSize: Int = 0

    init {
        loadUserInfo()
        log("Trying to load preferences")
        mainActivity?.lifecycleScope?.launch {
            localDatabaseSize = quoteDao?.getAll()?.size ?: 0
            context.dataStore.data
                .collect { preferences ->
                    _name.postValue(preferences[stringPreferencesKey(NAME_KEY)] ?: "")
                    log("Loaded name")
                }
        }
    }

    fun loadInfo(){
        quoteDao = quoteDatabase?.quoteDao()
        db.collection("info").document("quotes").get().addOnSuccessListener { document ->
            val remoteDatabaseVersion = (document.data?.get("version") as Long).toInt()
            if(remoteDatabaseVersion < (preferenceManager?.getLocalDatabaseVersion() ?: 1)){
                loadAllQuotes{
                    preferenceManager?.saveInfo(remoteDatabaseVersion)
                }
            }
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    private fun loadUserInfo(){
        auth.uid?.let{ userId ->
            db
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { snapshot ->

                    val name = snapshot.get("name").toString()
                    mainActivity?.lifecycleScope?.launch{
                        saveName(name)
                    }
                    _name.postValue(name)

                    isAdmin = (snapshot.get("admin") ?: false) as Boolean
                }
        }
    }

    private suspend fun saveName(name: String){
        context.dataStore.edit {  mutablePreferences ->
            mutablePreferences[stringPreferencesKey(NAME_KEY)] = name
            log("Saved name ($name)")
        }
    }

    fun getName(): LiveData<String> {
        return name
    }

    fun isAdmin(): Boolean{
        return isAdmin
    }

    fun register(
        name: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {}
    ){
        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
            auth.uid?.let { userId ->
                val userMap = hashMapOf(
                    "name" to name
                )
                db
                    .collection("users")
                    .document(userId)
                    .set(userMap)
                    .addOnSuccessListener {
                        onSuccess()
                    }
            }
        }.addOnFailureListener {
            onFailure()
        }
    }

    fun logIn(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {}
    ){
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener {
            onFailure()
        }
    }

    fun logOut(
        onSuccess: () -> Unit
    ){
        auth.signOut()
        onSuccess()
    }

    private fun log(message: String){
        Log.d("FirebaseManager", message)
    }

    fun uploadCsvElements(elements: List<Quote>, onSuccess: () -> Unit){
        if(!isAdmin){
            onSuccess()
            return
        }


        for (subList in formatCsvElementsToBatches(elements)) {
            db.runBatch { batch ->
                for(batchItem in subList) {
                    batch.set(db.collection("quotes").document(), batchItem)
                }
            }.addOnSuccessListener {
                onSuccess()
            }
        }

    }

    private fun formatCsvElementsToBatches(elements: List<Quote>, batchSize: Int = 500): ArrayList<List<Map<String, String>>>{

        val fullMapList: ArrayList<Map<String, String>> = arrayListOf()

        for(element in elements){
            fullMapList.add(getMapFromCsvElement(element))
        }

        val outputListOfLists: ArrayList<List<Map<String, String>>> = arrayListOf()

       var currentSteppedIndex = 0

       while(currentSteppedIndex < fullMapList.size){
           val endIndex = (currentSteppedIndex + batchSize).coerceAtMost(fullMapList.size)
           val subList = fullMapList.subList(currentSteppedIndex, endIndex)
           outputListOfLists.add(subList)
           currentSteppedIndex = endIndex
       }

        return outputListOfLists

    }

    private fun getMapFromCsvElement(element: Quote): HashMap<String, String> {

        return hashMapOf(
            "category" to element.category,
            "quote" to element.quote,
            "image_url" to element.imageUrl,
            "quote_url" to element.quoteUrl
        )

    }

    private fun loadAllQuotes(onSuccess: () -> Unit = {}){

        log("Loading all quotes")

        val quoteDao = quoteDatabase?.quoteDao()

        quoteDao?.let {
            db.collection("quotes").get().addOnSuccessListener { snapshot ->
                mainActivity?.lifecycleScope?.launch {
                    for (document in snapshot.documents) {
                        quoteDao.insertAll(parseQuote(document))
                    }
                    localDatabaseSize = quoteDao.getAll().size
                    onSuccess()
                }
            }
        }
    }

    fun getRandomQuote(getQuote: (Quote) -> Unit){

        if(localDatabaseSize != 0){
            getRandomQuoteFromLocalDatabase(getQuote)
            log("retrieving random quote from local db")
            return
        }

        loadAllQuotes(onSuccess = {
            getRandomQuoteFromLocalDatabase(getQuote)
            log("retrieving random quote after downloading all quotes")
        })

    }

    private fun getRandomQuoteFromLocalDatabase(getQuote: (Quote) -> Unit){
        mainActivity?.lifecycleScope?.launch {
            quoteDao?.getAll().also {
                log("getAll() returns $it")
                it?.let { quotes ->
                    val randomIndex = Random.nextInt(quotes.size)
                    getQuote(quotes[randomIndex])
                    log("retrieved '${quotes[randomIndex].quote}'")
                }
            }
        }
    }

}

fun parseQuote(documentSnapshot: DocumentSnapshot): Quote {
    return Quote(
        id = documentSnapshot.id,
        category = documentSnapshot["category"] as String,
        quote = documentSnapshot["quote"] as String,
        quoteUrl = documentSnapshot["quote_url"] as String,
        imageUrl = documentSnapshot["image_url"] as String
    )
}