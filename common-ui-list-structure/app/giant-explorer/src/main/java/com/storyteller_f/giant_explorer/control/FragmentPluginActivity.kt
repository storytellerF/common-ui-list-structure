package com.storyteller_f.giant_explorer.control

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.plugin_core.GiantExplorerPlugin
import com.storyteller_f.plugin_core.GiantExplorerPluginManager
import dalvik.system.DexClassLoader
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val giantExplorerPluginIni = "META-INF/giant-explorer-plugin.ini"

class FragmentPluginActivity : AppCompatActivity() {
    private val pluginResources by lazy {
        val packageArchiveInfo = packageInfo()
        val applicationInfo = packageArchiveInfo?.applicationInfo
        if (applicationInfo != null)
            packageManager.getResourcesForApplication(applicationInfo)
        else null
    }

    private fun packageInfo(): PackageInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(File(filesDir, "plugins/$pluginName").absolutePath, PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getPackageArchiveInfo(File(filesDir, "plugins/$pluginName").absolutePath, 0)
        }
    }
    lateinit var pluginName: String
    lateinit var pluginFragments: List<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_plugin)
        val uri = intent.data
        pluginName = intent.getStringExtra("plugin-name")!!
        val pluginManager = object : GiantExplorerPluginManager {
            override fun fileInputStream(path: String): FileInputStream {
                return getFileInstance(path, this@FragmentPluginActivity).fileInputStream
            }

            override fun fileOutputStream(path: String): FileOutputStream {
                return getFileInstance(path, this@FragmentPluginActivity).fileOutputStream
            }

            override fun listFiles(path: String): List<String> {
                return getFileInstance(path, this@FragmentPluginActivity).list().let { filesAndDirectories ->
                    filesAndDirectories.files.map {
                        it.fullPath
                    } + filesAndDirectories.directories.map {
                        it.fullPath
                    }
                }
            }

        }
        lifecycleScope.launch {
            val file = File(filesDir, "plugins/$pluginName")
            val classLoader = this.javaClass.classLoader
            val dexClassLoader = DexClassLoader(file.absolutePath, null, null, classLoader)
            val readText = dexClassLoader.getResourceAsStream(giantExplorerPluginIni).bufferedReader().readLines()
            val name = readText.first()
            pluginFragments = readText.last().split(",")
            println(pluginFragments)
            val loadClass = dexClassLoader.loadClass(name)
            val newInstance = loadClass.newInstance()
            if (newInstance is Fragment) {
                if (newInstance is GiantExplorerPlugin) {
                    newInstance.plugPluginManager(pluginManager)
                }
                newInstance.arguments = Bundle().apply {
                    putParcelable("uri", uri)
                }
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