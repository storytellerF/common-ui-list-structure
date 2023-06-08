package com.storyteller_f.common_ui_list_structure

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.storyteller_f.common_ui_list_structure.databinding.ActivityTestViewModelBinding
import com.storyteller_f.ui_list.event.viewBinding

class TestViewModelActivity : AppCompatActivity() {

    private val binding by viewBinding(ActivityTestViewModelBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navView: BottomNavigationView = binding.navView
        setSupportActionBar(binding.toolbar)
        val navController = findNavController(R.id.nav_host_fragment_activity_main2)
        navView.setupWithNavController(navController)
    }
}