package com.storyteller_f.common_ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

open class SimpleActivity : AppCompatActivity() {
    override fun onStart() {
        super.onStart()
        waiting.forEach { t ->
            supportFragmentManager.setFragmentResultListener(t.key, this) { requestKey, result ->
                if (waiting.containsKey(requestKey)) {
                    t.value.invoke(result)
                    waiting.remove(requestKey)
                }
            }
        }
    }

    fun dialog(requestKey: String, dialog: Class<out CommonDialogFragment<out ViewBinding>>, parameters: Bundle? = null, action: (Bundle) -> Unit) {
        dialog.newInstance().apply {
            arguments = parameters
        }.show(supportFragmentManager, requestKey)
        waiting[requestKey] = action
        supportFragmentManager.setFragmentResultListener(requestKey, this) { s, r ->
            if (waiting.containsKey(s)) {
                action.invoke(r)
                waiting.remove(s)
            }
        }

    }
}