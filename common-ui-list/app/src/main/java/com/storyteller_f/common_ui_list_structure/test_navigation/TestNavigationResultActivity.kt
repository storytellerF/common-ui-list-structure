package com.storyteller_f.common_ui_list_structure.test_navigation

import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.storyteller_f.common_ui.CommonActivity
import com.storyteller_f.common_ui.observe
import com.storyteller_f.common_ui.request
import com.storyteller_f.common_ui_list_structure.R
import com.storyteller_f.common_ui_list_structure.dialog.TestDialog2
import com.storyteller_f.common_ui_list_structure.databinding.ActivityTestNavigationResultBinding
import com.storyteller_f.ui_list.event.viewBinding

class TestNavigationResultActivity : CommonActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val binding by viewBinding(ActivityTestNavigationResultBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding
        setSupportActionBar(binding.toolbar)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            observe(request(TestDialog2::class.java), TestDialog2.Result::class.java) {
                Snackbar.make(view, it.test, Snackbar.LENGTH_LONG)
                    .setAction("activity->dialog", null).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}