package com.storyteller_f

import android.app.Activity
import android.app.Application
import com.google.gson.Gson

class App : Application() {
    val gson by lazy {
        Gson()
    }
}

fun Activity.requireGson() = (application as App).gson