package com.storyteller_f.file_system_remote

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import com.storyteller_f.file_system.instance.BaseContextFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import kotlinx.coroutines.yield
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

    private suspend fun ensureFile(): File {
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
                if (body == null) {
                    throw Exception("$uri body is empty")
                } else {
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

    private suspend fun writeStream(body: ResponseBody, file: File) {
        body.source().buffer.use { int ->
            file.inputStream().channel.use { out ->
                val byteBuffer = ByteBuffer.allocateDirect(1024)
                while (int.read(byteBuffer) != -1) {
                    yield()
                    byteBuffer.flip()
                    out.write(byteBuffer)
                    byteBuffer.clear()
                }
                tempFile = file
            }
        }
    }

    override suspend fun getFile(): FileItemModel {
        TODO("Not yet implemented")
    }

    override suspend fun getDirectory(): DirectoryItemModel {
        TODO("Not yet implemented")
    }

    override suspend fun getFileLength(): Long {
        return ensureFile().length()
    }

    override suspend fun getFileInputStream(): FileInputStream {
        return ensureFile().inputStream()
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun listInternal(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun isFile(): Boolean {
        return true
    }

    override suspend fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override suspend fun getDirectorySize(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isHidden(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        TODO("Not yet implemented")
    }
}
