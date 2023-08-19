package com.storyteller_f.file_system.instance.local

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.storyteller_f.file_system.FileSystemUriSaver
import com.storyteller_f.file_system.instance.BaseContextFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileCreatePolicy.*
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.util.FileInstanceUtility
import com.storyteller_f.file_system.util.FileUtility
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

/**
 * @param prefix 用来标识对象所在区域，可能是外部，也可能是内部。比如/storage/XXXX-XXXX
 * @param preferenceKey 一般是authority，用于获取document provider 的uri
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class DocumentLocalFileInstance(private val prefix: String, private val preferenceKey: String, context: Context, uri: Uri, initCurrent: Boolean = true) : BaseContextFileInstance(context, uri) {
    private var current: DocumentFile? = null

    /**
     * 有些document provider 的uri 对应的路径不是【/】
     */
    private val pathRelativeRoot: String by lazy {
        if (path == prefix) {
            "/"
        } else {
            path.substring(prefix.length)
        }.apply {
            assert(startsWith("/"))
        }
    }

    init {
        assert(path.startsWith(prefix))
        if (initCurrent) {
            current = documentFileFromUri()
        }
    }

    private fun documentFileFromUri(): DocumentFile? = getDocumentFile(NotCreate)

    /**
     * 获取指定目录的document file
     *
     * @return 返回目标文件
     */
    private fun getDocumentFile(policy: FileCreatePolicy): DocumentFile? {
        //此uri 是当前前缀下的根目录uri。fileInstance 的uri 是fileSystem 使用的uri。
        val rootUri = FileSystemUriSaver.instance.savedUri(context, preferenceKey)
        if (rootUri == null) {
            Log.e(TAG, "getDocumentFile: rootUri is null")
            return null
        }
        val rootFile = DocumentFile.fromTreeUri(context, rootUri)
        if (rootFile == null) {
            Log.e(TAG, "getDocumentFile: fromTreeUri is null")
            return null
        }
        if (!rootFile.canRead()) {
            Log.e(TAG, "getDocumentFile: 权限过期, 不可读写 $path prefix: $prefix")
            return null
        }
        if (pathRelativeRoot == "/") return rootFile
        val nameItemPath = pathRelativeRoot.substring(1).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val endElementIsFileName = policy is Create && policy.isFile
        val paths = if (endElementIsFileName) {
            nameItemPath.copyOfRange(0, nameItemPath.size - 1)
        } else nameItemPath
        var temp: DocumentFile = rootFile
        for (name in paths) {
            if (needStop()) break
            val foundFile = temp.findFile(name)
            temp = if (foundFile == null) {
                if (policy is NotCreate) {
                    Log.e(TAG, "getDocumentFile: 文件找不到$path prefix: $prefix")
                    return null
                }
                val created = temp.createDirectory(name)
                if (created == null) {
                    Log.e(TAG, "getDocumentFile: 文件创建失败$path prefix: $prefix")
                    return null
                } else created
            } else foundFile
        }

        //find file
        if (endElementIsFileName) {
            val fileName = nameItemPath[nameItemPath.size - 1]
            val file = temp.findFile(fileName)
            return file ?: temp.createFile("*/*", fileName)
        }
        return temp
    }

    override val directorySize: Long
        get() = getDocumentFileSize(current)

    private fun getDocumentFileSize(documentFile: DocumentFile?): Long {
        var size: Long = 0
        val documentFiles = documentFile!!.listFiles()
        for (documentFi in documentFiles) {
            if (needStop()) break
            size += if (documentFile.isFile) {
                documentFi.length()
            } else {
                getDocumentFileSize(documentFi)
            }
        }
        return size
    }

    override val parent: String?
        get() {
            val parentFile = current!!.parentFile ?: return null
            return parentFile.uri.path
        }

    override fun createDirectory(): Boolean {
        if (current != null) return true
        val created = getDocumentFile(Create(false))
        if (created != null) {
            current = created
            return true
        }
        return false
    }

    override fun createFile(): Boolean {
        if (current != null) return true
        val created = getDocumentFile(Create(true))
        if (created != null) {
            current = created
            return true
        }
        return false
    }

    @Throws(Exception::class)
    override fun toChild(name: String, policy: FileCreatePolicy): FileInstance? {
        if (!exists()) {
            Log.e(TAG, "toChild: 未经过初始化或者文件不存在：$path")
            return null
        }
        if (isFile) {
            throw Exception("当前是一个文件，无法向下操作")
        }

        val build = uri.buildUpon().path(File(path, name).absolutePath).build()
        val instance = DocumentLocalFileInstance(prefix, preferenceKey, context, build, false)
        instance.current = getChild(name, policy)
        return instance
    }

    /**
     * @param name 名称
     * @return 如果查找不到，而且不用创建，返回null
     * @throws Exception 会出现无法预计的结果时，不允许再次继续
     */
    @Throws(Exception::class)
    fun getChild(name: String?, policy: FileCreatePolicy?): DocumentFile? {
        val file = current!!.findFile(name!!)
        return file
            ?: if (policy !is Create) {
                null
            } else if (policy.isFile) {
                val createdFile = current!!.createFile("*/*", name)
                createdFile ?: throw Exception("创建文件失败")
            } else {
                val createdDirectory = current!!.createDirectory(name)
                createdDirectory ?: throw Exception("创建文件夹失败")
            }
    }

    @Throws(Exception::class)
    public override fun listInternal(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>) {
        if (current == null) {
            current = documentFileFromUri()
        }
        val c = current ?: throw Exception("no permission")
        val documentFiles = c.listFiles()
        for (documentFile in documentFiles) {
            if (needStop()) break
            val documentFileName = documentFile.name!!
            val detailString = FileUtility.getPermissions(documentFile)
            val t = child(documentFile, documentFileName)
            if (documentFile.isFile) {
                FileInstanceUtility.addFile(fileItems, t, detailString)!!.size = documentFile.length()
            } else {
                FileInstanceUtility.addDirectory(directoryItems, t, detailString)
            }
        }
    }

    private fun child(documentFile: DocumentFile, documentFileName: String): Pair<File, Uri?> {
        val child = child(documentFileName)
        return Pair<File, Uri?>(object : File(child.first.absolutePath) {
            override fun lastModified(): Long {
                return documentFile.lastModified()
            }

            override fun length(): Long {
                return documentFile.length()
            }
        }, child.second)
    }

    override fun deleteFileOrEmptyDirectory(): Boolean {
        return current!!.delete()
    }

    @Throws(Exception::class)
    override fun toParent(): BaseContextFileInstance {
        val parentFile = File(path).parentFile ?: throw Exception("到头了，无法继续向上寻找")
        val currentParentFile = current!!.parentFile
        return if (currentParentFile == null) {
            throw Exception("查找parent DocumentFile失败")
        } else if (!currentParentFile.isFile) {
            val parent = uri.buildUpon().path(parentFile.absolutePath).build()
            val instance = DocumentLocalFileInstance(prefix, prefix, context, parent, false)
            instance.current = currentParentFile
            instance
        } else {
            throw Exception("当前文件已存在，并且类型不同 源文件：" + currentParentFile.isFile)
        }
    }

    override val isFile: Boolean
        get() {
            if (current == null) {
                Log.e(TAG, "isFile: path:$path")
            }
            return current!!.isFile
        }

    override fun exists(): Boolean {
        return if (current == null) {
            false
        } else current!!.exists()
    }

    override val isDirectory: Boolean
        get() {
            if (current == null) {
                Log.e(TAG, "isDirectory: isDirectory:$path")
            }
            return current!!.isDirectory
        }

    override fun rename(newName: String): Boolean {
        return current!!.renameTo(newName)
    }

    override val isHidden: Boolean
        get() = false

    @get:Throws(FileNotFoundException::class)
    override val fileInputStream: FileInputStream
        @SuppressLint("Recycle")
        get() {
            val r = context.contentResolver.openFileDescriptor(current!!.uri, "r")
            return FileInputStream(r!!.fileDescriptor)
        }

    @get:Throws(FileNotFoundException::class)
    override val fileOutputStream: FileOutputStream
        @SuppressLint("Recycle")
        get() = FileOutputStream(context.contentResolver.openFileDescriptor(current!!.uri, "w")!!.fileDescriptor)
    override val fileLength: Long
        get() = current!!.length()

    override val file: FileItemModel
        get() {
            return FileItemModel(name, uri, false, current!!.lastModified(), false, FileUtility.getExtension(name))
        }

    override val directory: DirectoryItemModel
        get() {
            return DirectoryItemModel(name, uri, false, current!!.lastModified(), false)
        }

    companion object {
        private const val TAG = "DocumentLocalFileInstan"
        const val EXTERNAL_STORAGE_DOCUMENTS = "com.android.externalstorage.documents"
        fun getEmulated(context: Context, uri: Uri, prefix: String): DocumentLocalFileInstance {
            return DocumentLocalFileInstance(prefix, EXTERNAL_STORAGE_DOCUMENTS, context, uri)
        }

        /**
         * sd 卡使用特殊的preferenceKey，就是路径
         */
        fun getMounted(context: Context, uri: Uri, prefix: String): DocumentLocalFileInstance {
            //fixme sdCard 与emulated authority 相同，只是rootId 不同
            return DocumentLocalFileInstance(prefix, prefix, context, uri)
        }
    }
}
