package com.example.reday.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // 실서버 전환 시 이 URL만 교체
    private val BASE_URL = if (android.os.Build.FINGERPRINT.contains("generic") ||
        android.os.Build.FINGERPRINT.contains("emulator") ||
        android.os.Build.MODEL.contains("Emulator") ||
        android.os.Build.MODEL.contains("Android SDK"))
        "http://10.0.2.2:8000/"
    else
        "http://172.30.1.88:8000/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val memoryApi: MemoryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MemoryApiService::class.java)
    }
}
