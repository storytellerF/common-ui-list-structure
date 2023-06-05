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
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.view.iterator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.common_ktx.nn
import com.storyteller_f.common_ui.*
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.filter_core.Filter
import com.storyteller_f.giant_explorer.*
import com.storyteller_f.giant_explorer.BuildConfig
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.control.plugin.DefaultPluginManager
import com.storyteller_f.giant_explorer.control.plugin.FragmentPluginActivity
import com.storyteller_f.giant_explorer.control.plugin.WebViewPluginActivity
import com.storyteller_f.giant_explorer.control.plugin.stoppable
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentFileListBinding
import com.storyteller_f.giant_explorer.dialog.*
import com.storyteller_f.plugin_core.*
import com.storyteller_f.sort_ui.SortChain
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.source.SearchProducer
import com.storyteller_f.ui_list.source.search
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SharePasteTargetViewModel : ViewModel() {
    var list: MutableList<String> = mutableListOf()
    var dest: String? = null

    fun replace(uriList: List<Uri>, file: FileInstance) {
        list.clear()
        list.addAll(uriList.map { it.toString() })
        dest = file.path
    }
}

class FileListFragment : SimpleFragment<FragmentFileListBinding>(FragmentFileListBinding::inflate) {
    private val fileOperateBinder
        get() = (requireContext() as MainActivity).fileOperateBinder
    private val uuid by keyPrefix({ "uuid" }, avm({}) {
        GenericValueModel<String>().apply {
            data.value = UUID.randomUUID().toString()
        }
    })

    private val filters by keyPrefix({ "test" }, asvm({}) { it, _ ->
        StateValueModel(it, default = listOf<Filter<FileSystemItemModel>>())
    })
    private val sort by keyPrefix({ "sort" }, asvm({}) { it, _ ->
        StateValueModel(it, default = listOf<SortChain<FileSystemItemModel>>())
    })

    private val data by search({ requireDatabase to session.selected }, { (database, selected) ->
        SearchProducer(fileServiceBuilder(database)) { fileModel, _, sq ->
            FileItemHolder(fileModel, selected.value.orEmpty(), sq.display)
        }
    })

    private val filterHiddenFile by asvm({}) { it, _ ->
        StateValueModel(it, filterHiddenFileKey, false)
    }

    private val args by navArgs<FileListFragmentArgs>()

    private val session by vm({ args }) {
        FileExplorerSession(requireActivity().application, it.path, it.root)
    }
    private val displayGrid by keyPrefix("display", avm({}) { _ ->
        genericValueModel(false)
    })

    private val shareTarget by keyPrefix({ "temp" }, pvm({}) {
        SharePasteTargetViewModel()
    })

    override fun onBindViewEvent(binding: FragmentFileListBinding) {
        val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>()
        displayGrid.data.observe(owner) {
            binding.content.recyclerView.isVisible = false
            adapter.submitData(cycle, PagingData.empty())
            binding.content.recyclerView.layoutManager = when {
                it -> GridLayoutManager(requireContext(), 3)
                else -> LinearLayoutManager(requireContext())
            }
        }

        fileList(
            binding.content, adapter, data, session, filterHiddenFile.data, filters.data, sort.data, displayGrid.data, { h ->
                startActivity(Intent(requireContext(), MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    putExtra("start", FileListFragmentArgs(h.file.fullPath, args.root).toBundle())
                })
            }
        ) {
            (requireContext() as MainActivity).drawPath(it)
        }
        setupMenu()
    }

    private fun setupMenu() {
        (requireActivity() as? MenuHost)?.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.file_list_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                menu.findItem(R.id.paste_file)?.let {
                    it.isEnabled = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)?.hasPrimaryClip() == true
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
                R.id.add_file -> {
                    findNavController().navigate(R.id.action_fileListFragment_to_newNameDialog)
                    fragment(NewNameDialog.requestKey, NewNameDialog.NewNameResult::class.java) { nameResult ->
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

                else -> false
            }
        }, owner)
    }

    fun handleClipData(data: ClipData, destDirectory: String? = null) {
        val key = uuid.data.value ?: return
        Log.i(TAG, "handleClipData: key $key")
        viewLifecycleOwner.lifecycleScope.launch {
            val dest = destDirectory?.let {
                getFileInstance(it, requireContext(), stoppableTask = stoppable())
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
                val uriFromText = text.toUri()
                val u = when {
                    uriFromText.scheme == ContentResolver.SCHEME_FILE -> uriFromText
                    it.uri != null -> it.uri
                    URLUtil.isNetworkUrl(text) -> Uri.parse(text)
                    filePathMatcher.matches(text) -> {
                        Uri.fromFile(File(text))
                    }

                    else -> {
                        Toast.makeText(requireContext(), "正则失败 $text", Toast.LENGTH_LONG).show()
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
            if (uriList.isNotEmpty()) {
                shareTarget.replace(uriList, dest)
                val fileOperateBinderLocal = fileOperateBinder ?: kotlin.run {
                    Toast.makeText(requireContext(), "未连接服务", Toast.LENGTH_LONG).show()
                    return@launch
                }
                if (activity?.getSharedPreferences("${requireContext().packageName}_preferences", Activity.MODE_PRIVATE)?.getBoolean("notify_before_paste", true) == true) {
                    dialog(TaskConfirmDialog(), TaskConfirmDialog.Result::class.java) { r ->
                        if (r.confirm) fileOperateBinderLocal.compoundTask(uriList, dest, key)
                    }
                } else {
                    fileOperateBinderLocal.compoundTask(uriList, dest, key)
                }
            }

        }

    }


    @BindClickEvent(FileItemHolder::class)
    fun toChild(itemHolder: FileItemHolder) {
        val old = session.fileInstance.value ?: return
        if (itemHolder.file.item.isDirectory) {
            findNavController().navigate(R.id.action_fileListFragment_self, FileListFragmentArgs(File(old.path, itemHolder.file.name).absolutePath, old.fileSystemRoot).toBundle())
        } else {
            findNavController().navigate(R.id.action_fileListFragment_to_openFileDialog, OpenFileDialogArgs(itemHolder.file.fullPath, old.fileSystemRoot).toBundle())
            fragment(OpenFileDialog.key, OpenFileDialog.OpenFileResult::class.java) { r ->
                if (old.fileSystemRoot != FileInstanceFactory.publicFileSystemRoot) return@fragment
                val file = File(itemHolder.file.fullPath)
                val uriForFile = FileProvider.getUriForFile(requireContext(), BuildConfig.FILE_PROVIDER_AUTHORITY, file)
                Intent("android.intent.action.VIEW").apply {
                    addCategory("android.intent.category.DEFAULT")
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

            resolveInstalledPlugins(itemHolder, mimeTypeFromExtension)
            resolveNoInstalledPlugins(mimeTypeFromExtension, fullPath)

            val liPlugin = LiPlugin()
            val pluginManager = object : DefaultPluginManager(requireContext()) {
                override suspend fun requestPath(initPath: String?): String {
                    val completableDeferred = CompletableDeferred<String>()
                    dialog(RequestPathDialog(), RequestPathDialog.RequestPathResult::class.java) { result ->
                        completableDeferred.complete(result.path)
                    }
                    return completableDeferred.await()
                }

                override fun runInService(block: GiantExplorerService.() -> Boolean) {
                    fileOperateBinder?.pluginTask(key, block)
                }

            }
            liPlugin.plugPluginManager(pluginManager)
            val group = liPlugin.group(listOf(File(fullPath)))
            if (group.isNotEmpty()) {
                group.map {
                    menu.loopAdd(it.first).add(0, it.second, 0, "li").setOnMenuItemClickListener {
                        scope.launch {
                            liPlugin.start(fullPath, it.itemId)
                        }
                        return@setOnMenuItemClickListener true
                    }
                }

            }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete -> fileOperateBinder?.delete(itemHolder.file.item, detectSelected(itemHolder), key)
                    R.id.move_to -> moveOrCopy(true, itemHolder)
                    R.id.copy_to -> moveOrCopy(false, itemHolder)
                    R.id.copy_file -> ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)?.let { manager ->
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

                    R.id.properties -> findNavController().navigate(R.id.action_fileListFragment_to_propertiesDialog, PropertiesDialogArgs(fullPath).toBundle())

                }
                true
            }
        }.show()
    }

    private fun PopupMenu.resolveNoInstalledPlugins(mimeTypeFromExtension: String?, fullPath: String) {
        pluginManagerRegister.pluginsName().forEach { pluginName: String ->
            val pluginFile = File(pluginName)
            val subMenu = pluginManagerRegister.resolvePluginName(pluginName, requireContext()).meta.subMenu
            menu.loopAdd(listOf(subMenu)).add(pluginName).setOnMenuItemClickListener {
                if (pluginFile.name.endsWith("apk")) startActivity(Intent(requireContext(), FragmentPluginActivity::class.java).apply {
                    putExtra("plugin-name", pluginFile.name)
                    plugUri(mimeTypeFromExtension, fullPath)
                })
                else startActivity(Intent(requireContext(), WebViewPluginActivity::class.java).apply {
                    putExtra("plugin-name", pluginFile.name)
                    plugUri(mimeTypeFromExtension, fullPath)
                })
                true
            }
        }
    }

    private fun PopupMenu.resolveInstalledPlugins(itemHolder: FileItemHolder, mimeTypeFromExtension: String?) {
        val intent = Intent("com.storyteller_f.action.giant_explorer.PLUGIN")
        intent.addCategory("android.intent.category.DEFAULT")
        intent.plugUri(mimeTypeFromExtension, itemHolder.file.fullPath)

        val activities = requireContext().packageManager.queryIntentActivitiesCompat(intent, (PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_META_DATA).toLong())
        activities.forEach {
            val activityInfo = it.activityInfo ?: return@forEach
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
        dialog(RequestPathDialog(), RequestPathDialog.RequestPathResult::class.java) { result ->
            scope.launch {
                result.path.nn {
                    getFileInstance(it, requireContext(), stoppableTask = stoppable())
                }.nn { dest ->
                    val key = uuid.data.value ?: return@nn
                    val detectSelected = detectSelected(itemHolder)
                    Log.i(TAG, "moveOrCopy: uuid: $key")
                    fileOperateBinder?.moveOrCopy(dest, detectSelected, itemHolder.file.item, move, key)
                }
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
        val item = t.iterator().asSequence().firstOrNull {
            it.title == e
        }
        val subMenu = item?.subMenu
        if (item != null && subMenu != null) {
            subMenu
        } else
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

    override fun group(file: List<File>): List<Pair<List<String>, Int>> {
        return if (file.all {
                it.extension == "zip"
            }) listOf(listOf("archive", "extract to") to 108)
        else listOf(listOf("archive", "compress") to 109)
    }

    override suspend fun start(fullPath: String, id: Int) {
        val requestPath = pluginManager.requestPath()
        println("request path $requestPath")
        if (id == 108) {
            extract(pluginManager.fileInputStream(fullPath), File(requestPath))
        } else {
            val dest = pluginManager.fileOutputStream(requestPath)
            val zipOutputStream = ZipOutputStream(dest)
            zipOutputStream.use {
                compress(it, File(fullPath), "")
            }
        }
    }

    private fun compress(dest: ZipOutputStream, fullPath: File, offset: String) {
        val path = fullPath.absolutePath
        val name = fullPath.name
        if (pluginManager.isFile(path)) {
            val zipEntry = ZipEntry("$offset/$name")
            dest.putNextEntry(zipEntry)
            pluginManager.fileInputStream(path).use {
                read(it, dest)
            }
        } else {
            val listFiles = pluginManager.listFiles(path)
            listFiles.forEach {
                val subName = File(it).name
                val subFile = File(fullPath, subName)
                val subPath = subFile.absolutePath
                if (!pluginManager.isFile(subPath)) {
                    val subDir = ZipEntry("$offset/$it/")
                    dest.putNextEntry(subDir)
                }
                compress(dest, subFile, name)
            }
        }
    }

    private fun read(it: FileInputStream, dest: ZipOutputStream) {
        val buffer = ByteArray(1024)
        it.buffered().use {
            while (true) {
                val offset = it.read(buffer)
                if (offset != -1) {
                    dest.write(buffer, 0, offset)
                } else break
            }
        }
    }

    private fun extract(archive: InputStream, dest: File) {
        pluginManager.runInService {
            reportRunning()
            ZipInputStream(archive).use { stream ->
                while (true) {
                    stream.nextEntry?.let {
                        processEntry(dest, it, stream)
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
