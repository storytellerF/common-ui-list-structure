package com.storyteller_f.giant_explorer.control

import android.app.Application
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.common_ui.*
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.FileSystemUriSaver
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.control.plugin.PluginManageActivity
import com.storyteller_f.giant_explorer.control.plugin.stoppable
import com.storyteller_f.giant_explorer.control.remote.RemoteAccessType
import com.storyteller_f.giant_explorer.control.remote.RemoteManagerActivity
import com.storyteller_f.giant_explorer.control.root.RootAccessActivity
import com.storyteller_f.giant_explorer.database.RemoteSpec
import com.storyteller_f.giant_explorer.database.ShareSpec
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.ActivityMainBinding
import com.storyteller_f.giant_explorer.dialog.FileOperationDialog
import com.storyteller_f.giant_explorer.filter.*
import com.storyteller_f.giant_explorer.service.FileOperateBinder
import com.storyteller_f.giant_explorer.service.FileOperateService
import com.storyteller_f.giant_explorer.service.FileService
import com.storyteller_f.giant_explorer.service.FtpFileInstance
import com.storyteller_f.giant_explorer.service.FtpsFileInstance
import com.storyteller_f.giant_explorer.service.SFtpFileInstance
import com.storyteller_f.giant_explorer.service.SmbFileInstance
import com.storyteller_f.giant_explorer.service.WebDavFileInstance
import com.storyteller_f.giant_explorer.view.PathMan
import com.storyteller_f.multi_core.StoppableTask
import com.storyteller_f.ui_list.core.*
import com.storyteller_f.ui_list.event.viewBinding
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.thread

class FileExplorerSession(application: Application, path: String, root: String) : AndroidViewModel(application) {
    val selected = MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>()
    val fileInstance = MutableLiveData<FileInstance>()

    init {
        viewModelScope.launch {
            getFileInstanceAsync(path, application.applicationContext, root).let {
                fileInstance.value = it
            }
        }
    }
}

suspend fun getFileInstanceAsync(path: String, context: Context, root: String = FileInstanceFactory.publicFileSystemRoot) = suspendCancellableCoroutine {
    thread {
        val result = Result.success(getFileInstance(path, context, root, it.stoppable()))
        it.resumeWith(result)
    }
}

fun getFileInstance(path: String, context: Context, root: String = FileInstanceFactory.publicFileSystemRoot, stoppableTask: StoppableTask = StoppableTask.Blocking): FileInstance {
    if (root.startsWith("ftp://")) {
        return FtpFileInstance(path, root, RemoteSpec.parse(root))
    }
    if (root.startsWith("smb://")) {
        return SmbFileInstance(path, root, ShareSpec.parse(root))
    }
    if (root.startsWith("sftp://")) {
        return SFtpFileInstance(path, root, RemoteSpec.parse(root))
    }
    if (root.startsWith("ftpes://") || root.startsWith("ftps://")) {
        return FtpsFileInstance(path, root, RemoteSpec.parse(root))
    }
    if (root.startsWith("webdav://")) {
        return WebDavFileInstance(path, root, ShareSpec.parse(root))
    }
    return FileInstanceFactory.getFileInstance(path, context, root, stoppableTask)
}

class MainActivity : CommonActivity(), FileOperateService.FileOperateResultContainer {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val filterHiddenFile by svm({}) { it, _ ->
        StateValueModel(it, FileListFragment.filterHiddenFileKey, false)
    }
    private val dialogImpl = FilterDialogManager()

    private val filters by keyPrefix({ "filter" }, svm({ dialogImpl.filterDialog }, vmProducer = buildFilterDialogState))

    private val sort by keyPrefix({ "sort" }, svm({ dialogImpl.sortDialog }, vmProducer = buildSortDialogState))

    private val uuid by vm({}) {
        GenericValueModel<String>().apply {
            data.value = UUID.randomUUID().toString()
        }
    }

    private var currentRequestingKey: String? = null
    private val requestDocumentProvider = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        processDocumentProvider(it)
    }

    private fun processDocumentProvider(uri: Uri?) {
        val key = currentRequestingKey
        Log.i(TAG, "uri: $uri $key")
        if (uri != null && key != null) {
            if (key != uri.authority) {
                Toast.makeText(this, "选择错误", Toast.LENGTH_LONG).show()
                return
            }
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            FileSystemUriSaver.getInstance().saveUri(key, this, uri)
            currentRequestingKey = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uuid
        setSupportActionBar(binding.toolbar)
        supportNavigatorBarImmersive(binding.root)
        dialogImpl.init(this, {
            filters.data.value = it
        }, {
            sort.data.value = it
        })
        filters
        sort

        //连接服务
        val fileOperateIntent = Intent(this, FileOperateService::class.java)
        startService(fileOperateIntent)
        bindService(fileOperateIntent, connection, 0)

        Shell.getShell {
            if (it.isRoot) {
                val intent = Intent(this, FileService::class.java)
                //连接服务
                RootService.bind(intent, fileConnection)
            }

        }
        binding.switchRoot.setOnClick {
            openContextMenu(it)
        }
        registerForContextMenu(binding.switchRoot)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val navController = navHostFragment.navController

        scope.launch {
            callbackFlow {
                binding.pathMan.setPathChangeListener(object : PathMan.PathChangeListener {
                    override fun onSkipOnPathMan(pathString: String) {
                        trySend(pathString)
                    }

                    override fun root(): String {
                        return FileInstanceFactory.publicFileSystemRoot
                    }

                })
                awaitClose {
                    binding.pathMan.setPathChangeListener(null)
                }
            }.flowWithLifecycle(lifecycle).collectLatest {
                navController.navigate(R.id.fileListFragment, FileListFragmentArgs(it, FileInstanceFactory.publicFileSystemRoot).toBundle())
            }
        }
        navController.setGraph(R.navigation.nav_main, FileListFragmentArgs(FileInstanceFactory.rootUserEmulatedPath, FileInstanceFactory.publicFileSystemRoot).toBundle())
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.filterHiddenFile)?.updateIcon(filterHiddenFile.data.value == true)
        menu.findItem(R.id.paste_file)?.let {
            it.isEnabled = ContextCompat.getSystemService(this, ClipboardManager::class.java)?.hasPrimaryClip() == true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filterHiddenFile -> {
                val newState = filterHiddenFile.data.value?.not() ?: true
                item.updateIcon(newState)
                filterHiddenFile.data.value = newState
            }

            R.id.newWindow -> startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            })

            R.id.filter -> dialogImpl.showFilter()
            R.id.sort -> dialogImpl.showSort()
            R.id.open_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.open_root_access -> startActivity(Intent(this, RootAccessActivity::class.java))
            R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.plugin_manager -> startActivity(Intent(this, PluginManageActivity::class.java))
            R.id.remote_manager -> startActivity(Intent(this, RemoteManagerActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu ?: return
        val provider = Intent("android.content.action.DOCUMENTS_PROVIDER")
        val info = packageManager.queryIntentContentProvidersCompat(provider, 0)
        val savedUris = FileSystemUriSaver.getInstance().savedUris(this)

        menu.add("return").setOnMenuItemClickListener {
            findNavControl().navigate(R.id.fileListFragment, FileListFragmentArgs("/", FileInstanceFactory.publicFileSystemRoot).toBundle())
            true
        }
        scope.launch {
            requireDatabase.remoteAccessDao().listAsync().forEach {

                if (it.type == RemoteAccessType.smb || it.type == RemoteAccessType.webDav) {
                    val toUri = it.toShareSpec().toUri()
                    menu.add(toUri).setOnMenuItemClickListener {
                        findNavControl().navigate(R.id.fileListFragment, FileListFragmentArgs("/", toUri).toBundle())
                        true
                    }
                } else {
                    val toUri = it.toFtpSpec().toUri()
                    menu.add(toUri).setOnMenuItemClickListener {
                        findNavControl().navigate(R.id.fileListFragment, FileListFragmentArgs("/", toUri).toBundle())
                        true
                    }
                }
            }
        }
        info.forEach {
            val authority = it.providerInfo.authority
            val root = Uri.Builder().scheme("content").authority(authority).build().toString()
            val loadLabel = it.loadLabel(packageManager).toString()
//            val icon = it.loadIcon(packageManager)
            val contains = savedUris.contains(authority) && try {
                DocumentLocalFileInstance(this@MainActivity, "/", authority, root).exists()
            } catch (_: Exception) {
                false
            }
            menu.add(loadLabel)
                .setChecked(contains)
                .setCheckable(true)
//                .setActionView(ImageView(this).apply {
//                    setImageDrawable(icon)
//                })
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        tooltipText = authority
                    }
                }
                .setOnMenuItemClickListener {
                    switchRoot(authority)
                    true
                }

        }

    }

    private fun switchRoot(authority: String): Boolean {
        val savedUri = FileSystemUriSaver.getInstance().savedUri(authority, this)
        if (savedUri != null) {
            try {
                val root = Uri.Builder().scheme("content").authority(authority).build().toString()
                val instance = DocumentLocalFileInstance(this, "/", authority, root)
                if (instance.exists()) {
                    findNavControl().navigate(R.id.fileListFragment, FileListFragmentArgs(instance.path, root).toBundle())
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "switchRoot: ", e)
            }

        }
        currentRequestingKey = authority
        requestDocumentProvider.launch(null)
        return false
    }

    private fun findNavControl(): NavController {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        return navHostFragment.navController
    }

    private fun MenuItem.updateIcon(newState: Boolean) {
        isChecked = newState
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            iconTintList = if (newState) ColorStateList.valueOf(Color.GRAY) else ColorStateList.valueOf(Color.BLACK)
        }
    }

    var fileOperateBinder: FileOperateBinder? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Toast.makeText(this@MainActivity, "服务已连接", Toast.LENGTH_SHORT).show()
            val fileOperateBinderLocal = service as FileOperateBinder
            Log.i(TAG, "onServiceConnected: $fileOperateBinderLocal")
            fileOperateBinder = fileOperateBinderLocal
            fileOperateBinderLocal.let { binder ->
                binder.fileOperateResultContainer = WeakReference(this@MainActivity)
                binder.state.toDiffNoNull { i, i2 ->
                    i == i2
                }.observe(this@MainActivity, Observer {
                    Toast.makeText(this@MainActivity, "${it.first} ${it.second}", Toast.LENGTH_SHORT).show()
                    if (it.first == FileOperateBinder.state_null) {
                        FileOperationDialog().apply {
                            this.binder = fileOperateBinderLocal
                        }.show(supportFragmentManager, FileOperationDialog.tag)
                    }
                })
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            fileOperateBinder = null
            Toast.makeText(this@MainActivity, "服务已关闭", Toast.LENGTH_SHORT).show()
        }
    }

    private val fileConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            service?.let {
                FileSystemUriSaver.getInstance().remote = FileSystemManager.getRemote(service)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            FileSystemUriSaver.getInstance().remote = null
        }
    }

    override fun onSuccess(dest: String?, origin: String?) {
        scope.launch {
            Toast.makeText(this@MainActivity, "dest $dest origin $origin", Toast.LENGTH_SHORT).show()
        }
//        adapter.refresh()
    }

    override fun onError(string: String?) {
        scope.launch {
            Toast.makeText(this@MainActivity, "error: $string", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCancel() {
        scope.launch {
            Toast.makeText(this@MainActivity, "cancel", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unbindService(connection)
        } catch (e: Exception) {
            Toast.makeText(this, e.exceptionMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun drawPath(path: String) {
        binding.pathMan.drawPath(path)
    }
}

@Suppress("DEPRECATION")
fun PackageManager.queryIntentActivitiesCompat(searchDocumentProvider: Intent, flags: Long): MutableList<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(searchDocumentProvider, PackageManager.ResolveInfoFlags.of(flags))
    } else {
        queryIntentActivities(searchDocumentProvider, flags.toInt())
    }
}

@Suppress("DEPRECATION")
fun PackageManager.queryIntentContentProvidersCompat(searchDocumentProvider: Intent, flags: Long): MutableList<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentContentProviders(searchDocumentProvider, PackageManager.ResolveInfoFlags.of(flags))
    } else {
        queryIntentContentProviders(searchDocumentProvider, flags.toInt())
    }
}