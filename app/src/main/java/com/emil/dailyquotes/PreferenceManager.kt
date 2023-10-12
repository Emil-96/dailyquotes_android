package com.emil.dailyquotes

import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

const val PREFERENCE_KEY_QUOTE = "daily_quote"
const val PREFERENCE_KEY_DATE = "daily_date"

class PreferenceManager(){

    private var _quote: MutableLiveData<Quote> = MutableLiveData()
    val quote: LiveData<Quote> = _quote
    
    fun loadDailyQuote(){
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

