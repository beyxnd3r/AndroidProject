package com.example.androidproject

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.telephony.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import org.json.JSONArray
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.text.SimpleDateFormat
import java.util.*

class NetworkMonitorService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var telephonyManager: TelephonyManager

    private var currentLocation: Location? = null

    
    private val serverAddress = "tcp://192.168.1.183:5555"

    override fun onCreate() {
        super.onCreate()

        startForegroundNotification()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }



    private fun safe(value: Int): Int {
        return if (value == Int.MAX_VALUE) 0 else value
    }



    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            currentLocation = result.lastLocation
            collectAndSendData()
        }
    }

    private fun startLocationUpdates() {

        val request = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }



    private fun collectAndSendData() {

        val mainJson = JSONObject()
        val networksArray = JSONArray()

        telephonyManager.allCellInfo?.forEach { cell ->

            when (cell) {

                is CellInfoLte -> {
                    val obj = JSONObject()
                    val id = cell.cellIdentity
                    val sig = cell.cellSignalStrength

                    obj.put("type", "LTE")
                    obj.put("ci", id.ci)
                    obj.put("earfcn", id.earfcn)
                    obj.put("mcc", id.mccString)
                    obj.put("mnc", id.mncString)
                    obj.put("pci", id.pci)
                    obj.put("tac", id.tac)

                    obj.put("asu", safe(sig.asuLevel))
                    obj.put("cqi", safe(sig.cqi))
                    obj.put("rsrp", safe(sig.rsrp))
                    obj.put("rsrq", safe(sig.rsrq))
                    obj.put("rssi", safe(sig.rssi))
                    obj.put("rssnr", safe(sig.rssnr))
                    obj.put("timingAdvance", safe(sig.timingAdvance))

                    networksArray.put(obj)
                }

                is CellInfoGsm -> {
                    val obj = JSONObject()
                    val id = cell.cellIdentity
                    val sig = cell.cellSignalStrength

                    obj.put("type", "GSM")
                    obj.put("cid", id.cid)
                    obj.put("lac", id.lac)
                    obj.put("arfcn", id.arfcn)
                    obj.put("mcc", id.mccString)
                    obj.put("mnc", id.mncString)

                    obj.put("dbm", safe(sig.dbm))
                    obj.put("rssi", safe(sig.rssi))
                    obj.put("timingAdvance", safe(sig.timingAdvance))

                    networksArray.put(obj)
                }

                is CellInfoNr -> {
                    val obj = JSONObject()
                    val id = cell.cellIdentity as CellIdentityNr
                    val sig = cell.cellSignalStrength as CellSignalStrengthNr

                    obj.put("type", "NR")
                    obj.put("nci", id.nci)
                    obj.put("pci", id.pci)
                    obj.put("tac", id.tac)
                    obj.put("mcc", id.mccString)
                    obj.put("mnc", id.mncString)
                    obj.put("nrarfcn", id.nrarfcn)

                    obj.put("ssRsrp", safe(sig.ssRsrp))
                    obj.put("ssRsrq", safe(sig.ssRsrq))


                    obj.put("rssnr", safe(sig.ssSinr))

                    networksArray.put(obj)
                }
            }
        }

        currentLocation?.let {
            mainJson.put("latitude", it.latitude)
            mainJson.put("longitude", it.longitude)
            mainJson.put("altitude", it.altitude)
            mainJson.put("accuracy", it.accuracy)
        }

        mainJson.put(
            "time",
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )

        mainJson.put("networks", networksArray)

        sendToServer(mainJson)
    }



    private fun sendToServer(json: JSONObject) {
        Thread {
            try {
                val context = ZContext()
                val socket = context.createSocket(SocketType.REQ)

                socket.connect(serverAddress)

                val message = json.toString()

                Log.d("ZMQ", "Sending: $message")

                socket.send(message)

                val reply = socket.recvStr()

                Log.d("ZMQ", "Reply: $reply")

                socket.close()
                context.close()

            } catch (e: Exception) {
                Log.e("ZMQ", "Error: ${e.message}")
            }
        }.start()
    }



    private fun startForegroundNotification() {

        val channelId = "NetworkMonitorChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Network Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Network Monitor Running")
            .setContentText("Collecting LTE/GSM/NR + GPS data")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)
    }
}