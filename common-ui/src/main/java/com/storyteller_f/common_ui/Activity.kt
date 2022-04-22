package com.storyteller_f.common_ui

import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

open class SimpleActivity : AppCompatActivity() {
    override fun onStart() {
        super.onStart()
        waiting.forEach { t ->
            supportFragmentManager.setFragmentResultListener(t.key, this, t.value.action)
        }
    }

    fun <T : Parcelable> fragment(requestKey: String, dialog: Class<out CommonDialogFragment<out ViewBinding>>, parameters: Bundle? = null, action: (T) -> Unit) {
        dialog.newInstance().apply {
            arguments = parameters
        }.show(supportFragmentManager, requestKey)
        val callback = { s: String, r: Bundle ->
            if (waiting.containsKey(s)) {
                r.getParcelable<T>("result")?.let {
                    action.invoke(it)
                }
                waiting.remove(s)
            }
        }
        waiting[requestKey] = FragmentAction(callback)
        supportFragmentManager.setFragmentResultListener(requestKey, this, callback)
    }
}