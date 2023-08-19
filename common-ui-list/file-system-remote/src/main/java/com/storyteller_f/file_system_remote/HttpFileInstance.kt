package com.storyteller_f.file_system_remote

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import com.storyteller_f.file_system.instance.BaseContextFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class HttpFileInstance(uri: Uri, context: Context) : BaseContextFileInstance(context, uri) {
    /**
     * 保存到cache 目录
     */
    private lateinit var tempFile: File

    init {
        assert(uri.scheme == "http" || uri.scheme == "https")
    }

    @Synchronized
    private fun ensureFile(): File {
        if (::tempFile.isInitialized) {
            return tempFile
        } else {
            val okHttpClient = OkHttpClient()
            val execute =
                okHttpClient.newCall(Request.Builder().url(uri.toString()).build()).execute()
            if (!execute.isSuccessful) {
                throw Exception("$uri code is ${execute.code}")
            } else {
                val body = execute.body
                if (body == null) throw Exception("$uri body is empty")
                else {
                    val file = createFile(execute)
                    writeStream(body, file)
                    return file
                }

            }
        }
    }

    private fun createFile(execute: Response): File {
        val contentDisposition = execute.header("Content-Disposition")
        val contentType = execute.header("content-type")
        val guessFileName =
            URLUtil.guessFileName(uri.toString(), contentDisposition, contentType)
        return File(
            context.cacheDir,
            "${System.currentTimeMillis()}/$guessFileName"
        )
    }

    private fun writeStream(body: ResponseBody, file: File) {
        body.source().buffer.use { int ->
            file.inputStream().channel.use { out ->
                val byteBuffer = ByteBuffer.allocateDirect(1024)
                while (int.read(byteBuffer) != -1) {
                    if (needStop()) throw Exception("stopped")
                    byteBuffer.flip()
                    out.write(byteBuffer)
                    byteBuffer.clear()
                }
                tempFile = file
            }
        }
    }

    override val name: String
        get() {
            return ensureFile().name
        }

    override val file: FileItemModel
        get() {
            TODO("Not yet implemented")
        }

    override val directory: DirectoryItemModel
        get() {
            TODO("Not yet implemented")
        }

    override val fileLength: Long
        get() {
            return ensureFile().length()
        }

    override val fileInputStream: FileInputStream
        get() {
            return ensureFile().inputStream()
        }

    override val fileOutputStream: FileOutputStream
        get() {
            TODO("Not yet implemented")
        }

    override fun listInternal(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    ) {
        TODO("Not yet implemented")
    }

    override val isFile: Boolean
        get() {
            return true
        }

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override val isDirectory: Boolean
        get() {
            TODO("Not yet implemented")
        }

    override fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override val directorySize: Long
        get() {
            TODO("Not yet implemented")
        }

    override fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override val isHidden: Boolean
        get() {
            TODO("Not yet implemented")
        }

    override fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        TODO("Not yet implemented")
    }

}