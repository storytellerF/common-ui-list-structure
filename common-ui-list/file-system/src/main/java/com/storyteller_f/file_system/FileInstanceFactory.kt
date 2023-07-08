package com.storyteller_f.file_system

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.StatFs
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import com.storyteller_f.file_system.instance.local.RegularLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.AppLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.FakeLocalFileInstance
import com.storyteller_f.multi_core.StoppableTask
import java.io.File
import java.util.*

object FileInstanceFactory {
    const val storagePath = "/storage"
    const val emulatedRootPath = "/storage/emulated"

    const val rootUserEmulatedPath = "/storage/emulated/0"
    const val currentEmulatedPath = "/storage/self"

    @SuppressLint("SdCardPath")
    private const val sdcardPath = "/sdcard"

    private val publicPath = listOf("/system", "/mnt")

    /**
     * 除非是根路径，否则不可以是/
     */
    fun getFileInstance(context: Context, uri: Uri, stoppableTask: StoppableTask): FileInstance {
        val unsafePath = uri.path!!
        assert(!unsafePath.endsWith("/") || unsafePath.length == 1) {
            "invalid path [$unsafePath]"
        }
        val path = simplyPath(unsafePath, stoppableTask)
        val scheme = uri.scheme!!
        val safeUri = uri.buildUpon().path(path).build()

        return when (scheme) {
            ContentResolver.SCHEME_CONTENT -> DocumentLocalFileInstance("", uri.authority!!, context, safeUri)
            else -> getPublicFileSystemInstance(context, safeUri)
        }
    }

    private fun getPublicFileSystemInstance(context: Context, uri: Uri): FileInstance {
        assert(uri.scheme == ContentResolver.SCHEME_FILE) {
            "only permit local system $uri"
        }
        val prefix = getPrefix(context, uri)

        return when {
            prefix == "" || prefix == sdcardPath || prefix == context.appDataDir() ->
                RegularLocalFileInstance(context, uri)

            prefix == currentEmulatedPath -> when (Build.VERSION.SDK_INT) {
                Build.VERSION_CODES.Q -> {
                    FakeLocalFileInstance(context, uri)
                }
                else -> RegularLocalFileInstance(context, uri)
            }

            prefix == rootUserEmulatedPath -> when (Build.VERSION.SDK_INT) {
                Build.VERSION_CODES.Q ->
                    DocumentLocalFileInstance.getEmulated(context, uri, prefix)
                else -> RegularLocalFileInstance(context, uri)
            }

            prefix.startsWith(storagePath) -> when {
                //外接sd卡
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> RegularLocalFileInstance(context, uri)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> DocumentLocalFileInstance.getMounted(context, uri, prefix)
                Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1 -> RegularLocalFileInstance(context, uri)
                else -> RegularLocalFileInstance(context, uri)
            }

            prefix == "fake" -> FakeLocalFileInstance(context, uri)
            prefix == "fake-app" -> AppLocalFileInstance(context, uri)
            else -> throw Exception("无法识别 $uri $prefix")
        }
    }

    /**
     * 如果是根目录，返回空。
     */
    @JvmStatic
    fun getPrefix(
        context: Context,
        uri: Uri,
        stoppableTask: StoppableTask? = StoppableTask.Blocking
    ): String {
        val unsafePath = uri.path!!
        assert(!unsafePath.endsWith("/") || unsafePath.length == 1) {
            unsafePath
        }
        val path = simplyPath(unsafePath, stoppableTask)
        /**
         * 只有publicFileSystem 才会有prefix 的区别，其他的都不需要。
         */
        return when {
            uri.scheme!! != ContentResolver.SCHEME_FILE -> ""
            else -> getPublicFileSystemPrefix(context, path)
        }
    }

    private fun getPublicFileSystemPrefix(context: Context, path: String): String {
        return when {
            publicPath.any { path.startsWith(it) } -> ""
            path.startsWith(sdcardPath) -> sdcardPath
            path.startsWith(context.appDataDir()) -> context.appDataDir()
            path.startsWith(rootUserEmulatedPath) -> rootUserEmulatedPath
            path.startsWith(currentEmulatedPath) -> currentEmulatedPath
            path == emulatedRootPath || path == storagePath -> "fake"
            path.startsWith(storagePath) -> {
                // /storage/XXXX-XXXX 或者是/storage/XXXX-XXXX/test。最终结果应该是/storage/XXXX-XXXX
                var endIndex = path.indexOf("/", storagePath.length + 1)
                if (endIndex == -1) endIndex = path.length
                path.substring(0, endIndex)
            }

            path.startsWith("/data/app/") -> "fake-app"
            else -> "fake"
        }
    }

    @SuppressLint("SdCardPath")
    private fun Context.appDataDir() = "/data/data/$packageName"

    @Suppress("DEPRECATION")
    fun getSpace(prefix: String?): Long {
        val stat = StatFs(prefix)
        val blockSize: Long
        val availableBlocks: Long
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.blockSizeLong
            availableBlocks = stat.availableBlocksLong
        } else {
            blockSize = stat.blockSize.toLong()
            availableBlocks = stat.availableBlocks.toLong()
        }
        return blockSize * availableBlocks
    }

    @Throws(Exception::class)
    fun toChild(
        context: Context,
        fileInstance: FileInstance,
        name: String,
        policy: FileCreatePolicy,
        stoppableTask: StoppableTask
    ): FileInstance {
        assert(name.last() != '/') {
            "$name is not a valid name"
        }
        if (name == ".") {
            return fileInstance
        }
        if (name == "..") {
            return toParent(context, fileInstance, stoppableTask = stoppableTask)
        }
        val path = File(fileInstance.path, name).absolutePath
        val childUri = fileInstance.uri.buildUpon().path(path).build()

        val currentPrefix = getPrefix(context, fileInstance.uri, stoppableTask = stoppableTask)
        val childPrefix = getPrefix(context, childUri, stoppableTask = stoppableTask)
        return if (currentPrefix == childPrefix) {
            fileInstance.toChild(name, policy)!!
        } else {
            getFileInstance(context, childUri, stoppableTask = stoppableTask)
        }
    }

    @Throws(Exception::class)
    fun toParent(
        context: Context,
        fileInstance: FileInstance,
        stoppableTask: StoppableTask
    ): FileInstance {
        val parentPath = File(fileInstance.path).parent
        val parentUri = fileInstance.uri.buildUpon().path(parentPath).build()

        val parentPrefix = getPrefix(context, parentUri, stoppableTask = stoppableTask)
        val childPrefix = getPrefix(context, fileInstance.uri, stoppableTask = stoppableTask)
        return if (parentPrefix == childPrefix) {
            fileInstance.toParent()
        } else {
            getFileInstance(context, parentUri, stoppableTask = stoppableTask)
        }
    }

    @JvmStatic
    fun simplyPath(path: String, stoppableTask: StoppableTask?): String {
        assert(path[0] == '/') {
            "$path is not valid"
        }
        val stack = LinkedList<String>()
        var position = 1
        stack.add("/")
        val nameStack = LinkedList<Char>()
        while (position < path.length) {
            if (stoppableTask?.needStop() == true) break
            val current = path[position++]
            if (current == '/') {
                if (stack.last != "/" || nameStack.size != 0) {
                    val name = nameStack.joinToString("")
                    nameStack.clear()
                    when (name) {
                        ".." -> {
                            stack.removeLast()
                            stack.removeLast()//弹出上一个 name
                        }

                        "." -> {
                            //无效操作
                        }

                        else -> {
                            stack.add(name)
                            stack.add("/")
                        }
                    }
                }
            } else nameStack.add(current)
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
}