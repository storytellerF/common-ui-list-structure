package com.storyteller_f.giant_explorer.service

import com.hierynomus.msfscc.fileinformation.FileAllInformation
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.giant_explorer.database.SmbSpec
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

val smbClient by lazy {
    SMBClient()
}

fun SmbSpec.requireDiskShare(): DiskShare {
    val connect = smbClient.connect(server, port)
    val authenticationContext = AuthenticationContext(user, password.toCharArray(), "")
    val session = connect.authenticate(authenticationContext)
    return session.connectShare(share) as DiskShare
}

val smbSessions = mutableMapOf<SmbSpec, DiskShare>()

class SmbFileInstance(path: String, fileSystemRoot: String, val smbSpec: SmbSpec) : FileInstance(path, fileSystemRoot) {
    var information: FileAllInformation? = null
    var share: DiskShare? = null

    init {
        initCurrentFile()
    }

    private fun initCurrentFile(): Pair<DiskShare, FileAllInformation> {
        val connectShare = getDiskShare()
        share = connectShare
        val fileInformation = connectShare.getFileInformation(path)
        information = fileInformation
        return connectShare to fileInformation
    }

    private fun getDiskShare(): DiskShare {
        val orPut = smbSessions.getOrPut(smbSpec) {
            smbSpec.requireDiskShare()
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
        val (share, _) = reconnectIfNeed()
        share.list(path).filter {
            it.fileName != "." && it.fileName != ".."
        }.forEach {
            val child = File(path, it.fileName)
            val fileInformation = share.getFileInformation(child.absolutePath)
            val lastModifiedTime = fileInformation.basicInformation.changeTime.windowsTimeStamp
            if (fileInformation.standardInformation.isDirectory) {
                directoryItems.add(DirectoryItemModel(it.fileName, child.absolutePath, false, lastModifiedTime, false))
            } else {
                fileItems.add(FileItemModel(it.fileName, child.absolutePath, false, lastModifiedTime, false, file.extension))
            }
        }
    }

    override fun isFile(): Boolean {
        return !isDirectory
    }

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isDirectory(): Boolean {
        val reconnectIfNeed = reconnectIfNeed()
        return reconnectIfNeed.second.standardInformation.isDirectory
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
