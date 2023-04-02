package com.storyteller_f.giant_explorer.control.root

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.giant_explorer.databinding.FragmentRootIntroBinding

val MagiskUrl = "https://github.com/topjohnwu/Magisk"
val kernelSuUrl = "https://github.com/tiann/KernelSU"

class RootIntroFragment : SimpleFragment<FragmentRootIntroBinding>(FragmentRootIntroBinding::inflate) {

    override fun onBindViewEvent(binding: FragmentRootIntroBinding) {

        binding.button.setOnClickListener {
            open(MagiskUrl)
        }
        binding.kernelSu.setOnClickListener {
            open(kernelSuUrl)
        }
    }

    private fun open(url: String) {
        val newSession = (requireActivity() as RootAccessActivity).newSession
        val height = ScreenMetricsCompat.getScreenSize(requireContext()).height
        val builder = CustomTabsIntent.Builder().setInitialActivityHeightPx((height * 0.7).toInt())
        if (newSession != null) builder.setSession(newSession)
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
    }

}

object ScreenMetricsCompat {
    private val api: Api =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ApiLevel30()
        else Api()

    /**
     * Returns screen size in pixels.
     */
    fun getScreenSize(context: Context): Size = api.getScreenSize(context)

    @Suppress("DEPRECATION")
    private open class Api {
        open fun getScreenSize(context: Context): Size {
            val display = ContextCompat.getSystemService(context, WindowManager::class.java)?.defaultDisplay
            val metrics = if (display != null) {
                DisplayMetrics().also { display.getRealMetrics(it) }
            } else {
                Resources.getSystem().displayMetrics
            }
            return Size(metrics.widthPixels, metrics.heightPixels)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private class ApiLevel30 : Api() {
        override fun getScreenSize(context: Context): Size {
            val metrics: WindowMetrics = context.getSystemService(WindowManager::class.java).currentWindowMetrics
            return Size(metrics.bounds.width(), metrics.bounds.height())
        }
    }
}