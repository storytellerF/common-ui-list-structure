package com.storyteller_f.file_system_remote

import android.net.Uri
import android.util.Log
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.util.permissions
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClientConfig
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter

val ftpsClients = mutableMapOf<RemoteSpec, FtpsInstance>()

class FtpsFileInstance(private val spec: RemoteSpec, uri: Uri) : FileInstance(uri) {
    private var ftpFile: FTPFile? = null

    companion object {
        private const val TAG = "FtpInstance"
    }

    init {
        initCurrentFile()
    }

    private fun initCurrentFile(): FTPFile? {
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

    private fun getInstance(): FtpsInstance? {
        val ftpInstance = ftpsClients.getOrPut(spec) {
            FtpsInstance(spec)
        }
        if (ftpInstance.connectIfNeed()) {
            return ftpInstance
        }
        return null
    }

    override fun getFile(): FileItemModel {
        TODO("Not yet implemented")
    }

    override fun getDirectory(): DirectoryItemModel {
        TODO("Not yet implemented")
    }

    override fun getFileLength(): Long {
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
            val (file, child) = child(name)
            val lastModifiedTime = it.timestamp.timeInMillis
            val permission = it.getPermissions()
            if (it.isFile) {
                fileItems.add(FileItemModel(name, child, false, lastModifiedTime, it.isSymbolicLink, file.extension).apply {
                    permissions = permission
                })
            } else if (it.isDirectory) {
                directoryItems.add(DirectoryItemModel(name, child, false, lastModifiedTime, it.isSymbolicLink).apply {
                    permissions = permission
                })
            }
        }
    }

    private fun FTPFile.getPermissions(): String {
        val canRead = hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION)
        val canWrite = hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION)
        val canExecute = hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION)
        return permissions(canRead, canWrite, canExecute, isFile)
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

class FtpsInstance(private val spec: RemoteSpec) {
    private val ftps: FTPSClient = FTPSClient(spec.type == RemoteAccessType.ftps).apply {
        addProtocolCommandListener(PrintCommandListener(PrintWriter(System.out)))
        val ftpClientConfig = FTPClientConfig()
        configure(ftpClientConfig)
    }

    @Throws(IOException::class)
    fun open(): Boolean {
        connect()
        val login = ftps.login(spec.user, spec.password)
        ftps.enterLocalPassiveMode()
        return login
    }

    private fun connect() {
        if (ftps.isConnected) return
        ftps.connect(spec.server, spec.port)
        val reply = ftps.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftps.disconnect()
            throw IOException("Exception in connecting to FTP Server")
        }
    }

    fun get(path: String?): FTPFile? {
        return ftps.mlistFile(path)
    }

    @Throws(IOException::class)
    fun close() {
        ftps.disconnect()
    }

    @Throws(IOException::class)
    fun listFiles(path: String?): Array<out FTPFile>? {
        return ftps.listFiles(path)
    }

    @Throws(IOException::class)
    fun downloadFile(source: String?, destination: String?) {
        val out = FileOutputStream(destination)
        ftps.retrieveFile(source, out)
    }

    @Throws(IOException::class)
    fun putFileToPath(file: File?, path: String?) {
        ftps.storeFile(path, FileInputStream(file))
    }

    fun connectIfNeed(): Boolean {
        return if (ftps.isAvailable) true
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