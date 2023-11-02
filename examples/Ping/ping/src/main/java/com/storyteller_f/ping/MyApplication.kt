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

/**
 * 点击一个视频进入预览页时设置。在用户点击应用时，app 还无法立刻把结果保存到selected
 * 所以在展示的时候有限使用preview 定义的uri，同时在selected 设置完成后，preview 应该清除
 */
val preview = stringPreferencesKey("preview")
val selected = stringPreferencesKey("selected")