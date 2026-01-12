package com.example.weatherforecastapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.lang.Exception

class WeatherViewModel : ViewModel() {

    private val _weatherData = MutableLiveData<WeatherResponse>()
    val weatherData: LiveData<WeatherResponse> get() = _weatherData

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading


    private val apiKey = "0686c5e63e186e22a8350bfa9b835673"

    fun fetchWeatherByCity(city: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getWeatherData(city, apiKey)
                _weatherData.value = response
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message ?: "City not found or network error"}"
                _isLoading.value = false
            }
        }
    }

    fun fetchWeatherByLocation(lat: Double, lon: Double) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getWeatherByCoordinates(lat, lon, apiKey)
                _weatherData.value = response
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message ?: "Location error"}"
                _isLoading.value = false
            }
        }
    }
}