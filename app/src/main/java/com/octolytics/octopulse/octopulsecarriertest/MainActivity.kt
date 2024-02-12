package com.octolytics.octopulse.octopulsecarriertest

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.TextPaint
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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
        val iconView = findViewById<ImageView>(R.id.icon_privileges)

        if (telephonyManager?.hasCarrierPrivileges() == true) {
            textView.setTextColor(Color.GREEN)
            textView.text = "Tiene privilegios de operador"
            iconView.setImageResource(R.drawable.ic_check)
            iconView.setColorFilter(Color.GREEN)
        } else {
            textView.setTextColor(Color.RED)
            textView.text = "No tiene privilegios de operador"
            iconView.setImageResource(R.drawable.ic_close)
            iconView.setColorFilter(Color.RED)
        }

        textView.visibility = View.VISIBLE
        iconView.visibility = View.VISIBLE
        addTextBorder(textView)
    }

    private fun addTextBorder(textView: TextView) {
        val textPaint: TextPaint = textView.paint
        textPaint.strokeWidth = 1f
        textView.clipToOutline = true
        textPaint.setShadowLayer(6f, 0f, 0f, Color.BLACK)
    }
}