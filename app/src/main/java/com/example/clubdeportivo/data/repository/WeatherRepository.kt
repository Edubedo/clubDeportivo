package com.example.clubdeportivo.data.repository

import com.example.clubdeportivo.data.model.WeatherData
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

interface WeatherRepository {
    suspend fun obtenerClimaActual(): WeatherData
}

class OpenMeteoWeatherRepository(
    private val latitude: Double = CLUB_LATITUDE,
    private val longitude: Double = CLUB_LONGITUDE
) : WeatherRepository {
    override suspend fun obtenerClimaActual(): WeatherData = withContext(Dispatchers.IO) {
        val latitud = String.format(Locale.US, "%.6f", latitude)
        val longitud = String.format(Locale.US, "%.6f", longitude)
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitud&longitude=$longitud" +
                "&current=temperature_2m,weather_code&timezone=auto"
        )
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"

        try {
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                throw IllegalStateException("Weather service returned HTTP $statusCode")
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(response).getJSONObject("current")
            WeatherData(
                temperatureCelsius = current.getDouble("temperature_2m"),
                weatherCode = current.getInt("weather_code")
            )
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CLUB_LATITUDE = 19.110833
        const val CLUB_LONGITUDE = -104.327778
    }
}
