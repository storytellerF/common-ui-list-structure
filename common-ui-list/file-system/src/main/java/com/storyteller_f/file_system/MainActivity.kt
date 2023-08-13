package com.storyteller_f.file_system

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.storyteller_f.compat_ktx.getParcelableCompat
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import com.storyteller_f.file_system.util.FileUtility
import com.storyteller_f.multi_core.StoppableTask
import kotlinx.coroutines.CompletableDeferred

class MainActivity : AppCompatActivity() {

    companion object {
        var task: CompletableDeferred<Boolean>? = null
        const val REQUEST_SAF_EMULATED = "REQUEST_CODE_SAF_EMULATED"
        const val REQUEST_SAF_SDCARD = "REQUEST_CODE_SAF_SDCARD"
        const val REQUEST_EMULATED = "REQUEST_CODE_EMULATED"
        const val REQUEST_MANAGE = "REQUEST_MANAGE"
        const val REQUEST_CODE_EMULATED = 3

        fun Intent.putBundle(type: String, uri: Uri) {
            putExtras(
                Bundle().apply {
                    putParcelable("path", uri)
                    putString("permission", type)
                }
            )
        }

        fun Intent.fromBundle() = extras!!.let {
            it.getString("permission")!! to it.getParcelableCompat("path", Uri::class.java)!!
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val (code, uri) = intent.fromBundle()
        when (code) {
            REQUEST_EMULATED -> ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_EMULATED
            )
            REQUEST_SAF_EMULATED -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                requestForEmulatedSAF(uri)
            }

            REQUEST_SAF_SDCARD -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                requestForSdcard(uri)
            }

            REQUEST_MANAGE -> requestForManageFile()
        }
    }

    private fun requestForManageFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    success()
                } else failure()
            }.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        } else throw Exception("错误使用request manage！")
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun requestForSdcard(path: Uri) {
        val prefix = FileInstanceFactory.getPrefix(this, path, stoppableTask =  StoppableTask.Blocking)!!
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (processResult(it, prefix.key)) return@registerForActivityResult
            failure()
        }.launch(
            FileUtility.produceSafRequestIntent(this, prefix.key)
        )
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun processResult(it: ActivityResult, preferenceKey: String): Boolean {
        if (it.resultCode == RESULT_OK) {
            val uri = it.data?.data
            if (uri != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                FileSystemUriSaver.instance.saveUri(this, preferenceKey, uri)
                success()
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun requestForEmulatedSAF(path: Uri) {
        val prefix = FileInstanceFactory.getPrefix(this, path, stoppableTask = StoppableTask.Blocking)!!
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (processResult(it, DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS)) return@registerForActivityResult
            failure()
        }.launch(
            FileUtility.produceSafRequestIntent(
                this,
                prefix.key
            )
        )
    }

    private fun failure() {
        task?.complete(false)
        finish()
    }

    private fun success() {
        Toast.makeText(this, "授予权限成功", Toast.LENGTH_SHORT).show()
        task?.complete(true)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_EMULATED) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                success()
                return
            }
        }
        failure()
    }
}

/**
 * @return 返回是否含有权限。对于没有权限的，调用 requestPermissionForSpecialPath
 */
fun Context.checkPathPermission(uri: Uri): Boolean {
    if (uri.scheme != ContentResolver.SCHEME_FILE) return true
    return when(val prefix = FileInstanceFactory.getPrefix(this, uri, StoppableTask.Blocking)!!) {
        LocalFileSystemPrefix.RootEmulated -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> DocumentLocalFileInstance.getEmulated(this, uri, prefix.key).exists()
            else -> ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        is LocalFileSystemPrefix.Mounted -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> DocumentLocalFileInstance.getMounted(this, uri, prefix.key).exists()
            else -> ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        else -> true
    }
}