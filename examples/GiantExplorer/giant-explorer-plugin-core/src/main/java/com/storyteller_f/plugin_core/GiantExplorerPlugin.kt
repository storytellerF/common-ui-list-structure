package com.storyteller_f.plugin_core

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

interface GiantExplorerPlugin {
    fun plugPluginManager(pluginManager: GiantExplorerPluginManager)

    /**
     * 至少包含一个。
     */
    fun group(file: List<File>): List<Pair<List<String>, Int>>
}

interface GiantExplorerService {
    fun reportRunning()
}

interface GiantExplorerShellPlugin : GiantExplorerPlugin {
    suspend fun start(uriString: String, id: Int)
}

interface GiantExplorerPluginManager {

    fun fileInputStream(uriString: String): FileInputStream

    fun fileOutputStream(uriString: String): FileOutputStream

    fun listFiles(uriString: String): List<String>

    /**
     * @return uriString
     */
    suspend fun requestPath(initUri: String? = null): String

    /**
     * 没有ensureFile，当请求fileInputStream 或者fileOutputStream 时自动处理
     */
    fun ensureDir(uriString: String)

    fun runInService(block: GiantExplorerService.() -> Boolean)

    /**
     * 获取uri 对应的path 的parent uri
     * @return uriString
     */
    fun resolveParentUri(uriString: String): String?

    /**
     * 获取uri 对应的path
     */
    fun resolvePath(uriString: String): String?

    /**
     * 获取uri 对应的path 的parent path
     * @return path
     */
    fun resolveParentPath(uriString: String): String?

    fun isFile(uriString: String): Boolean
}