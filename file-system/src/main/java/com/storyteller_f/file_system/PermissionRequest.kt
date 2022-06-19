package com.storyteller_f.file_system

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CompletableDeferred

suspend fun Context.requestPermissionForSpecialPath(path: String) {
    val task = CompletableDeferred<Boolean>()
    when {
        path.startsWith(FileInstanceFactory.rootUserEmulatedPath) -> {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> requestManageExternalPermission(
                    task
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> requestSAF(
                    path,
                    MainActivity.REQUEST_SAF_EMULATED, task
                )
                else -> requestWriteExternalStorage(task)
            }
        }
        path == FileInstanceFactory.storagePath -> {
            Log.w(
                "requestPermission",
                "checkPermission: 当前还不会的对整体的/storage请求权限"
            )
        }
        path.startsWith(FileInstanceFactory.storagePath) -> {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> requestManageExternalPermission(
                    task
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> requestSAF(
                    path,
                    MainActivity.REQUEST_SAF_SDCARD,
                    task
                )
                else -> requestWriteExternalStorage(task)
            }
        }
    }
    task.await()
}

private suspend fun Context.requestWriteExternalStorage(task: CompletableDeferred<Boolean>) {
    if (yesOrNo("权限不足", "查看文件夹系统必须授予权限", "授予", "取消")) {
        MainActivity.task = task
        startActivity(Intent(this, MainActivity::class.java).apply {
            MainActivity.getBundle(MainActivity.REQUEST_EMULATED, "", this)
        })
    } else {
        task.complete(false)

    }
}

private suspend fun Context.requestSAF(
    path: String,
    requestCodeSAF: String,
    task: CompletableDeferred<Boolean>
) {
    if (yesOrNo("需要授予权限", "在Android 10 或者访问存储卡，需要获取SAF权限", "去授予", "取消")) {
        MainActivity.task = task
        startActivity(Intent(this, MainActivity::class.java).apply {
            MainActivity.getBundle(requestCodeSAF, path, this)
        })
    } else {
        task.complete(false)
    }
}

@RequiresApi(api = Build.VERSION_CODES.R)
private suspend fun Context.requestManageExternalPermission(task: CompletableDeferred<Boolean>) {
    if (yesOrNo("需要授予权限", "在Android 11上，需要获取Manage External Storage权限", "去授予", "取消")) {
        MainActivity.task = task
        startActivity(Intent(this, MainActivity::class.java).apply {
            MainActivity.getBundle(MainActivity.REQUEST_MANAGE, "", this)
        })
    } else {
        task.complete(false)

    }
}

private suspend fun Context.yesOrNo(
    title: String,
    message: String,
    yesString: String,
    noString: String
): Boolean {
    val t = CompletableDeferred<Boolean>()
    AlertDialog.Builder(this).setTitle(title)
        .setMessage(message)
        .setPositiveButton(yesString) { _: DialogInterface?, _: Int ->
            t.complete(true)
        }
        .setNegativeButton(noString) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            t.complete(false)
        }.show()
    return t.await()
}