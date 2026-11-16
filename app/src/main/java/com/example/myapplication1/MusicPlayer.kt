package com.example.myapplication1

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * Фоновая музыка игры. Работает отдельно от звуков ходов (StepAudioPlayer):
 *  - рекомендуемый режим: трек выбирается по настроению хода (MusicCues), треки сменяются плавно,
 *    а при возврате к треку он продолжается с места, где прервался;
 *  - после боя спокойная музыка включается не сразу (бой держится минимум 3 хода);
 *  - свои треки: играют по очереди из выбранного плейлиста, можно листать и выбирать по ходу игры;
 *  - на время звуков хода музыка приглушается;
 *  - победа / поражение — отдельная финальная мелодия (один раз).
 */
class MusicPlayer(private val context: Context) {

    /** Вызывается при смене трека — название, чтобы показать игроку. */
    var onTrackChanged: ((String) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var currentKey: String? = null
    private var currentTitle = ""
    private var looping = false
    private var prepared = false
    private val resumeAt = HashMap<String, Int>()

    private var volume = GameSettings.musicVolume(context)
    private var duck = 1f
    private var fade = 1f
    private var fadeTarget = 1f
    private var fadeStep = 0f
    private var afterFade: (() -> Unit)? = null

    private var mood: String? = null
    private var moodPriority = 0
    private var stepsSinceSwitch = 0
    private var finished = false // сыграла финальная мелодия

    private var queue: List<GameSettings.Track> = emptyList()
    private var queueIndex = 0
    private var errorsInRow = 0
    private var paused = false

    private val fader = object : Runnable {
        override fun run() {
            fade = if (fadeStep > 0) minOf(fadeTarget, fade + fadeStep) else maxOf(fadeTarget, fade + fadeStep)
            applyVolume()
            if (fade != fadeTarget) {
                handler.postDelayed(this, 40)
            } else {
                val a = afterFade
                afterFade = null
                a?.invoke()
            }
        }
    }

    val enabled get() = GameSettings.musicEnabled(context)
    val isCustom get() = GameSettings.musicMode(context) == GameSettings.MODE_CUSTOM
    val title get() = currentTitle

    // ---------------------------------------------------------------- ходы игры

    /** Вызывать на каждом ходе. ending = "victory" / "defeat" / null. */
    fun onStep(stepId: String, ending: String?) {
        if (!enabled || finished) return
        if (ending != null) {
            playJingle(if (ending == "victory") MusicCues.VICTORY else MusicCues.DEFEAT,
                if (ending == "victory") "Победа" else "Поражение")
            return
        }
        if (isCustom) {
            if (player == null) startCustom(0)
            return
        }
        val m = MusicCues.BY_STEP[stepId]
        val target = m?.let { MusicCues.MOODS[it] }
        if (target == null) {
            if (player == null && m != "victory" && m != "defeat") startMood("brief") // первый ход без метки
            stepsSinceSwitch++
            return
        }
        if (m == mood) {
            stepsSinceSwitch++
            return
        }
        // спокойная музыка не прерывает бой/напряжение слишком рано
        if (target.priority < moodPriority && stepsSinceSwitch < 3 && player != null) {
            stepsSinceSwitch++
            return
        }
        startMood(m)
    }

    private fun startMood(m: String) {
        val target = MusicCues.MOODS[m] ?: return
        mood = m
        moodPriority = target.priority
        stepsSinceSwitch = 0
        val key = "asset:music/${target.track}"
        if (key == currentKey) return
        switchTo(key, target.title, loop = true)
    }

    private fun playJingle(file: String, title: String) {
        finished = true
        switchTo("asset:music/$file", title, loop = false, remember = false)
    }

    // ---------------------------------------------------------------- свои треки

    fun reloadCustomQueue() {
        queue = GameSettings.customQueue(context)
    }

    private fun startCustom(index: Int) {
        reloadCustomQueue()
        if (queue.isEmpty()) {
            stopNow()
            currentTitle = "Нет своих треков"
            onTrackChanged?.invoke(currentTitle)
            return
        }
        queueIndex = ((index % queue.size) + queue.size) % queue.size
        val t = queue[queueIndex]
        switchTo("uri:${t.uri}", t.name, loop = false, remember = false)
    }

    fun next() { if (isCustom && enabled) startCustom(queueIndex + 1) }
    fun previous() { if (isCustom && enabled) startCustom(queueIndex - 1) }
    fun playTrack(index: Int) { if (isCustom && enabled) startCustom(index) }
    fun customQueue(): List<GameSettings.Track> { reloadCustomQueue(); return queue }
    fun currentIndex() = queueIndex

    // ---------------------------------------------------------------- управление

    /** Перезапуск после смены настроек (вкл/выкл, режим, плейлист). */
    fun restart(stepId: String) {
        finished = false
        mood = null
        moodPriority = 0
        stopNow()
        if (!enabled) {
            currentTitle = "Музыка выключена"
            onTrackChanged?.invoke(currentTitle)
            return
        }
        if (isCustom) startCustom(0) else {
            onStep(stepId, null)
            if (player == null) startMood("brief")
        }
    }

    fun setDucked(d: Boolean) {
        val v = if (d) 0.3f else 1f
        if (v != duck) {
            duck = v
            applyVolume()
        }
    }

    fun setVolume(v: Float) {
        volume = v
        applyVolume()
    }

    fun pause() {
        paused = true
        try { if (prepared && player?.isPlaying == true) player?.pause() } catch (_: Exception) { }
    }

    fun resume() {
        paused = false
        try { if (prepared && player?.isPlaying == false) player?.start() } catch (_: Exception) { }
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        stopNow()
    }

    // ---------------------------------------------------------------- внутреннее

    private fun switchTo(key: String, title: String, loop: Boolean, remember: Boolean = true) {
        val old = player
        if (old != null && prepared) {
            // запоминаем место, чтобы вернуться к треку позже
            if (looping && remember) currentKey?.let { k -> try { resumeAt[k] = old.currentPosition } catch (_: Exception) { } }
            startFade(0f, 600) {
                stopNow()
                open(key, title, loop)
            }
        } else {
            stopNow()
            open(key, title, loop)
        }
    }

    private fun open(key: String, title: String, loop: Boolean) {
        val mp = MediaPlayer()
        player = mp
        currentKey = key
        currentTitle = title
        looping = loop
        prepared = false
        fade = 0f
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            if (key.startsWith("asset:")) {
                context.assets.openFd(key.removePrefix("asset:")).use { afd ->
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            } else {
                mp.setDataSource(context, Uri.parse(key.removePrefix("uri:")))
            }
            mp.isLooping = loop
            mp.setOnPreparedListener {
                if (player !== mp) return@setOnPreparedListener
                prepared = true
                errorsInRow = 0
                resumeAt[key]?.let { pos -> if (pos in 1 until mp.duration) mp.seekTo(pos) }
                applyVolume()
                if (!paused) mp.start()
                startFade(1f, 900, null)
                onTrackChanged?.invoke(title)
            }
            mp.setOnCompletionListener {
                if (player !== mp) return@setOnCompletionListener
                if (key.startsWith("uri:")) handler.post { next() } // свой трек закончился — следующий
            }
            mp.setOnErrorListener { _, _, _ ->
                if (player === mp) handler.post { onError(key) }
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            handler.post { onError(key) }
        }
    }

    private fun onError(key: String) {
        stopNow()
        if (key.startsWith("uri:") && isCustom) {
            errorsInRow++
            if (errorsInRow < queue.size.coerceAtLeast(1)) {
                startCustom(queueIndex + 1) // файл недоступен (удалён / нет доступа) — пропускаем
                return
            }
            currentTitle = "Не удалось открыть свои треки"
            onTrackChanged?.invoke(currentTitle)
        }
    }

    private fun startFade(target: Float, ms: Long, then: (() -> Unit)?) {
        handler.removeCallbacks(fader)
        fadeTarget = target
        val steps = (ms / 40).coerceAtLeast(1)
        fadeStep = (target - fade) / steps
        afterFade = then
        if (fadeStep == 0f) {
            fade = target
            applyVolume()
            then?.invoke()
            afterFade = null
            return
        }
        handler.post(fader)
    }

    private fun applyVolume() {
        val v = (volume * duck * fade).coerceIn(0f, 1f)
        try { if (prepared) player?.setVolume(v, v) } catch (_: Exception) { }
    }

    private fun stopNow() {
        handler.removeCallbacks(fader)
        afterFade = null
        player?.let { mp ->
            try {
                mp.setOnPreparedListener(null)
                mp.setOnCompletionListener(null)
                mp.setOnErrorListener(null)
                mp.reset()
            } catch (_: Exception) { }
            mp.release()
        }
        player = null
        prepared = false
        currentKey = null
    }
}
