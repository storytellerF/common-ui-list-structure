package com.storyteller_f.li

import com.storyteller_f.plugin_core.GiantExplorerPluginManager
import com.storyteller_f.plugin_core.GiantExplorerService
import com.storyteller_f.plugin_core.GiantExplorerShellPlugin
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class LiPlugin : GiantExplorerShellPlugin {
    private lateinit var pluginManager: GiantExplorerPluginManager
    override fun plugPluginManager(pluginManager: GiantExplorerPluginManager) {
        this.pluginManager = pluginManager
    }

    override fun accept(file: List<File>): Boolean {
        return file.all {
            it.extension == "zip"
        }
    }

    override fun group(): List<String> {
        return listOf("archive", "extract to")
    }

    override suspend fun start(fullPath: String) {
        val requestPath = pluginManager.requestPath()
        println("request path $requestPath")
        unCompress(pluginManager.fileInputStream(fullPath), File(requestPath))
    }

    private fun unCompress(archive: InputStream, dest: File) {
        pluginManager.runInService {
            reportRunning()
            ZipInputStream(archive).use { stream ->
                while (true) {
                    val nextEntry = stream.nextEntry
                    nextEntry?.let {
                        processEntry(dest, nextEntry, stream)
                    } ?: break
                }
            }
            true
        }
    }

    private fun processEntry(dest: File, nextEntry: ZipEntry, stream: ZipInputStream) {
        val child = File(dest, nextEntry.name)
        println(nextEntry.name)
        if (nextEntry.isDirectory) {
            pluginManager.ensureDir(child)
        } else {
            write(pluginManager.fileOutputStream(child.absolutePath), stream)
        }
    }

    private fun write(file: FileOutputStream, stream: ZipInputStream) {
        val buffer = ByteArray(1024)
        file.buffered().use {
            while (true) {
                val offset = stream.read(buffer)
                if (offset != -1) {
                    it.write(buffer, 0, offset)
                } else break
            }
        }
    }

}