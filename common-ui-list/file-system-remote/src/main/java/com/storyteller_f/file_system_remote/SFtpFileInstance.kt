package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.FileInputStream
import java.io.FileOutputStream

val sftpChannels = mutableMapOf<RemoteSpec, SFTPClient>()

class SFtpFileInstance(private val spec: RemoteSpec, uri: Uri) : FileInstance(uri) {
    private var remoteFile: RemoteFile? = null
    private var attribute: FileAttributes? = null

    private fun initCurrent(): Pair<RemoteFile, FileAttributes> {
        val orPut = getInstance()
        val open = orPut.open(path)
        val fetchAttributes = open.fetchAttributes()
        remoteFile = open
        attribute = fetchAttributes
        return open to fetchAttributes
    }

    private fun getInstance(): SFTPClient {
        val orPut = sftpChannels.getOrPut(spec) {
            spec.sftpClient()
        }
        return orPut
    }

    private fun reconnectIfNeed(): Pair<RemoteFile, FileAttributes> {
        var c = remoteFile
        var attributes = attribute
        if (c == null || attributes == null) {
            val initCurrent = initCurrent()
            c = initCurrent.first
            attributes= initCurrent.second
        }
        return c to attributes
    }

    override suspend fun getFile(): FileItemModel {
        TODO("Not yet implemented")
    }

    override suspend fun getDirectory(): DirectoryItemModel {
        TODO("Not yet implemented")
    }

    override suspend fun getFileLength(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun listInternal(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>) {
        getInstance().ls(path).forEach {
            val attributes = it.attributes
            val (file, child) = child(it.name)
            val isSymLink = attributes.mode.type == FileMode.Type.SYMLINK
            if (it.isDirectory) {
                directoryItems.add(DirectoryItemModel(it.name, child, false, attributes.mtime, isSymLink))
            } else {
                fileItems.add(FileItemModel(it.name, child, false, attributes.mtime, isSymLink, file.extension))
            }
        }
    }

    override suspend fun isFile(): Boolean {
        val reconnectIfNeed = reconnectIfNeed()
        return reconnectIfNeed.second.type == FileMode.Type.REGULAR
    }

    override suspend fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isDirectory(): Boolean {
        val reconnectIfNeed = reconnectIfNeed()
        return reconnectIfNeed.second.type == FileMode.Type.DIRECTORY
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

    override suspend fun isSymbolicLink(): Boolean {
        val reconnectIfNeed = reconnectIfNeed()
        return reconnectIfNeed.second.type == FileMode.Type.SYMLINK
    }
}

fun RemoteSpec.sftpClient(): SFTPClient {
    val sshClient = SSHClient()
    sshClient.addHostKeyVerifier(PromiscuousVerifier())
    sshClient.connect(server, port)
    sshClient.authPassword(user, password)
    return sshClient.newSFTPClient()
}

fun RemoteSpec.checkSftp() {
    sftpClient()
}