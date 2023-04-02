package com.storyteller_f.giant_explorer.control.plugin

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.storyteller_f.giant_explorer.FileSystemProviderResolver
import com.storyteller_f.giant_explorer.FragmentPluginConfiguration
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.control.getFileInstance
import com.storyteller_f.giant_explorer.pluginManagerRegister
import com.storyteller_f.multi_core.StoppableTask
import com.storyteller_f.plugin_core.GiantExplorerPlugin
import com.storyteller_f.plugin_core.GiantExplorerPluginManager
import com.storyteller_f.plugin_core.GiantExplorerService
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

const val giantExplorerPluginIni = "META-INF/giant-explorer-plugin.ini"

abstract class DefaultPluginManager(val context: Context) : GiantExplorerPluginManager {
    override fun fileInputStream(path: String): FileInputStream {
        return getFileInstance(path, context, stoppableTask = StoppableTask.Blocking).apply {
            createFile()
        }.fileInputStream
    }

    override fun fileOutputStream(path: String): FileOutputStream {
        return getFileInstance(path, context, stoppableTask = StoppableTask.Blocking).apply {
            createFile()
        }.fileOutputStream
    }

    override fun listFiles(path: String): List<String> {
        return getFileInstance(path, context, stoppableTask = StoppableTask.Blocking).listSafe().let { filesAndDirectories ->
            filesAndDirectories.files.map {
                it.fullPath
            } + filesAndDirectories.directories.map {
                it.fullPath
            }
        }
    }

    override fun resolveParentUri(uriString: String): String? {
        val parse = Uri.parse(uriString)
        val resolvePath = FileSystemProviderResolver.resolvePath(parse) ?: return null
        val parent = File(resolvePath).parent ?: return null
        return FileSystemProviderResolver.build(false, parent).toString()
    }

    override fun resolveParentPath(uriString: String): String? {
        val parse = Uri.parse(uriString)
        val resolvePath = FileSystemProviderResolver.resolvePath(parse) ?: return null
        return File(resolvePath).parent
    }

    override fun resolvePath(uriString: String): String? {
        val parse = Uri.parse(uriString)
        return FileSystemProviderResolver.resolvePath(parse)
    }

    override fun ensureDir(child: File) {
        getFileInstance(child.absolutePath, context, stoppableTask = StoppableTask.Blocking).createDirectory()
    }

    override fun isFile(path: String): Boolean {
        return getFileInstance(path, context, stoppableTask = StoppableTask.Blocking).isFile
    }
}

class FragmentPluginActivity : AppCompatActivity() {
    lateinit var pluginName: String
    lateinit var pluginFragments: List<String>
    private val pluginFile by lazy { File(filesDir, "plugins/$pluginName") }

    private val pluginResources by lazy {
        val absolutePath = pluginFile.absolutePath
        val packageArchiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(absolutePath, PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getPackageArchiveInfo(absolutePath, 0)
        }
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
            override suspend fun requestPath(initPath: String?): String {
                return ""
            }

            override fun runInService(block: GiantExplorerService.() -> Boolean) {
                TODO("Not yet implemented")
            }

        }
        lifecycleScope.launch {
            val revolvePlugin = pluginManagerRegister.resolvePluginName(pluginName, this@FragmentPluginActivity) as FragmentPluginConfiguration
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
                if (savedInstanceState == null)
                    supportFragmentManager.beginTransaction().replace(R.id.content, newInstance).commit()
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