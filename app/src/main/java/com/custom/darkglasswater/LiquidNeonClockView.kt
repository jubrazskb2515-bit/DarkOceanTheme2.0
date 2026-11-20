package com.custom.darkglasswater

import android.content.Context
import android.graphics.*
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
) : View(context, attrs, defStyleAttr) {

    // ১২টি উচ্চ-উজ্জ্বলতার নিয়ন রঙ (কড়া রোদেও স্পষ্ট দেখার জন্য)
    private val neonColors = intArrayOf(
        Color.parseColor("#00FFFF"), // Electric Cyan
        Color.parseColor("#FF00FF"), // Neon Magenta
        Color.parseColor("#39FF14"), // Neon Lime Green
        Color.parseColor("#FFFF00"), // Bright Neon Yellow
        Color.parseColor("#FF3131"), // Neon Red
        Color.parseColor("#FF5F1F"), // Neon Orange
        Color.parseColor("#00BFFF"), // Deep Sky Neon
        Color.parseColor("#BF00FF"), // Electric Purple
        Color.parseColor("#00FF7F"), // Spring Neon Green
        Color.parseColor("#FF1493"), // Deep Neon Pink
        Color.parseColor("#7DF9FF"), // Electric Ice Blue
        Color.parseColor("#FFFFFF")  // Pure High-Contrast White
    )

    private var currentColorIndex = 0
    private var wavePhase = 0f

    private val glassBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#D9080C14") // ৮৫% গাঢ় ডার্ক গ্লাস
    }

    private val glassHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val neonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2.5f)
    }

    private val neonBorderGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(6f)
        maskFilter = BlurMaskFilter(dpToPx(8f), BlurMaskFilter.Blur.NORMAL)
    }

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = spToPx(34f)
        color = Color.WHITE
    }

    private val textGlowPaint = Paint(textPaint).apply {
        maskFilter = BlurMaskFilter(dpToPx(10f), BlurMaskFilter.Blur.NORMAL)
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textSize = spToPx(13f)
    }

    private val boxRect = RectF()
    private val clipPath = Path()
    private val wavePath = Path()

    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
    private val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
    private val secFormat = SimpleDateFormat("ss", Locale.getDefault())

    private val animator = object : Runnable {
        override fun run() {
            wavePhase += 0.12f
            if (wavePhase > PI * 2) {
                wavePhase -= (PI * 2).toFloat()
            }
            invalidate()
            handler.postDelayed(this, 32)
        }
    }

    private val colorCycler = object : Runnable {
        override fun run() {
            currentColorIndex = (currentColorIndex + 1) % neonColors.size
            updateColors()
            handler.postDelayed(this, 3000) // প্রতি ৩ সেকেন্ডে রঙ পরিবর্তন
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        updateColors()
    }

    private fun updateColors() {
        val activeColor = neonColors[currentColorIndex]
        val nextColor = neonColors[(currentColorIndex + 1) % neonColors.size]

        neonBorderPaint.color = activeColor
        neonBorderGlowPaint.color = activeColor

        val waveColor1 = Color.argb(95, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor))
        val waveColor2 = Color.argb(65, Color.red(nextColor), Color.green(nextColor), Color.blue(nextColor))

        if (height > 0) {
            wavePaint.shader = LinearGradient(
                0f, height * 0.4f, width.toFloat(), height.toFloat(),
                waveColor1, waveColor2, Shader.TileMode.CLAMP
            )
        } else {
            wavePaint.shader = null
            wavePaint.color = waveColor1
        }

        textGlowPaint.color = activeColor
        subTextPaint.color = activeColor
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // ঠিক ১.৫ × ১.৫ ইঞ্চি সাইজ
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_IN,
            1.5f,
            resources.displayMetrics
        ).toInt()
        setMeasuredDimension(sizePx, sizePx)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = dpToPx(8f)
        boxRect.set(padding, padding, w - padding, h - padding)

        val cornerRadius = dpToPx(24f)
        clipPath.reset()
        clipPath.addRoundRect(boxRect, cornerRadius, cornerRadius, Path.Direction.CW)

        glassHighlightPaint.shader = LinearGradient(
            0f, boxRect.top, 0f, boxRect.centerY(),
            Color.parseColor("#33FFFFFF"), Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        updateColors()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cornerRadius = dpToPx(24f)

        canvas.save()
        canvas.clipPath(clipPath)

        // ১. ডার্ক গ্লাস ব্যাকগ্রাউন্ড
        canvas.drawRect(boxRect, glassBgPaint)

        // ২. রঙিন তরল পানির ঢেউ
        wavePath.reset()
        val waterLevel = boxRect.top + (boxRect.height() * 0.58f)
        val amplitude = dpToPx(5f)

        wavePath.moveTo(boxRect.left, boxRect.bottom)
        wavePath.lineTo(boxRect.left, waterLevel)

        var x = boxRect.left
        while (x <= boxRect.right) {
            val normalizedX = (x - boxRect.left) / boxRect.width()
            val y = waterLevel + sin(normalizedX * 2 * PI + wavePhase).toFloat() * amplitude
            wavePath.lineTo(x, y)
            x += 6f
        }

        wavePath.lineTo(boxRect.right, boxRect.bottom)
        wavePath.close()
        canvas.drawPath(wavePath, wavePaint)

        // গ্লাসের ওপরের হালকা রিফ্লেকশন
        canvas.drawRect(boxRect.left, boxRect.top, boxRect.right, boxRect.centerY(), glassHighlightPaint)
        canvas.restore()

        // ৩. উজ্জ্বল নিয়ন বর্ডার ও গ্লো
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, neonBorderGlowPaint)
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, neonBorderPaint)

        // ৪. সময় রেন্ডার করা
        val now = Date()
        val timeStr = timeFormat.format(now)
        val subStr = "${secFormat.format(now)} ${amPmFormat.format(now).uppercase(Locale.getDefault())}"

        val centerX = boxRect.centerX()
        val timeY = boxRect.centerY() + dpToPx(4f)

        canvas.drawText(timeStr, centerX, timeY, textGlowPaint)
        canvas.drawText(timeStr, centerX, timeY, textPaint)
        canvas.drawText(subStr, centerX, timeY + dpToPx(20f), subTextPaint)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(animator)
        handler.postDelayed(colorCycler, 3000)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(animator)
        handler.removeCallbacks(colorCycler)
    }

    fun cycleColorNow() {
        currentColorIndex = (currentColorIndex + 1) % neonColors.size
        updateColors()
        invalidate()
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }
}
