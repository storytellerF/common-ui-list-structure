package com.storyteller_f.file_system

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

suspend fun ComponentActivity.requestPermissionForSpecialPath(path: String) {
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
                "checkPermission: 当前还不会的对整体的/storage/请求权限"
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

private fun ComponentActivity.requestWriteExternalStorage(task: CompletableDeferred<Boolean>) {
    AlertDialog.Builder(this)
        .setTitle("权限不足")
        .setMessage("查看文件夹系统必须授予权限")
        .setNegativeButton(
            "授予"
        ) { _, _ ->
            MainActivity.task = task
            startActivity(Intent(this, MainActivity::class.java).apply {
                MainActivity.getBundle(MainActivity.REQUEST_EMULATED, "", this)
            })
        }
        .setPositiveButton("取消") { _, _ ->
            task.complete(false)
        }.show()
}

private fun ComponentActivity.requestSAF(
    path: String,
    requestCodeSAF: String,
    task: CompletableDeferred<Boolean>
) {
    AlertDialog.Builder(this).setTitle("需要授予权限")
        .setMessage("在Android 10 或者访问存储卡，需要获取SAF权限")
        .setPositiveButton(
            "去授予"
        ) { _, _ ->
            MainActivity.task = task
            startActivity(Intent(this, MainActivity::class.java).apply {
                MainActivity.getBundle(requestCodeSAF, path, this)
            })
        }
        .setNegativeButton(
            "取消"
        ) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            task.complete(false)
        }.show()
}

@RequiresApi(api = Build.VERSION_CODES.R)
private fun ComponentActivity.requestManageExternalPermission(task: CompletableDeferred<Boolean>) {
    AlertDialog.Builder(this).setTitle("需要授予权限")
        .setMessage("在Android 11上，需要获取Manage External Storage权限")
        .setPositiveButton(
            "去授予"
        ) { _: DialogInterface?, _: Int ->
            MainActivity.task = task
            startActivity(Intent(this, MainActivity::class.java).apply {
                MainActivity.getBundle(MainActivity.REQUEST_MANAGE, "", this)
            })
        }
        .setNegativeButton(
            "取消"
        ) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            task.complete(false)
        }.show()
}