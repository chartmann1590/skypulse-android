package com.charles.skypulse.app.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AdsbRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenSkyRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AdsbDbRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Fr24Retrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubRetrofit
