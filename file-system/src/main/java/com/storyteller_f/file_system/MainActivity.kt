package com.storyteller_f.file_system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.storyteller_f.common_ktx.context
import com.storyteller_f.file_system.instance.local.document.ExternalDocumentLocalFileInstance
import com.storyteller_f.file_system.instance.local.document.MountedLocalFileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
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

        fun getBundle(type: String, path: String, intent: Intent) {
            intent.putExtras(
                Bundle().apply {
                    putString("path", path)
                    putString("permission", type)
                }
            )
        }

        fun fromBundle(bundle: Intent) = bundle.extras!!.let {
            it.getString("permission") to it.getString("path")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val fromBundle = fromBundle(intent)
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
                val uri = it.data
                if (uri != null) {
                    val sharedPreferences =
                        getSharedPreferences(
                            MountedLocalFileInstance.Name,
                            MODE_PRIVATE
                        )
                    sharedPreferences.edit()
                        .putString(MountedLocalFileInstance.ROOT_URI, uri.toString())
                        .apply()
                    toast()
                    return@registerForActivityResult
                }
            }
            failure()
        }.launch(
            FileUtility.produceRequestSaf(
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
                val uri = it.data
                if (uri != null) {
                    val sharedPreferences = getSharedPreferences(
                        ExternalDocumentLocalFileInstance.Name,
                        MODE_PRIVATE
                    )
                    sharedPreferences.edit()
                        .putString(
                            ExternalDocumentLocalFileInstance.STORAGE_URI,
                            uri.toString()
                        )
                        .apply()
                    toast()
                    return@registerForActivityResult
                }
            }
            failure()
        }.launch(
            FileUtility.produceRequestSaf(
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

fun ImageView.fileIcon(fileSystemItemModel: FileSystemItemModel) {
    if (fileSystemItemModel is FileItemModel) {
        if (fileSystemItemModel.fullPath.startsWith("/data/app/")) {
            setImageDrawable(context.packageManager.getApplicationIcon(fileSystemItemModel.name))
            return
        }
        val extension = fileSystemItemModel.extension
        if (extension != null) {
            setImageResource(
                when (extension) {
                    "mp3", "wav", "flac" -> R.drawable.ic_music
                    "zip", "7z", "rar" -> R.drawable.ic_archive
                    "jpg", "jpeg", "png", "gif" -> R.drawable.ic_image
                    "mp4", "rmvb", "ts", "avi", "mkv", "3gp", "mov", "flv", "m4s" -> R.drawable.ic_video
                    "url" -> R.drawable.ic_url
                    "txt", "c" -> R.drawable.ic_text
                    "js" -> R.drawable.ic_js
                    "pdf" -> R.drawable.ic_pdf
                    "doc", "docx" -> R.drawable.ic_word
                    "xls", "xlsx" -> R.drawable.ic_excel
                    "ppt", "pptx" -> R.drawable.ic_ppt
                    "iso" -> R.drawable.ic_disk
                    "exe", "msi" -> R.drawable.ic_exe
                    "psd" -> R.drawable.ic_psd
                    "torrent" -> R.drawable.ic_torrent
                    else -> R.drawable.ic_unknow
                }
            )
        } else {
            val absolutePath: String = fileSystemItemModel.fullPath
            if (absolutePath.indexOf("/data/app/") == 0) {
                try {
                    val packageName = absolutePath.substring(10, absolutePath.indexOf("-"))
                    setImageDrawable(context.packageManager.getApplicationIcon(packageName))
                } catch (e: Exception) {
                    setImageResource(R.drawable.ic_baseline_android_24)
                    e.printStackTrace()
                }
            } else {
                setImageResource(R.drawable.ic_binary)
            }
        }
    } else setImageResource(R.drawable.ic_folder_explorer)
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
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                val externalFileInstance = getSharedPreferences(
                    ExternalDocumentLocalFileInstance.Name,
                    Context.MODE_PRIVATE
                )
                val string = externalFileInstance.getString(
                    ExternalDocumentLocalFileInstance.STORAGE_URI,
                    null
                )
                string != null
            }
            else -> {
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
        dest == FileInstanceFactory.emulatedRootPath -> true
        dest == "/storage" -> true
        dest.startsWith("/storage") -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                val externalFileInstance =
                    getSharedPreferences(
                        MountedLocalFileInstance.Name,
                        Context.MODE_PRIVATE
                    )
                val string =
                    externalFileInstance.getString(MountedLocalFileInstance.ROOT_URI, null)
                string != null
            }
            else -> {
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
        else -> ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}