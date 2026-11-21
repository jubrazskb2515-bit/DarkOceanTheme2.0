package com.custom.darkglasswater

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.sin

class LiquidNeonClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var tiltAngle = 0f
    private var targetTiltAngle = 0f
    private var wavePhase = 0f
    private var colorIndex = 0

    // কড়া রোদেও স্পষ্ট দেখা যাবে এমন ১২টি উজ্জ্বল নিয়ন রঙ
    private val neonColors = intArrayOf(
        Color.parseColor("#00FFFF"), // ১. নিয়ন সায়ান
        Color.parseColor("#FF00FF"), // ২. নিয়ন ম্যাজেন্টা
        Color.parseColor("#39FF14"), // ৩. নিয়ন গ্রিন
        Color.parseColor("#FFFF00"), // ৪. নিয়ন ইয়েলো
        Color.parseColor("#FF5F1F"), // ৫. নিয়ন অরেঞ্জ
        Color.parseColor("#00BFFF"), // ৬. ইলেকট্রিক ব্লু
        Color.parseColor("#FF1493"), // ৭. নিয়ন ডিপ পিঙ্ক
        Color.parseColor("#BF00FF"), // ৮. ইলেকট্রিক পার্পল
        Color.parseColor("#00FF7F"), // ৯. নিয়ন স্প্রিং গ্রিন
        Color.parseColor("#FF3131"), // ১০. নিয়ন রেড
        Color.parseColor("#7DF9FF"), // ১১. ইলেকট্রিক আইস ব্লু
        Color.parseColor("#FFD700")  // ১২. উজ্জ্বল গোল্ডেন নিয়ন
    )

    private val glassBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(185, 10, 14, 24)
        style = Paint.Style.FILL
    }

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2.8f)
    }

    private val glassHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.2f)
        color = Color.argb(110, 255, 255, 255)
    }

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = dpToPx(28f)
    }

    private val secondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = dpToPx(13f)
    }

    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = dpToPx(10.5f)
        color = Color.WHITE
    }

    private val clipPath = Path()
    private val wavePath = Path()
    private val boxRect = RectF()
    private val highlightRect = RectF()

    private val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
    private val secAmPmFormat = SimpleDateFormat("ss a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())

    private val handler = Handler(Looper.getMainLooper())
    private var lastSecond = -1

    private val animationRunnable = object : Runnable {
        override fun run() {
            wavePhase += 0.14f
            if (wavePhase > 2 * PI) wavePhase -= (2 * PI).toFloat()

            tiltAngle += (targetTiltAngle - tiltAngle) * 0.12f

            val currentSecond = Calendar.getInstance().get(Calendar.SECOND)
            if (currentSecond != lastSecond) {
                lastSecond = currentSecond
                colorIndex = (colorIndex + 1) % neonColors.size
            }

            invalidate()
            handler.postDelayed(this, 30)
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        handler.post(animationRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(animationRunnable)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            targetTiltAngle = (x * 4.5f).coerceIn(-35f, 35f)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // ঠিক ১.৫ × ১.৫ ইঞ্চি সাইজ নির্ধারণ
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_IN,
            1.5f,
            resources.displayMetrics
        ).toInt()
        setMeasuredDimension(sizePx, sizePx)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val padding = dpToPx(4f)
        val cornerRadius = dpToPx(24f)
        val activeColor = neonColors[colorIndex]

        boxRect.set(padding, padding, w - padding, h - padding)

        // ১. রাউন্ডেড কাঁচের বক্স ক্লিপিং
        clipPath.reset()
        clipPath.addRoundRect(boxRect, cornerRadius, cornerRadius, Path.Direction.CW)

        canvas.save()
        canvas.clipPath(clipPath)

        // ২. ডার্ক গ্লাস ব্যাকগ্রাউন্ড
        canvas.drawRect(boxRect, glassBgPaint)

        // ৩. ফোন কাত করলে নড়াচড়া করা তরল পানির ঢেউ
        canvas.save()
        canvas.rotate(tiltAngle, w / 2f, h / 2f)

        val waterLevel = h * 0.56f
        val waveAmplitude = dpToPx(5.5f)
        wavePath.reset()
        wavePath.moveTo(-w, h * 1.5f)
        wavePath.lineTo(-w, waterLevel)

        var x = -w
        while (x <= w * 2f) {
            val y = waterLevel + (waveAmplitude * sin((x / (w * 0.45f)) + wavePhase)).toFloat()
            wavePath.lineTo(x, y)
            x += 8f
        }

        wavePath.lineTo(w * 2f, h * 1.5f)
        wavePath.close()

        waterPaint.color = Color.argb(
            135,
            Color.red(activeColor),
            Color.green(activeColor),
            Color.blue(activeColor)
        )
        canvas.drawPath(wavePath, waterPaint)
        canvas.restore()
        canvas.restore()

        // ৪. উজ্জ্বল নিয়ন আউটার গ্লো বর্ডার
        borderPaint.color = activeColor
        borderPaint.setShadowLayer(dpToPx(8f), 0f, 0f, activeColor)
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, borderPaint)

        // কাঁচের ভেতরের হালকা সাদা রিফ্লেকশন
        highlightRect.set(
            padding + dpToPx(2f),
            padding + dpToPx(2f),
            w - padding - dpToPx(2f),
            h - padding - dpToPx(2f)
        )
        canvas.drawRoundRect(highlightRect, cornerRadius - dpToPx(2f), cornerRadius - dpToPx(2f), glassHighlightPaint)

        // ৫. কড়া রোদেও স্পষ্ট দেখা যাওয়া উজ্জ্বল ঘড়ি ও তারিখ
        val now = Date()
        val timeText = timeFormat.format(now)
        val secText = secAmPmFormat.format(now).uppercase(Locale.getDefault())
        val dateText = dateFormat.format(now)

        timePaint.color = Color.WHITE
        timePaint.setShadowLayer(dpToPx(10f), 0f, 0f, activeColor)
        canvas.drawText(timeText, w / 2f, h * 0.45f, timePaint)

        secondPaint.color = activeColor
        secondPaint.setShadowLayer(dpToPx(6f), 0f, 0f, Color.BLACK)
        canvas.drawText(secText, w / 2f, h * 0.63f, secondPaint)

        datePaint.setShadowLayer(dpToPx(5f), 0f, 0f, Color.BLACK)
        canvas.drawText(dateText, w / 2f, h * 0.80f, datePaint)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
