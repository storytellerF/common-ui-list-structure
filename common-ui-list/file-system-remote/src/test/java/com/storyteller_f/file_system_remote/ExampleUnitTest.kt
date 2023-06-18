package com.storyteller_f.file_system_remote

import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.runners.MethodSorters
import org.mockftpserver.fake.FakeFtpServer
import org.mockftpserver.fake.UserAccount
import org.mockftpserver.fake.filesystem.DirectoryEntry
import org.mockftpserver.fake.filesystem.FileEntry
import org.mockftpserver.fake.filesystem.FileSystem
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Ignore("MockFtpServer 老是出错")
class ExampleUnitTest {
    companion object {
        const val username = "user"
        const val password = "password"
        const val server = "localhost"
        const val scheme = "ftp"
    }
    private var fakeFtpServer: FakeFtpServer? = null

    private var ftpInstance: FtpInstance? = null

    @Before
    @Throws(IOException::class)
    fun setup() {
        val fileSystem: FileSystem = UnixFakeFileSystem()
        fileSystem.add(DirectoryEntry("/data"))
        fileSystem.add(FileEntry("/data/foobar.txt", "abcdef 1234567890"))
        fakeFtpServer = FakeFtpServer().apply {
            addUserAccount(UserAccount(username, password, "/data"))
            this.fileSystem = fileSystem
            serverControlPort = 0
            start()
        }
        Thread.sleep(2000)

        ftpInstance = FtpInstance(RemoteSpec(server, fakeFtpServer!!.serverControlPort, username, password, scheme)).apply {
            open()
        }
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        ftpInstance?.close()
        fakeFtpServer?.stop()
    }

    @Test
    @Throws(IOException::class)
    fun testUrlConnectionDownload() {
        val ftpUrl = String.format("$scheme://$username:$password@$server:%d/foobar.txt", fakeFtpServer!!.serverControlPort)
        val urlConnection = URL(ftpUrl).openConnection()
        val inputStream = urlConnection.getInputStream()
        val downloadedFile = File("downloaded_foobar.txt")
        if (downloadedFile.exists()) downloadedFile.delete()
        Files.copy(inputStream, downloadedFile.toPath())
        inputStream.close()
        assert(downloadedFile.exists())
        downloadedFile.delete() // cleanup
    }

    @Test
    @Throws(IOException::class)
    fun testExists() {
        val files = ftpInstance?.listFiles("").orEmpty()
        assert(files.any {
            it.name == "foobar.txt"
        })
    }

    @Test
    @Throws(IOException::class)
    fun testUploadAndDownload() {
        //上传文件
        val localFile = File(javaClass.classLoader?.getResource("baz.txt")?.toURI()!!)
        val path = "/buz.txt"
        ftpInstance!!.putFileToPath(localFile, path)
        fakeFtpServer!!.fileSystem?.listFiles("/")?.forEach {
            println(it)
        }
        Thread.sleep(2000)
        assert(fakeFtpServer!!.fileSystem.exists(path))
        //然后下载
        val downloadFilePath = "downloaded_buz.txt"
        val file = File(downloadFilePath)
        if (file.exists()) file.delete()
        ftpInstance!!.downloadFile(path, downloadFilePath)
        assert(file.exists())
        file.delete() // cleanup
    }

}