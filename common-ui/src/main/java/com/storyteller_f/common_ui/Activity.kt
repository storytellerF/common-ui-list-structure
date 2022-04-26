package com.storyteller_f.common_ui

import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
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

fun ComponentActivity.supportNavigatorBarImmersive(view: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true
    window.navigationBarColor = Color.parseColor("#01000000")
    view.setOnApplyWindowInsetsListener { v, insets ->
        val top = WindowInsetsCompat.toWindowInsetsCompat(insets, v).getInsets(WindowInsetsCompat.Type.statusBars())
        v.updatePadding(top = top.top)
        println("top:$top")
        insets
    }
}