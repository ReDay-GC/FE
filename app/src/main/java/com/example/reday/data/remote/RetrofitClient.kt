package com.example.reday.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val SPRING_BASE_URL = "http://43.203.100.198:8080"
    private val AI_BASE_URL = "http://reday-dev.duckdns.org:8000/"

    var accessToken: String? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Spring Boot 서버용 클라이언트 (auth 토큰 포함)
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .apply { accessToken?.let { addHeader("Authorization", "Bearer $it") } }
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // Python AI 서버용 클라이언트 (토큰 불필요)
    private val aiClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SPRING_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val aiRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AI_BASE_URL)
            .client(aiClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val memoryApi: MemoryApiService by lazy {
        aiRetrofit.create(MemoryApiService::class.java)
    }

    val authApi: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val userApi: UserApiService by lazy {
        retrofit.create(UserApiService::class.java)
    }

    val notificationApi: NotificationApiService by lazy {
        retrofit.create(NotificationApiService::class.java)
    }

    val noticeApi: NoticeApiService by lazy {
        retrofit.create(NoticeApiService::class.java)
    }

    val inquiryApi: InquiryApiService by lazy {
        retrofit.create(InquiryApiService::class.java)
    }

    val recordApi: RecordApiService by lazy {
        retrofit.create(RecordApiService::class.java)
    }

    val springMemoryApi: SpringMemoryApiService by lazy {
        retrofit.create(SpringMemoryApiService::class.java)
    }
}
