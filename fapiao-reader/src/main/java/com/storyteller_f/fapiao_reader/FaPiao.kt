package com.storyteller_f.fapiao_reader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class FaPiao(val code: String, val time: Date, val total: Float, val number: String) : Parcelable {
}