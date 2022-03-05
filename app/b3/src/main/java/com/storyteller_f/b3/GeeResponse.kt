package com.storyteller_f.b3
import com.google.gson.annotations.SerializedName


data class GeeResponse(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("data")
    val `data`: Data? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("ttl")
    val ttl: Int? = null
)

data class Data(
    @SerializedName("geetest")
    val geetest: Geetest? = null,
    @SerializedName("tencent")
    val tencent: Tencent? = null,
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("type")
    val type: String? = null
)

data class Geetest(
    @SerializedName("challenge")
    val challenge: String? = null,
    @SerializedName("gt")
    val gt: String? = null
)

data class Tencent(
    @SerializedName("appid")
    val appid: String? = null
)