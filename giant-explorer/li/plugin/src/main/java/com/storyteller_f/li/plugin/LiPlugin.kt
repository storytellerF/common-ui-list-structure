package com.storyteller_f.li.plugin

import androidx.core.net.toUri
import com.storyteller_f.plugin_core.GiantExplorerPluginManager
import com.storyteller_f.plugin_core.GiantExplorerShellPlugin
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LiPlugin : GiantExplorerShellPlugin {
    private lateinit var pluginManager: GiantExplorerPluginManager
    override fun plugPluginManager(pluginManager: GiantExplorerPluginManager) {
        this.pluginManager = pluginManager
    }

    override fun group(file: List<File>): List<Pair<List<String>, Int>> {
        return if (file.all {
                it.extension == "zip"
            }) listOf(listOf("archive", "extract to") to 108)
        else listOf(listOf("archive", "compress") to 109)
    }

    override suspend fun start(uriString: String, id: Int) {
        val requestPath = pluginManager.requestPath()
        println("request path $requestPath")
        if (id == 108) {
            extract(pluginManager.fileInputStream(uriString), File(requestPath))
        } else {
            val dest = pluginManager.fileOutputStream(requestPath)
            val zipOutputStream = ZipOutputStream(dest)
            zipOutputStream.use {
                compress(it, File(uriString), "")
            }
        }
    }

    private fun compress(dest: ZipOutputStream, fullPath: File, offset: String) {
        val path = fullPath.absolutePath
        val name = fullPath.name
        if (pluginManager.isFile(path)) {
            val zipEntry = ZipEntry("$offset/$name")
            dest.putNextEntry(zipEntry)
            pluginManager.fileInputStream(path).use {
                read(it, dest)
            }
        } else {
            val listFiles = pluginManager.listFiles(path)
            listFiles.forEach {
                val subName = File(it).name
                val subFile = File(fullPath, subName)
                val subPath = subFile.absolutePath
                if (!pluginManager.isFile(subPath)) {
                    val subDir = ZipEntry("$offset/$it/")
                    dest.putNextEntry(subDir)
                }
                compress(dest, subFile, name)
            }
        }
    }

    private fun read(it: FileInputStream, dest: ZipOutputStream) {
        val buffer = ByteArray(1024)
        it.buffered().use {
            while (true) {
                val offset = it.read(buffer)
                if (offset != -1) {
                    dest.write(buffer, 0, offset)
                } else break
            }
        }
    }

    private fun extract(archive: InputStream, dest: File) {
        pluginManager.runInService {
            reportRunning()
            ZipInputStream(archive).use { stream ->
                while (true) {
                    stream.nextEntry?.let {
                        processEntry(dest, it, stream)
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
            pluginManager.ensureDir(child.toUri().toString())
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