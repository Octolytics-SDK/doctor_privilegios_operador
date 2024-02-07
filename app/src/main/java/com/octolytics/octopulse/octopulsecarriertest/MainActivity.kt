package com.octolytics.octopulse.octopulsecarriertest

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val telephonyManager = (getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)
        Toast.makeText(this, telephonyManager?.hasCarrierPrivileges().toString(), Toast.LENGTH_LONG)
    }
}