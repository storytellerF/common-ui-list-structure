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

    override fun getName(): String {
        return ensureFile().name
    }

    override fun getFile(): FileItemModel {
        TODO("Not yet implemented")
    }

    override fun getDirectory(): DirectoryItemModel {
        TODO("Not yet implemented")
    }

    override fun getFileLength(): Long {
        return ensureFile().length()
    }

    override fun getFileInputStream(): FileInputStream {
        return ensureFile().inputStream()
    }

    override fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override fun listInternal(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    ) {
        TODO("Not yet implemented")
    }

    override fun isFile(): Boolean {
        return true
    }

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun rename(newName: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToParent() {
        TODO("Not yet implemented")
    }

    override fun getDirectorySize(): Long {
        TODO("Not yet implemented")
    }

    override fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isHidden(): Boolean {
        TODO("Not yet implemented")
    }

    override fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toChild(name: String, policy: FileCreatePolicy?): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToChild(name: String, policy: FileCreatePolicy?) {
        TODO("Not yet implemented")
    }

    override fun changeTo(path: String) {
        TODO("Not yet implemented")
    }
}