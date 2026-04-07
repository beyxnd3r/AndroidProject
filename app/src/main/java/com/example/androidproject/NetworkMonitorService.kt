package com.example.androidproject
import android.util.Log
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.telephony.*
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import org.json.JSONArray
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.text.SimpleDateFormat
import java.util.*

class NetworkMonitorService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var telephonyManager: TelephonyManager

    private var currentLocation: Location? = null

    private val serverAddress = "tcp://192.168.1.183:5556"

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

                    obj.put("asu", sig.asuLevel)
                    obj.put("cqi", sig.cqi)
                    obj.put("rsrp", sig.rsrp)
                    obj.put("rsrq", sig.rsrq)
                    obj.put("rssi", sig.rssi)
                    obj.put("rssnr", sig.rssnr)
                    obj.put("timingAdvance", sig.timingAdvance)

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

                    obj.put("dbm", sig.dbm)
                    obj.put("rssi", sig.rssi)
                    obj.put("timingAdvance", sig.timingAdvance)

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

                    obj.put("ssRsrp", sig.ssRsrp)
                    obj.put("ssRsrq", sig.ssRsrq)
                    obj.put("ssSinr", sig.ssSinr)

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

                socket.connect("tcp://192.168.1.183:5556")

                val sent = socket.send(json.toString(), 0)

                if (sent) {
                    val reply = socket.recvStr()
                    println("Server reply: $reply")
                }

                socket.close()
                context.close()

            } catch (e: Exception) {
                e.printStackTrace()
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