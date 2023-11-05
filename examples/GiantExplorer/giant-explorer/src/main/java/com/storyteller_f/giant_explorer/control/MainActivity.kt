package com.storyteller_f.giant_explorer.control

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.DocumentsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.common_ui.CommonActivity
import com.storyteller_f.common_ui.owner
import com.storyteller_f.common_ui.request
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui.supportNavigatorBarImmersive
import com.storyteller_f.common_vm_ktx.StateValueModel
import com.storyteller_f.common_vm_ktx.svm
import com.storyteller_f.common_vm_ktx.toDiffNoNull
import com.storyteller_f.file_system.FileSystemUriStore
import com.storyteller_f.file_system.getCurrentUserEmulatedPath
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import com.storyteller_f.file_system.rawTree
import com.storyteller_f.file_system_ktx.getFileInstance
import com.storyteller_f.file_system_root.RootAccessFileInstance
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.control.plugin.PluginManageActivity
import com.storyteller_f.giant_explorer.control.remote.RemoteManagerActivity
import com.storyteller_f.giant_explorer.control.root.RootAccessActivity
import com.storyteller_f.giant_explorer.control.task.BackgroundTaskConfigActivity
import com.storyteller_f.giant_explorer.databinding.ActivityMainBinding
import com.storyteller_f.giant_explorer.dialog.FileOperationDialog
import com.storyteller_f.giant_explorer.dialog.FilterDialogFragment
import com.storyteller_f.giant_explorer.dialog.SortDialogFragment
import com.storyteller_f.giant_explorer.dialog.VolumeSpaceDialog
import com.storyteller_f.giant_explorer.service.FileOperateBinder
import com.storyteller_f.giant_explorer.service.FileOperateService
import com.storyteller_f.giant_explorer.service.FileService
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.event.viewBinding
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import java.util.Properties

class FileExplorerSession(application: Application, uri: Uri) : AndroidViewModel(application) {
    val selected = MutableLiveData<List<Pair<DataItemHolder, Int>>>()
    val fileInstance = MutableLiveData<FileInstance>()

    init {
        viewModelScope.launch {
            getFileInstance(application.applicationContext, uri).let {
                fileInstance.value = it
            }
        }
    }
}

class MainActivity : CommonActivity(), FileOperateService.FileOperateResultContainer {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val filterHiddenFile by svm({}) { it, _ ->
        StateValueModel(it, "filter-hidden-file", false)
    }

    private val fileListViewModel by svm({}) { handle, _ ->
        FileListViewModel(handle)
    }

    private var currentRequestingAuthority: String? = null
    private var currentRequestingTree: String? = null
    private val requestDocumentProvider =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            processDocumentProvider(it)
        }
    private val drawableToggle by lazy {
        ActionBarDrawerToggle(this, binding.drawer, 0, 0)
    }
    private val menuProvider by lazy {
        DocumentProviderMenuProvider(binding.navView.menu, this, ::switchDocumentProvider, ::switchUriRoot)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportNavigatorBarImmersive(binding.content)

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
        binding.drawer.addDrawerListener(drawableToggle)
        fileListViewModel.displayGrid.distinctUntilChanged().observe(owner) {
            binding.switchDisplay.isActivated = it
        }
        binding.switchDisplay.setOnClick {
            fileListViewModel.displayGrid.value = it.isChecked
        }
        setupNav()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawableToggle.syncState()
    }

    override fun onResume() {
        super.onResume()
        menuProvider.flashFileSystemRootMenu()
    }

    private fun processDocumentProvider(uri: Uri?) {
        val requestingAuthority = currentRequestingAuthority
        val requestingTree = currentRequestingTree
        Log.i(TAG, "uri: $uri key: $requestingAuthority")
        if (uri != null && requestingAuthority != null) {
            val authority = uri.authority
            val tree = DocumentsContract.getTreeDocumentId(uri)
            if (requestingAuthority != authority || (requestingTree != null && requestingTree != tree)) {
                Toast.makeText(this, "选择错误", Toast.LENGTH_LONG).show()
                return
            }
            currentRequestingAuthority = null
            currentRequestingTree = null
            saveUriAndSwitch(uri, tree, authority)
            menuProvider.flashFileSystemRootMenu()
        }
    }

    private fun saveUriAndSwitch(uri: Uri, tree: String, authority: String) {
        contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        FileSystemUriStore.instance.saveUri(this, authority, uri, tree)

        switchDocumentProvider(authority, tree)
    }

    private fun switchDocumentProvider(authority: String, tree: String?) {
        scope.launch {
            switchDocumentProviderRoot(authority, tree)
        }
    }

    private fun setupNav() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val navController = navHostFragment.navController
        observePathMan(navController)
        val startDestinationArgs = intent.getBundleExtra("start") ?: FileListFragmentArgs(
            File(
                getCurrentUserEmulatedPath()
            ).toUri()
        ).toBundle()
        navController.setGraph(R.navigation.nav_main, startDestinationArgs)
    }

    private fun observePathMan(navController: NavController) {
        scope.launch {
            callbackFlow {
                binding.pathMan.setPathChangeListener { pathString -> trySend(pathString) }
                awaitClose {
                    binding.pathMan.setPathChangeListener(null)
                }
            }.flowWithLifecycle(lifecycle).collectLatest {
                val bundle =
                    findNavControl().currentBackStackEntry?.arguments ?: return@collectLatest
                val uri = FileListFragmentArgs.fromBundle(bundle).uri
                val build = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    val path = if (it == "/") "" else it
                    val tree = uri.rawTree
                    uri.buildUpon().path("/$tree$path").build()
                } else uri.buildUpon().path(it).build()
                navController.navigate(
                    R.id.fileListFragment, FileListFragmentArgs(build).toBundle()
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.filterHiddenFile)?.updateEye(filterHiddenFile.data.value == true)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> drawableToggle.onOptionsItemSelected(item)
            R.id.filterHiddenFile -> toggleHiddenFile(item)
            R.id.newWindow -> newWindow()
            R.id.filter -> request(FilterDialogFragment::class)
            R.id.sort -> request(SortDialogFragment::class)
            R.id.open_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.open_root_access -> startActivity(Intent(this, RootAccessActivity::class.java))
            R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.plugin_manager -> startActivity(Intent(this, PluginManageActivity::class.java))
            R.id.remote_manager -> startActivity(Intent(this, RemoteManagerActivity::class.java))
            R.id.volume_space -> request(VolumeSpaceDialog::class)
            R.id.background_task -> startActivity(
                Intent(
                    this, BackgroundTaskConfigActivity::class.java
                )
            )
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawableToggle.onConfigurationChanged(newConfig)
    }

    private fun toggleHiddenFile(item: MenuItem) {
        val newState = filterHiddenFile.data.value?.not() ?: true
        item.updateEye(newState)
        filterHiddenFile.data.value = newState
    }

    private fun newWindow() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        })
    }

    private fun switchUriRoot(uri: Uri) {
        closeDrawer()
        findNavControl().navigate(
            R.id.fileListFragment, FileListFragmentArgs(uri).toBundle()
        )
    }

    private suspend fun switchDocumentProviderRoot(authority: String, tree: String?): Boolean {
        if (tree != null) {
            documentProviderRoot(authority, tree)?.let {
                switchUriRoot(it)
                return true
            }
        }
        closeDrawer()
        currentRequestingAuthority = authority
        currentRequestingTree = tree

        val presetTreeKey = Properties().apply {
            load(assets.open("tree.keys"))
        }.getProperty(authority)
        val defaultTreeDocumentUri =
            if (presetTreeKey != null && FileSystemUriStore.instance.savedUri(
                    this,
                    authority,
                    presetTreeKey
                ) == null
            ) {
                DocumentsContract.buildRootUri(authority, presetTreeKey)
            } else null
        requestDocumentProvider.launch(defaultTreeDocumentUri)
        return false
    }

    private fun closeDrawer() {
        binding.drawer.close()
    }

    private fun findNavControl(): NavController {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        return navHostFragment.navController
    }

    private fun MenuItem.updateEye(newState: Boolean) {
        isChecked = newState
        icon = ContextCompat.getDrawable(
            this@MainActivity, if (newState) R.drawable.ic_eye_blind else R.drawable.ic_eye_vision
        )
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
                }.observe(this@MainActivity) {
                    Toast.makeText(
                        this@MainActivity, "${it.first} ${it.second}", Toast.LENGTH_SHORT
                    ).show()
                    if (it.first == FileOperateBinder.state_null) {
                        FileOperationDialog().apply {
                            this.binder = fileOperateBinderLocal
                        }.show(supportFragmentManager, FileOperationDialog.DIALOG_TAG)
                    }
                }
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
                RootAccessFileInstance.remote = FileSystemManager.getRemote(service)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            RootAccessFileInstance.remote = null
        }
    }

    override fun onSuccess(uri: Uri?, originUri: Uri?) {
        scope.launch {
            Toast.makeText(this@MainActivity, "dest $uri origin $originUri", Toast.LENGTH_SHORT)
                .show()
        }
//        adapter.refresh()
    }

    override fun onError(errorMessage: String?) {
        scope.launch {
            Toast.makeText(this@MainActivity, "error: $errorMessage", Toast.LENGTH_SHORT).show()
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

    companion object {
        private const val TAG = "MainActivity"
    }
}

fun PackageManager.queryIntentActivitiesCompat(
    searchDocumentProvider: Intent, flags: Long
): MutableList<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(searchDocumentProvider, PackageManager.ResolveInfoFlags.of(flags))
    } else {
        queryIntentActivities(searchDocumentProvider, flags.toInt())
    }
}

fun PackageManager.queryIntentContentProvidersCompat(
    searchDocumentProvider: Intent, flags: Long
): MutableList<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentContentProviders(
            searchDocumentProvider, PackageManager.ResolveInfoFlags.of(flags)
        )
    } else {
        queryIntentContentProviders(searchDocumentProvider, flags.toInt())
    }
}

suspend fun Activity.documentProviderRoot(
    authority: String,
    tree: String,
): Uri? {
    val savedUris = FileSystemUriStore.instance.savedUris(this)
    return if (!savedUris.contains(authority))
        null
    else try {
        val uri = DocumentLocalFileInstance.uriFromAuthority(authority, tree)
        if (getFileInstance(this, uri).exists()) uri else null
    } catch (e: Exception) {
        null
    }
}