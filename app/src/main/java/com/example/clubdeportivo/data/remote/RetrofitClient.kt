package com.example.clubdeportivo.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Punto único de conexión con la API. Cuando el backend esté desplegado,
 * cambia BASE_URL por la URL real (debe terminar en "/").
 *
 * 10.0.2.2 es la dirección que usa el EMULADOR de Android para referirse
 * al "localhost" de tu computadora (no funciona en un celular físico).
 */
object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:3000/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
