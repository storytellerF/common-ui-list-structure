package com.storyteller_f.giant_explorer.control

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.common_ktx.mm
import com.storyteller_f.common_ui.*
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.BuildConfig
import com.storyteller_f.giant_explorer.FileSystemProviderResolver
import com.storyteller_f.giant_explorer.PluginManager
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentFileListBinding
import com.storyteller_f.giant_explorer.dialog.*
import com.storyteller_f.plugin_core.*
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.source.SearchProducer
import com.storyteller_f.ui_list.source.search
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class TempVM : ViewModel() {
    var list: MutableList<String> = mutableListOf()
    var dest: String? = null
}

class FileListFragment : SimpleFragment<FragmentFileListBinding>(FragmentFileListBinding::inflate) {
    private val fileOperateBinder
        get() = (requireContext() as MainActivity).fileOperateBinder
    private val uuid by keyPrefix({ "uuid" }, avm({}) {
        GenericValueModel<String>().apply {
            data.value = UUID.randomUUID().toString()
        }
    })

    private val data by search({ requireDatabase to session.selected }, { (database, selected) ->
        SearchProducer(fileServiceBuilder(database)) { fileModel, _ ->
            FileItemHolder(fileModel, selected)
        }
    })

    private val filterHiddenFile by asvm({}) { it, _ ->
        StateValueModel(it, filterHiddenFileKey, false)
    }

    private val args by navArgs<FileListFragmentArgs>()

    private val session by vm({ args.path }) {
        FileExplorerSession(requireActivity().application, it)
    }
    private val temp by keyPrefix({ "temp" }, pvm({}) {
        TempVM()
    })

    override fun onBindViewEvent(binding: FragmentFileListBinding) {
        val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>()
        supportDirectoryContent(
            binding.content, adapter, data, session, filterHiddenFile.data
        ) {
            (requireContext() as MainActivity).drawPath(it)
        }
        (requireActivity() as? MenuHost)?.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.file_list_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
                R.id.add_file -> {
                    findNavController().navigate(R.id.action_fileListFragment_to_newNameDialog)
                    fragment(NewNameDialog.requestKey) { nameResult: NewNameDialog.NewNameResult ->
                        session.fileInstance.value?.toChild(nameResult.name, true, true)
                    }
                    true
                }
                R.id.paste_file -> {
                    ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)?.let { manager ->
                        manager.primaryClip?.let { data ->
                            handleClipData(data)
                        }
                    }
                    true

                }
                R.id.background_task -> {
                    startActivity(Intent(requireContext(), BackgroundTaskConfigActivity::class.java))
                    true
                }
                else -> false
            }
        }, owner)
    }

    fun handleClipData(data: ClipData, destDirectory: String? = null) {
        val key = uuid.data.value ?: return
        Log.i(TAG, "handleClipData: key $key")
        viewLifecycleOwner.lifecycleScope.launch {
            val dest = destDirectory?.let {
                FileInstanceFactory.getFileInstance(it, requireContext())
            } ?: session.fileInstance.value ?: kotlin.run {
                Toast.makeText(requireContext(), "无法确定目的地", Toast.LENGTH_LONG).show()
                return@launch
            }
            val mutableList = MutableList(data.itemCount) {
                data.getItemAt(it)
            }
            val filePathMatcher = Regex("^/([\\w.]+/)*[\\w.]+$")
            val uriList = mutableList.mapNotNull {
                val text = it.coerceToText(requireContext()).toString()
                val u = when {
                    it.uri != null -> it.uri
                    URLUtil.isNetworkUrl(text) -> Uri.parse(text)
                    filePathMatcher.matches(text) -> {
                        Uri.fromFile(File(text))
                    }
                    else -> {
                        Toast.makeText(requireContext(), "正则失败$text", Toast.LENGTH_LONG).show()
                        null
                    }
                }
                u?.takeIf { uri -> uri.toString().isNotEmpty() }
            }
            if (uriList.any {
                    it.scheme == ContentResolver.SCHEME_FILE && it.path == destDirectory
                }) {
                //静默处理
                return@launch
            }
            temp.list.clear()
            temp.list.addAll(uriList.map { it.toString() })
            temp.dest = dest.path
            val fileOperateBinderLocal = fileOperateBinder ?: kotlin.run {
                Toast.makeText(requireContext(), "未连接服务", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (activity?.getSharedPreferences("${requireContext().packageName}_preferences", Activity.MODE_PRIVATE)?.getBoolean("notify_before_paste", true) == true) {
                dialog(TaskConfirmDialog()) { r: TaskConfirmDialog.Result ->
                    if (r.confirm) fileOperateBinderLocal.compoundTask(uriList, dest, key)
                }
            } else {
                fileOperateBinderLocal.compoundTask(uriList, dest, key)
            }
        }

    }


    @BindClickEvent(FileItemHolder::class)
    fun toChild(itemHolder: FileItemHolder) {
        if (itemHolder.file.item.isDirectory) {
            val old = session.fileInstance.value ?: return
            findNavController().navigate(R.id.action_fileListFragment_self, FileListFragmentArgs(File(old.path, itemHolder.file.name).absolutePath).toBundle())
        } else {
            findNavController().navigate(R.id.action_fileListFragment_to_openFileDialog, OpenFileDialogArgs(itemHolder.file.fullPath).toBundle())
            fragment(OpenFileDialog.key) { r: OpenFileDialog.OpenFileResult ->
                Intent("android.intent.action.VIEW").apply {
                    addCategory("android.intent.category.DEFAULT")
                    val file = File(itemHolder.file.fullPath)
                    val uriForFile = FileProvider.getUriForFile(requireContext(), BuildConfig.FILE_PROVIDER_AUTHORITY, file)
                    setDataAndType(uriForFile, r.mimeType)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }.let {
                    val ellipsizedText = TextUtils.ellipsize(itemHolder.file.name, TextPaint(), 100f, TextUtils.TruncateAt.MIDDLE)
                    startActivity(Intent.createChooser(it, "open $ellipsizedText by"))
                }
            }
        }
    }

    @BindClickEvent(FileItemHolder::class, "fileIcon")
    fun fileMenu(view: View, itemHolder: FileItemHolder) {
        val fullPath = itemHolder.file.fullPath
        val key = uuid.data.value ?: return

        PopupMenu(requireContext(), view).apply {
            inflate(R.menu.item_context_menu)
            val mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(File(fullPath).extension)

            resolvePlugins(itemHolder, mimeTypeFromExtension)
            PluginManager.list.forEach { plugin ->
                menu.add(plugin.name).setOnMenuItemClickListener {
                    if (plugin.name.endsWith("apk")) startActivity(Intent(requireContext(), FragmentPluginActivity::class.java).apply {
                        putExtra("plugin-name", plugin.name)
                        plugUri(mimeTypeFromExtension, fullPath)
                    })
                    else startActivity(Intent(requireContext(), WebViewPluginActivity::class.java).apply {
                        putExtra("plugin-name", plugin.name)
                        plugUri(mimeTypeFromExtension, fullPath)
                    })
                    true
                }
            }

            val liPlugin = LiPlugin()
            val pluginManager = object : DefaultPluginManager(requireContext()) {
                override suspend fun requestPath(initPath: String?): String {
                    val completableDeferred = CompletableDeferred<String>()
                    dialog(RequestPathDialog()) { result: RequestPathDialog.RequestPathResult ->
                        completableDeferred.complete(result.path)
                    }
                    return completableDeferred.await()
                }

                override fun runInService(block: GiantExplorerService.() -> Boolean) {
                    fileOperateBinder?.pluginTask(key, block)
                }

            }
            liPlugin.plugPluginManager(pluginManager)
            if (liPlugin.accept(listOf(File(fullPath)))) {
                menu.loopAdd(liPlugin.group()).add("li").setOnMenuItemClickListener {
                    scope.launch {

                        liPlugin.start(fullPath)
                    }
                    return@setOnMenuItemClickListener true
                }
            }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete -> {
                        fileOperateBinder?.delete(itemHolder.file.item, detectSelected(itemHolder), key)
                    }
                    R.id.move_to -> {
                        moveOrCopy(true, itemHolder)
                    }
                    R.id.copy_to -> {
                        moveOrCopy(false, itemHolder)
                    }
                    R.id.copy_file -> {
                        ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)?.let { manager ->
                            val map = detectSelected(itemHolder).map {
                                Uri.fromFile(File(it.fullPath))
                            }
                            val apply = ClipData.newPlainText(clipDataKey, map.first().toString()).apply {
                                if (map.size > 1) map.subList(1, map.size).forEach {
                                    addItem(ClipData.Item(it))
                                }
                            }
                            manager.setPrimaryClip(apply)
                        }
                    }
                    R.id.properties -> {
                        findNavController().navigate(R.id.action_fileListFragment_to_propertiesDialog, PropertiesDialogArgs(fullPath).toBundle())
                    }

                }
                true
            }
        }.show()
    }

    private fun PopupMenu.resolvePlugins(itemHolder: FileItemHolder, mimeTypeFromExtension: String?) {
        val intent = Intent("com.storyteller_f.action.giant_explorer.PLUGIN")
        intent.addCategory("android.intent.category.DEFAULT")
        intent.plugUri(mimeTypeFromExtension, itemHolder.file.fullPath)

        val activities = requireContext().packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_META_DATA)
        activities.forEach {
            val activityInfo = it?.activityInfo ?: return@forEach
            val groups = activityInfo.metaData?.getString("group")?.split("/") ?: return@forEach
            val title = activityInfo.metaData?.getString("title") ?: return@forEach
            menu.loopAdd(groups).add(title).setOnMenuItemClickListener {
                intent.setPackage(requireContext().packageName).component = ComponentName(activityInfo.packageName, activityInfo.name)
                startActivity(intent)
                return@setOnMenuItemClickListener true
            }
        }

    }

    private fun moveOrCopy(move: Boolean, itemHolder: FileItemHolder) {
        dialog(RequestPathDialog()) { result: RequestPathDialog.RequestPathResult ->
            result.path.mm {
                FileInstanceFactory.getFileInstance(it, requireContext())
            }.mm { dest ->
                val key = uuid.data.value ?: return@mm
                val detectSelected = detectSelected(itemHolder)
                Log.i(TAG, "moveOrCopy: uuid: $key")
                fileOperateBinder?.moveOrCopy(dest, detectSelected, itemHolder.file.item, move, key)
            }
        }
    }

    companion object {
        const val filterHiddenFileKey = "filter-hidden-file"
        const val clipDataKey = "file explorer"
        private const val TAG = "FileListFragment"
    }

    private fun detectSelected(itemHolder: FileItemHolder) = session.selected.value?.map { pair -> (pair.first as FileItemHolder).file.item } ?: listOf(itemHolder.file.item)

    override fun requestKey() = "file-list"

}

private fun Menu.loopAdd(strings: List<String>): Menu {
    return strings.fold(this) { t, e ->
        t.addSubMenu(e)
    }
}

private fun Intent.plugUri(mimeType: String?, fullPath: String) {
    val build = FileSystemProviderResolver.build(false, fullPath)
    putExtra("path", fullPath)
    setDataAndType(build, mimeType)
    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
}

class LiPlugin : GiantExplorerShellPlugin {
    private lateinit var pluginManager: GiantExplorerPluginManager
    override fun plugPluginManager(pluginManager: GiantExplorerPluginManager) {
        this.pluginManager = pluginManager
    }

    override fun accept(file: List<File>): Boolean {
        return file.all {
            it.extension == "zip"
        }
    }

    override fun group(): List<String> {
        return listOf("archive", "extract to")
    }

    override suspend fun start(fullPath: String) {
        val requestPath = pluginManager.requestPath()
        println("request path $requestPath")
        unCompress(pluginManager.fileInputStream(fullPath), File(requestPath))
    }

    private fun unCompress(archive: InputStream, dest: File) {
        pluginManager.runInService {
            reportRunning()
            ZipInputStream(archive).use { stream ->
                while (true) {
                    val nextEntry = stream.nextEntry
                    nextEntry?.let {
                        processEntry(dest, nextEntry, stream)
                    } ?: break
                }
            }
            true
        }
    }

    private fun processEntry(dest: File, nextEntry: ZipEntry, stream: ZipInputStream) {
        val child = File(dest, nextEntry.name)
        println(nextEntry.name)
        if (nextEntry.isDirectory) {
            pluginManager.ensureDir(child)
        } else {
            write(pluginManager.fileOutputStream(child.absolutePath), stream)
        }
    }

    private fun write(file: FileOutputStream, stream: ZipInputStream) {
        val buffer = ByteArray(1024)
        file.buffered().use {
            while (true) {
                val offset = stream.read(buffer)
                if (offset != -1) {
                    it.write(buffer, 0, offset)
                } else break
            }
        }
    }

}