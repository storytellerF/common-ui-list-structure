package com.storyteller_f.giant_explorer.utils

import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.multi_core.StoppableTask
import java.io.*
import java.nio.charset.StandardCharsets

class TorrentFile {
    companion object {

        /**
         * 获得种子的名字，耗时操作
         *
         * @param fileInstance 用来获取输入流
         * @return 种子名
         * @throws IOException io 异常
         */
        @Throws(Exception::class)
        fun getTorrentName(fileInstance: FileInstance, worker: StoppableTask): String {
            val bEncodedDictionary = Decode.bDecode(fileInstance.bufferedInputSteam, worker)
            val encodedDictionary: BEncodedDictionary =
                bEncodedDictionary.get("info") as BEncodedDictionary
            return String(
                encodedDictionary.get("name").toString().toByteArray(StandardCharsets.ISO_8859_1),
                StandardCharsets.UTF_8
            )
        }
    }
}