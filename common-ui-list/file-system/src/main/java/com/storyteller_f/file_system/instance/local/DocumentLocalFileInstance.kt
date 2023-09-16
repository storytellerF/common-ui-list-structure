package com.storyteller_f.file_system.instance.local

import android.annotation.SuppressLint
import android.content.ContentResolver
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
import com.storyteller_f.file_system.instance.GetDocumentFile
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.util.FileInstanceUtility
import com.storyteller_f.file_system.util.FileUtility
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

/**
 * 如果是通过挂载路径 访问/storage/emulated/0，uri 是file:///storage/emulated/0/Downloads，prefix 是/storage/emulated/0，tree 是primary:
 * 如果是通过DocumentProvider 访问前者，uri 是content://authority/primary:/Downloads，prefix 是primary:，tree 是primary:
 * @param prefix 用来标识对象所在区域，便于从path 中截取root对应的真正的路径。
 * 如果是内存卡就是/storage/XXXX-XXXX。
 * 如果是mounted，就是/storage/emulated/0。
 * 如果是DocumentProvider，就是/ 加上treeId，并且treeId可以是/。内存卡和mounted 也可以通过DocumentProvider 直接访问
 * @param preferenceKey 一般是authority，用于获取存储在FileSystemUriSaver 中取出rootUri。
 * 如果是DocumentProvider，tree 也是必须的。
 * @param tree 是DocumentProvider 的treeId，用来区分不同DocumentProvider 多个根的情况
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class DocumentLocalFileInstance(
    private val prefix: String,
    private val preferenceKey: String,
    private val tree: String,
    context: Context,
    uri: Uri
) : BaseContextFileInstance(context, uri) {
    private var _instance: DocumentFile? = null

    private suspend fun getInstanceRelinkIfNeed(): DocumentFile? {
        val temp = _instance
        if (temp == null) {
            _instance = (getDocumentFile(NotCreate) as? GetDocumentFile.Success)?.file
        }
        return _instance
    }

    /**
     * 有些document provider 的uri 对应的路径不是path，所以需要从中截取真正的路径
     * 比如mounted 路径，完整路径是/storage/emulated/0/Downloads，
     * prefix 是/storage/emulated/0，
     * path 是/storage/emulated/0/Downloads，
     * pathRelativeRoot 是/Downloads
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
    }

    /**
     * 获取指定目录的document file
     *
     * @return 返回目标文件
     */
    private suspend fun getDocumentFile(policy: FileCreatePolicy): GetDocumentFile {
        // 此uri 是当前前缀下的根目录uri。fileInstance 的uri 是fileSystem 使用的uri。
        val rootUri = FileSystemUriSaver.instance.savedUri(context, preferenceKey, tree)
            ?: return GetDocumentFile.Failed(Exception("rootUri is null"))
        val rootFile = DocumentFile.fromTreeUri(context, rootUri)
            ?: return GetDocumentFile.Failed(Exception("fromTreeUri is null"))
        if (!rootFile.canRead()) {
            return GetDocumentFile.Failed(Exception("权限过期, 不可读写"))
        }
        if (pathRelativeRoot == "/") return GetDocumentFile.Success(rootFile)
        val nameItemPath = pathRelativeRoot.substring(
            1
        ).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val createdLastFileIfNeed = policy is Create && policy.isFile
        val paths = if (createdLastFileIfNeed) {
            nameItemPath.copyOfRange(0, nameItemPath.size - 1)
        } else {
            nameItemPath
        }
        return loopDocumentFile(
            rootFile,
            paths,
            policy,
            if (createdLastFileIfNeed) nameItemPath.last() else null
        )
    }

    private suspend fun loopDocumentFile(
        rootFile: DocumentFile,
        paths: Array<String>,
        policy: FileCreatePolicy,
        fileName: String?
    ): GetDocumentFile {
        var temp: DocumentFile = rootFile
        for (name in paths) {
            yield()
            val foundFile = temp.findFile(name)
            temp = when {
                foundFile != null -> foundFile
                policy is NotCreate -> {
                    return GetDocumentFile.Failed(Exception("文件找不到$path prefix: $prefix"))
                }

                else -> temp.createDirectory(name)
                    ?: return GetDocumentFile.Failed(Exception("文件创建失败$path prefix: $prefix"))
            }
        }

        return if (fileName == null) {
            GetDocumentFile.Success(temp)
        } else {
            val file = temp.findFile(fileName)
            if (file != null) {
                GetDocumentFile.Success(file)
            } else {
                val created: DocumentFile? = temp.createFile("*/*", fileName)
                if (created != null) {
                    GetDocumentFile.Success(created)
                } else {
                    GetDocumentFile.Failed(Exception("创建失败"))
                }
            }
        }
    }

    override suspend fun getDirectorySize(): Long = getDocumentFileSize(getInstanceRelinkIfNeed())

    private suspend fun getDocumentFileSize(documentFile: DocumentFile?): Long {
        var size: Long = 0
        val documentFiles = documentFile!!.listFiles()
        for (documentFi in documentFiles) {
            yield()
            size += if (documentFile.isFile) {
                documentFi.length()
            } else {
                getDocumentFileSize(documentFi)
            }
        }
        return size
    }

    override suspend fun createDirectory(): Boolean {
        if (getInstanceRelinkIfNeed() != null) return true
        val created = tryCreate(false)
        if (created != null) {
            _instance = created
            return true
        }
        return false
    }

    override suspend fun createFile(): Boolean {
        if (getInstanceRelinkIfNeed() != null) return true
        val created = tryCreate(true)
        if (created != null) {
            _instance = created
            return true
        }
        return false
    }

    private suspend fun tryCreate(isFile: Boolean) =
        (getDocumentFile(Create(isFile)) as? GetDocumentFile.Success)?.file

    @Throws(Exception::class)
    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance? {
        if (!exists()) {
            Log.e(TAG, "toChild: 未经过初始化或者文件不存在：$path")
            return null
        }
        if (isFile()) {
            throw Exception("当前是一个文件，无法向下操作")
        }

        val build = uri.buildUpon().path(File(path, name).absolutePath).build()
        val instance = DocumentLocalFileInstance(prefix, preferenceKey, tree, context, build)
        instance._instance = getChild(name, policy)
        return instance
    }

    /**
     * @param name 名称
     * @return 如果查找不到，而且不用创建，返回null
     * @throws Exception 会出现无法预计的结果时，不允许再次继续
     */
    @Throws(Exception::class)
    suspend fun getChild(name: String?, policy: FileCreatePolicy?): DocumentFile? {
        val instanceRelinkIfNeed = getInstanceRelinkIfNeed()
        val file = instanceRelinkIfNeed!!.findFile(name!!)
        return file
            ?: if (policy !is Create) {
                null
            } else if (policy.isFile) {
                val createdFile = instanceRelinkIfNeed.createFile("*/*", name)
                createdFile ?: throw Exception("创建文件失败")
            } else {
                val createdDirectory = instanceRelinkIfNeed.createDirectory(name)
                createdDirectory ?: throw Exception("创建文件夹失败")
            }
    }

    @Throws(Exception::class)
    public override suspend fun listInternal(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    ) {
        val c = getInstanceRelinkIfNeed() ?: throw Exception("no permission")
        val documentFiles = c.listFiles()
        for (documentFile in documentFiles) {
            yield()
            val documentFileName = documentFile.name!!
            val detailString = FileUtility.getPermissions(documentFile)
            val t = child(documentFile, documentFileName)
            if (documentFile.isFile) {
                FileInstanceUtility.addFile(fileItems, t, detailString)!!.size =
                    documentFile.length()
            } else {
                FileInstanceUtility.addDirectory(directoryItems, t, detailString)
            }
        }
    }

    private fun child(documentFile: DocumentFile, documentFileName: String): Pair<File, Uri?> {
        val child = child(documentFileName)
        return Pair<File, Uri?>(
            object : File(child.first.absolutePath) {
                override fun lastModified(): Long {
                    return documentFile.lastModified()
                }

                override fun length(): Long {
                    return documentFile.length()
                }
            },
            child.second
        )
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        return getInstanceRelinkIfNeed()!!.delete()
    }

    @Throws(Exception::class)
    override suspend fun toParent(): BaseContextFileInstance {
        val parentFile = File(path).parentFile ?: throw Exception("到头了，无法继续向上寻找")
        val currentParentFile = getInstanceRelinkIfNeed()!!.parentFile
        checkNotNull(currentParentFile) {
            "查找parent DocumentFile失败"
        }
        check(currentParentFile.isFile) {
            "当前文件已存在，并且类型不同 源文件：" + currentParentFile.isFile
        }

        val parent = uri.buildUpon().path(parentFile.absolutePath).build()
        val instance = DocumentLocalFileInstance(prefix, prefix, tree, context, parent)
        instance._instance = currentParentFile
        return instance
    }

    override suspend fun isFile(): Boolean {
        val instanceRelinkIfNeed = getInstanceRelinkIfNeed()
        if (instanceRelinkIfNeed == null) {
            Log.e(TAG, "isFile: path:$path")
        }
        return instanceRelinkIfNeed!!.isFile
    }

    override suspend fun exists(): Boolean {
        return getInstanceRelinkIfNeed()?.exists() ?: false
    }

    override suspend fun isDirectory(): Boolean {
        val instanceRelinkIfNeed = getInstanceRelinkIfNeed()
        if (instanceRelinkIfNeed == null) {
            Log.e(TAG, "isDirectory: isDirectory:$path")
        }
        return instanceRelinkIfNeed!!.isDirectory
    }

    override suspend fun rename(newName: String): Boolean {
        return getInstanceRelinkIfNeed()!!.renameTo(newName)
    }

    override suspend fun isHidden(): Boolean = false

    @SuppressLint("Recycle")
    @Throws(FileNotFoundException::class)
    override suspend fun getFileInputStream(): FileInputStream {
        val r = context.contentResolver.openFileDescriptor(getInstanceRelinkIfNeed()!!.uri, "r")
        return FileInputStream(r!!.fileDescriptor)
    }

    @SuppressLint("Recycle")
    @Throws(FileNotFoundException::class)
    override suspend fun getFileOutputStream(): FileOutputStream = FileOutputStream(
        context.contentResolver.openFileDescriptor(
            getInstanceRelinkIfNeed()!!.uri,
            "w"
        )!!.fileDescriptor
    )

    override suspend fun getFileLength(): Long = getInstanceRelinkIfNeed()!!.length()

    override suspend fun getFile(): FileItemModel {
        return FileItemModel(
            name,
            uri,
            false,
            getInstanceRelinkIfNeed()!!.lastModified(),
            false,
            FileUtility.getExtension(name)
        )
    }

    override suspend fun getDirectory(): DirectoryItemModel {
        return DirectoryItemModel(
            name,
            uri,
            false,
            getInstanceRelinkIfNeed()!!.lastModified(),
            false
        )
    }

    companion object {
        private const val TAG = "DocumentLocalFileInstan"
        const val EXTERNAL_STORAGE_DOCUMENTS = "com.android.externalstorage.documents"
        const val EXTERNAL_STORAGE_DOCUMENTS_TREE = "primary:"

        /**
         * 不是通过DocumentProvider 直接访问的。
         */
        fun getEmulated(context: Context, uri: Uri, prefix: String): DocumentLocalFileInstance {
            assert(uri.scheme == ContentResolver.SCHEME_FILE)
            return DocumentLocalFileInstance(
                prefix,
                EXTERNAL_STORAGE_DOCUMENTS,
                EXTERNAL_STORAGE_DOCUMENTS_TREE,
                context,
                uri
            )
        }

        /**
         * 不是通过DocumentProvider 直接访问的。
         * sd 卡使用特殊的preferenceKey，正常来说就是路径
         */
        fun getMounted(context: Context, uri: Uri, prefix: String): DocumentLocalFileInstance {
            assert(uri.scheme == ContentResolver.SCHEME_FILE)
            val tree = getMountedTree(prefix)
            return DocumentLocalFileInstance(prefix, EXTERNAL_STORAGE_DOCUMENTS, tree, context, uri)
        }

        fun getMountedTree(prefix: String) = prefix.substring(prefix.lastIndexOf("/") + 1)

        fun uriFromAuthority(authority: String, tree: String): Uri {
            return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority)
                .path("/$tree")
                .build()
        }
    }
}
