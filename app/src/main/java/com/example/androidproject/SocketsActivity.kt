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
    private lateinit var btnSendHello: Button
    private lateinit var btnSendLocation: Button
    private lateinit var btnBack: Button

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var isSendingLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sockets)

        tvStatus = findViewById(R.id.tvStatus)
        btnSendHello = findViewById(R.id.btnSendHello)
        btnSendLocation = findViewById(R.id.btnSendLocation)
        btnBack = findViewById(R.id.btnBackMain)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestPermissionsIfNeeded()
        initLocation()

        btnSendHello.setOnClickListener {
            Thread { sendHello() }.start()
        }

        btnSendLocation.setOnClickListener {
            if (!isSendingLocation) startLocationUpdates()
            else stopLocationUpdates()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }



    private fun requestPermissionsIfNeeded() {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
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
            smallestDisplacement = 0f
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                Log.d("GPS", "lat=${location.latitude}, lon=${location.longitude}")
                sendLocationToServer(location)
            }
        }
    }



    private fun sendHello() {
        val context = ZContext()
        val socket = context.createSocket(SocketType.REQ)

        try {
            socket.connect("tcp://10.0.2.2:5555")

            val json = """
                {
                  "type": "hello",
                  "message": "Hello from Android!"
                }
            """.trimIndent()

            socket.send(json.toByteArray(ZMQ.CHARSET))
            val reply = socket.recv()

            handler.post {
                tvStatus.text = "Ответ сервера: ${String(reply)}"
            }
        } catch (e: Exception) {
            handler.post {
                tvStatus.text = "Ошибка Hello: ${e.message}"
            }
        } finally {
            socket.close()
            context.close()
        }
    }



    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        isSendingLocation = true
        btnSendLocation.text = "Остановить отправку"
        tvStatus.text = "Передача местоположения запущена"

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        isSendingLocation = false
        btnSendLocation.text = "Отправить локацию на сервер"
        tvStatus.text = "Передача местоположения остановлена"

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
            val context = ZContext()
            val socket = context.createSocket(SocketType.REQ)

            try {
                socket.connect("tcp://10.0.2.2:5555")
                socket.send(json.toByteArray(ZMQ.CHARSET))
                socket.recv()

                handler.post {
                    tvStatus.text =
                        "Location:\n${location.latitude}, ${location.longitude}"
                }
            } finally {
                socket.close()
                context.close()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}
