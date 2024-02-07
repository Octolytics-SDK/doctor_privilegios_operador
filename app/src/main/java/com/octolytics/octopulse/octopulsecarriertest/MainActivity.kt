package com.octolytics.octopulse.octopulsecarriertest

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.View
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkCarrierPrivileges()
    }

    override fun onResume() {
        super.onResume()
        checkCarrierPrivileges()
    }

    private fun checkCarrierPrivileges() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val textView = findViewById<TextView>(R.id.tv_privileges)

        if (telephonyManager?.hasCarrierPrivileges() == true) {
            textView.setTextColor(Color.GREEN)
            textView.text = "Tiene privilegios de operador"
            textView.visibility = View.VISIBLE
        } else {
            textView.setTextColor(Color.RED)
            textView.text = "No tiene privilegios de operador"
            textView.visibility = View.VISIBLE
        }
    }
}