package com.storyteller_f.giant_explorer.service

import android.net.Uri
import android.util.Log
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.util.permissions
import com.storyteller_f.giant_explorer.control.remote.RemoteAccessType
import com.storyteller_f.giant_explorer.database.RemoteAccessSpec
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter

val ftpClients = mutableMapOf<FtpSpec, FtpInstance>()

class FtpFileInstance(path: String, fileSystemRoot: String, private val spec: FtpSpec) : FileInstance(path, fileSystemRoot) {
    var ftpFile: FTPFile? = null

    companion object {
        private const val TAG = "FtpInstance"
    }

    init {
        initCurrentFile()
    }

    fun initCurrentFile(): FTPFile? {
        val ftpInstance = getInstance()
        return try {
            val get = ftpInstance?.get(path)
            ftpFile = get
            get
        } catch (e: Exception) {
            Log.e(TAG, "initCurrentFile: ", e)
            null
        }
    }

    private fun getInstance(): FtpInstance? {
        val ftpInstance = ftpClients.getOrPut(spec) {
            FtpInstance(spec)
        }
        if (ftpInstance.connectIfNeed()) {
            return ftpInstance
        }
        return null
    }

    override fun getBufferedReader(): BufferedReader {
        TODO("Not yet implemented")
    }

    override fun getBufferedWriter(): BufferedWriter {
        TODO("Not yet implemented")
    }

    override fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override fun listInternal(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>) {

        val listFiles = getInstance()?.listFiles(path)
        listFiles?.forEach {
            val name = it.name
            val path = File(path, name).absolutePath
            val lastModifiedTime = it.timestamp.timeInMillis
            val canRead = it.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION)
            val canWrite = it.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION)
            val canExecute = it.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION)
            val permission = permissions(canRead, canWrite, canExecute, it.isFile)
            if (it.isFile) {
                fileItems.add(FileItemModel(name, path, false, lastModifiedTime, it.isSymbolicLink, File(path).extension).apply {
                    permissions = permission
                })
            } else {
                directoryItems.add(DirectoryItemModel(name, path, false, lastModifiedTime, it.isSymbolicLink).apply {
                    permissions = permission
                })
            }
        }
    }

    override fun isFile(): Boolean {
        val current = reconnectIfNeed()
        return current?.isFile == true
    }

    private fun reconnectIfNeed(): FTPFile? {
        var current = ftpFile
        if (current == null) {
            current = initCurrentFile()
        }
        return current
    }

    override fun exists(): Boolean {
        return reconnectIfNeed() != null
    }

    override fun isDirectory(): Boolean {
        return reconnectIfNeed()?.isDirectory == true
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

    override fun toChild(name: String, isFile: Boolean, createWhenNotExists: Boolean): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToChild(name: String, isFile: Boolean, createWhenNotExists: Boolean) {
        TODO("Not yet implemented")
    }

    override fun changeTo(path: String) {
        TODO("Not yet implemented")
    }

}

data class FtpSpec(val server: String, val port: Int, val user: String, val password: String, val isSftp: Boolean) {
    fun toUri(): String {
        val scheme = if (isSftp) {
            "sftp"
        } else "ftp"
        return "$scheme://$user:$password@$server:$port/"
    }

    fun toRemote(): RemoteAccessSpec {
        return RemoteAccessSpec(server, port, user, password, type = if (isSftp) RemoteAccessType.sftp else RemoteAccessType.ftp)
    }

    companion object {
        fun parse(url: String): FtpSpec {
            val parse = Uri.parse(url)
            val authority = parse.authority!!
            val split = authority.split("@")
            val (user, pass) = split.first().split(":")
            val (loc, port) = split.last().split(":")
            return FtpSpec(loc, port.toInt(), user, pass, url.startsWith("s"))
        }

    }
}

class FtpInstance(private val spec: FtpSpec) {
    private val ftp: FTPClient = FTPClient().apply {
        addProtocolCommandListener(PrintCommandListener(PrintWriter(System.out)))
    }

    @Throws(IOException::class)
    fun open(): Boolean {
        ftp.connect(spec.server, spec.port)
        val reply = ftp.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect()
            throw IOException("Exception in connecting to FTP Server")
        }
        return ftp.login(spec.user, spec.password)
    }

    fun get(path: String?): FTPFile? {
        return ftp.mlistFile(path)
    }

    @Throws(IOException::class)
    fun close() {
        ftp.disconnect()
    }

    @Throws(IOException::class)
    fun listFiles(path: String?): Array<out FTPFile>? {
        return ftp.listFiles(path)
    }

    @Throws(IOException::class)
    fun downloadFile(source: String?, destination: String?) {
        val out = FileOutputStream(destination)
        ftp.retrieveFile(source, out)
    }

    @Throws(IOException::class)
    fun putFileToPath(file: File?, path: String?) {
        ftp.storeFile(path, FileInputStream(file))
    }

    fun connectIfNeed(): Boolean {
        return if (ftp.isConnected) true
        else try {
            open()
        } catch (e: Exception) {
            Log.e(TAG, "connectIfNeed: ", e)
            false
        }
    }

    companion object {
        private const val TAG = "FtpInstance"
    }
}