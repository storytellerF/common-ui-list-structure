package com.storyteller_f.common_ui_list_structure

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.storyteller_f.common_ui_list_structure.databinding.ActivityMain2Binding
import com.storyteller_f.ui_list.event.viewBinding

class MainActivity2 : AppCompatActivity() {

    private val binding: ActivityMain2Binding by viewBinding(ActivityMain2Binding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navView: BottomNavigationView = binding.navView
        setSupportActionBar(binding.toolbar)
        val navController = findNavController(R.id.nav_host_fragment_activity_main2)
        navView.setupWithNavController(navController)
    }
}