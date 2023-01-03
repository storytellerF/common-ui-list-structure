package com.storyteller_f.b3.api

import android.util.Log
import com.storyteller_f.b3.ui.login.GeetestB3Response
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

interface Common {
    @GET("x/passport-login/captcha?source=main_web")
    suspend fun requestGee(): GeetestB3Response

    companion object {
        private const val BASE_URL = "https://passport.bilibili.com/"

        fun create(): Common {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BASIC
            val myX509TrustManager = MyX509TrustManager()
            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(
                null,
                arrayOf(myX509TrustManager),
                SecureRandom()
            )

            val logInterceptor =
                HttpLoggingInterceptor(HttpLogger())
            logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder()
                .cookieJar(PersistenceCookieJar())
                .sslSocketFactory(sslContext.socketFactory, myX509TrustManager)
                .addInterceptor(logger)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(Common::class.java)
        }
    }
}

private class MyX509TrustManager : X509TrustManager {
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        for (certificate in chain) {
            try {
                certificate.checkValidity()
            } catch (e: CertificateExpiredException) {
                e.printStackTrace()
            } catch (e: CertificateNotYetValidException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
    }
}

private class PersistenceCookieJar : CookieJar {
    var cache: MutableList<Cookie> = ArrayList()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cache.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val invalidCookies: MutableList<Cookie> = ArrayList()
        val validCookies: MutableList<Cookie> = ArrayList()
        for (cookie in cache) {
            if (cookie.expiresAt < System.currentTimeMillis()) {
                invalidCookies.add(cookie)
            } else if (cookie.matches(url)) {
                validCookies.add(cookie)
            }
        }
        cache.removeAll(invalidCookies)
        return validCookies
    }
}

private class HttpLogger : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        Log.d("HttpLogger", "Logger: $message")
    }
}