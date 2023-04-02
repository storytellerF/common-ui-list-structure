package com.storyteller_f.giant_explorer.control.root

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.databinding.ActivityRootAccessBinding
import com.topjohnwu.superuser.Shell
import kotlin.concurrent.thread

class RootAccessActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityRootAccessBinding

    var newSession: CustomTabsSession? = null
    private val connection: CustomTabsServiceConnection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            thread {
                val warmup = client.warmup(0)
                Log.i(TAG, "onCustomTabsServiceConnected: warmup $warmup")
                newSession = client.newSession(object : CustomTabsCallback() {

                })
                newSession?.mayLaunchUrl(Uri.parse(MagiskUrl), null, null)
                newSession?.mayLaunchUrl(Uri.parse(kernelSuUrl), null, null)
            }

        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityRootAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_root_access)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener {
            Shell.getShell { shell ->
                Snackbar.make(binding.root, shell.isRoot.toString(), Snackbar.LENGTH_SHORT).show()
            }
        }
        val bindCustomTabsService = CustomTabsClient.bindCustomTabsService(this, CUSTOM_TAB_PACKAGE_NAME, connection)
        Log.i(TAG, "onCreate: bind $bindCustomTabsService")
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_root_access)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    companion object {
        private const val TAG = "RootAccessActivity"
        private const val CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome" // Change when in stable
    }
}