package com.storyteller_f.giant_explorer.control.plugin

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.storyteller_f.compat_ktx.packageInfoCompat
import com.storyteller_f.file_system_ktx.getFileInstance
import com.storyteller_f.giant_explorer.FileSystemProviderResolver
import com.storyteller_f.giant_explorer.FragmentPluginConfiguration
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.pluginManagerRegister
import com.storyteller_f.plugin_core.GiantExplorerPlugin
import com.storyteller_f.plugin_core.GiantExplorerPluginManager
import com.storyteller_f.plugin_core.GiantExplorerService
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

const val giantExplorerPluginIni = "META-INF/giant-explorer-plugin.ini"
suspend fun Context.fileInputStream1(uriString: String) =
    getFileInstance(this, uriString.toUri()).apply {
        createFile()
    }.getFileInputStream()

abstract class DefaultPluginManager(val context: Context) : GiantExplorerPluginManager {
    override suspend fun fileInputStream(uriString: String): FileInputStream {
        return context.fileInputStream1(uriString)
    }

    override suspend fun fileOutputStream(uriString: String): FileOutputStream {
        return getFileInstance(context, uriString.toUri()).apply {
            createFile()
        }.getFileOutputStream()
    }

    override suspend fun listFiles(uriString: String): List<String> {
        return getFileInstance(context, uriString.toUri()).list().let { filesAndDirectories ->
            filesAndDirectories.files.map {
                it.fullPath
            } + filesAndDirectories.directories.map {
                it.fullPath
            }
        }
    }

    override fun resolveParentUri(uriString: String): String? {
        val resolvePath = FileSystemProviderResolver.resolvePath(uriString.toUri()) ?: return null
        val parent = File(resolvePath).parent ?: return null
        return FileSystemProviderResolver.build(false, parent).toString()
    }

    override fun resolveParentPath(uriString: String): String? {
        val resolvePath = FileSystemProviderResolver.resolvePath(uriString.toUri()) ?: return null
        return File(resolvePath).parent
    }

    override fun resolvePath(uriString: String): String? {
        return FileSystemProviderResolver.resolvePath(uriString.toUri())
    }

    override suspend fun ensureDir(uriString: String) {
        getFileInstance(context, uriString.toUri()).createDirectory()
    }

    override suspend fun isFile(uriString: String): Boolean {
        return getFileInstance(context, uriString.toUri()).isFile()
    }
}

class FragmentPluginActivity : AppCompatActivity() {
    private lateinit var pluginName: String
    private lateinit var pluginFragments: List<String>
    private val pluginFile by lazy { File(filesDir, "plugins/$pluginName") }

    private val pluginResources by lazy {
        val absolutePath = pluginFile.absolutePath
        val packageArchiveInfo = packageManager.packageInfoCompat(absolutePath)
        val applicationInfo = packageArchiveInfo?.applicationInfo
        if (applicationInfo != null) {
            applicationInfo.publicSourceDir = absolutePath
            applicationInfo.sourceDir = absolutePath
            packageManager.getResourcesForApplication(applicationInfo)
        } else null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_plugin)
        val uri = intent.data
        pluginName = intent.getStringExtra("plugin-name")!!
        val pluginManager = object : DefaultPluginManager(this) {
            override suspend fun requestPath(initUri: String?): String {
                return ""
            }

            override fun runInService(block: suspend GiantExplorerService.() -> Boolean) {

            }

        }
        lifecycleScope.launch {
            val revolvePlugin = pluginManagerRegister.resolvePluginName(
                pluginName,
                this@FragmentPluginActivity
            ) as FragmentPluginConfiguration
            val dexClassLoader = revolvePlugin.classLoader
            val name = revolvePlugin.startFragment
            pluginFragments = revolvePlugin.pluginFragments
            val loadClass = dexClassLoader.loadClass(name)
            val newInstance = loadClass.newInstance()
            if (newInstance is Fragment) {
                if (newInstance is GiantExplorerPlugin) {
                    newInstance.plugPluginManager(pluginManager)
                }
                newInstance.arguments = Bundle().apply {
                    putParcelable("uri", uri)
                }
                if (savedInstanceState == null) {
                    val beginTransaction = supportFragmentManager.beginTransaction()
                    beginTransaction.replace(R.id.content, newInstance)
                    beginTransaction.commit()
                }
            }
        }
    }

    override fun getResources(): Resources {
        val stackTrace = Thread.currentThread().stackTrace
        val listOf = if (this::pluginFragments.isInitialized) pluginFragments else listOf()
        if (stackTrace.any {
                listOf.contains(it.className)
            }) {
            return pluginResources!!
        }
        return super.getResources()
    }
}