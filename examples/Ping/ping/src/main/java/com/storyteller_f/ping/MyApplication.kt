package com.storyteller_f.ping

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.material.color.DynamicColors
import com.storyteller_f.ping.ui_list.HolderBuilder
import com.storyteller_f.ui_list.core.holders

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        holders(HolderBuilder::add)
    }
}


// At the top level of your kotlin file:
val Context.dataStore by preferencesDataStore(name = "settings")
val preview = stringPreferencesKey("preview")
val selected = stringPreferencesKey("selected")