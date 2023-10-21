package com.storyteller_f.giant_explorer.control.plugin

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import com.storyteller_f.file_system.decodeByBase64
import com.storyteller_f.file_system.encodeByBase64
import com.storyteller_f.file_system.rawTree
import com.storyteller_f.giant_explorer.BuildConfig
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

data class PluginMeta(val version: String, val path: String, val name: String, val subMenu: String)

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
            val readText = File(extractedPath, "config").readText().split("\n")
            val version = readText.first()
            val subMenu = readText.lastOrNull() ?: "other"
            return HtmlPluginConfiguration(meta.copy(version = version, subMenu = subMenu), extractedPath)
        }
    }
}

class PluginManager {
    /**
     * key 插件名称
     * value configuration
     */
    private val map = mutableMapOf<String, PluginConfiguration>()
    private val raw = mutableMapOf<String, String>()

    /**
     * 记录一个插件
     */
    @Synchronized
    fun foundPlugin(file: File) {
        val name = file.name
        raw[name] = file.absolutePath
    }

    /**
     * 根据路径解析插件
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
        val pluginMeta = PluginMeta("1.0", path, name, "other")
        val configuration = if (pluginType == PluginType.fragment) FragmentPluginConfiguration.resolve(
            pluginMeta
        )
        else runBlocking {
            HtmlPluginConfiguration.resolve(pluginMeta, context)
        }
        map[name] = configuration
        return configuration
    }

    /**
     * 根据名称解析插件
     */
    @WorkerThread
    @Synchronized
    fun resolvePluginName(name: String, context: Context) = resolvePlugin(pluginPath(name), context)

    /**
     * 所有查找到插件名称
     */
    fun pluginsName(): Set<String> {
        return map.keys + raw.keys
    }

    /**
     * 通过插件名称获取路径
     */
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

/**
 * path 第一个是通过base64 编码的原始uri 的标识信息。
 * 如果是CONTENT_FILE，即为file://
 * 如果是CONTENT，即为content://authority/tree
 * 如果是REMOTE，即为pf://authority
 */
object FileSystemProviderResolver {

    /**
     * 将外部uri 转换成内部使用的uri
     */
    fun resolve(uri: Uri): Uri? {
        if (uri.authority == BuildConfig.FILE_SYSTEM_ENCRYPTED_PROVIDER_AUTHORITY) return null
        val pathSegments = uri.pathSegments
        val encodedFront = pathSegments.first()
        val front = encodedFront.decodeByBase64()
        val truePath = uri.path!!.substring(encodedFront.length + 1)
        Log.d(TAG, "resolvePath: $front $truePath $encodedFront")
        return (front + truePath).toUri()
    }

    fun share(encrypted: Boolean, uri: Uri): Uri? {
        val authority = if (encrypted) BuildConfig.FILE_SYSTEM_ENCRYPTED_PROVIDER_AUTHORITY
        else BuildConfig.FILE_SYSTEM_PROVIDER_AUTHORITY
        val (id, path) = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val rawTree = uri.rawTree
            val path = uri.path!!.substring(rawTree.length + 1)
            "content://${uri.authority}/$rawTree" to path
        } else {
            "${uri.scheme}://${uri.authority}" to uri.path!!
        }
        val encodeByBase64 = id.encodeByBase64()
        val newPath = encodeByBase64 + path
        Log.d(TAG, "build: $newPath $encodeByBase64")
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).path(newPath).build()
    }

    private const val TAG = "FileSystemProviderReslv"
}