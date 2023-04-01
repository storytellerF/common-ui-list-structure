package com.storyteller_f.file_system

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.StatFs
import androidx.annotation.WorkerThread
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.RootAccessFileInstance
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import com.storyteller_f.file_system.instance.local.RegularLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.EmulatedLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.FakeDirectoryLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.StorageLocalFileInstance
import com.storyteller_f.multi_core.StoppableTask
import java.util.*

object FileInstanceFactory {
    const val rootUserEmulatedPath = "/storage/emulated/0"
    const val emulatedRootPath = "/storage/emulated"
    private const val currentEmulatedPath = "/storage/self"
    const val storagePath = "/storage"

    @SuppressLint("SdCardPath")
    private const val sdcardPath = "/sdcard"
    const val publicFileSystemRoot = "/"
    const val rootFileSystemRoot = "root"

    private val publicPath = listOf("/system", "/mnt")

    /**
     * @param root 如果是普通文件系统，是/。如果是document provider，是authority。如果有root 权限，是root
     */
    @WorkerThread
    fun getFileInstance(unsafePath: String, context: Context, root: String = publicFileSystemRoot, stoppableTask: StoppableTask): FileInstance {
        assert(!unsafePath.endsWith("/") || unsafePath.length == 1) {
            "invalid path [$unsafePath]"
        }
        val path = simplyPath(unsafePath, stoppableTask)
        val prefix = getPrefix(path, context, root, stoppableTask)
        val remote = FileSystemUriSaver.getInstance().remote

        return when {
            root == rootFileSystemRoot && remote != null -> RootAccessFileInstance(path, remote)
            root != publicFileSystemRoot -> DocumentLocalFileInstance(context, path, root, root)
            else -> getPublicFileSystemInstance(prefix, context, path, root)
        }
    }

    private fun getPublicFileSystemInstance(prefix: String, context: Context, path: String, root: String): FileInstance {
        return when {
            prefix == "" -> RegularLocalFileInstance(context, path, root)
            prefix == context.appDataDir() -> RegularLocalFileInstance(context, path, root)
            prefix == sdcardPath -> RegularLocalFileInstance(context, path, root)
            prefix == storagePath -> StorageLocalFileInstance(context)
            prefix == emulatedRootPath -> EmulatedLocalFileInstance(context)
            prefix == currentEmulatedPath -> RegularLocalFileInstance(context, path, root)
            prefix == rootUserEmulatedPath -> when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R && Build.VERSION.SDK_INT > Build.VERSION_CODES.P -> DocumentLocalFileInstance.getEmulated(context, path)
                else -> RegularLocalFileInstance(context, path, root)
            }

            path.startsWith(storagePath) -> when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> RegularLocalFileInstance(context, path, root)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> DocumentLocalFileInstance.getMounted(context, path)
                Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1 -> RegularLocalFileInstance(context, path, root)
                else -> RegularLocalFileInstance(context, path, root)
            }

            prefix == "fake" -> FakeDirectoryLocalFileInstance(path, context)
            else -> throw Exception("无法识别 $path $prefix $root")
        }
    }

    /**
     * 如果是根目录，返回空。
     */
    @JvmStatic
    fun getPrefix(unsafePath: String, context: Context, root: String = publicFileSystemRoot, stoppableTask: StoppableTask): String {
        assert(!unsafePath.endsWith("/") || unsafePath.length == 1) {
            unsafePath
        }
        val path = simplyPath(unsafePath, stoppableTask)
        /**
         * 只有publicFileSystem 才会有prefix 的区别，其他的都不需要。
         */
        return when {
            root != publicFileSystemRoot -> ""
            else -> getPublicFileSystemPrefix(path, context)
        }
    }

    private fun getPublicFileSystemPrefix(path: String, context: Context): String {
        return when {
            publicPath.any { path.startsWith(it) } -> ""
            path.startsWith(sdcardPath) -> sdcardPath
            path.startsWith(context.appDataDir()) -> context.appDataDir()
            path == storagePath -> storagePath
            path.startsWith(rootUserEmulatedPath) -> rootUserEmulatedPath
            path.startsWith(emulatedRootPath) -> emulatedRootPath
            path.startsWith(currentEmulatedPath) -> currentEmulatedPath
            path.startsWith(storagePath) -> {
                var endIndex = path.indexOf("/", storagePath.length)
                if (endIndex == -1) endIndex = path.length
                path.substring(0, endIndex + 1)
            }

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
    fun toChild(fileInstance: FileInstance, name: String, isFile: Boolean, context: Context, createWhenNoExists: Boolean, stoppableTask: StoppableTask): FileInstance {
        assert(name.last() != '/') {
            "$name is not a valid name"
        }
        if (name == ".") {
            return fileInstance
        }
        if (name == "..") {
            return toParent(fileInstance, context, stoppableTask = stoppableTask)
        }
        val parentPath = fileInstance.path
        val parentPrefix = getPrefix(parentPath, context, stoppableTask = stoppableTask)
        val unsafePath = "$parentPath/$name"
        val path = simplyPath(unsafePath, stoppableTask)
        val childPrefix = getPrefix(path, context, stoppableTask = stoppableTask)
        return if (parentPrefix == childPrefix) {
            fileInstance.toChild(name, isFile, createWhenNoExists)
        } else {
            getFileInstance(path, context, stoppableTask = stoppableTask)
        }
    }

    @Throws(Exception::class)
    fun toParent(fileInstance: FileInstance, context: Context, stoppableTask: StoppableTask): FileInstance {
        val parentPrefix = getPrefix(fileInstance.parent, context, stoppableTask = stoppableTask)
        val childPrefix = getPrefix(fileInstance.path, context, stoppableTask = stoppableTask)
        return if (parentPrefix == childPrefix) {
            fileInstance.toParent()
        } else {
            getFileInstance(fileInstance.parent, context, stoppableTask = stoppableTask)
        }
    }

    @JvmStatic
    fun simplyPath(path: String, stoppableTask: StoppableTask): String {
        assert(path[0] == '/') {
            "$path is not valid"
        }
        val stack = LinkedList<String>()
        var position = 1
        stack.add("/")
        val nameStack = LinkedList<Char>()
        while (position < path.length) {
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