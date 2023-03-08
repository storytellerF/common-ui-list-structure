package com.storyteller_f.file_system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.storyteller_f.common_ktx.context
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import com.storyteller_f.file_system.util.FileUtility
import kotlinx.coroutines.CompletableDeferred

class MainActivity : AppCompatActivity() {

    companion object {
        var task: CompletableDeferred<Boolean>? = null
        const val REQUEST_SAF_EMULATED = "REQUEST_CODE_SAF_EMULATED"
        const val REQUEST_SAF_SDCARD = "REQUEST_CODE_SAF_SDCARD"
        const val REQUEST_EMULATED = "REQUEST_CODE_EMULATED"
        const val REQUEST_MANAGE = "REQUEST_MANAGE"
        const val REQUEST_CODE_EMULATED = 3

        fun Intent.getBundle(type: String, path: String) {
            putExtras(
                Bundle().apply {
                    putString("path", path)
                    putString("permission", type)
                }
            )
        }

        fun Intent.fromBundle() = extras!!.let {
            it.getString("permission") to it.getString("path")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val fromBundle = intent.fromBundle()
        when (fromBundle.first!!) {
            REQUEST_EMULATED -> ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_EMULATED
            )
            REQUEST_SAF_EMULATED -> requestForEmulatedSAF(fromBundle)
            REQUEST_SAF_SDCARD -> requestForSdcard(fromBundle)
            REQUEST_MANAGE -> requestForManageFile()
        }
    }

    private fun requestForManageFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result: ActivityResult ->
                val ok = result.resultCode == RESULT_OK
                //boolean is=Environment.isExternalStorageManager();
                if (ok) {
                    toast()
                } else failure()
            }.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        } else throw Exception("错误使用request manage！")
    }

    private fun requestForSdcard(fromBundle: Pair<String?, String?>) {
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                val uri = it.data?.data
                if (uri != null) {
                    FileSystemUriSaver.getInstance().saveUri(DocumentLocalFileInstance.mountedKey, this, uri)
                    toast()
                    return@registerForActivityResult
                }
            }
            failure()
        }.launch(
            FileUtility.produceSafRequestIntent(
                FileInstanceFactory.getPrefix(fromBundle.second!!, this),
                this
            )
        )
    }

    private fun requestForEmulatedSAF(fromBundle: Pair<String?, String?>) {
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                val uri = it.data?.data
                if (uri != null) {
                    FileSystemUriSaver.getInstance().saveUri(DocumentLocalFileInstance.emulatedKey, this, uri)
                    toast()
                    return@registerForActivityResult
                }
            }
            failure()
        }.launch(
            FileUtility.produceSafRequestIntent(
                FileInstanceFactory.getPrefix(fromBundle.second!!, this),
                this
            )
        )
    }

    private fun failure() {
        task?.complete(false)
        finish()
    }

    private fun toast() {
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
                toast()
                return
            }
        }
        failure()
    }
}

fun Fragment.checkPathPermission(dest: String) = context {
    checkPathPermission(dest)
}

/**
 * @return 返回是否含有权限。对于没有权限的，调用 requestPermissionForSpecialPath
 */
fun Context.checkPathPermission(dest: String): Boolean {
    return when {
        dest.startsWith(FileInstanceFactory.rootUserEmulatedPath) -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> DocumentLocalFileInstance.getEmulated(this, dest).exists()
            else -> ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        dest == FileInstanceFactory.emulatedRootPath -> true
        dest == "/storage" -> true
        dest.startsWith("/storage") -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> DocumentLocalFileInstance.getMounted(this, dest).exists()
            else -> ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        else -> ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}