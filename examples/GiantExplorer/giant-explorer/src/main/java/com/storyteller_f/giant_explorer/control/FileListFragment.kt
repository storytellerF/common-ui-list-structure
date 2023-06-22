package com.storyteller_f.giant_explorer.control

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
import androidx.core.view.iterator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.common_ktx.safeLet
import com.storyteller_f.common_ui.*
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.instance.Create
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.NotCreate
import com.storyteller_f.file_system.model.FileSystemItemModelLite
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.*
import com.storyteller_f.giant_explorer.BuildConfig
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.control.plugin.DefaultPluginManager
import com.storyteller_f.giant_explorer.control.plugin.FragmentPluginActivity
import com.storyteller_f.giant_explorer.control.plugin.WebViewPluginActivity
import com.storyteller_f.giant_explorer.control.plugin.stoppable
import com.storyteller_f.giant_explorer.databinding.FragmentFileListBinding
import com.storyteller_f.giant_explorer.dialog.*
import com.storyteller_f.plugin_core.*
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

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
        genericValueModel(UUID.randomUUID().toString())
    })

    private val args by navArgs<FileListFragmentArgs>()

    private val observer = FileListObserver(this, { args }, selfScope)

    private val shareTarget by keyPrefix({ "shareTarget" }, pvm({}) {
        SharePasteTargetViewModel()
    })

    override fun onBindViewEvent(binding: FragmentFileListBinding) {
        val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>()

        observer.setup(binding.content, adapter, { holder ->
            openFolderInNewPage(holder)
        }) {
            (requireContext() as MainActivity).drawPath(it)
        }

        setupMenu()
    }

    private fun openFolderInNewPage(holder: FileItemHolder) {
        val uri = observer.fileInstance?.toChild(holder.file.name, NotCreate)?.uri ?: return
        startActivity(Intent(requireContext(), MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            putExtra(
                "start",
                FileListFragmentArgs(uri).toBundle()
            )
        })
    }

    private fun setupMenu() {
        (requireActivity() as? MenuHost)?.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.file_list_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                updatePasteButtonState(menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
                R.id.add_file -> addFile()
                R.id.paste_file -> pasteFiles()
                else -> false
            }


        }, owner)
    }

    private fun updatePasteButtonState(menu: Menu) {
        menu.findItem(R.id.paste_file)?.let {
            it.isEnabled = ContextCompat.getSystemService(
                requireContext(),
                ClipboardManager::class.java
            )?.hasPrimaryClip() == true
        }
    }

    private fun addFile(): Boolean {
        findNavController().navigate(R.id.action_fileListFragment_to_newNameDialog)
        fragment(
            NewNameDialog.requestKey,
            NewNameDialog.NewNameResult::class.java
        ) { nameResult ->
            observer.fileInstance?.toChild(nameResult.name, Create(true))
        }
        return true
    }

    private fun pasteFiles(): Boolean {
        ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
            ?.let { manager ->
                manager.primaryClip?.let { data ->
                    pasteFiles(data)
                }
            }
        return true
    }

    fun pasteFiles(data: ClipData, destDirectory: Uri? = null) {
        val key = uuid.data.value ?: return
        Log.i(TAG, "handleClipData: key $key")
        viewLifecycleOwner.lifecycleScope.launch {
            val dest = destDirectory.safeLet {
                getFileInstance(requireContext(), it, stoppableTask = stoppable())
            } ?: observer.fileInstance ?: kotlin.run {
                Toast.makeText(requireContext(), "无法确定目的地", Toast.LENGTH_LONG).show()
                return@launch
            }
            val uriList = resolveUri(data)
            if (uriList.any {
                    it.scheme == ContentResolver.SCHEME_FILE && it == destDirectory
                }) {
                //静默处理
                return@launch
            }
            if (uriList.isNotEmpty()) {
                startPasteFiles(uriList, dest, key)
            }

        }

    }

    private fun resolveUri(data: ClipData): List<Uri> {
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
        return uriList
    }

    private fun startPasteFiles(
        uriList: List<Uri>,
        dest: FileInstance,
        key: String
    ) {
        val items = uriList.map {
            FileSystemItemModelLite(it.path!!, it)
        }
        shareTarget.replace(uriList, dest)
        val fileOperateBinderLocal = fileOperateBinder ?: kotlin.run {
            Toast.makeText(requireContext(), "未连接服务", Toast.LENGTH_LONG).show()
            return
        }
        if (activity?.getSharedPreferences(
                "${requireContext().packageName}_preferences",
                Activity.MODE_PRIVATE
            )?.getBoolean("notify_before_paste", true) == true
        ) {
            dialog(TaskConfirmDialog(), TaskConfirmDialog.Result::class.java) { r ->
                if (r.confirm) fileOperateBinderLocal.moveOrCopy(dest, items,  null, false, key)
            }
        } else {
            fileOperateBinderLocal.moveOrCopy(dest, items, null, false, key)
        }
    }


    @BindClickEvent(FileItemHolder::class)
    fun toChild(itemHolder: FileItemHolder) {
        val old = observer.fileInstance ?: return
        if (itemHolder.file.item.isDirectory) {
            findNavController().navigate(
                R.id.action_fileListFragment_self,
                FileListFragmentArgs(
                    old.toChild(itemHolder.file.name, NotCreate).uri,
                ).toBundle()
            )
        } else {
            val uri = old.uri
            findNavController().navigate(
                R.id.action_fileListFragment_to_openFileDialog,
                OpenFileDialogArgs(uri).toBundle()
            )
            fragment(OpenFileDialog.key, OpenFileDialog.OpenFileResult::class.java) { r ->
                if (uri.scheme != ContentResolver.SCHEME_FILE) return@fragment
                val file = File(itemHolder.file.fullPath)
                val uriForFile = FileProvider.getUriForFile(
                    requireContext(),
                    BuildConfig.FILE_PROVIDER_AUTHORITY,
                    file
                )
                Intent("android.intent.action.VIEW").apply {
                    addCategory("android.intent.category.DEFAULT")
                    setDataAndType(uriForFile, r.mimeType)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }.let {
                    val ellipsizedText = TextUtils.ellipsize(
                        itemHolder.file.name,
                        TextPaint(),
                        100f,
                        TextUtils.TruncateAt.MIDDLE
                    )
                    startActivity(Intent.createChooser(it, "open $ellipsizedText by"))
                }
            }
        }
    }

    @BindClickEvent(FileItemHolder::class, "fileIcon")
    fun fileMenu(view: View, itemHolder: FileItemHolder) {
        val fullPath = itemHolder.file.fullPath
        val name = itemHolder.file.name
        val uri = observer.fileInstance?.toChild(name, NotCreate)?.uri ?: return
        val key = uuid.data.value ?: return

        PopupMenu(requireContext(), view).apply {
            inflate(R.menu.item_context_menu)
            val mimeTypeFromExtension =
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(File(fullPath).extension)

            resolveInstalledPlugins(itemHolder, mimeTypeFromExtension)
            resolveNoInstalledPlugins(mimeTypeFromExtension, fullPath)
            resolveModulePlugin(key, fullPath)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete -> delete(itemHolder, key)
                    R.id.move_to -> moveOrCopy(true, itemHolder)
                    R.id.copy_to -> moveOrCopy(false, itemHolder)
                    R.id.copy_file -> copyFilePathToClipboard(itemHolder)
                    R.id.properties -> showPropertiesDialog(uri)
                }
                true
            }
        }.show()
    }

    private fun PopupMenu.resolveModulePlugin(
        key: String,
        fullPath: String
    ) {
        val liPlugin = try {
            javaClass.classLoader?.loadClass("com.storyteller_f.li.plugin.LiPlugin")
                ?.newInstance() as? GiantExplorerShellPlugin
        } catch (e: Exception) {
            null
        } ?: return
        val pluginManager = defaultPluginManager(key)
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
    }

    private fun defaultPluginManager(key: String) =
        object : DefaultPluginManager(requireContext()) {
            override suspend fun requestPath(initUri: String?): String {
                val completableDeferred = CompletableDeferred<String>()
                dialog(
                    RequestPathDialog(),
                    RequestPathDialog.RequestPathResult::class.java
                ) { result ->
                    completableDeferred.complete(result.path)
                }
                return completableDeferred.await()
            }

            override fun runInService(block: GiantExplorerService.() -> Boolean) {
                fileOperateBinder?.pluginTask(key, block)
            }

        }

    private fun showPropertiesDialog(fullPath: Uri) {
        findNavController().navigate(
            R.id.action_fileListFragment_to_propertiesDialog,
            PropertiesDialogArgs(fullPath).toBundle()
        )
    }

    private fun delete(
        itemHolder: FileItemHolder,
        key: String
    ) {
        fileOperateBinder?.delete(
            itemHolder.file.item,
            detectSelected(itemHolder),
            key
        )
    }

    private fun copyFilePathToClipboard(itemHolder: FileItemHolder) {
        ContextCompat.getSystemService(
            requireContext(),
            ClipboardManager::class.java
        )?.let { manager ->
            val map = detectSelected(itemHolder).map {
                Uri.fromFile(File(it.fullPath))
            }
            val clipData =
                ClipData.newPlainText(clipDataKey, map.first().toString()).apply {
                    if (map.size > 1) map.subList(1, map.size).forEach {
                        addItem(ClipData.Item(it))
                    }
                }
            manager.setPrimaryClip(clipData)
        }
    }

    private fun PopupMenu.resolveNoInstalledPlugins(
        mimeTypeFromExtension: String?,
        fullPath: String
    ) {
        pluginManagerRegister.pluginsName().forEach { pluginName: String ->
            val pluginFile = File(pluginName)
            val subMenu =
                pluginManagerRegister.resolvePluginName(pluginName, requireContext()).meta.subMenu
            menu.loopAdd(listOf(subMenu)).add(pluginName).setOnMenuItemClickListener {
                startNotInstalledPlugin(pluginFile, mimeTypeFromExtension, fullPath)
            }
        }
    }

    private fun startNotInstalledPlugin(
        pluginFile: File,
        mimeTypeFromExtension: String?,
        fullPath: String
    ): Boolean {
        if (pluginFile.name.endsWith("apk")) startActivity(
            Intent(
                requireContext(),
                FragmentPluginActivity::class.java
            ).apply {
                putExtra("plugin-name", pluginFile.name)
                plugUri(mimeTypeFromExtension, fullPath)
            })
        else startActivity(
            Intent(
                requireContext(),
                WebViewPluginActivity::class.java
            ).apply {
                putExtra("plugin-name", pluginFile.name)
                plugUri(mimeTypeFromExtension, fullPath)
            })
        return true
    }

    private fun PopupMenu.resolveInstalledPlugins(
        itemHolder: FileItemHolder,
        mimeTypeFromExtension: String?
    ) {
        val intent = Intent("com.storyteller_f.action.giant_explorer.PLUGIN")
        intent.addCategory("android.intent.category.DEFAULT")
        intent.plugUri(mimeTypeFromExtension, itemHolder.file.fullPath)

        val activities = requireContext().packageManager.queryIntentActivitiesCompat(
            intent,
            (PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_META_DATA).toLong()
        )
        activities.forEach {
            addToMenu(it, intent)
        }

    }

    private fun PopupMenu.addToMenu(
        it: ResolveInfo,
        intent: Intent
    ) {
        val activityInfo = it.activityInfo ?: return
        val groups = activityInfo.metaData?.getString("group")?.split("/") ?: return
        val title = activityInfo.metaData?.getString("title") ?: return
        menu.loopAdd(groups).add(title).setOnMenuItemClickListener {
            intent.setPackage(requireContext().packageName).component =
                ComponentName(activityInfo.packageName, activityInfo.name)
            startActivity(intent)
            return@setOnMenuItemClickListener true
        }
    }

    private fun moveOrCopy(move: Boolean, itemHolder: FileItemHolder) {
        dialog(RequestPathDialog(), RequestPathDialog.RequestPathResult::class.java) { result ->
            scope.launch {
                result.path.safeLet {
                    getFileInstance(requireContext(), File(it).toUri(), stoppableTask = stoppable())
                }.safeLet { dest ->
                    moveOrCopy(itemHolder, dest, move)
                }
            }
        }
    }

    private fun moveOrCopy(
        itemHolder: FileItemHolder,
        dest: FileInstance,
        move: Boolean
    ) {
        val key = uuid.data.value ?: return
        val detectSelected = detectSelected(itemHolder)
        Log.i(TAG, "moveOrCopy: uuid: $key")
        fileOperateBinder?.moveOrCopy(
            dest,
            detectSelected,
            itemHolder.file.item,
            move,
            key
        )
    }

    companion object {
        const val filterHiddenFileKey = "filter-hidden-file"
        const val clipDataKey = "file explorer"
        private const val TAG = "FileListFragment"
    }

    private fun detectSelected(itemHolder: FileItemHolder) =
        observer.selected?.map { pair -> (pair.first as FileItemHolder).file.item } ?: listOf(
            itemHolder.file.item
        )

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

