package com.storyteller_f.ping

import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.storyteller_f.common_ui.scope
import com.storyteller_f.ping.database.Wallpaper
import com.storyteller_f.ping.database.requireMainDatabase
import com.storyteller_f.ping.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            scope.launch {
                val dir = "wallpaper-${System.currentTimeMillis()}"
                val file = File(cacheDir, "$dir/video.mp4").ensureFile() ?: return@launch
                withContext(Dispatchers.IO) {
                    file.outputStream().sink().buffer().use { writer ->
                        contentResolver.openInputStream(uri)?.source()?.buffer()?.use {
                            writer.writeAll(it)
                        }
                    }
                }
                val iconFile = File(cacheDir, "$dir/thumbnail.jpg").ensureFile() ?: return@launch
                createVideoThumbnailFromUri(this@MainActivity, file.toUri())?.let { thumbnail ->
                    withContext(Dispatchers.IO) {
                        FileOutputStream(iconFile).use {
                            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, it)
                        }
                    }
                }
                requireMainDatabase.dao().insertAll(listOf(Wallpaper(file.absolutePath, "test", Calendar.getInstance().time, iconFile.absolutePath)))
            }
        }
        binding.fab.setOnClickListener {
            pickFile.launch(arrayOf("video/*"))
        }
    }

    private suspend fun File.ensureFile(): File? {
        if (!exists()) {
            parentFile?.ensureDirs() ?: return null
            if (!withContext(Dispatchers.IO) {
                    createNewFile()
                }) {
                return null
            }
        }
        return this
    }

    private suspend fun File.ensureDirs(): File? {
        if (!exists()) {
            if (!withContext(Dispatchers.IO) {
                    mkdirs()
                }) {
                return null
            }
        }
        return this
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
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}