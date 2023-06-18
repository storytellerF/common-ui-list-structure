package com.storyteller_f.file_system_remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class WebDavDatum(
    var href: String = "",
    var name: String = "",
    var path: String = "",
    var lastModified: Long = 0L,
    var isHidden: Boolean = false,
    var isFile: Boolean = true
)

class WebDavClient(private val baseUrl: String, user: String, password: String) {

    private val client =
        OkHttpClient.Builder().addInterceptor(BasicAuthInterceptor(user, password)).build()

    suspend fun list(href: String): MutableList<WebDavDatum> {
        val requestBody = ByteArray(0).toRequestBody(null, 0, 0)

        val request: Request = Request.Builder()
            .url(baseUrl + href) // 文件夹的URL
            .method("PROPFIND", requestBody)
            .header("Depth", "1") // 设置请求头
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string().orEmpty()
                println(responseBody)
                parseWebDAVResponse(responseBody)
            } else {
                throw Exception("code ${response.code} message: ${response.message}")
            }
        }
    }


    private fun parseWebDAVResponse(responseBody: String): MutableList<WebDavDatum> {
        val resultList = mutableListOf<WebDavDatum>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val xpp = factory.newPullParser()
        xpp.setInput(StringReader(responseBody))
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        var eventType = xpp.eventType
        var node: WebDavDatum? = null
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (xpp.name) {
                        "response" -> {
                            node = WebDavDatum()
                        }

                        "href" -> {
                            val nextText = xpp.nextText()
                            val startsWith = nextText.startsWith(baseUrl)
                            node!!.href = if (startsWith) nextText else "$baseUrl$nextText"
                            node.path = if (startsWith) nextText.substring(baseUrl.length) else nextText
                        }

                        "getlastmodified" -> {
                            val date = dateFormat.parse(xpp.nextText())!!
                            node!!.lastModified = date.time
                        }

                        "displayname" -> {
                            node!!.name = xpp.nextText()
                        }

                        "ishidden" -> {
                            node!!.isHidden = xpp.nextText().toBoolean()
                        }

                        "isfolder"-> {
                            node!!.isFile = false
                        }
                        "collection" -> {
                            node!!.isFile = false
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (xpp.name == "response") {
                        resultList.add(node!!)
                        node = null
                    }
                }
            }

            eventType = xpp.next()
        }

        return resultList
    }
}

class BasicAuthInterceptor(private val username: String, private val password: String) :
    Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Authorization", Credentials.basic(username, password))
            .build()
        return chain.proceed(request)
    }
}