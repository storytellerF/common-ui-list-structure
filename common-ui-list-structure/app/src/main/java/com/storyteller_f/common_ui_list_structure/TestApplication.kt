package com.storyteller_f.common_ui_list_structure

import androidx.multidex.MultiDexApplication

class TestApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        val list = listOf(com.storyteller_f.common_ui_list_structure.adapter_produce.HolderBuilder::add)
        list.fold(0) { acc, kFunction1 ->
            kFunction1(acc)
        }
    }
}