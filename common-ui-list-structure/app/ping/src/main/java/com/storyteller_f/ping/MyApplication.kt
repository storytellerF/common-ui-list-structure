package com.storyteller_f.ping

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.storyteller_f.ping.adapter_produce.HolderBuilder

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        listOf(HolderBuilder::add).fold(0) { acc, kFunction1 ->
            kFunction1(acc)
        }
    }
}


// At the top level of your kotlin file:
val Context.dataStore by preferencesDataStore(name = "settings")
val preview = stringPreferencesKey("preview")
val selected = stringPreferencesKey("selected")