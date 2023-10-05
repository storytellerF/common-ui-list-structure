package com.storyteller_f.giant_explorer.control.plugin

import android.annotation.SuppressLint
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.storyteller_f.common_ui.scope
import com.storyteller_f.file_system_ktx.ensureFile
import com.storyteller_f.giant_explorer.databinding.ActivityWebviewPluginBinding
import com.storyteller_f.giant_explorer.pluginManagerRegister
import com.storyteller_f.ui_list.event.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class WebViewPluginActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityWebviewPluginBinding::inflate)
    private val webView get() = binding.webView
    private val messageChannel by lazy {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL)) {
            WebViewCompat.createWebMessageChannel(webView)
        } else null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding
        val uriData = intent.data
        val pluginName = intent.getStringExtra("plugin-name")!!
        messageChannel
        bindApi(webView, uriData)
        scope.launch {
            val revolvePluginName = pluginManagerRegister.resolvePluginName(pluginName, this@WebViewPluginActivity) as HtmlPluginConfiguration
            setupWebView(revolvePluginName.extractedPath)
            val indexFile = File(revolvePluginName.extractedPath, "index.html")
            val toString = Uri.fromFile(indexFile).toString()
            webView.loadDataWithBaseURL(BASE_URL, indexFile.readText(), null, null, null)
            Log.i(TAG, "onCreate: $toString")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(extractedPath: String) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(webView, "test", setOf(BASE_URL)) { view, message, sourceOrigin, isMainFrame, replyProxy ->
                Log.d(TAG, "onCreate() called with: view = $view, message = ${message.data}, sourceOrigin = $sourceOrigin, isMainFrame = $isMainFrame, replyProxy = $replyProxy")
                replyProxy.postMessage("from android")
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                Log.i(TAG, "shouldOverrideUrlLoading: ${request?.url}")
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                Log.d(TAG, "shouldInterceptRequest() called with: request = ${request?.url}")
                if (request != null) {
                    val url = request.url
                    val path = url.path
                    if (url.toString().startsWith(BASE_URL) && path?.endsWith(".js") == true) {
                        return WebResourceResponse("text/javascript", "utf-8", FileInputStream(File(extractedPath, path)))
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                Log.d(TAG, "onReceivedHttpError() called with: view = $view, request = ${request?.url}, errorResponse = ${errorResponse?.reasonPhrase}")
                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.d(TAG, "onReceivedError() called with: view = $view, request = ${request?.url}, error = ${error?.description} ${error?.errorCode}")
                }
                super.onReceivedError(view, request, error)
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                Log.d(TAG, "onReceivedSslError() called with: view = $view, handler = $handler, error = $error")
                super.onReceivedSslError(view, handler, error)
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
    }

    @Suppress("JavascriptInterface")
    private fun bindApi(webView: WebView, data: Uri?) {
        val pluginManager = object {

            @JavascriptInterface
            fun base64(path: String, callbackId: String) {
                scope.launch {
                    val readBytes = fileInputStream1(path).readBytes()
                    val result = Base64.encodeToString(readBytes, Base64.NO_WRAP)
                    webView.post {
                        webView.callback(callbackId, "'$result'")
                        messageChannel?.let {
                            if (WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
                                val webMessageCompat = WebMessageCompat(result, it)
                                WebViewCompat.postWebMessage(webView, webMessageCompat, Uri.parse(BASE_URL))
                            }
                        }
                    }
                }

            }

        }
        val f = object : WebViewFilePlugin {
            @JavascriptInterface
            override fun fullPath(): String {
                val u = data ?: return ""
                return FileSystemProviderResolver.resolve(u)?.path.toString()
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
        const val BASE_URL = "http://www.example.com"

    }

}

interface WebViewFilePlugin {
    fun fullPath(): String
    fun fileName(): String
}

fun WebView.callback(callbackId: String, parameters: String?) {
    evaluateJavascript("callback($callbackId, $parameters)") { }
}

suspend fun File.ensureExtract(extracted: String) {
    withContext(Dispatchers.IO) {
        ZipInputStream(FileInputStream(this@ensureExtract)).use {
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
                } else break
            }
        }
    }
}