package com.example.myapplication1

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat


class MainActivitySettings : BaseActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_settings)
        val exitb: Button = findViewById(R.id.exitToMenub)
        val easyb: Button = findViewById(R.id.easyb)
        val normb: Button = findViewById(R.id.normalb)
        val hardb: Button = findViewById(R.id.hardb)
        val voiceSw: SwitchCompat = findViewById(R.id.voice_sw)
        val texttoslozhnost: TextView = findViewById(R.id.VibranaSlozhnost)

        listOf(exitb, easyb, normb, hardb).forEach { it.addPressEffect() }

        voiceSw.isChecked = GameSettings.voiceEnabled(this)
        voiceSw.setOnCheckedChangeListener { _, checked -> GameSettings.setVoiceEnabled(this, checked) }

        fun select(b: Button, text: String) {
            texttoslozhnost.text = text
            GameSettings.setDifficultyText(this, text)
            listOf(easyb, normb, hardb).forEach { it.isSelected = it === b }
        }
        val saved = GameSettings.difficultyText(this)
        texttoslozhnost.text = saved
        easyb.isSelected = "легк" in saved
        hardb.isSelected = "сложная" in saved
        normb.isSelected = !easyb.isSelected && !hardb.isSelected

        // ---------- музыка
        val musicSw: SwitchCompat = findViewById(R.id.music_sw)
        val btnRec: Button = findViewById(R.id.btnMusicRecommended)
        val btnCustom: Button = findViewById(R.id.btnMusicCustom)
        val btnLibrary: Button = findViewById(R.id.btnMusicLibrary)
        val volume: SeekBar = findViewById(R.id.musicVolume)
        listOf(btnRec, btnCustom, btnLibrary).forEach { it.addPressEffect() }

        fun renderMusic() {
            val on = GameSettings.musicEnabled(this)
            val custom = GameSettings.musicMode(this) == GameSettings.MODE_CUSTOM
            musicSw.isChecked = on
            btnRec.isSelected = !custom
            btnCustom.isSelected = custom
            listOf(btnRec, btnCustom, btnLibrary, volume).forEach { it.isEnabled = on; it.alpha = if (on) 1f else 0.45f }
        }
        musicSw.setOnCheckedChangeListener { _, checked -> GameSettings.setMusicEnabled(this, checked); renderMusic() }
        btnRec.setOnClickListener { GameSettings.setMusicMode(this, GameSettings.MODE_RECOMMENDED); renderMusic() }
        btnCustom.setOnClickListener {
            GameSettings.setMusicMode(this, GameSettings.MODE_CUSTOM)
            renderMusic()
            if (GameSettings.tracks(this).isEmpty()) startActivity(Intent(this, MusicLibraryActivity::class.java))
        }
        btnLibrary.setOnClickListener { startActivity(Intent(this, MusicLibraryActivity::class.java)) }
        volume.progress = (GameSettings.musicVolume(this) * 100).toInt()
        volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) GameSettings.setMusicVolume(this@MainActivitySettings, p / 100f)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        renderMusic()

        exitb.setOnClickListener { finish() }
        easyb.setOnClickListener { select(easyb, "Выбрана легкая сложность") }
        normb.setOnClickListener { select(normb, "Выбрана средняя сложность") }
        hardb.setOnClickListener { select(hardb, "Выбрана сложная сложность") }
    }
}
