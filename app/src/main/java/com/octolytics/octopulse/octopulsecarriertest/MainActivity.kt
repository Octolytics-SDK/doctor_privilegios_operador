package com.octolytics.octopulse.octopulsecarriertest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val OPERATOR_NAME = "Telefónica"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updatePermissionStatusUI()
        if (isGranted) {
            checkCarrierPrivileges()
        } else {
            showPermissionDeniedState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupOperatorName()
        setupPermissionButton()
        checkPermissionAndProceed()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatusUI()
        if (hasReadPhoneStatePermission()) {
            checkCarrierPrivileges()
        }
    }

    private fun setupOperatorName() {
        findViewById<TextView>(R.id.tv_operator_name).text = OPERATOR_NAME
    }

    private fun setupPermissionButton() {
        findViewById<Button>(R.id.btn_request_permission).setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    private fun updatePermissionStatusUI() {
        val hasPermission = hasReadPhoneStatePermission()
        val iconPermission = findViewById<ImageView>(R.id.icon_permission)
        val tvPermissionStatus = findViewById<TextView>(R.id.tv_permission_status)
        val btnRequestPermission = findViewById<Button>(R.id.btn_request_permission)

        if (hasPermission) {
            iconPermission.setImageResource(R.drawable.ic_check)
            iconPermission.setColorFilter(Color.parseColor("#4CAF50"))
            tvPermissionStatus.text = "Concedido"
            tvPermissionStatus.setTextColor(Color.parseColor("#4CAF50"))
            btnRequestPermission.isVisible = false
        } else {
            iconPermission.setImageResource(R.drawable.ic_close)
            iconPermission.setColorFilter(Color.parseColor("#F44336"))
            tvPermissionStatus.text = "No concedido"
            tvPermissionStatus.setTextColor(Color.parseColor("#F44336"))
            btnRequestPermission.isVisible = true
        }
    }

    private fun hasReadPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermissionAndProceed() {
        when {
            hasReadPhoneStatePermission() -> {
                checkCarrierPrivileges()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE) -> {
                showPermissionRationaleDialog()
            }
            else -> {
                showPermissionRationaleDialog()
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso requerido")
            .setMessage(
                "Necesitamos el permiso de lectura del estado del teléfono (READ_PHONE_STATE) " +
                "para poder verificar la información de las tarjetas SIM instaladas y " +
                "determinar si la aplicación tiene privilegios de operador en cada una de ellas."
            )
            .setPositiveButton("Conceder permiso") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                showPermissionDeniedState()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedState() {
        val container = findViewById<LinearLayout>(R.id.container_slots)
        val buttonView = findViewById<Button>(R.id.btn_refresh)

        buttonView.setOnClickListener {
            checkPermissionAndProceed()
        }

        container.removeAllViews()
        container.addView(
            createSlotCard(
                slotIndex = -1,
                title = "Permiso denegado",
                carrierName = null,
                subtitle = "No se puede acceder a la información de las SIMs sin el permiso requerido",
                hasPrivileges = false,
                isEmpty = true
            )
        )
    }

    private fun checkCarrierPrivileges() {
        val container = findViewById<LinearLayout>(R.id.container_slots)
        val buttonView = findViewById<Button>(R.id.btn_refresh)

        buttonView.setOnClickListener {
            refresh()
        }

        container.removeAllViews()

        val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        val activeSubscriptions = try {
            subscriptionManager?.activeSubscriptionInfoList ?: emptyList()
        } catch (e: SecurityException) {
            Log.e("AppTest", "Permission denied for subscription info", e)
            emptyList()
        }

        val slotCount = telephonyManager?.phoneCount ?: 2

        Log.d("AppTest", "Phone slot count: $slotCount")
        Log.d("AppTest", "Active subscriptions: ${activeSubscriptions.size}")

        if (activeSubscriptions.isEmpty()) {
            container.addView(createSlotCard(-1, "Sin SIMs", null, "No hay tarjetas SIM insertadas", false, true))
            return
        }

        for (slotIndex in 0 until slotCount) {
            val subscriptionInfo = activeSubscriptions.find { it.simSlotIndex == slotIndex }

            if (subscriptionInfo != null) {
                val displayName = subscriptionInfo.displayName?.toString() ?: "SIM desconocida"
                val carrierName = subscriptionInfo.carrierName?.toString() ?: ""
                val subscriptionId = subscriptionInfo.subscriptionId

                val slotTelephonyManager = telephonyManager?.createForSubscriptionId(subscriptionId)
                val hasPrivileges = slotTelephonyManager?.hasCarrierPrivileges() == true

                Log.d("AppTest", "Slot $slotIndex: $displayName ($carrierName), subId: $subscriptionId, privileges: $hasPrivileges")

                container.addView(createSlotCard(slotIndex, displayName, carrierName, null, hasPrivileges, false))
            } else {
                Log.d("AppTest", "Slot $slotIndex: Empty")
                container.addView(createSlotCard(slotIndex, "Vacío", null, "Sin SIM insertada", false, true))
            }
        }
    }

    private fun createSlotCard(
        slotIndex: Int,
        title: String,
        carrierName: String?,
        subtitle: String?,
        hasPrivileges: Boolean,
        isEmpty: Boolean
    ): CardView {
        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(10, 5, 10, 24)
            }
            radius = 12f * resources.displayMetrics.density
            cardElevation = 8f
            setContentPadding(32, 32, 32, 32)
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                setMargins(0, 0, 24, 0)
            }
            when {
                isEmpty -> {
                    setImageResource(R.drawable.ic_close)
                    setColorFilter(Color.GRAY)
                }
                hasPrivileges -> {
                    setImageResource(R.drawable.ic_check)
                    setColorFilter(Color.parseColor("#4CAF50"))
                }
                else -> {
                    setImageResource(R.drawable.ic_close)
                    setColorFilter(Color.parseColor("#F44336"))
                }
            }
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val slotLabel = if (slotIndex >= 0) "Slot ${slotIndex + 1}" else ""
        val titleText = TextView(this).apply {
            text = if (slotLabel.isNotEmpty()) "$slotLabel: $title" else title
            textSize = 18f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }

        val carrierText = TextView(this).apply {
            text = carrierName ?: ""
            textSize = 13f
            setTextColor(Color.DKGRAY)
            visibility = if (carrierName.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        }

        val statusText = TextView(this).apply {
            text = when {
                subtitle != null -> subtitle
                hasPrivileges -> "✓ Tiene privilegios de operador"
                else -> "✗ No tiene privilegios de operador"
            }
            textSize = 14f
            setTextColor(
                when {
                    isEmpty -> Color.GRAY
                    hasPrivileges -> Color.parseColor("#4CAF50")
                    else -> Color.parseColor("#F44336")
                }
            )
        }

        textLayout.addView(titleText)
        textLayout.addView(carrierText)
        textLayout.addView(statusText)

        contentLayout.addView(icon)
        contentLayout.addView(textLayout)

        card.addView(contentLayout)

        return card
    }

    private fun refresh() {
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        val container = findViewById<LinearLayout>(R.id.container_slots)

        lifecycleScope.launch(Dispatchers.Main) {
            runCatching {
                container.isVisible = false
                progressBar.isVisible = true
                delay(1000)
                progressBar.isVisible = false
                container.isVisible = true
                updatePermissionStatusUI()
                if (hasReadPhoneStatePermission()) {
                    checkCarrierPrivileges()
                } else {
                    showPermissionDeniedState()
                }
            }
        }
    }
}