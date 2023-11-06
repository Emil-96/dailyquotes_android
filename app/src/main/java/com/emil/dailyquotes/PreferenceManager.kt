package com.emil.dailyquotes

import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.emil.dailyquotes.room.Quote
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

const val PREFERENCE_KEY_QUOTE = "daily_quote"
const val PREFERENCE_KEY_DATE = "daily_date"
const val PREFERENCE_KEY_VERSION = "database_version"

class PreferenceManager{

    private var _quote: MutableLiveData<Quote> = MutableLiveData()
    val quote: LiveData<Quote> = _quote

    private var quoteDatabaseVersion: Int = 0

    init {
        loadInfo {
            firebaseManager?.loadInfo()
        }
    }

    private fun loadInfo(onFinished: () -> Unit){
        mainActivity?.lifecycleScope?.launch {
            mainActivity?.dataStore?.data?.collect{ preferences ->
                quoteDatabaseVersion = preferences[intPreferencesKey(PREFERENCE_KEY_VERSION)] ?: 0
                onFinished()
            }
        }
    }

    fun saveInfo(
        databaseVersion: Int
    ){
        mainActivity?.lifecycleScope?.launch {
            mainActivity?.dataStore?.edit { mutablePreferences ->
                mutablePreferences[intPreferencesKey(PREFERENCE_KEY_VERSION)] = databaseVersion
            }
        }
    }

    fun getLocalDatabaseVersion(): Int{
        return quoteDatabaseVersion
    }
    
    fun loadDailyQuote(){
        Log.d("PreferenceManager", "loading random quote")
        mainActivity?.lifecycleScope?.launch {
            mainActivity?.dataStore?.data?.collect { preferences ->
                val savedDate = preferences[stringPreferencesKey(PREFERENCE_KEY_DATE)]
                val savedQuote = preferences[stringPreferencesKey(PREFERENCE_KEY_QUOTE)]

                if (savedDate == LocalDate.now().toString()) {
                    savedQuote?.let { jsonQuote ->
                        _quote.postValue(parseQuote(jsonQuote))
                    }
                } else {
                    firebaseManager?.getRandomQuote { fetchedQuote ->
                        _quote.postValue(fetchedQuote)
                        mainActivity?.lifecycleScope?.launch {
                            saveDailyQuote(fetchedQuote)
                        }
                    }
                }
            }
        }
    }

    private suspend fun saveDailyQuote(quote: Quote){
        mainActivity?.dataStore?.edit {  mutablePreferences ->
            mutablePreferences[stringPreferencesKey(PREFERENCE_KEY_QUOTE)] = parseQuoteToJson(quote)
            mutablePreferences[stringPreferencesKey(PREFERENCE_KEY_DATE)] = LocalDate.now().toString()
            Log.d("PreferenceManager", "Saved quote at date ${Calendar.DATE} ${parseQuoteToJson(quote)}")
        }
    }
}

