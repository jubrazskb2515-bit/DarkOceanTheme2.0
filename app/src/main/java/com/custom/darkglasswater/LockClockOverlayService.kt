package com.custom.darkglasswater

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import kotlin.math.abs

class LockClockOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var keyguardManager: KeyguardManager

    private var touchCatcherView: View? = null
    private var clockContainer: FrameLayout? = null
    private var isCatcherAdded = false
    private var isClockAdded = false

    private val handler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable {
        hideNeonClock()
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (keyguardManager.isKeyguardLocked) {
                        showTouchCatcher()
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    hideNeonClock()
                    removeTouchCatcher()
                }
                Intent.ACTION_USER_PRESENT -> {
                    hideNeonClock()
                    removeTouchCatcher()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        startForegroundNotification()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        if (keyguardManager.isKeyguardLocked) {
            showTouchCatcher()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_TEST_CLOCK") {
            showNeonClock()
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "dark_glass_lock_clock_channel"
        val channelName = "Dark Glass Lock Clock Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = notificationBuilder
            .setContentTitle("Dark Glass Water Clock")
            .setContentText("লক স্ক্রিনে ডাবল ট্যাপ ঘড়ি চালু আছে")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun showTouchCatcher() {
        if (isCatcherAdded) return

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f
        var downY = 0f

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isClockAdded) {
                    hideNeonClock()
                } else {
                    showNeonClock()
                }
                return true
            }
        })

        val view = View(this).apply {
            setBackgroundColor(0x00000000)
            setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = abs(event.rawX - downX)
                        val dy = abs(event.rawY - downY)
                        // ইউজার ফোন আনলক করতে সোয়াইপ করলে সাথে সাথে ক্যাচার সরে যাবে
                        if (dx > touchSlop * 2 || dy > touchSlop * 2) {
                            removeTouchCatcher()
                        }
                    }
                }
                true
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(view, params)
            touchCatcherView = view
            isCatcherAdded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeTouchCatcher() {
        if (!isCatcherAdded || touchCatcherView == null) return
        try {
            windowManager.removeView(touchCatcherView)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            touchCatcherView = null
            isCatcherAdded = false
        }
    }

    private fun showNeonClock() {
        handler.removeCallbacks(autoHideRunnable)
        if (isClockAdded) {
            handler.postDelayed(autoHideRunnable, 12000)
            return
        }

        val clockView = LiquidNeonClockView(this).apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            setOnClickListener {
                hideNeonClock()
            }
        }

        val container = FrameLayout(this).apply {
            addView(clockView)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(container, params)
            clockContainer = container
            isClockAdded = true

            clockView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(240)
                .start()

            // ১২ সেকেন্ড পর ব্যাটারি বাঁচাতে ঘড়িটি অটোমেটিক হাইড হবে
            handler.postDelayed(autoHideRunnable, 12000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideNeonClock() {
        handler.removeCallbacks(autoHideRunnable)
        if (!isClockAdded || clockContainer == null) return
        try {
            windowManager.removeView(clockContainer)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            clockContainer = null
            isClockAdded = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        hideNeonClock()
        removeTouchCatcher()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
