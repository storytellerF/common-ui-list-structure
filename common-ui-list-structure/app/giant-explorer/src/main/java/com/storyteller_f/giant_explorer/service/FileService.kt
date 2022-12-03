package com.storyteller_f.giant_explorer.service

import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager

class FileService : RootService() {

    override fun onBind(intent: Intent): IBinder {
        return FileSystemManager.getService()
    }
}