package com.storyteller_f.file_system

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import com.storyteller_f.file_system.instance.local.RegularLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.AppLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.FakeLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.getMyId
import java.io.File
import java.util.*

sealed class LocalFileSystemPrefix(val key: String) {

    /**
     * 公共目录，没有权限限制
     */
    object Public : LocalFileSystemPrefix("public")

    /**
     * /sdcard 本身以及所有的子文件
     */
    @SuppressLint("SdCardPath")
    object SdCard : LocalFileSystemPrefix("/sdcard")

    /**
     * /storage/self 本身
     */
    object Self : LocalFileSystemPrefix("/storage/self")

    /**
     * /storage/self/primary 本身以及所有的子文件
     */
    object SelfPrimary : LocalFileSystemPrefix("/storage/self/primary")

    /**
     * /storage/emulated/0 本身以及所有的子文件
     */
    class RootEmulated(uid: Long) : LocalFileSystemPrefix("/storage/emulated/$uid")

    /**
     * /storage/emulated 本身
     */
    object EmulatedRoot : LocalFileSystemPrefix("/storage/emulated")

    /**
     * /storage 本身
     */
    object Storage : LocalFileSystemPrefix("/storage")

    /**
     * 外接存储设备
     */
    class Mounted(key: String) : LocalFileSystemPrefix(key)

    /**
     * app 沙盒目录
     */
    class AppData(key: String) : LocalFileSystemPrefix(key)

    /**
     * 用户安装的app
     */
    object InstalledApps : LocalFileSystemPrefix("/data/app")

    object Root : LocalFileSystemPrefix("/")

    /**
     * /data 本身
     */
    object Data : LocalFileSystemPrefix("/data")

    /**
     * /data/data 本身
     */
    object Data2 : LocalFileSystemPrefix("/data/data")

    /**
     * /data/user
     */
    object DataUser : LocalFileSystemPrefix("/data/user")

    @SuppressLint("SdCardPath")
    class DataRootUser(uid: Long) : LocalFileSystemPrefix("/data/user/$uid")
}

object FileInstanceFactory {
    const val storagePath = "/storage"
    const val emulatedRootPath = "/storage/emulated"

    @SuppressLint("SdCardPath")
    private const val userDataFrontPath = "/data/user/"
    const val userEmulatedFrontPath = "/storage/emulated/"
    const val rootUserEmulatedPath = "/storage/emulated/0"
    const val currentEmulatedPath = "/storage/self"

    private val publicPath = listOf("/system", "/mnt")

    fun getCurrentUserEmulatedPath(context: Context): String {
        return File(emulatedRootPath, context.getMyId().toString()).absolutePath
    }

    /**
     * 除非是根路径，否则不可以是/
     */
    fun getFileInstance(context: Context, uri: Uri): FileInstance {
        val unsafePath = uri.path!!
        assert(!unsafePath.endsWith("/") || unsafePath.length == 1) {
            "invalid path [$unsafePath]"
        }
        val path = simplyPath(unsafePath)
        val scheme = uri.scheme!!
        val safeUri = uri.buildUpon().path(path).build()

        return when (scheme) {
            ContentResolver.SCHEME_CONTENT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val tree = uri.tree
                DocumentLocalFileInstance("/$tree", uri.authority!!, tree, context, safeUri)
            } else {
                TODO("VERSION.SDK_INT < LOLLIPOP")
            }

            else -> getPublicFileSystemInstance(context, safeUri)
        }
    }

    private fun getPublicFileSystemInstance(context: Context, uri: Uri): FileInstance {
        assert(uri.scheme == ContentResolver.SCHEME_FILE) {
            "only permit local system $uri"
        }

        return when (val prefix = getPrefix(context, uri)!!) {
            is LocalFileSystemPrefix.AppData -> RegularLocalFileInstance(context, uri)
            LocalFileSystemPrefix.Data -> FakeLocalFileInstance(context, uri)
            LocalFileSystemPrefix.Data2 -> FakeLocalFileInstance(context, uri)
            is LocalFileSystemPrefix.DataRootUser -> RegularLocalFileInstance(context, uri)
            LocalFileSystemPrefix.DataUser -> FakeLocalFileInstance(context, uri)
            LocalFileSystemPrefix.EmulatedRoot -> FakeLocalFileInstance(context, uri)
            LocalFileSystemPrefix.InstalledApps -> AppLocalFileInstance(context, uri)
            is LocalFileSystemPrefix.Mounted -> when {
                // 外接sd卡
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> RegularLocalFileInstance(
                    context,
                    uri
                )

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> DocumentLocalFileInstance.getMounted(
                    context,
                    uri,
                    prefix.key
                )

                Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1 -> RegularLocalFileInstance(
                    context,
                    uri
                )

                else -> RegularLocalFileInstance(context, uri)
            }

            LocalFileSystemPrefix.Public -> RegularLocalFileInstance(context, uri)
            LocalFileSystemPrefix.Root -> FakeLocalFileInstance(context, uri)
            is LocalFileSystemPrefix.RootEmulated -> when (Build.VERSION.SDK_INT) {
                Build.VERSION_CODES.Q -> DocumentLocalFileInstance.getEmulated(
                    context,
                    uri,
                    prefix.key
                )

                else -> RegularLocalFileInstance(context, uri)
            }

            LocalFileSystemPrefix.SdCard -> if (Build.VERSION_CODES.Q == Build.VERSION.SDK_INT) {
                DocumentLocalFileInstance.getEmulated(context, uri, prefix.key)
            } else {
                RegularLocalFileInstance(context, uri)
            }

            LocalFileSystemPrefix.Self -> when (Build.VERSION.SDK_INT) {
                Build.VERSION_CODES.Q -> {
                    FakeLocalFileInstance(context, uri)
                }

                else -> RegularLocalFileInstance(context, uri)
            }

            LocalFileSystemPrefix.SelfPrimary -> if (Build.VERSION_CODES.Q == Build.VERSION.SDK_INT) {
                DocumentLocalFileInstance.getEmulated(context, uri, prefix.key)
            } else {
                RegularLocalFileInstance(context, uri)
            }

            LocalFileSystemPrefix.Storage -> FakeLocalFileInstance(context, uri)
        }
    }

    /**
     * 如果是根目录，返回空。
     */
    @JvmStatic
    fun getPrefix(
        context: Context,
        uri: Uri,
    ): LocalFileSystemPrefix? {
        val unsafePath = uri.path!!
        assert(!unsafePath.endsWith("/") || unsafePath.length == 1) {
            unsafePath
        }
        val path = simplyPath(unsafePath)
        /**
         * 只有publicFileSystem 才会有prefix 的区别，其他的都不需要。
         */
        return when {
            uri.scheme!! != ContentResolver.SCHEME_FILE -> null
            else -> getPublicFileSystemPrefix(context, path)
        }
    }

    @SuppressLint("SdCardPath")
    private fun getPublicFileSystemPrefix(context: Context, path: String): LocalFileSystemPrefix =
        when {
            publicPath.any { path.startsWith(it) } -> LocalFileSystemPrefix.Public
            path.startsWith(LocalFileSystemPrefix.SdCard.key) -> LocalFileSystemPrefix.SdCard
            path.startsWith(context.appDataDir()) -> LocalFileSystemPrefix.AppData(context.appDataDir())
            path.startsWith(userEmulatedFrontPath) -> LocalFileSystemPrefix.RootEmulated(
                path.substring(
                    userEmulatedFrontPath.length
                ).toLong()
            )

            path == currentEmulatedPath -> LocalFileSystemPrefix.Self
            path.startsWith(currentEmulatedPath) -> LocalFileSystemPrefix.SelfPrimary
            path == LocalFileSystemPrefix.EmulatedRoot.key -> LocalFileSystemPrefix.EmulatedRoot
            path == LocalFileSystemPrefix.Storage.key -> LocalFileSystemPrefix.Storage
            path.startsWith(storagePath) -> LocalFileSystemPrefix.Mounted(extractSdName(path))
            path == LocalFileSystemPrefix.Root.key -> LocalFileSystemPrefix.Root
            path == LocalFileSystemPrefix.Data.key -> LocalFileSystemPrefix.Data
            path.startsWith(LocalFileSystemPrefix.Data2.key) -> LocalFileSystemPrefix.Data2
            path == LocalFileSystemPrefix.DataUser.key -> LocalFileSystemPrefix.DataUser
            path.startsWith(userDataFrontPath) -> LocalFileSystemPrefix.DataRootUser(
                path.substring(
                    userDataFrontPath.length
                ).toLong()
            )

            path.startsWith(LocalFileSystemPrefix.InstalledApps.key) -> LocalFileSystemPrefix.InstalledApps
            else -> throw Exception("unrecognized path")
        }

    /**
     * /storage/XXXX-XXXX 或者是/storage/XXXX-XXXX/test。最终结果应该是/storage/XXXX-XXXX
     */
    private fun extractSdName(path: String): String {
        var endIndex = path.indexOf("/", storagePath.length + 1)
        if (endIndex == -1) endIndex = path.length
        return path.substring(0, endIndex)
    }

    @SuppressLint("SdCardPath")
    private fun Context.appDataDir() = "/data/data/$packageName"

    @Throws(Exception::class)
    suspend fun toChild(
        context: Context,
        fileInstance: FileInstance,
        name: String,
        policy: FileCreatePolicy
    ): FileInstance {
        assert(name.last() != '/') {
            "$name is not a valid name"
        }
        if (name == ".") {
            return fileInstance
        }
        if (name == "..") {
            return toParent(context, fileInstance)
        }
        val path = File(fileInstance.path, name).absolutePath
        val childUri = fileInstance.uri.buildUpon().path(path).build()

        val currentPrefix = getPrefix(context, fileInstance.uri)
        val childPrefix = getPrefix(context, childUri)
        return if (currentPrefix == childPrefix) {
            fileInstance.toChild(name, policy)!!
        } else {
            getFileInstance(context, childUri)
        }
    }

    @Throws(Exception::class)
    suspend fun toParent(
        context: Context,
        fileInstance: FileInstance
    ): FileInstance {
        val parentPath = File(fileInstance.path).parent
        val parentUri = fileInstance.uri.buildUpon().path(parentPath).build()

        val parentPrefix = getPrefix(context, parentUri)
        val childPrefix = getPrefix(context, fileInstance.uri)
        return if (parentPrefix == childPrefix) {
            fileInstance.toParent()
        } else {
            getFileInstance(context, parentUri)
        }
    }

    @JvmStatic
    fun simplyPath(path: String): String {
        assert(path[0] == '/') {
            "$path is not valid"
        }
        val stack = LinkedList<String>()
        var position = 1
        stack.add("/")
        val nameStack = LinkedList<Char>()
        while (position < path.length) {
            val current = path[position++]
            checkPath(current, stack, nameStack)
        }
        val s = nameStack.joinToString("")
        if (s.isNotEmpty()) {
            if (s == "..") {
                if (stack.size > 1) {
                    stack.removeLast()
                    stack.removeLast()
                }
            } else if (s != ".") stack.add(s)
        }
        if (stack.size > 1 && stack.last == "/") stack.removeLast()
        return stack.joinToString("")
    }

    private fun checkPath(
        current: Char,
        stack: LinkedList<String>,
        nameStack: LinkedList<Char>
    ) {
        if (current == '/') {
            if (stack.last != "/" || nameStack.size != 0) {
                val name = nameStack.joinToString("")
                nameStack.clear()
                when (name) {
                    ".." -> {
                        stack.removeLast()
                        stack.removeLast() // 弹出上一个 name
                    }

                    "." -> {
                        // 无效操作
                    }

                    else -> {
                        stack.add(name)
                        stack.add("/")
                    }
                }
            }
        } else {
            nameStack.add(current)
        }
    }
}

val Uri.tree: String
    get() {
        assert(scheme == ContentResolver.SCHEME_CONTENT)
        return pathSegments.first()!!
    }
