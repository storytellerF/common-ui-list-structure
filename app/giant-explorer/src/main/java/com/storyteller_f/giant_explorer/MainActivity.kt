package com.storyteller_f.giant_explorer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_vm_ktx.GenericValueModel
import com.storyteller_f.common_vm_ktx.sVM
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.document.ExternalDocumentLocalFileInstance
import com.storyteller_f.file_system.instance.local.document.MountedLocalFileInstance
import com.storyteller_f.file_system.requestPermissionForSpecialPath
import com.storyteller_f.file_system_ktx.isFile
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.ActivityMainBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileBinding
import com.storyteller_f.giant_explorer.model.FileModel
import com.storyteller_f.ui_list.core.*
import com.storyteller_f.ui_list.data.SimpleResponse
import com.storyteller_f.ui_list.event.viewBinding
import com.storyteller_f.ui_list.ui.ListWithState
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private val fileInstance by sVM {
        GenericValueModel<FileInstance>().apply {
            data.value =
                FileInstanceFactory.getFileInstance("/storage/emulated/0/", this@MainActivity)
        }
    }
    private val data by search(SearchProducer(
        { path: FileInstance, start, count ->//path 的后面没有斜线，格式类似与/root/hello ，而不是/root/hello/
            val listSafe = path.listSafe()
            val listFiles = listSafe.files.plus(listSafe.directories)
            if ((start - 1) * count > listFiles.size) SimpleResponse(0)
            else {

                val map = listFiles
                    .subList(
                        (start - 1) * count, start + min(count, listFiles.size - start)
                    )
                    .map {
                        val length = if (it.isFile) {
                            it.size
                        } else {
                            //从数据库中查找
                            val search = requireDatabase().reposDao().search(it.absolutePath)
                            if (search != null && search.lastUpdateTime > it.lastModifiedTime) {
                                search.size
                            } else -1
                        }
                        FileModel(it.name, it.absolutePath, length, it.isHidden)
                    }
                SimpleResponse(
                    total = listFiles.size,
                    items = map,
                    if (listFiles.size > count * start) start + 1 else null
                )
            }

        }, { it, _ ->
            FileItemHolder(it)
        }
    ))
    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.content.up(adapter, lifecycleScope, ListWithState.Companion::remote)
        fileInstance.data.observe(this) {
            //检查权限
            lifecycleScope.launch {
                if (checkPathPermission(it.path)) {
                    requestPermissionForSpecialPath(it.path)
                }
            }
            data.observer(this, it) { pagingData ->
                adapter.submitData(pagingData)
            }
        }
    }

    fun checkPathPermission(dest: String): Boolean {
        return if (dest.startsWith("/storage/emulated/0/")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                !Environment.isExternalStorageManager()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val externalFileInstance = getSharedPreferences(
                    ExternalDocumentLocalFileInstance.Name,
                    Context.MODE_PRIVATE
                )
                val string = externalFileInstance.getString(
                    ExternalDocumentLocalFileInstance.STORAGE_URI,
                    null
                )
                string == null
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            } else {
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            }
        } else if (dest == "/storage/") {
            false
        } else if (dest.startsWith("/storage/")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                !Environment.isExternalStorageManager()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val externalFileInstance =
                    getSharedPreferences(MountedLocalFileInstance.Name, Context.MODE_PRIVATE)
                val string = externalFileInstance.getString(MountedLocalFileInstance.ROOT_URI, null)
                string == null
            } else {
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            }
        } else {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        }
    }
}

class FileItemHolder(val file: FileModel) : DataItemHolder() {
    override fun areItemsTheSame(other: DataItemHolder) =
        (other as FileItemHolder).file.fullPath == file.fullPath

}

@BindItemHolder(FileItemHolder::class)
class FileViewHolder(private val binding: ViewHolderFileBinding) :
    AdapterViewHolder<FileItemHolder>(binding) {
    override fun bindData(itemHolder: FileItemHolder) {
        binding.fileName.text = itemHolder.file.name
    }

}