package com.storyteller_f.common_ui

import android.content.Context
import android.content.res.Configuration
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
import com.storyteller_f.compat_ktx.getParcelableCompat

abstract class CommonActivity : AppCompatActivity(), RegistryFragment {
    override fun onStart() {
        super.onStart()
        //主要用于旋转屏幕
        waitingInActivity.forEach { t ->
            val action = t.value.action
            if (t.value.registerKey == registryKey()) {
                val callback = { requestKey: String, result: Bundle ->
                    if (waitingInFragment.containsKey(requestKey)) {
                        result.getParcelableCompat("result", Parcelable::class.java)?.let { action(this, it) }
                        waitingInFragment.remove(requestKey)
                    }
                }
                supportFragmentManager.clearFragmentResultListener(t.key)
                supportFragmentManager.setFragmentResultListener(t.key, this, callback)
            }
        }
    }
}

fun <T : Parcelable, A> A.dialog(dialog: Class<out CommonDialogFragment>, result: Class<T>, parameters: Bundle? = null, action: A.(T) -> Unit) where A : CommonActivity {
    val apply = dialog.newInstance().apply {
        arguments = parameters
        show(supportFragmentManager, this.tag())
    }
    val requestKey = apply.requestKey()
    val callback = { s: String, r: Bundle ->
        if (waitingInActivity.containsKey(s)) {
            r.getParcelableCompat("result", result)?.let {
                action(it)
            }
            waitingInActivity.remove(s)
        }
    }
    @Suppress("UNCHECKED_CAST")
    waitingInActivity[requestKey] = ActivityAction(action as (CommonActivity, Parcelable) -> Unit, registryKey())
    supportFragmentManager.setFragmentResultListener(requestKey, this, callback)
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
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = !isNightMode
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNightMode
    /**
     * 如果提供一个透明色，在低版本中会自动添加一个颜色
     */
    window.navigationBarColor = Color.parseColor("#01000000")
    view.setOnApplyWindowInsetsListener { v, insets ->
        val top = WindowInsetsCompat.toWindowInsetsCompat(insets, v).getInsets(WindowInsetsCompat.Type.statusBars())
        v.updatePadding(top = top.top)
        insets
    }
}

val Context.isNightMode
    get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
