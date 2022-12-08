package com.example.example3

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LocationService: Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start(){
        val notification = NotificationCompat.Builder(this,"location")
            .setContentTitle("Tracking location...")
            .setContentText("Location: null ")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        locationClient
            .getLocationUpdates(10000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                val lat = location.latitude.toString()//.takeLast(3)
                val long = location.longitude.toString()//.takeLast(3)
                println("this is my new location: $lat , $long")
                postDataVolley(lat = lat, lng = long, id = "3")
                val updateNotification = notification.setContentText(
                    "Location ($lat, $long)"
                )

                notificationManager.notify(1, updateNotification.build())
            }.launchIn(serviceScope)

        startForeground(1, notification.build())
    }

    private fun stop(){
        stopForeground(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    private fun postDataVolley(lat: String, lng: String, id: String){
        println("$lat, $lng")
        val queue = Volley.newRequestQueue(this)
        val url = "http://traccarws.traxi.mx/?id=${id}&lat=${lat}&lon=${lng}&hdop=3&course=0"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(
            Request.Method.POST,
            url,
            { response ->
                // Display the first 500 characters of the response string.
                //val text = "Response is: ${response.substring(0, 500)}"
                println("response custom $response")
            },
            {
                //val text = "That didn't work!"
                error -> println("traccar $error")
            })
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }
}