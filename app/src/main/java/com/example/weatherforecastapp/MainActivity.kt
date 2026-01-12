package com.example.weatherforecastapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: WeatherViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // UI Elements
    private lateinit var etCity: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnLocation: Button
    private lateinit var cardWeather: CardView
    private lateinit var tvCity: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var tvCondition: TextView
    private lateinit var tvFeelsLike: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvWindSpeed: TextView
    private lateinit var tvPressure: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize UI elements
        initViews()

        // Set up click listeners
        setupClickListeners()

        // Observe ViewModel data
        observeViewModel()

        // Request location permission
        requestLocationPermission()

        // Load default city weather on startup
        viewModel.fetchWeatherByCity("Nairobi")
    }

    private fun initViews() {
        etCity = findViewById(R.id.etCity)
        btnSearch = findViewById(R.id.btnSearch)
        btnLocation = findViewById(R.id.btnLocation)
        cardWeather = findViewById(R.id.cardWeather)
        tvCity = findViewById(R.id.tvCity)
        tvTemperature = findViewById(R.id.tvTemperature)
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon)
        tvCondition = findViewById(R.id.tvCondition)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvWindSpeed = findViewById(R.id.tvWindSpeed)
        tvPressure = findViewById(R.id.tvPressure)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
    }

    private fun setupClickListeners() {
        btnSearch.setOnClickListener {
            val city = etCity.text.toString().trim()
            if (city.isNotEmpty()) {
                viewModel.fetchWeatherByCity(city)
            } else {
                Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show()
            }
        }

        btnLocation.setOnClickListener {
            if (checkLocationPermission()) {
                getCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.weatherData.observe(this) { weather ->
            if (weather != null) {
                updateUI(weather)
                cardWeather.visibility = android.view.View.VISIBLE
                tvError.visibility = android.view.View.GONE
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            if (isLoading) {
                cardWeather.visibility = android.view.View.GONE
                tvError.visibility = android.view.View.GONE
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (error != null && error.isNotEmpty()) {
                tvError.text = error
                tvError.visibility = android.view.View.VISIBLE
                cardWeather.visibility = android.view.View.GONE
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(weather: WeatherResponse) {
        // City and Country
        tvCity.text = "${weather.cityName}, ${weather.sys.country}"

        // Temperature (rounded to 1 decimal)
        val temp = String.format("%.1f", weather.main.temperature)
        tvTemperature.text = "${temp}°C"

        // Weather Icon
        val iconUrl = "https://openweathermap.org/img/wn/${weather.weather[0].icon}@4x.png"
        Glide.with(this)
            .load(iconUrl)
            .into(ivWeatherIcon)

        // Weather Condition (capitalize first letter)
        val description = weather.weather[0].description
        tvCondition.text = description.replaceFirstChar { it.uppercase() }

        // Additional Details
        val feelsLike = String.format("%.1f", weather.main.feelsLike)
        tvFeelsLike.text = "${feelsLike}°C"
        tvHumidity.text = "${weather.main.humidity}%"
        tvWindSpeed.text = "${weather.wind.speed} m/s"
        tvPressure.text = "${weather.main.pressure} hPa"
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied. Please use city search.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    Toast.makeText(this, "Getting weather for your location...", Toast.LENGTH_SHORT).show()
                    viewModel.fetchWeatherByLocation(it.latitude, it.longitude)
                } ?: run {
                    Toast.makeText(this, "Unable to get current location. Try city search.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Location error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}