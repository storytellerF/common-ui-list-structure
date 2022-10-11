package com.storyteller_f.b3.ui.login

import com.geetest.sdk.GT3ErrorBean
import com.geetest.sdk.GT3Listener

abstract class Gt3ListenerAdapter : GT3Listener() {
    override fun onReceiveCaptchaCode(p0: Int) {
    }

    override fun onStatistics(p0: String?) {
    }

    override fun onClosed(p0: Int) {
    }

    override fun onSuccess(p0: String?) {
    }

    override fun onFailed(p0: GT3ErrorBean?) {
    }
}