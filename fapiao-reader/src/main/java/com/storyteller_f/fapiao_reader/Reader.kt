package com.storyteller_f.fapiao_reader

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Reader {
    companion object {
        val formate = SimpleDateFormat("yyyy 年 MM 月 dd日", Locale.CHINA)
    }

    fun reader(path: File): FaPiao? {
        val load = PDDocument.load(path)
        return load.use {
            val pdfTextStripper = PDFTextStripper()
            pdfTextStripper.sortByPosition = true
            val text = pdfTextStripper.getText(load) ?: return null
            println(text)
            val code = Regex("发票代码:([\\d]{12})").find(text)?.groupValues?.get(1) ?: return null
            val number = Regex("发票号码:([\\d]{8})").find(text)?.groupValues?.get(1) ?: return null
            val dateString = Regex("开票日期: ([\\d]{4} 年 [\\d]{2} 月 [\\d]{2}日)").find(text)?.groupValues?.get(1) ?: return null
            val total = Regex("\\(小写\\)¥([\\d]+\\.[\\d]+)").find(text)?.groupValues?.get(1) ?: return null
            val date = formate.parse(dateString) ?: return null
            println(date)
            FaPiao(code, date, total.toFloat(), number)
        }

    }

}