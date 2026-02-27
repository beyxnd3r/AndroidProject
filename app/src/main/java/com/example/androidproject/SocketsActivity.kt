package com.example.androidproject

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.text.SimpleDateFormat
import java.util.*

class SocketsActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnSendLocation: Button

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var isSendingLocation = false


    private var zmqContext: ZContext? = null
    private var zmqSocket: ZMQ.Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sockets)

        tvStatus = findViewById(R.id.tvStatus)
        btnSendLocation = findViewById(R.id.btnSendLocation)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestPermissionsIfNeeded()
        initLocation()
        connectZmq()

        btnSendLocation.setOnClickListener {
            if (!isSendingLocation) startLocationUpdates()
            else stopLocationUpdates()
        }
    }

    private fun connectZmq() {
        try {
            zmqContext = ZContext()
            zmqSocket = zmqContext!!.createSocket(SocketType.REQ)
            zmqSocket!!.connect("tcp://172.29.145.106:5555")
        } catch (e: Exception) {
            zmqSocket = null
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }
    }

    private fun initLocation() {
        locationRequest = LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                sendLocationToServer(location)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        isSendingLocation = true
        btnSendLocation.text = "STOP"
        tvStatus.text = "Sending location..."

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        isSendingLocation = false
        btnSendLocation.text = "START"
        tvStatus.text = "Stopped"
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun sendLocationToServer(location: android.location.Location) {

        val time = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        val json = """
            {
              "type": "location",
              "latitude": ${location.latitude},
              "longitude": ${location.longitude},
              "altitude": ${location.altitude},
              "time": "$time"
            }
        """.trimIndent()

        Thread {
            try {
                if (zmqSocket == null) connectZmq()

                zmqSocket?.send(json.toByteArray(ZMQ.CHARSET))
                zmqSocket?.recv()

                handler.post {
                    tvStatus.text =
                        "Lat: ${location.latitude}\nLon: ${location.longitude}"
                }
            } catch (e: Exception) {
                zmqSocket?.close()
                zmqSocket = null
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        zmqSocket?.close()
        zmqContext?.close()
    }
}