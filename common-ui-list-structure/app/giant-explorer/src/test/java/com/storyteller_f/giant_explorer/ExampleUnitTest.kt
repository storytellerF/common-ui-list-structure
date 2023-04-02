package com.storyteller_f.giant_explorer

import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.giant_explorer.service.FtpInstance
import com.storyteller_f.giant_explorer.service.FtpSpec
import com.storyteller_f.multi_core.StoppableTask
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockftpserver.fake.FakeFtpServer
import org.mockftpserver.fake.UserAccount
import org.mockftpserver.fake.filesystem.DirectoryEntry
import org.mockftpserver.fake.filesystem.FileEntry
import org.mockftpserver.fake.filesystem.FileSystem
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Files

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    private var fakeFtpServer: FakeFtpServer? = null

    private var ftpInstance: FtpInstance? = null

    @Before
    @Throws(IOException::class)
    fun setup() {
        val fileSystem: FileSystem = UnixFakeFileSystem()
        fileSystem.add(DirectoryEntry("/data"))
        fileSystem.add(FileEntry("/data/foobar.txt", "abcdef 1234567890"))
        fakeFtpServer = FakeFtpServer().apply {
            addUserAccount(UserAccount("user", "password", "/data"))
            this.fileSystem = fileSystem
            serverControlPort = 0
            start()
        }

        ftpInstance = FtpInstance(FtpSpec("localhost", fakeFtpServer!!.serverControlPort, "user", "password")).apply {
            open()
        }
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        ftpInstance?.close()
        fakeFtpServer!!.stop()
    }

    @Test
    fun addition_isCorrect() {
        val simplyPath = FileInstanceFactory.simplyPath(FileInstanceFactory.rootUserEmulatedPath, StoppableTask.Blocking)
        assert(simplyPath == FileInstanceFactory.rootUserEmulatedPath)
    }

    @Test
    @Throws(IOException::class)
    fun givenRemoteFile_whenDownloading_thenItIsOnTheLocalFilesystem() {
        val ftpUrl = String.format(
            "ftp://user:password@localhost:%d/foobar.txt", fakeFtpServer?.serverControlPort
        )
        val urlConnection = URL(ftpUrl).openConnection()
        val inputStream = urlConnection.getInputStream()
        Files.copy(inputStream, File("downloaded_buz.txt").toPath())
        inputStream.close()
        assert(File("downloaded_buz.txt").exists())
        File("downloaded_buz.txt").delete() // cleanup
    }

    @Test
    @Throws(IOException::class)
    fun givenRemoteFile_whenListingRemoteFiles_thenItIsContainedInList() {
        val files = ftpInstance?.listFiles("").orEmpty()
        assert(files.any {
            it.name == "foobar.txt"
        })
    }

    @Test
    @Throws(IOException::class)
    fun givenRemoteFile_whenDownloading_thenItIsOnTheLocalFilesystem1() {
        ftpInstance?.downloadFile("/buz.txt", "downloaded_buz.txt")
        assert(File("downloaded_buz.txt").exists())
        File("downloaded_buz.txt").delete() // cleanup
    }

    @Test
    @Throws(URISyntaxException::class, IOException::class)
    fun givenLocalFile_whenUploadingIt_thenItExistsOnRemoteLocation() {
        val file = File(javaClass.classLoader?.getResource("baz.txt")?.toURI())
        ftpInstance?.putFileToPath(file, "/buz.txt")
        assert(fakeFtpServer!!.fileSystem.exists("/buz.txt"))
    }
}