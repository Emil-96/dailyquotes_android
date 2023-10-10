package com.emil.dailyquotes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseManager{
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private var _name: MutableLiveData<String> = MutableLiveData("")
    private val name: LiveData<String> = _name

    init {
        loadName()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    private fun loadName(){
        auth.uid?.let{ userId ->
            db
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { snapshot ->
                    _name.postValue(snapshot.get("name").toString())
                }
        }
    }

    fun getName(): LiveData<String> {
        return name
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

}