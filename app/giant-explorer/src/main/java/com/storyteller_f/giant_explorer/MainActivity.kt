package com.storyteller_f.giant_explorer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ui.setVisible
import com.storyteller_f.common_vm_ktx.GenericValueModel
import com.storyteller_f.common_vm_ktx.sVM
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.checkPathPermission
import com.storyteller_f.file_system.fillIcon
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.TorrentFileModel
import com.storyteller_f.file_system.requestPermissionForSpecialPath
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.ActivityMainBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileBinding
import com.storyteller_f.giant_explorer.model.FileModel
import com.storyteller_f.ui_list.core.*
import com.storyteller_f.ui_list.data.SimpleResponse
import com.storyteller_f.ui_list.event.viewBinding
import com.storyteller_f.ui_list.ui.ListWithState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private val fileInstance by sVM {
        GenericValueModel<FileInstance>().apply {
            data.value =
                FileInstanceFactory.getFileInstance("/storage/emulated/0", this@MainActivity)
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
                    .map { model ->
                        val length = if (model is FileItemModel) {
                            requireDatabase().mdDao().search(model.fullPath)?.let {
                                if (it.lastUpdateTime > model.lastModifiedTime) {
                                    model.md = it.data
                                }
                            }
                            if (model is TorrentFileModel)
                                requireDatabase().torrentDao().search(model.fullPath)?.let {
                                    if (it.lastUpdateTime > model.lastModifiedTime)
                                        model.torrentName = it.torrent
                                }
                            model.size
                        } else {
                            //从数据库中查找
                            val search = requireDatabase().sizeDao().search(model.fullPath)
                            if (search != null && search.lastUpdateTime > model.lastModifiedTime) {
                                search.size
                            } else -1
                        }
                        model.formattedSize = format1024(length)
                        model.size = length
                        FileModel(model.name, model.fullPath, length, model.isHidden, model)
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
        val pathMan = binding.pathMan

        binding.content.up(adapter, lifecycleScope, ListWithState.Companion::remote)
        fileInstance.data.observe(this) {
            //检查权限
            lifecycleScope.launch {
                if (!checkPathPermission(it.path)) {
                    requestPermissionForSpecialPath(it.path)
                }
            }
            data.observer(this, it) { pagingData ->
                adapter.submitData(pagingData)
            }
            pathMan.drawPath(it.path)
        }
        callbackFlow {
            pathMan.setPathChangeListener {
                trySend(it)
            }
            awaitClose {
                pathMan.setPathChangeListener(null)
            }
        }.onEach {
            fileInstance.data.value = FileInstanceFactory.getFileInstance(it, this)
        }.launchIn(lifecycleScope)
    }

    private fun format1024(args: Long): String {
        if (args < 0) {
            return "Error"
        }
        val flags = arrayOf("B.", "KB", "MB", "GB", "TB")
        var flag = 0
        var size = args.toDouble()
        while (size >= 1024) {
            if (Thread.currentThread().isInterrupted) {
                return "stopped"
            }
            size /= 1024.0
            flag += 1
        }
        return String.format(Locale.CHINA, "%.2f %s", size, flags[flag])
    }

    @BindClickEvent(FileItemHolder::class)
    fun toChild(itemHolder: FileItemHolder) {
        if (itemHolder.file.item.isDirectory) {
            val fileInstance1 = fileInstance.data.value ?: return
            fileInstance.data.value = FileInstanceFactory.toChild(
                fileInstance1,
                itemHolder.file.name,
                false,
                this
            )
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
        binding.fileIcon.fillIcon(itemHolder.file.item)
        val item = itemHolder.file.item
        binding.fileSize.setVisible(item.size != -1L) {
            it.text = item.formattedSize
        }
        binding.fileMD.setVisible(item is FileItemModel && item.md.valid()) {
            it.text = (item as? FileItemModel)?.md
        }

        binding.torrentName.setVisible(item is TorrentFileModel && item.torrentName.valid()) {
            it.text = (item as? TorrentFileModel)?.torrentName
        }

        binding.modifiedTime.setVisible(item.lastModifiedTime > 0 && item.formattedLastModifiedTime.valid()) {
            it.text = item.formattedLastModifiedTime
        }

        binding.detail.text = item.detail
    }
}

fun String?.valid() = this?.trim()?.isNotEmpty() == true
