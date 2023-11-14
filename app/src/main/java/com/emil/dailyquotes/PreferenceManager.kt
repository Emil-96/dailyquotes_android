package com.emil.dailyquotes

import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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

/**
 * A class to handle all interaction with the local storage.
 */
class PreferenceManager{

    private var _quote: MutableLiveData<Quote> = MutableLiveData()
    val quote: LiveData<Quote> = _quote

    private var quoteDatabaseVersion: Long = 0L

    /**
     * The constructor.
     */
    init {
        loadInfo {
            firebaseManager?.loadInfo()
        }
    }

    /**
     * Loads the saved data of the information about the remote database at the time of the last fetch.
     *
     * @param onFinished The code to be executed once the information is retrieved.
     */
    private fun loadInfo(onFinished: () -> Unit){
        mainActivity?.lifecycleScope?.launch {
            mainActivity?.dataStore?.data?.collect{ preferences ->
                quoteDatabaseVersion = preferences[longPreferencesKey(PREFERENCE_KEY_VERSION)] ?: 0L
                onFinished()
            }
        }
    }

    /**
     * Saves the info to the local storage.
     *
     * @param databaseVersion The version number of the database at that time.
     */
    fun saveInfo(
        databaseVersion: Long
    ){
        mainActivity?.lifecycleScope?.launch {
            mainActivity?.dataStore?.edit { mutablePreferences ->
                mutablePreferences[longPreferencesKey(PREFERENCE_KEY_VERSION)] = databaseVersion
            }
        }
    }

    /**
     * @return The database version only if it has been retrieved first.
     */
    fun getLocalDatabaseVersion(): Long{
        return quoteDatabaseVersion
    }

    /**
     * Loads the quote that has been saved to the local storage as the quote of the day.
     */
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

    /**
     * Saves a given [Quote] element to the local storage as the quote of the day.
     *
     * @param quote The quote to be saved.
     */
    private suspend fun saveDailyQuote(quote: Quote){
        mainActivity?.dataStore?.edit {  mutablePreferences ->
            mutablePreferences[stringPreferencesKey(PREFERENCE_KEY_QUOTE)] = parseQuoteToJson(quote)
            mutablePreferences[stringPreferencesKey(PREFERENCE_KEY_DATE)] = LocalDate.now().toString()
            Log.d("PreferenceManager", "Saved quote at date ${Calendar.DATE} ${parseQuoteToJson(quote)}")
        }
    }
}

