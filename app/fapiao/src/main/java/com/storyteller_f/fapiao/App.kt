package com.storyteller_f.fapiao

import android.app.Application
import com.storyteller_f.fapiao.adapter_produce.Temp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Temp.add()
        PDFBoxResourceLoader.init(applicationContext);
    }
}