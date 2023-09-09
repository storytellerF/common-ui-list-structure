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

    override suspend fun listInternal(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    ) {
        val (share, _) = reconnectIfNeed()
        share.list(path).filter {
            it.fileName != "." && it.fileName != ".."
        }.forEach {
            val (file, child) = child(it.fileName)
            val fileInformation = share.getFileInformation(file.absolutePath)
            val lastModifiedTime = fileInformation.basicInformation.changeTime.windowsTimeStamp
            if (fileInformation.standardInformation.isDirectory) {
                directoryItems.add(
                    DirectoryItemModel(
                        it.fileName,
                        child,
                        false,
                        lastModifiedTime,
                        false
                    )
                )
            } else {
                fileItems.add(
                    FileItemModel(
                        it.fileName,
                        child,
                        false,
                        lastModifiedTime,
                        false,
                        file.extension
                    )
                )
            }
        }
    }

    override suspend fun isFile(): Boolean {
        return !isDirectory()
    }

    override suspend fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isDirectory(): Boolean {
        val reconnectIfNeed = reconnectIfNeed()
        return reconnectIfNeed.second.standardInformation.isDirectory
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
