package com.storyteller_f.giant_explorer.control

import android.os.Build
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.storyteller_f.common_ktx.mm
import com.storyteller_f.common_ui.CommonActivity
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system_ktx.ensureFile
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.databinding.ActivityPluginManageBinding
import com.storyteller_f.giant_explorer.dialog.RequestPathDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class PluginManageActivity : CommonActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityPluginManageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityPluginManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_plugin_manage)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        val pluginRoot = File(filesDir, "plugins")
        binding.fab.setOnClickListener {
            dialog(RequestPathDialog::class.java) { result: RequestPathDialog.RequestPathResult ->
                result.path.mm {
                    getFileInstance(it, this)
                }.mm { dest ->
                    lifecycleScope.launch {
                        addPlugin(dest, pluginRoot)
                    }

                }
            }
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAnchorView(R.id.fab)
//                .setAction("Action", null).show()
        }
    }

    private suspend fun addPlugin(dest: FileInstance, pluginRoot: File) {
        val name = dest.name
        val pluginFile = File(pluginRoot, name).ensureFile() ?: return
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(dest.fileInputStream, pluginFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } else {
                TODO("VERSION.SDK_INT < O")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_plugin_manage)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}