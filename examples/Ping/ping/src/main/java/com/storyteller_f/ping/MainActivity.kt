package com.storyteller_f.ping

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract.Document
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.storyteller_f.common_ui.scope
import com.storyteller_f.file_system_ktx.ensureFile
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
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment).navController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            scope.launch {
                saveResult(uri)
            }
        }
        binding.fab.setOnClickListener {
            pickFile.launch(arrayOf("*/*"))
        }
    }

    private suspend fun saveResult(uri: Uri) {
        val dir = "wallpaper-${System.currentTimeMillis()}"
        val type = contentResolver.getType(uri) ?: return
        val name =
            contentResolver.query(uri, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)
                ?.use {
                    if (it.moveToFirst()) {
                        val columnIndex = it.getColumnIndex(Document.COLUMN_DISPLAY_NAME)
                        if (columnIndex == -1) null else it.getString(columnIndex)
                    } else null
                } ?: dir

        saveVideoResult(dir, uri, name, type)
    }

    private suspend fun saveVideoResult(dir: String, uri: Uri, name: String, type: String) {
        val target = if (type.startsWith("video")) "$dir/video.mp4" else "$dir/scene.gltf"
        val file = File(
            cacheDir,
            target
        ).ensureFile() ?: return
        withContext(Dispatchers.IO) {
            file.outputStream().sink().buffer().use { writer ->
                contentResolver.openInputStream(uri)?.use { stream ->
                    stream.source().buffer().use {
                        writer.writeAll(it)
                    }
                }
            }
        }
        val element = if (type.startsWith("video")) {
            val iconFile = File(cacheDir, "$dir/thumbnail.jpg").ensureFile() ?: return
            createVideoThumbnailFromUri(this@MainActivity, file.toUri())?.let { thumbnail ->
                withContext(Dispatchers.IO) {
                    FileOutputStream(iconFile).use {
                        thumbnail.compress(Bitmap.CompressFormat.JPEG, 60, it)
                    }
                }
            }
            Wallpaper(
                file.absolutePath,
                name,
                Calendar.getInstance().time,
                iconFile.absolutePath
            )
        } else {
            Wallpaper(
                file.absolutePath,
                name,
                Calendar.getInstance().time,
                ""
            )
        }
        requireMainDatabase.dao().insertAll(listOf(element))
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