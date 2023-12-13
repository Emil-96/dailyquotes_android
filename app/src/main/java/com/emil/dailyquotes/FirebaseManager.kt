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
private val EMAIL_KEY = "firebase_email"

/**
 * A class to handle all interaction with the remote backend.
 *
 * @param context A context object required to save certain things locally.
 */
class FirebaseManager(private val context: Context){
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private var _name: MutableLiveData<String> = MutableLiveData("")
    private var _email: MutableLiveData<String> = MutableLiveData("")
    private val name: LiveData<String> = _name
    private val email: LiveData<String> = _email

    private var isAdmin = false

    private var quoteDao: QuoteDao? = quoteDatabase?.quoteDao()
    private var localDatabaseSize: Int = 0

    /**
     * The constructor.
     */
    init {
        loadUserInfo()
        log("Trying to load preferences")
        mainActivity?.lifecycleScope?.launch {
            localDatabaseSize = quoteDao?.getAll()?.size ?: 0
            context.dataStore.data
                .collect { preferences ->
                    _name.postValue(preferences[stringPreferencesKey(NAME_KEY)] ?: "")
                    _email.postValue(preferences[stringPreferencesKey(EMAIL_KEY)] ?: "")
                    log("Loaded name (${name.value}) and email (${email.value})")
                }
        }
    }

    /**
     * @return True if a user is signed in.
     */
    fun isSignedIn(): Boolean{
        return auth.currentUser != null
    }

    /**
     * Fetches the database information of the remote database.
     */
    fun loadInfo(){
        quoteDao = quoteDatabase?.quoteDao()
        db.collection("info").document("quotes").get().addOnSuccessListener { document ->
            val remoteDatabaseVersion = (document.data?.get("version") as Long)
            if(remoteDatabaseVersion < (preferenceManager?.getLocalDatabaseVersion() ?: 1)){
                loadAllQuotes{
                    preferenceManager?.saveInfo(remoteDatabaseVersion)
                }
            }
        }
    }

    /**
     * @return The current Firebase user.
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Fetches the user information and save the result locally.
     */
    private fun loadUserInfo(){
        log("Loading user info")
        auth.uid?.let{ userId ->
            db
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { snapshot ->

                    val name = snapshot.get("name").toString()
                    val email = auth.currentUser?.email

                    log("retrieved name \"$name\" and email \"$email\"")

                    mainActivity?.lifecycleScope?.launch{
                        saveUserInfo(name, email ?: "")
                    }
                    _name.postValue(name)

                    isAdmin = (snapshot.get("admin") ?: false) as Boolean
                }
        }
    }

    /**
     * Saves the user info to the local storage.
     *
     * @param name The name to be saved.
     * @param email The email to be saved.
     */
    private suspend fun saveUserInfo(name: String, email: String){
        context.dataStore.edit {  mutablePreferences ->
            mutablePreferences[stringPreferencesKey(NAME_KEY)] = name
            mutablePreferences[stringPreferencesKey(EMAIL_KEY)] = email
            log("Saved name ($name) and email ($email)")
        }
    }

    /**
     * @return The name of the user.
     */
    fun getName(): LiveData<String> {
        return name
    }

    /**
     * @return The email of the user.
     */
    fun getEmail(): LiveData<String> {
        return email
    }

    /**
     * @return The admin state of the user.
     */
    fun isAdmin(): Boolean{
        return isAdmin
    }

    /**
     * Registers a user to the remote backend.
     *
     * @param name The name of the user.
     * @param email The email address of the user.
     * @param password The password of the user.
     * @param onSuccess The code that should be executed when the registration succeeds.
     * @param onFailure The code that should be executed when the registration fails.
     */
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

    /**
     * Logs in a user to the remote backend.
     *
     * @param email The email address of the user.
     * @param password The password of the user.
     * @param onSuccess The code that should be executed when the login succeeds.
     * @param onFailure The code that should be executed when the login fails.
     */
    fun logIn(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {}
    ){
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
            loadUserInfo()
            onSuccess()
        }.addOnFailureListener {
            onFailure()
        }
    }

    /**
     * Logs out the current user.
     *
     * @param onSuccess The code to be executed when the user is logged out.
     */
    fun logOut(
        onSuccess: () -> Unit
    ){
        auth.signOut()
        mainActivity?.lifecycleScope?.launch {
            saveUserInfo("", "")
            isAdmin = false
            onSuccess()
        }
    }

    /**
     * A debugging method to log a message to the console.
     *
     * @param message The message to be logged.
     */
    private fun log(message: String){
        Log.d("FirebaseManager", message)
    }

    /**
     * Uploads a list of [Quote] elements to the remote database.
     *
     * @param elements The list of [Quote] elements that to be uploaded.
     * @param onSuccess The code to be executed when the upload succeeds.
     */
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

    /**
     * Creates batches of [Map] elements representing [Quote] elements.
     *
     * Uploading to Firebase supports only batches of size up to 500 elements (as of 2023),
     * so the list of all [Quote] elements cannot be simply converted to a corresponding list of [Map] elements of the same size
     * but has to be cut into multiple smaller lists.
     *
     * @param elements The list of all [Quote] elements to be uploaded.
     * @param batchSize The size of the batches.
     */
    private fun formatCsvElementsToBatches(elements: List<Quote>, batchSize: Int = 500): ArrayList<List<Map<String, String>>>{

        val fullMapList: ArrayList<Map<String, String>> = arrayListOf()

        for(element in elements){
            fullMapList.add(parseQuoteToMap(element))
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

    /**
     * Takes a [Quote] element and turns it into a [Map] element that is compatible with the remote database.
     *
     * @param element The [Quote] to be parsed.
     *
     * @return The corresponding [Map] element representing the [Quote] element.
     */
    private fun parseQuoteToMap(element: Quote): HashMap<String, String> {

        return hashMapOf(
            "category" to element.category,
            "quote" to element.quote,
            "image_url" to element.imageUrl,
            "quote_url" to element.quoteUrl
        )

    }

    /**
     * Fetches all quotes from the remote database and saves them to the local database.
     *
     * @param onSuccess The code to be executed when the fetch succeeds.
     */
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

    /**
     * Selects a random quote. Tries to get a random quote from the local database.
     * If that fails, it fetches the remote database and selects a random quote from there.
     *
     * @param getQuote The code that takes in a [Quote] element and processes it once a random quote is selected.
     */
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

    /**
     * Selects a random quote from the local database.
     *
     * It is supposed to only be used as a helper in [getRandomQuote].
     *
     * @param getQuote The code that takes in a [Quote] element and processes it once a random quote is selected.
     */
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

/**
 * Takes in a [DocumentSnapshot] representing a quote and parses it to a [Quote] element.
 *
 * @param documentSnapshot The document representing a quote.
 *
 * @return The parsed [Quote] element.
 */
private fun parseQuote(documentSnapshot: DocumentSnapshot): Quote {
    return Quote(
        id = documentSnapshot.id,
        category = documentSnapshot["category"] as String,
        quote = documentSnapshot["quote"] as String,
        quoteUrl = documentSnapshot["quote_url"] as String,
        imageUrl = documentSnapshot["image_url"] as String
    )
}