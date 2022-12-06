package com.storyteller_f.yue

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.storyteller_f.plugin_core.GiantExplorerPlugin
import com.storyteller_f.plugin_core.GiantExplorerPluginManager
import com.storyteller_f.yue.databinding.ActivityMainBinding
import com.storyteller_f.yue_plugin.YueFragmentArgs
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val pluginManager = object : GiantExplorerPluginManager {
        override fun fileInputStream(path: String): FileInputStream {
            return FileInputStream(path)
        }

        override fun fileOutputStream(path: String): FileOutputStream {
            return FileOutputStream(path)
        }

        override fun listFiles(path: String): List<String> {
            return filesDir.list()?.toList() ?: emptyList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupFullscreen()
        setupNavigation()
        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        navigateToYue(intent)
    }

    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.navigationBarColor = Color.TRANSPARENT
        binding.root.setOnApplyWindowInsetsListener { v, insets ->
            val top = WindowInsetsCompat.toWindowInsetsCompat(insets, v).getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = top)
            insets
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navHostFragment.childFragmentManager.addFragmentOnAttachListener { _, fragment ->
            if (fragment is GiantExplorerPlugin) {
                fragment.plugPluginManager(pluginManager)
            }
            Log.i(TAG, "onCreate: ${fragment.javaClass.canonicalName}")
        }
        val navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToYue(intent)
    }

    private fun navigateToYue(intent: Intent?) {
        intent ?: return
        val path = intent.getStringExtra("path") ?: return
        val uri = intent.data ?: return
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        navController.popBackStack(R.id.FirstFragment, true)
        navController.navigate(R.id.YueFragment, YueFragmentArgs(path, uri).toBundle())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}