package com.storyteller_f.plugin_core

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

interface GiantExplorerPlugin {
    fun plugPluginManager(pluginManager: GiantExplorerPluginManager)

    fun accept(file: List<File>): Boolean

    /**
     * 至少包含一个。
     */
    fun group(): List<String>
}

interface GiantExplorerPluginManager {

    fun fileInputStream(path: String): FileInputStream

    fun fileOutputStream(path: String): FileOutputStream

    fun listFiles(path: String): List<String>
}