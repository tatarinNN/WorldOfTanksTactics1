package com.example.myapplication1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import com.bumptech.glide.Glide

/** Начало игры: имя, пол, аватар и включение озвучки. */
class CharacterActivity : BaseActivity() {

    private var female = false
    private var selected = "m1"
    private var musicChoice = GameSettings.MODE_RECOMMENDED

    private lateinit var etName: EditText
    private lateinit var btnMale: Button
    private lateinit var btnFemale: Button
    private lateinit var avatarList: LinearLayout
    private lateinit var avatarPreview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character)

        etName = findViewById(R.id.etName)
        btnMale = findViewById(R.id.btnMale)
        btnFemale = findViewById(R.id.btnFemale)
        avatarList = findViewById(R.id.avatarList)
        avatarPreview = findViewById(R.id.avatarPreview)
        val swVoice: SwitchCompat = findViewById(R.id.swVoice)
        val btnStart: Button = findViewById(R.id.btnStart)
        val btnBack: Button = findViewById(R.id.btnBack)
        val btnMusicRec: Button = findViewById(R.id.btnMusicRec)
        val btnMusicCustom: Button = findViewById(R.id.btnMusicCustom)
        val btnMusicOff: Button = findViewById(R.id.btnMusicOff)
        val btnMusicLibrary: Button = findViewById(R.id.btnMusicLibrary)

        // ---------- какую музыку слушать
        musicChoice = when {
            !GameSettings.musicEnabled(this) -> "off"
            GameSettings.musicMode(this) == GameSettings.MODE_CUSTOM -> GameSettings.MODE_CUSTOM
            else -> GameSettings.MODE_RECOMMENDED
        }
        fun renderMusic() {
            btnMusicRec.isSelected = musicChoice == GameSettings.MODE_RECOMMENDED
            btnMusicCustom.isSelected = musicChoice == GameSettings.MODE_CUSTOM
            btnMusicOff.isSelected = musicChoice == "off"
            btnMusicLibrary.visibility = if (musicChoice == GameSettings.MODE_CUSTOM) View.VISIBLE else View.GONE
        }
        btnMusicRec.setOnClickListener { musicChoice = GameSettings.MODE_RECOMMENDED; renderMusic() }
        btnMusicOff.setOnClickListener { musicChoice = "off"; renderMusic() }
        btnMusicCustom.setOnClickListener {
            musicChoice = GameSettings.MODE_CUSTOM
            renderMusic()
            if (GameSettings.tracks(this).isEmpty()) startActivity(Intent(this, MusicLibraryActivity::class.java))
        }
        btnMusicLibrary.setOnClickListener { startActivity(Intent(this, MusicLibraryActivity::class.java)) }
        listOf(btnMusicRec, btnMusicCustom, btnMusicOff, btnMusicLibrary).forEach { it.addPressEffect() }
        renderMusic()

        etName.setText(GameSettings.playerName(this))
        female = GameSettings.playerFemale(this)
        selected = GameSettings.playerAvatar(this)
        swVoice.isChecked = GameSettings.voiceEnabled(this)

        etName.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(v.windowToken, 0)
                v.clearFocus()
                hideSystemBars()
            }
            false
        }

        listOf(btnMale, btnFemale, btnStart, btnBack).forEach { it.addPressEffect() }
        btnMale.setOnClickListener { setGender(false) }
        btnFemale.setOnClickListener { setGender(true) }
        btnBack.setOnClickListener { finish() }
        btnStart.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "Введите имя"
                etName.shake()
                etName.requestFocus()
                return@setOnClickListener
            }
            GameSettings.saveProfile(this, name, female, selected)
            GameSettings.setVoiceEnabled(this, swVoice.isChecked)
            GameSettings.setMusicEnabled(this, musicChoice != "off")
            if (musicChoice != "off") GameSettings.setMusicMode(this, musicChoice)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        renderGender()
    }

    private fun setGender(f: Boolean) {
        if (female == f) return
        female = f
        selected = Avatars.forGender(f).first()
        renderGender()
    }

    private fun renderGender() {
        btnMale.isSelected = !female
        btnFemale.isSelected = female
        buildAvatarList()
        showPreview()
    }

    private fun buildAvatarList() {
        avatarList.removeAllViews()
        val d = resources.displayMetrics.density
        val size = (66 * d).toInt()
        val margin = (5 * d).toInt()
        val pad = (4 * d).toInt()
        for (id in Avatars.forGender(female)) {
            val iv = ImageView(this)
            iv.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, 0, margin, 0)
            }
            iv.setPadding(pad, pad, pad, pad)
            iv.setBackgroundResource(R.drawable.bg_avatar_option)
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            iv.contentDescription = "Аватар $id"
            iv.isSelected = id == selected
            Glide.with(this).load(Avatars.player(id)).into(iv)
            iv.setOnClickListener {
                selected = id
                for (i in 0 until avatarList.childCount) avatarList.getChildAt(i).isSelected = false
                iv.isSelected = true
                showPreview()
            }
            avatarList.addView(iv)
        }
        (avatarList.parent as? ViewGroup)?.scrollTo(0, 0)
    }

    private fun showPreview() {
        Glide.with(this).load(Avatars.player(selected)).into(avatarPreview)
        avatarPreview.pop()
    }
}
