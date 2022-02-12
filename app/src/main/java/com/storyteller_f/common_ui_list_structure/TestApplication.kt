package com.storyteller_f.common_ui_list_structure

import androidx.multidex.MultiDexApplication
import com.storyteller_f.common_ui_list_structure.adapter_produce.Temp.add

class TestApplication: MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        add()
    }
}