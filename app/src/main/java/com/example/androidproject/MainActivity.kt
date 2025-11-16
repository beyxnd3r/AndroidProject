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

        btnCalc.setOnClickListener {
            val intent = Intent(this, CalculatorActivity::class.java)
            startActivity(intent)
        }

        btnMedia.setOnClickListener {
            val intent = Intent(this, MediaPlayerActivity::class.java)
            startActivity(intent)
        }
    }
}
