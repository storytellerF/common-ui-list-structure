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
    suspend fun start(fullPath: String, id: Int)
}

interface GiantExplorerPluginManager {

    fun fileInputStream(path: String): FileInputStream

    fun fileOutputStream(path: String): FileOutputStream

    fun listFiles(path: String): List<String>

    suspend fun requestPath(initPath: String? = null): String

    /**
     * 没有ensureFile，当请求fileInputStream 或者fileOutputStream 时自动处理
     */
    fun ensureDir(child: File)

    fun runInService(block: GiantExplorerService.() -> Boolean)

    /**
     * 获取uri 对应的path 的parent uri
     */
    fun resolveParentUri(uriString: String): String?

    /**
     * 获取uri 对应的path
     */
    fun resolvePath(uriString: String): String?

    /**
     * 获取uri 对应的path 的parent path
     */
    fun resolveParentPath(uriString: String): String?

    fun isFile(path: String): Boolean
}