package com.storyteller_f.b3.api

import com.storyteller_f.b3.ui.login.GeetestResult
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName
import com.storyteller_f.b3.ui.login.geetest_url
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


interface GeetestAPI {
    @POST("/validate-slide")
    suspend fun invalidate(@Query("t") current: Long, @Body geetest: GeetestResult): GeetestEnd

    companion object {
        private const val BASE_URL = geetest_url

        fun create(): GeetestAPI {
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
                .create(GeetestAPI::class.java)
        }
    }
}

data class GeetestEnd(
    @SerializedName("info")
    val info: String? = null,
    @SerializedName("status")
    val status: String? = null
)