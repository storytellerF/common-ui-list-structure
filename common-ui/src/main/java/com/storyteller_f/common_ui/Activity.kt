package com.storyteller_f.common_ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

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

    fun listener(requestKey: String, action: (Bundle) -> Unit) {
        waiting[requestKey] = action
    }

    operator fun set(requestKey: String, action: (Bundle) -> Unit) {
        listener(requestKey, action)
    }
}