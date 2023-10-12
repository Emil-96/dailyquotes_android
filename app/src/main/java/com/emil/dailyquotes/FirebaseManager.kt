package com.emil.dailyquotes

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
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

    init {
        loadUserInfo()
        log("Trying to load preferences")
        mainActivity?.lifecycleScope?.launch {
            context.dataStore.data
                .collect { preferences ->
                    _name.postValue(preferences[stringPreferencesKey(NAME_KEY)] ?: "")
                    log("Loaded name")
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

        val map = hashMapOf(
            "category" to element.category,
            "quote" to element.quote,
            "image_url" to element.imageLink,
            "quote_url" to element.quoteUrl
        )

        return map

    }

    fun getRandomQuote(getQuote: (Quote) -> Unit){
        db.collection("quotes").get().addOnSuccessListener {  snapshot ->
            //Log.d("Firestore", "Quotes:\n${snapshot.toString()}")
            val randomIndex = Random.nextInt(snapshot.documents.size)
            parseQuote(snapshot.documents[randomIndex])?.let{ randomQuote ->
                getQuote(randomQuote)
            }
        }
    }

}