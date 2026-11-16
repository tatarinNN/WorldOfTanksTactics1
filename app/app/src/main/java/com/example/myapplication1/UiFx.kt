package com.example.myapplication1

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/** Кнопка слегка «вдавливается» при нажатии. Обычный setOnClickListener продолжает работать. */
@SuppressLint("ClickableViewAccessibility")
fun View.addPressEffect() {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(140).setInterpolator(OvershootInterpolator(3f)).start()
        }
        false
    }
}

/** Кнопки по очереди выезжают справа. */
fun animateEntrance(views: List<View>, startDelay: Long = 150) {
    views.forEachIndexed { i, v ->
        v.alpha = 0f
        v.translationX = 80f * v.resources.displayMetrics.density
        v.animate()
            .alpha(1f)
            .translationX(0f)
            .setStartDelay(startDelay + i * 90L)
            .setDuration(420)
            .setInterpolator(DecelerateInterpolator(2f))
            .start()
    }
}

/** «Подпрыгивание» — для смены аватарки. */
fun View.pop() {
    scaleX = 0.82f
    scaleY = 0.82f
    animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(OvershootInterpolator(2.5f)).start()
}

/** Тряска — для ошибки ввода. */
fun View.shake() {
    val d = 8f * resources.displayMetrics.density
    animate().translationX(d).setDuration(50).withEndAction {
        animate().translationX(-d).setDuration(50).withEndAction {
            animate().translationX(d / 2).setDuration(50).withEndAction {
                animate().translationX(0f).setDuration(50).start()
            }.start()
        }.start()
    }.start()
}
