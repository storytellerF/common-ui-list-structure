package com.storyteller_f.giant_explorer

import android.app.Application
import com.storyteller_f.giant_explorer.adapter_produce.Temp

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Temp.add()
    }
}