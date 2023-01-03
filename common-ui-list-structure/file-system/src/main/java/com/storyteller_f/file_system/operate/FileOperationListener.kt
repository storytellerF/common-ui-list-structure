package com.storyteller_f.file_system.operate

import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.message.Message

interface FileOperationListener {
    /**
     * 当一个文件处理完成
     *
     * @param type 暂时没有用到
     */
    fun onFileDone(fileInstance: FileInstance?, message: Message?, size: Long, type: Int)

    /**
     * 当一个文件夹处理完成
     *
     * @param type 暂时没有用到
     */
    fun onDirectoryDone(fileInstance: FileInstance?, message: Message?, type: Int)
    fun onError(message: Message?, type: Int)
}

interface FileOperationForemanProgressListener {
    /**
     * 进度改变
     *
     * @param progress 新的进度
     */
    fun onProgress(progress: Int, key: String)

    /**
     * 正在做的工作
     *
     * @param state 新的状态信息
     */
    fun onState(state: String?, key: String)

    /**
     * 进入某个文件夹
     *
     * @param tip 新的提示
     */
    fun onTip(tip: String?, key: String)

    /**
     * 需要展示的详细信息
     */
    fun onDetail(detail: String?, level: Int, key: String)

    /**
     * 还剩余的任务
     */
    fun onLeft(fileCount: Int, folderCount: Int, size: Long, key: String)

    /**
     * 任务完成，可以刷新页面
     *
     * @param dest
     */
    fun onComplete(dest: String?, isSuccess: Boolean, key: String)
}

open class DefaultForemanProgressListener : FileOperationForemanProgressListener {
    override fun onProgress(progress: Int, key: String) = Unit

    override fun onState(state: String?, key: String) = Unit

    override fun onTip(tip: String?, key: String) = Unit

    override fun onDetail(detail: String?, level: Int, key: String) = Unit

    override fun onLeft(fileCount: Int, folderCount: Int, size: Long, key: String) = Unit

    override fun onComplete(dest: String?, isSuccess: Boolean, key: String) = Unit

}