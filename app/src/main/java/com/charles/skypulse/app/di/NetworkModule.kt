package com.charles.skypulse.app.di

import com.charles.skypulse.app.data.remote.AdsbDbApi
import com.charles.skypulse.app.data.remote.AdsbLolApi
import com.charles.skypulse.app.data.remote.ApiEndpoints
import com.charles.skypulse.app.data.remote.Fr24Api
import com.charles.skypulse.app.data.remote.OpenSkyApi
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.CertificatePinner
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
        // Allow many concurrent requests per host so airport route lookups (one per nearby
        // aircraft) fan out quickly.
        val dispatcher = okhttp3.Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 32
        }
        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // Identify the client to the open APIs, but don't clobber a per-request
                // User-Agent (FR24's feed needs a browser UA set via @Headers).
                val original = chain.request()
                val request = if (original.header("User-Agent") == null) {
                    original.newBuilder()
                        .header("User-Agent", "SkyPulse/1.0 (Android; open ADS-B client)")
                        .build()
                } else {
                    original
                }
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

    @Provides
    @Singleton
    @Fr24Retrofit
    fun provideFr24Retrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiEndpoints.FR24_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideFr24Api(@Fr24Retrofit retrofit: Retrofit): Fr24Api =
        retrofit.create(Fr24Api::class.java)

    @Provides
    @Singleton
    @GitHubRetrofit
    fun provideGitHubRetrofit(client: OkHttpClient, json: Json): Retrofit {
        // Certificate pinning: Sectigo intermediate (survives leaf rotation) + current leaf backup.
        // Pins match network_security_config.xml — update both when the intermediate rotates.
        val githubPinner = CertificatePinner.Builder()
            .add("api.github.com", "sha256/ZSagvDzjltLkewXEBuDxIzpW/dpVw1Juvvmd0hhkzdY=") // intermediate
            .add("api.github.com", "sha256/QVnLDkTvhX8bfBbaP6XeqWLCOja893s79lYfjQc/hWI=") // leaf backup
            .build()
        val githubClient = client.newBuilder()
            .certificatePinner(githubPinner)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Authorization", "Bearer ${com.charles.skypulse.app.BuildConfig.GITHUB_API_TOKEN}")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .build()
                chain.proceed(request)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(githubClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideGitHubApi(@GitHubRetrofit retrofit: Retrofit): com.charles.skypulse.app.data.remote.GitHubApiService =
        retrofit.create(com.charles.skypulse.app.data.remote.GitHubApiService::class.java)
}
