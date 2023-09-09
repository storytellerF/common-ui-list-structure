package com.storyteller_f.common_ui_list_structure

import androidx.multidex.MultiDexApplication
import com.google.android.material.color.DynamicColors
import com.storyteller_f.ui_list.core.holders

class TestApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        holders(com.storyteller_f.common_ui_list_structure.ui_list.HolderBuilder::add)
    }
}
