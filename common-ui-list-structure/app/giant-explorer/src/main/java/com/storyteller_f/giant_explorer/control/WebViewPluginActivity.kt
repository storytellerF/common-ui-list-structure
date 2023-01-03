package com.storyteller_f.giant_explorer.control

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.storyteller_f.common_ui.scope
import com.storyteller_f.file_system_ktx.ensureFile
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.plugin_core.FileSystemProviderResolver
import com.storyteller_f.plugin_core.GiantExplorerPluginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.concurrent.thread

class WebViewPluginActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_plugin)
        val data = intent.data
        val webView = findViewById<WebView>(R.id.web_view)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                Log.i(TAG, "shouldOverrideUrlLoading: ${request?.url}")
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.i(TAG, "onConsoleMessage: ${consoleMessage?.message()}")
                return super.onConsoleMessage(consoleMessage)
            }
        }
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
        }
        bindApi(webView, data)
        val pluginName = intent.getStringExtra("plugin-name")
        val file = File(filesDir, "plugins/$pluginName")
        val extracted = File(filesDir, "plugins/${file.nameWithoutExtension}")
        scope.launch {
            ensureExtract(extracted, file)
            val toString = Uri.fromFile(File(extracted, "index.html")).toString()
            webView.loadUrl(toString)
            Log.i(TAG, "onCreate: $toString")
        }
    }

    private suspend fun ensureExtract(extracted: File, file: File) {
        withContext(Dispatchers.IO) {
            ZipInputStream(FileInputStream(file)).use {
                val byteArray = ByteArray(1024)
                while (true) {
                    val nextEntry = it.nextEntry
                    if (nextEntry != null) {
                        val name = nextEntry.name
                        val fileOutputStream = FileOutputStream(File(extracted, name).ensureFile())
                        fileOutputStream.use { out ->
                            while (true) {
                                val read = it.read(byteArray)
                                if (read != -1) {
                                    out.write(byteArray, 0, read)
                                } else break
                            }
                        }
                        Log.i(TAG, "onCreate: $name ${nextEntry.isDirectory}")
                    } else break
                }
            }
        }
    }

    @Suppress("JavascriptInterface")
    private fun bindApi(webView: WebView, data: Uri?) {
        val pluginManager = object : GiantExplorerPluginManager {

            @JavascriptInterface
            override fun fileInputStream(path: String) = getFileInstance(path, this@WebViewPluginActivity).fileInputStream

            @JavascriptInterface
            override fun fileOutputStream(path: String) = getFileInstance(path, this@WebViewPluginActivity).fileOutputStream

            @JavascriptInterface
            override fun listFiles(path: String): List<String> {
                return getFileInstance(path, this@WebViewPluginActivity).list().let { filesAndDirectories ->
                    filesAndDirectories.files.map {
                        it.fullPath
                    } + filesAndDirectories.directories.map {
                        it.fullPath
                    }
                }
            }

            @JavascriptInterface
            fun base64(path: String, callbackId: String) {
                thread {
                    val readBytes = fileInputStream(path).readBytes()
                    val result = Base64.encodeToString(readBytes, Base64.NO_WRAP)
                    webView.post {
                        webView.callback(callbackId, "'$result'")
                    }
                }

            }

        }
        val f = object : WebViewFilePlugin {
            @JavascriptInterface
            override fun fullPath(): String {
                val u = data ?: return ""
                val (_, path) = FileSystemProviderResolver.resolve(u) ?: return ""
                return path
            }

            @JavascriptInterface
            override fun fileName(): String {
                return "not implemented"
            }
        }
        webView.addJavascriptInterface(pluginManager, "plugin")
        webView.addJavascriptInterface(f, "file")
    }

    companion object {
        private const val TAG = "WebViewPluginActivity"
    }

}

interface WebViewFilePlugin {
    fun fullPath(): String
    fun fileName(): String
}

fun WebView.callback(callbackId: String, parameters: String?) {
    evaluateJavascript("callback($callbackId, $parameters)") { }
}