package com.storyteller_f.fapiao

import android.app.Application
import com.storyteller_f.fapiao.adapter_produce.HolderBuilder
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        listOf(HolderBuilder::add).fold(0) { acc, kFunction1 ->
            kFunction1(acc)
        }
        PDFBoxResourceLoader.init(applicationContext);
    }
}