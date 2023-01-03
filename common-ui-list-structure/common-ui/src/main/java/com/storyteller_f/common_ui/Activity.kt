package com.storyteller_f.common_ui

import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.databinding.ViewDataBinding
import androidx.viewbinding.ViewBinding

abstract class CommonActivity : AppCompatActivity(), RegistryFragment {
    override fun onStart() {
        super.onStart()
        //主要用于旋转屏幕
        waitingInActivity.forEach { t ->
            @Suppress("UNCHECKED_CAST") val action = t.value.action as Function2<CommonActivity, Parcelable, Unit>
            if (t.value.registerKey == registryKey()) {
                val callback = { s: String, r: Bundle ->
                    if (waitingInFragment.containsKey(s)) {
                        r.getParcelable<Parcelable>("result")?.let {
                            action(this, it)
                        }
                        waitingInFragment.remove(s)
                    }
                }
                supportFragmentManager.clearFragmentResultListener(t.key)
                supportFragmentManager.setFragmentResultListener(t.key, this, callback)
            }
        }
    }

    fun <T : Parcelable> dialog(dialog: Class<out CommonDialogFragment>, parameters: Bundle? = null, action: (T) -> Unit) {
        val apply = dialog.newInstance().apply {
            arguments = parameters
            show(supportFragmentManager, this.tag())
        }
        val requestKey = apply.requestKey()
        val callback = { s: String, r: Bundle ->
            if (waitingInActivity.containsKey(s)) {
                r.getParcelable<T>("result")?.let {
                    action.invoke(it)
                }
                waitingInActivity.remove(s)
            }
        }
        waitingInActivity[requestKey] = ActivityAction(callback, registryKey())
        supportFragmentManager.setFragmentResultListener(requestKey, this, callback)
    }
}

abstract class SimpleActivity<T : ViewBinding>(
    val viewBindingFactory: (LayoutInflater) -> T
) : AppCompatActivity(), RegistryFragment {
    private var _binding: T? = null
    val binding: T get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindingLocal = viewBindingFactory(layoutInflater)
        setContentView(bindingLocal.root)
        _binding = bindingLocal
        (binding as? ViewDataBinding)?.lifecycleOwner = this
        onBindViewEvent(binding)
    }

    abstract fun onBindViewEvent(binding: T)
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
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
        insets
    }
}