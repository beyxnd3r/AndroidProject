package com.example.androidproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CalculatorActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private var firstNumber = ""
    private var secondNumber = ""
    private var operator = ""
    private var isResultShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        tvResult = findViewById(R.id.tvResult)


        findViewById<Button>(R.id.btnBackMain).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        val numberButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        for (id in numberButtons) {
            findViewById<Button>(id).setOnClickListener {
                onNumberClick((it as Button).text.toString())
            }
        }

        findViewById<Button>(R.id.btnPlus).setOnClickListener { onOperatorClick("+") }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { onOperatorClick("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperatorClick("*") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperatorClick("/") }
        findViewById<Button>(R.id.btnClear).setOnClickListener { clear() }
        findViewById<Button>(R.id.btnDeleteOne).setOnClickListener { deleteOne() }
        findViewById<Button>(R.id.btnEquals).setOnClickListener { calculate() }
        findViewById<Button>(R.id.btnDot).setOnClickListener { onDotClick() }
        findViewById<Button>(R.id.btnSign).setOnClickListener { toggleSign() }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { toPercent() }
    }

    private fun onNumberClick(num: String) {
        if (isResultShown) {
            clear()
            isResultShown = false
        }

        if (operator.isEmpty()) {
            firstNumber += num
            tvResult.text = firstNumber
        } else {
            secondNumber += num
            tvResult.text = secondNumber
        }
    }

    private fun onOperatorClick(op: String) {
        if (firstNumber.isEmpty()) return
        if (secondNumber.isNotEmpty()) calculateIntermediate()
        operator = op
        isResultShown = false
    }

    private fun calculateIntermediate() {
        val a = firstNumber.toDouble()
        val b = secondNumber.toDouble()
        val result = when (operator) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b != 0.0) a / b else Double.NaN
            else -> 0.0
        }
        firstNumber = if (result % 1 == 0.0) result.toInt().toString() else result.toString()
        secondNumber = ""
        tvResult.text = firstNumber
    }

    private fun onDotClick() {
        val num = if (operator.isEmpty()) firstNumber else secondNumber
        if (!num.contains(".")) {
            val updated = if (num.isEmpty()) "0." else "$num."
            if (operator.isEmpty()) firstNumber = updated else secondNumber = updated
            tvResult.text = updated
        }
    }

    private fun toggleSign() {
        if (operator.isEmpty()) {
            if (firstNumber.startsWith("-")) firstNumber = firstNumber.drop(1)
            else if (firstNumber.isNotEmpty()) firstNumber = "-$firstNumber"
            tvResult.text = firstNumber
        } else {
            if (secondNumber.startsWith("-")) secondNumber = secondNumber.drop(1)
            else if (secondNumber.isNotEmpty()) secondNumber = "-$secondNumber"
            tvResult.text = secondNumber
        }
    }

    private fun toPercent() {
        if (operator.isEmpty() && firstNumber.isNotEmpty()) {
            firstNumber = (firstNumber.toDouble() / 100).toString()
            tvResult.text = firstNumber
        } else if (secondNumber.isNotEmpty()) {
            secondNumber = (secondNumber.toDouble() / 100).toString()
            tvResult.text = secondNumber
        }
    }

    private fun calculate() {
        if (firstNumber.isEmpty() || operator.isEmpty() || secondNumber.isEmpty()) return
        calculateIntermediate()
        operator = ""
        isResultShown = true
    }

    private fun clear() {
        firstNumber = ""
        secondNumber = ""
        operator = ""
        tvResult.text = "0"
        isResultShown = false
    }

    private fun deleteOne() {
        if (operator.isEmpty()) {
            if (firstNumber.isNotEmpty()) {
                firstNumber = firstNumber.dropLast(1)
                tvResult.text = if (firstNumber.isEmpty()) "0" else firstNumber
            }
        } else {
            if (secondNumber.isNotEmpty()) {
                secondNumber = secondNumber.dropLast(1)
                tvResult.text = if (secondNumber.isEmpty()) "0" else secondNumber
            }
        }
    }
}
