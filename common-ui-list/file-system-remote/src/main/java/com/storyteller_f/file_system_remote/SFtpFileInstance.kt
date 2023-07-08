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
import java.io.File
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
            TODO("Not yet implemented")
        }

    override val fileInputStream: FileInputStream
        get() {
            TODO("Not yet implemented")
        }

    override val fileOutputStream: FileOutputStream
        get() {
            TODO("Not yet implemented")
        }

    override fun listInternal(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>) {
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

    override val isFile: Boolean
        get() {
            val reconnectIfNeed = reconnectIfNeed()
            return reconnectIfNeed.second.type == FileMode.Type.REGULAR
        }

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override val isDirectory: Boolean
        get() {
            val reconnectIfNeed = reconnectIfNeed()
            return reconnectIfNeed.second.type == FileMode.Type.DIRECTORY
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

    override fun changeToParent() {
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

    override fun changeToChild(name: String, policy: FileCreatePolicy) {
        TODO("Not yet implemented")
    }

    override fun changeTo(path: String) {
        TODO("Not yet implemented")
    }

    override val isSymbolicLink: Boolean
        get() {
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