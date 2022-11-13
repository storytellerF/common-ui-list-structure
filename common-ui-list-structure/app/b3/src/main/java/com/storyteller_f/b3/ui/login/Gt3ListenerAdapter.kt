package com.storyteller_f.b3.ui.login

import com.geetest.sdk.GT3ErrorBean
import com.geetest.sdk.GT3Listener

abstract class Gt3ListenerAdapter : GT3Listener() {
    override fun onReceiveCaptchaCode(p0: Int) = Unit

    override fun onStatistics(p0: String?) = Unit

    override fun onClosed(p0: Int) = Unit

    override fun onSuccess(p0: String?) = Unit

    override fun onFailed(p0: GT3ErrorBean?) = Unit
}