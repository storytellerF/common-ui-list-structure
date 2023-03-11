package com.storyteller_f.giant_explorer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import com.storyteller_f.giant_explorer.control.ensureExtract
import com.storyteller_f.giant_explorer.control.giantExplorerPluginIni
import com.storyteller_f.ui_list.core.Model
import dalvik.system.DexClassLoader
import kotlinx.coroutines.runBlocking
import java.io.File

object PluginType {
    const val fragment = 0
    const val html = 1
}

abstract class PluginConfiguration(val meta: PluginMeta): Model {
    override fun commonId(): String {
        return meta.path
    }
}

data class PluginMeta(val version: String, val path: String, val name: String)

class FragmentPluginConfiguration(meta: PluginMeta, val classLoader: ClassLoader, val startFragment: String, val pluginFragments: List<String>): PluginConfiguration(meta) {

    companion object {
        fun resolve(meta: PluginMeta): FragmentPluginConfiguration {
            val dexClassLoader = DexClassLoader(meta.path, null, null, meta.javaClass.classLoader)
            val readText = dexClassLoader.getResourceAsStream(giantExplorerPluginIni).bufferedReader().readLines()
            val startFragment = readText.first()
            val pluginFragments = readText[1].split(",")
            val version = readText.last()
            return FragmentPluginConfiguration(meta.copy(version = version), dexClassLoader, startFragment, pluginFragments)
        }
    }

}

class HtmlPluginConfiguration(meta: PluginMeta, val extractedPath: String) : PluginConfiguration(meta) {

    companion object {
        suspend fun resolve(meta: PluginMeta, context: Context): HtmlPluginConfiguration {
            val pluginFile = File(meta.path)
            val extractedPath = File(context.filesDir, "plugins/${pluginFile.nameWithoutExtension}").absolutePath
            File(meta.path).ensureExtract(extractedPath)
            val version = File(extractedPath, "config").readText()
            return HtmlPluginConfiguration(meta.copy(version = version), extractedPath)
        }
    }
}

class PluginManager {
    /**
     * key name
     * value configuration
     */
    private val map = mutableMapOf<String, PluginConfiguration>()
    private val raw = mutableMapOf<String, String>()

    @Synchronized
    fun foundPlugin(file: File) {
        val name = file.name
        raw[name] = file.absolutePath
    }

    /**
     * 涉及io 读写应该在线程中执行
     */
    @WorkerThread
    @Synchronized
    fun resolvePlugin(path: String, context: Context): PluginConfiguration {
        val file = File(path)
        val name = file.name
        val extension = file.extension
        map[name]?.let {
            return it
        }
        val pluginType = if (extension == "apk") PluginType.fragment else PluginType.html
        if (raw.contains(name)) raw.remove(name)
        val pluginMeta = PluginMeta("1.0", path, name)
        val configuration = if (pluginType == PluginType.fragment) FragmentPluginConfiguration.resolve(pluginMeta)
        else runBlocking {
            HtmlPluginConfiguration.resolve(pluginMeta, context)
        }
        map[name] = configuration
        return configuration
    }

    @WorkerThread
    @Synchronized
    fun resolvePluginName(name: String, context: Context): PluginConfiguration {
        return resolvePlugin(pluginPath(name), context)
    }

    fun pluginsName(): Set<String> {
        return map.keys + raw.keys
    }

    private fun pluginPath(name: String): String {
        if (raw.contains(name)) {
            return raw[name]!!
        }
        return map[name]!!.meta.path
    }

    @Synchronized
    fun removeAllPlugin() {
        map.clear()
        raw.clear()
    }

    companion object
}

object FileSystemProviderResolver {
    fun resolvePath(uri: Uri): String? {
        if (uri.authority == BuildConfig.FILE_SYSTEM_ENCRYPTED_PROVIDER_AUTHORITY) return null
        return uri.path
    }

    fun build(encrypted: Boolean, path: String): Uri? {
        val authority = if (encrypted) BuildConfig.FILE_SYSTEM_ENCRYPTED_PROVIDER_AUTHORITY
        else BuildConfig.FILE_SYSTEM_PROVIDER_AUTHORITY
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).path(path).build()
    }
}