package com.storyteller_f.b3.ui.login

import com.google.gson.annotations.SerializedName


data class GeetestB3Response(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("data")
    val dataSaved: Data? = null,
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

data class GeetestResult(
    @SerializedName("geetest_challenge")
    val geetestChallenge: String? = null,
    @SerializedName("geetest_seccode")
    val geetestSeccode: String? = null,
    @SerializedName("geetest_validate")
    val geetestValidate: String? = null
)

const val geetest_url = "https://www.geetest.com/demo/gt/"