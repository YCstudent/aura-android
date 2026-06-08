package com.edistrive.aura.data.network

import com.edistrive.aura.data.local.AuthPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val BASE_URL = "https://zgjcyl.com/api/"
    
    @Provides
    @Singleton
    fun provideAuthInterceptor(authPreferences: AuthPreferences): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val token = authPreferences.getToken()
            
            val newRequest = if (token != null) {
                request.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                request
            }
            
            chain.proceed(newRequest)
        }
    }
    
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(retryInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(
                okhttp3.ConnectionPool(
                    maxIdleConnections = 5,
                    keepAliveDuration = 30,
                    TimeUnit.SECONDS
                )
            )
            .build()
    }

    /**
     * 连接中断重试拦截器 — "Software caused connection abort" 时自动重试一次。
     */
    private fun retryInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            try {
                chain.proceed(request)
            } catch (e: java.net.SocketException) {
                // 连接被服务端断开，重试一次
                if (e.message?.contains("abort") == true || e.message?.contains("reset") == true) {
                    chain.proceed(request)
                } else {
                    throw e
                }
            } catch (e: java.io.IOException) {
                if (e.message?.contains("Software caused connection abort") == true) {
                    chain.proceed(request)
                } else {
                    throw e
                }
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
