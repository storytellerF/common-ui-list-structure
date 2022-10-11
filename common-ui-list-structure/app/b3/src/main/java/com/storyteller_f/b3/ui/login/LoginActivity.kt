package com.storyteller_f.b3.ui.login

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.geetest.sdk.*
import com.storyteller_f.b3.R
import com.storyteller_f.b3.api.Common
import com.storyteller_f.b3.api.GeetestAPI
import com.storyteller_f.b3.databinding.ActivityLoginBinding
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_vm_ktx.GenericValueModel
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.requireGson
import com.storyteller_f.ui_list.event.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    val geetestResult by vm({}) {
        GenericValueModel<GeetestResult>()
    }
    var requestTask: Job? = null
    private val binding by viewBinding(ActivityLoginBinding::inflate)

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gtu = GT3GeetestUtils(this)
        val gtb = GT3ConfigBean().apply {
            pattern = 1
            isCanceledOnTouchOutside = true
            isUnCanceledOnTouchKeyCodeBack = true
            loadImageView = GT3LoadImageView(this@LoginActivity).apply {
                iconRes = R.drawable.loading_test
                loadViewHeight = 48
                loadViewWidth = 48
                corners = 5
                dialogOffsetY = 5
            }
            listener = object : Gt3ListenerAdapter() {
                override fun onClosed(p0: Int) {
                    Log.d(TAG, "onClosed() called with: p0 = $p0")
                    super.onClosed(p0)
                    requestTask?.cancel()
                }

                override fun onDialogResult(result: String?) {
                    Log.d(TAG, "onDialogResult() called with: result = $result")
                    super.onDialogResult(result)
                    lifecycleScope.launch {
                        try {
                            val fromJson = requireGson().fromJson(result, GeetestResult::class.java)
                            val invalidate =
                                GeetestAPI.create().invalidate(System.currentTimeMillis(), fromJson)
                            if (invalidate.status == "success") {
                                gtu.showSuccessDialog()
                            } else {
                                gtu.showFailedDialog()
                            }
                        } catch (e: Exception) {

                        }

                    }
                }

                override fun onButtonClick() {
                    Log.d(TAG, "onButtonClick() called")
                    requestTask = lifecycleScope.launch {
                        try {
                            val geetest = Common.create().requestGee().dataSaved?.geetest
                            println("button click geetest $geetest")
                            this@apply.api1Json = JSONObject(requireGson().toJson(geetest))
                            gtu.getGeetest()
                        } catch (e: Exception) {

                        }

                    }
                }
            }
        }
        gtu.init(gtb)
        binding.login.setOnClick {
            gtu.startCustomFlow()
        }
    }
}
