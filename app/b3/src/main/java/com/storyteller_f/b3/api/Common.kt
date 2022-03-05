package com.storyteller_f.b3.api

import com.storyteller_f.b3.GeeResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface Common {
    @GET("x/passport-login/captcha?source=main_web")
    fun requestGee(): GeeResponse

    companion object {
        private const val BASE_URL = "https://passport.bilibili.com/"

        fun create(): Common {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BASIC

            val client = OkHttpClient.Builder()
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