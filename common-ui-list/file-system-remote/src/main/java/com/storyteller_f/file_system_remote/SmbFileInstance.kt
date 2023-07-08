package com.storyteller_f.file_system_remote

import android.net.Uri
import com.hierynomus.msfscc.fileinformation.FileAllInformation
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

val smbClient by lazy {
    SMBClient()
}

fun ShareSpec.requireDiskShare(): DiskShare {
    val connect = smbClient.connect(server, port)
    val authenticationContext = AuthenticationContext(user, password.toCharArray(), "")
    val session = connect.authenticate(authenticationContext)
    return session.connectShare(share) as DiskShare
}

fun ShareSpec.checkSmb() {
    requireDiskShare().close()
}

val smbSessions = mutableMapOf<ShareSpec, DiskShare>()

class SmbFileInstance(private val shareSpec: ShareSpec, uri: Uri) : FileInstance(uri) {
    private var information: FileAllInformation? = null
    private var share: DiskShare? = null

    private fun initCurrentFile(): Pair<DiskShare, FileAllInformation> {
        val connectShare = getDiskShare()
        share = connectShare
        val fileInformation = connectShare.getFileInformation(path)
        information = fileInformation
        return connectShare to fileInformation
    }

    private fun getDiskShare(): DiskShare {
        val orPut = smbSessions.getOrPut(shareSpec) {
            shareSpec.requireDiskShare()
        }
        return orPut
    }

    private fun reconnectIfNeed(): Pair<DiskShare, FileAllInformation> {
        var information = information
        var share = share
        if (information == null || share == null) {
            val initCurrentFile = initCurrentFile()
            share = initCurrentFile.first
            information = initCurrentFile.second
        }
        return share to information
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
        val (share, _) = reconnectIfNeed()
        share.list(path).filter {
            it.fileName != "." && it.fileName != ".."
        }.forEach {
            val (file, child) = child(it.fileName)
            val fileInformation = share.getFileInformation(file.absolutePath)
            val lastModifiedTime = fileInformation.basicInformation.changeTime.windowsTimeStamp
            if (fileInformation.standardInformation.isDirectory) {
                directoryItems.add(DirectoryItemModel(it.fileName, child, false, lastModifiedTime, false))
            } else {
                fileItems.add(FileItemModel(it.fileName, child, false, lastModifiedTime, false, file.extension))
            }
        }
    }

    override val isFile: Boolean
        get() {
            return !isDirectory
        }

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override val isDirectory: Boolean
        get() {
            val reconnectIfNeed = reconnectIfNeed()
            return reconnectIfNeed.second.standardInformation.isDirectory
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
}
