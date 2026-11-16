package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class MainActivityMenu : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val dopb: Button = findViewById(R.id.dopolnitelno_button)
        val playb: Button = findViewById(R.id.play_button)
        val settingsb: Button = findViewById(R.id.settings_b)
        val exitb: Button = findViewById(R.id.fullExit_button)
        val buttons = listOf(playb, dopb, settingsb, exitb)
        buttons.forEach { it.addPressEffect() }
        if (savedInstanceState == null) animateEntrance(buttons)

        dopb.setOnClickListener {
            startActivity(Intent(this, DopolnitelnoMain::class.java))
        }
        settingsb.setOnClickListener {
            startActivity(Intent(this, MainActivitySettings::class.java))
        }
        exitb.setOnClickListener {
            finishAffinity()
        }
        // Перед игрой — анкета игрока (имя, пол, аватар, озвучка)
        playb.setOnClickListener {
            startActivity(Intent(this, CharacterActivity::class.java))
        }
    }
}
