package com.example.myapplication1

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Анимация пепла: серые хлопья медленно падают и покачиваются,
 * редкие оранжевые угольки поднимаются вверх и мерцают.
 * Не перехватывает нажатия, кладётся в layout под кнопки.
 */
class AshView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private class Particle {
        var x = 0f
        var y = 0f
        var vx = 0f
        var vy = 0f
        var size = 0f
        var rot = 0f
        var vr = 0f
        var alpha = 0
        var ember = false
        var phase = 0f
        var swayAmp = 0f
        var swayFreq = 0f
        var color = 0
    }

    private val density = resources.displayMetrics.density
    private val particles = ArrayList<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastFrame = 0L
    private var time = 0f

    /** Количество частиц (меньше — экономнее для батареи). */
    var particleCount = 70

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        particles.clear()
        repeat(particleCount) { particles.add(spawn(Particle(), randomY = true)) }
    }

    private fun spawn(p: Particle, randomY: Boolean): Particle {
        val w = width.coerceAtLeast(1).toFloat()
        val h = height.coerceAtLeast(1).toFloat()
        p.ember = Random.nextFloat() < 0.18f
        p.x = Random.nextFloat() * w
        if (p.ember) {
            p.y = if (randomY) Random.nextFloat() * h else h + 10 * density
            p.vy = -(10f + Random.nextFloat() * 22f) * density
            p.size = (1.2f + Random.nextFloat() * 1.6f) * density
            val r = 230 + Random.nextInt(25)
            val g = 90 + Random.nextInt(80)
            p.color = (0xFF shl 24) or (r shl 16) or (g shl 8) or 30
        } else {
            p.y = if (randomY) Random.nextFloat() * h else -10 * density
            p.vy = (14f + Random.nextFloat() * 30f) * density
            p.size = (1.5f + Random.nextFloat() * 3.2f) * density
            val v = 90 + Random.nextInt(90)
            p.color = (0xFF shl 24) or (v shl 16) or (v shl 8) or (v - 8).coerceAtLeast(0)
        }
        p.vx = (Random.nextFloat() - 0.3f) * 10f * density // лёгкий ветер вправо
        p.rot = Random.nextFloat() * 360f
        p.vr = (Random.nextFloat() - 0.5f) * 120f
        p.alpha = 90 + Random.nextInt(140)
        p.phase = Random.nextFloat() * 2f * PI.toFloat()
        p.swayAmp = (6f + Random.nextFloat() * 14f) * density
        p.swayFreq = 0.6f + Random.nextFloat() * 1.2f
        return p
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastFrame = 0L
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1_000_000_000f).coerceAtMost(0.05f)
        lastFrame = now
        time += dt
        val w = width.toFloat()
        val h = height.toFloat()

        for (p in particles) {
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rot += p.vr * dt
            val sway = sin(time * p.swayFreq + p.phase) * p.swayAmp
            val drawX = p.x + sway
            if (p.y > h + 20 * density || p.y < -20 * density || drawX < -40 * density || drawX > w + 40 * density) {
                spawn(p, randomY = false)
                continue
            }
            if (p.ember) {
                val flicker = 0.6f + 0.4f * sin(time * 9f + p.phase)
                val a = (p.alpha * flicker).toInt().coerceIn(0, 255)
                glow.color = p.color
                glow.alpha = a / 4
                canvas.drawCircle(drawX, p.y, p.size * 3f, glow)
                paint.color = p.color
                paint.alpha = a
                canvas.drawCircle(drawX, p.y, p.size, paint)
            } else {
                paint.color = p.color
                paint.alpha = p.alpha
                canvas.save()
                canvas.rotate(p.rot, drawX, p.y)
                canvas.drawRect(drawX - p.size, p.y - p.size * 0.55f, drawX + p.size, p.y + p.size * 0.55f, paint)
                canvas.restore()
            }
        }
        if (isShown) postInvalidateOnAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            lastFrame = 0L
            postInvalidateOnAnimation()
        }
    }
}
