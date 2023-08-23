package com.storyteller_f.file_system_ktx

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system_remote.getRemoteInstance
import com.storyteller_f.file_system_remote.supportScheme
import com.storyteller_f.file_system_root.RootAccessFileInstance

fun getFileInstance(
    context: Context,
    uri: Uri,
): FileInstance {
    val scheme = uri.scheme!!
    return when {
        scheme == RootAccessFileInstance.rootFileSystemScheme -> RootAccessFileInstance.instance(uri)!!
        supportScheme.contains(scheme) -> {
            getRemoteInstance(uri)
        }
        else -> FileInstanceFactory.getFileInstance(context, uri)
    }
}