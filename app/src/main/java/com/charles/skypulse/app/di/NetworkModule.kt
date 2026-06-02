package com.charles.skypulse.app.di

import com.charles.skypulse.app.data.remote.AdsbDbApi
import com.charles.skypulse.app.data.remote.AdsbLolApi
import com.charles.skypulse.app.data.remote.ApiEndpoints
import com.charles.skypulse.app.data.remote.OpenSkyApi
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        // Allow more concurrent requests per host so airport route lookups fan out quickly.
        val dispatcher = okhttp3.Dispatcher().apply { maxRequestsPerHost = 12 }
        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // Identify the client politely to the open APIs.
                val request = chain.request().newBuilder()
                    .header("User-Agent", "SkyPulse/1.0 (Android; open ADS-B client)")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @AdsbRetrofit
    fun provideAdsbRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiEndpoints.ADSB_LOL_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @OpenSkyRetrofit
    fun provideOpenSkyRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiEndpoints.OPENSKY_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAdsbApi(@AdsbRetrofit retrofit: Retrofit): AdsbLolApi =
        retrofit.create(AdsbLolApi::class.java)

    @Provides
    @Singleton
    fun provideOpenSkyApi(@OpenSkyRetrofit retrofit: Retrofit): OpenSkyApi =
        retrofit.create(OpenSkyApi::class.java)

    @Provides
    @Singleton
    @AdsbDbRetrofit
    fun provideAdsbDbRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiEndpoints.ADSBDB_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAdsbDbApi(@AdsbDbRetrofit retrofit: Retrofit): AdsbDbApi =
        retrofit.create(AdsbDbApi::class.java)
}
