package com.storyteller_f.giant_explorer.control

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.*
import com.bumptech.glide.Glide
import com.storyteller_f.common_ui.CommonActivity
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.databinding.ActivityAboutBinding
import com.storyteller_f.ui_list.event.viewBinding

class AboutActivity : CommonActivity() {
    private val binding by viewBinding(ActivityAboutBinding::inflate)
    private val CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome" // Change when in stable
    var newSession: CustomTabsSession? = null
    private val connection: CustomTabsServiceConnection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            client.warmup(0)
            newSession = client.newSession(object : CustomTabsCallback() {

            })
            newSession?.mayLaunchUrl(Uri.parse(MagiskUrl), null, null)
            newSession?.mayLaunchUrl(Uri.parse(kernelSuUrl), null, null)
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Glide.with(this).load(R.drawable.storytellerf_github_business_card).into(binding.image)
        CustomTabsClient.bindCustomTabsService(this, CUSTOM_TAB_PACKAGE_NAME, connection)
        binding.image.setOnClick {
            val newSession = newSession
            val height = ScreenMetricsCompat.getScreenSize(this).height
            val builder = CustomTabsIntent.Builder().setInitialActivityHeightPx((height * 0.7).toInt())
            if (newSession != null) builder.setSession(newSession)
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(this, Uri.parse("https://github.com/storytellerF/common-ui-list-structure"))
        }
    }
}