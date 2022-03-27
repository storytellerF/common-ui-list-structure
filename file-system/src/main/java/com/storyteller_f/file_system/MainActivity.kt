package com.storyteller_f.file_system

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.storyteller_f.file_system.instance.local.document.ExternalDocumentLocalFileInstance
import com.storyteller_f.file_system.instance.local.document.MountedLocalFileInstance
import com.storyteller_f.file_system.util.FileUtility
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.CompletableFuture

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
            REQUEST_EMULATED -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_EMULATED
                )
            }
            REQUEST_SAF_EMULATED -> {
                registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    if (it.resultCode == Activity.RESULT_OK) {
                        val uri = it.data
                        if (uri != null) {
                            val sharedPreferences = getSharedPreferences(
                                ExternalDocumentLocalFileInstance.Name,
                                Context.MODE_PRIVATE
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
                        FileInstanceFactory.getPrefix(fromBundle.second),
                        this
                    )
                )
            }
            REQUEST_SAF_SDCARD -> {
                registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    if (it.resultCode == Activity.RESULT_OK) {
                        val uri = it.data
                        if (uri != null) {
                            val sharedPreferences =
                                getSharedPreferences(
                                    MountedLocalFileInstance.Name,
                                    Context.MODE_PRIVATE
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
                        FileInstanceFactory.getPrefix(fromBundle.second),
                        this
                    )
                )
            }
            REQUEST_MANAGE -> {
                registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result: ActivityResult ->
                    val ok = result.resultCode == Activity.RESULT_OK
                    //boolean is=Environment.isExternalStorageManager();
                    if (ok) {
                        toast()
                    } else failure()
                }.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
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