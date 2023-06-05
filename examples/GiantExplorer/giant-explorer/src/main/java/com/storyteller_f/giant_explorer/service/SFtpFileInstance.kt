package com.storyteller_f.giant_explorer.service

import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.giant_explorer.database.RemoteSpec
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

val sftpChannels = mutableMapOf<RemoteSpec, SFTPClient>()

class SFtpFileInstance(path: String, fileSystemRoot: String, val spec: RemoteSpec) : FileInstance(path, fileSystemRoot) {
    var remoteFile: RemoteFile? = null
    var attribute: FileAttributes? = null

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

    override fun getFile(): FileItemModel {
        TODO("Not yet implemented")
    }

    override fun getDirectory(): DirectoryItemModel {
        TODO("Not yet implemented")
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
        getInstance().ls(path).forEach {
            val attributes = it.attributes
            val isSymLink = attributes.mode.type == FileMode.Type.SYMLINK
            if (it.isDirectory) {
                directoryItems.add(DirectoryItemModel(it.name, it.path, false, attributes.mtime, isSymLink))
            } else {
                fileItems.add(FileItemModel(it.name, it.path, false, attributes.mtime, isSymLink, File(it.path).extension))
            }
        }
    }

    override fun isFile(): Boolean {
        val reconnectIfNeed = reconnectIfNeed()
        return reconnectIfNeed.second.type == FileMode.Type.REGULAR
    }

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isDirectory(): Boolean {
        val reconnectIfNeed = reconnectIfNeed()
        return reconnectIfNeed.second.type == FileMode.Type.DIRECTORY
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

    override fun isSymbolicLink(): Boolean {
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