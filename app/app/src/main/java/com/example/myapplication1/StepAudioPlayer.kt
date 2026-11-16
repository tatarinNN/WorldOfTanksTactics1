package com.example.myapplication1

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

/**
 * Озвучка ходов. Файлы: assets/audio/<id хода>.mp3 (или .ogg / .m4a / .wav),
 * например assets/audio/L012.mp3. id хода — первый аргумент в F1("L012", ...).
 *
 * Плеер всегда ОДИН: при переходе на следующий ход предыдущая дорожка останавливается
 * и освобождается до того, как начнётся новая, поэтому звуки не накладываются,
 * даже если игрок жмёт «Дальше» очень быстро.
 */
class StepAudioPlayer(
    private val context: Context,
    private val onState: (State) -> Unit,
) {
    data class State(
        val available: Boolean, // у хода есть файл озвучки
        val active: Boolean,    // плеер создан (озвучка включена)
        val playing: Boolean,
        val positionMs: Int,
        val durationMs: Int,
    )

    private var player: MediaPlayer? = null
    private var prepared = false
    private var available = false
    private var token = 0
    private val handler = Handler(Looper.getMainLooper())

    /** id хода -> имя файла в assets/audio */
    private val files: Map<String, String> by lazy {
        val names = try {
            context.assets.list(DIR)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val result = HashMap<String, String>()
        for (ext in EXTENSIONS) {
            for (n in names) {
                if (n.endsWith(".$ext", ignoreCase = true)) {
                    result.putIfAbsent(n.substringBeforeLast('.'), n)
                }
            }
        }
        result
    }

    private val ticker = object : Runnable {
        override fun run() {
            emit()
            if (player?.isPlaying == true) handler.postDelayed(this, 250)
        }
    }

    fun hasAudio(stepId: String): Boolean = files.containsKey(stepId)

    /** Остановить текущую дорожку и (если озвучка включена) запустить дорожку хода. */
    fun play(stepId: String, enabled: Boolean) {
        stopInternal()
        val name = files[stepId]
        available = name != null
        if (name == null || !enabled) {
            emit()
            return
        }
        val myToken = ++token
        try {
            val mp = MediaPlayer()
            player = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            context.assets.openFd("$DIR/$name").use { afd ->
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mp.setOnPreparedListener {
                if (myToken == token && player === mp) {
                    prepared = true
                    mp.start()
                    startTicker()
                }
            }
            mp.setOnCompletionListener {
                if (player === mp) {
                    handler.removeCallbacks(ticker)
                    emit()
                }
            }
            mp.setOnErrorListener { _, _, _ ->
                if (player === mp) stopInternal()
                emit()
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            stopInternal()
        }
        emit()
    }

    /** Пауза / продолжить. После окончания дорожки — проиграть заново. */
    fun toggle() {
        val mp = player ?: return
        if (!prepared) return
        if (mp.isPlaying) {
            mp.pause()
            handler.removeCallbacks(ticker)
        } else {
            mp.start()
            startTicker()
        }
        emit()
    }

    fun seekTo(ms: Int) {
        val mp = player ?: return
        if (!prepared) return
        mp.seekTo(ms)
        emit()
    }

    fun pause() {
        val mp = player ?: return
        if (prepared && mp.isPlaying) mp.pause()
        handler.removeCallbacks(ticker)
        emit()
    }

    fun stop() {
        stopInternal()
        emit()
    }

    fun release() {
        stopInternal()
    }

    private fun stopInternal() {
        token++
        handler.removeCallbacks(ticker)
        player?.let { mp ->
            try {
                mp.setOnPreparedListener(null)
                mp.setOnCompletionListener(null)
                mp.setOnErrorListener(null)
                mp.reset()
            } catch (_: Exception) {
            }
            mp.release()
        }
        player = null
        prepared = false
    }

    private fun startTicker() {
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun emit() {
        val mp = player
        val ready = mp != null && prepared
        onState(
            State(
                available = available,
                active = mp != null,
                playing = ready && mp!!.isPlaying,
                positionMs = if (ready) mp!!.currentPosition else 0,
                durationMs = if (ready) mp!!.duration.coerceAtLeast(0) else 0,
            )
        )
    }

    companion object {
        const val DIR = "audio"
        private val EXTENSIONS = listOf("mp3", "ogg", "m4a", "wav")
    }
}
