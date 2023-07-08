package com.storyteller_f.file_system.instance.local

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.instance.BaseContextFileInstance

/**
 * 定义接口，方法
 */
abstract class LocalFileInstance(context: Context, uri: Uri) : BaseContextFileInstance(context, uri) {
    companion object {
        private const val TAG = "FileInstance"
    }
}
