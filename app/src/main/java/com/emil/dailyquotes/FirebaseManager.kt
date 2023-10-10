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

}