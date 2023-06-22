package com.storyteller_f.giant_explorer.service

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.IBinder
import android.util.Log

class FileOperateService : Service() {
    override fun onCreate() {
        Log.d(TAG, "onCreate() called")
        super.onCreate()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called")
        super.onDestroy()
    }

    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind() called with: intent = [$intent]")
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind() called with: intent = [$intent]")
        return super.onUnbind(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "onConfigurationChanged() called with: newConfig = [$newConfig]")
        super.onConfigurationChanged(newConfig)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand() called with: intent = [$intent], flags = [$flags], startId = [$startId]")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return FileOperateBinder(applicationContext)
    }

    interface FileOperateResultContainer {
        /**
         * @param uri 去路径
         * @param originUri 来路径
         */
        fun onSuccess(uri: Uri?, originUri: Uri?)
        fun onError(errorMessage: String?)
        fun onCancel()
    }

    companion object {
        private const val TAG = "FileOperateService"
    }
}