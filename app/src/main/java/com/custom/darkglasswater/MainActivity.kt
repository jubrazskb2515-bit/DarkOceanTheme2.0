package com.custom.darkglasswater

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var btnOverlayPerm: Button
    private lateinit var btnStartService: Button
    private lateinit var btnTestClock: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#070B14"))
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(36), dp(20), dp(36))
        }

        // টাইটেল
        val titleText = TextView(this).apply {
            text = "DARK GLASS WATER"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00FFFF"))
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        }

        val subTitleText = TextView(this).apply {
            text = "লক স্ক্রিন ডাবল-ট্যাপ নিয়ন ওয়াটার ঘড়ি"
            textSize = 14f
            setTextColor(Color.parseColor("#B0C4DE"))
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(24))
        }

        // ১.৫ x ১.৫ ইঞ্চি লাইভ ঘড়ির প্রিভিউ
        val previewClock = LiquidNeonClockView(this)
        val previewParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(20)
        }

        // স্ট্যাটাস কার্ড
        statusText = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = createGlassCardDrawable("#121B2E", "#00FFFF")
        }
        val statusParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(20)
        }

        // বাটন ১: ওভারলে পারমিশন
        btnOverlayPerm = createNeonButton("১. ওভারলে পারমিশন দিন (Overlay)", "#FF00FF") {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "ওভারলে পারমিশন ইতিমধ্যে চালু আছে!", Toast.LENGTH_SHORT).show()
            }
        }

        // বাটন ২: লক স্ক্রিন সার্ভিস চালু
        btnStartService = createNeonButton("২. লক স্ক্রিন ডাবল-ট্যাপ চালু করুন", "#39FF14") {
            checkAndStartClockService()
        }

        // বাটন ৩: ঘড়ি টেস্ট করুন
        btnTestClock = createNeonButton("৩. এখনই ঘড়িটি টেস্ট করুন", "#00FFFF") {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "আগে ১ নম্বর বাটন থেকে ওভারলে পারমিশন দিন", Toast.LENGTH_LONG).show()
                return@createNeonButton
            }
            val intent = Intent(this, LockClockOverlayService::class.java).apply {
                action = "ACTION_TEST_CLOCK"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "স্ক্রিনের মাঝখানে ঘড়িটি দেখুন (ট্যাপ করলে হাইড হবে)", Toast.LENGTH_SHORT).show()
        }

        // নির্দেশনা টেক্সট
        val guideText = TextView(this).apply {
            text = "• ফোন লক করে স্ক্রিন অন করুন এবং স্ক্রিনের ওপর ডাবল ট্যাপ (Double Tap) করুন।\n" +
                    "• ফোন ডানে-বামে কাত করলে বক্সের ভেতরের রঙিন পানি নড়বে।\n" +
                    "• আনলক করতে সাধারণভাবে স্ক্রিন সোয়াইপ করুন।"
            textSize = 13f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = createGlassCardDrawable("#0D1322", "#334155")
        }
        val guideParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(12)
        }

        rootLayout.addView(titleText)
        rootLayout.addView(subTitleText)
        rootLayout.addView(previewClock, previewParams)
        rootLayout.addView(statusText, statusParams)
        rootLayout.addView(btnOverlayPerm)
        rootLayout.addView(btnStartService)
        rootLayout.addView(btnTestClock)
        rootLayout.addView(guideText, guideParams)

        scrollView.addView(rootLayout)
        setContentView(scrollView)

        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val hasOverlay = Settings.canDrawOverlays(this)
        if (hasOverlay) {
            statusText.text = "✅ ওভারলে পারমিশন চালু আছে!\nএখন ২ নম্বর বাটনে চাপ দিয়ে সার্ভিস চালু করুন।"
            statusText.background = createGlassCardDrawable("#0F291E", "#39FF14")
        } else {
            statusText.text = "⚠️ লক স্ক্রিনে ঘড়ি দেখানোর জন্য ওভারলে পারমিশন প্রয়োজন।\nনিচের ১ নম্বর বাটনে চাপ দিন।"
            statusText.background = createGlassCardDrawable("#2A121E", "#FF3131")
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun checkAndStartClockService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "আগে ১ নম্বর বাটন থেকে ওভারলে পারমিশন দিন!", Toast.LENGTH_LONG).show()
            return
        }
        val serviceIntent = Intent(this, LockClockOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        statusText.text = "🚀 লক স্ক্রিন নিয়ন ওয়াটার ঘড়ি চালু হয়েছে!\nফোন লক করে ডাবল ট্যাপ করে দেখুন।"
        statusText.background = createGlassCardDrawable("#0E2433", "#00FFFF")
        Toast.makeText(this, "লক স্ক্রিন ঘড়ি সার্ভিস চালু হয়েছে!", Toast.LENGTH_SHORT).show()
    }

    private fun createNeonButton(label: String, borderColorHex: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            isAllCaps = false
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = createGlassCardDrawable("#131C31", borderColorHex)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createGlassCardDrawable(fillHex: String, strokeHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(Color.parseColor(fillHex))
            setStroke(dp(2), Color.parseColor(strokeHex))
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
