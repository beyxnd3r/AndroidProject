package com.example.androidproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (!isTaskRoot) {
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val btnCalc = findViewById<Button>(R.id.btnOpenCalculator)
        val btnMedia = findViewById<Button>(R.id.btnOpenMedia)
        val btnLocation = findViewById<Button>(R.id.btnOpenLocation)
        btnCalc.setOnClickListener {
            val intent = Intent(this, CalculatorActivity::class.java)
            startActivity(intent)
        }

        btnMedia.setOnClickListener {
            val intent = Intent(this, MediaPlayerActivity::class.java)
            startActivity(intent)
        }

        btnLocation.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnZmq).setOnClickListener {
            startActivity(Intent(this, SocketsActivity::class.java))
        }
        findViewById<Button>(R.id.btnNetworkService).setOnClickListener {
            startActivity(Intent(this, NetworkMonitorActivity::class.java))
        }
    }
}
