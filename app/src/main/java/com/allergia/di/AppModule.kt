package com.allergia.di

import android.content.Context
import androidx.room.Room
import com.allergia.api.GeminiService
import com.allergia.api.OpenRouterApi
import com.allergia.data.database.AppDatabase
import com.allergia.data.database.DiaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "allergia.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideDiaryDao(db: AppDatabase): DiaryDao = db.diaryDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenRouterApi(okHttpClient: OkHttpClient): OpenRouterApi {
        return Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenRouterApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiService(
        api: OpenRouterApi,
        @ApplicationContext context: Context
    ): GeminiService {
        return GeminiService(api, context)
    }

    @Provides
    @Singleton
    fun provideFoodPhotoService(
        api: OpenRouterApi,
        @ApplicationContext context: Context
    ): com.allergia.api.FoodPhotoService {
        return com.allergia.api.FoodPhotoService(api, context)
    }

    @Provides
    @Singleton
    fun provideLabelPhotoService(
        api: OpenRouterApi,
        @ApplicationContext context: Context
    ): com.allergia.api.LabelPhotoService {
        return com.allergia.api.LabelPhotoService(api, context)
    }
}
