package com.storyteller_f.ping

import android.app.Application
import com.storyteller_f.ping.adapter_produce.Temp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Temp.add()
    }
}