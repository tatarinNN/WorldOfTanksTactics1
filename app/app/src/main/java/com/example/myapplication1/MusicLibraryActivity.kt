package com.example.myapplication1

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton

/**
 * Свои треки игрока: добавить с телефона, удалить, собрать плейлисты («избранные серии»)
 * и выбрать, какой из них играет в игре.
 */
class MusicLibraryActivity : BaseActivity() {

    private lateinit var trackList: LinearLayout
    private lateinit var playlistList: LinearLayout
    private lateinit var tracksEmpty: TextView
    private val checked = LinkedHashSet<String>()

    private val pickTracks = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        val list = GameSettings.tracks(this).toMutableList()
        var added = 0
        for (uri in uris) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            if (list.none { it.uri == uri.toString() }) {
                list.add(GameSettings.Track(uri.toString(), displayName(uri)))
                added++
            }
        }
        GameSettings.saveTracks(this, list)
        Toast.makeText(this, "Добавлено треков: $added", Toast.LENGTH_SHORT).show()
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_library)
        trackList = findViewById(R.id.trackList)
        playlistList = findViewById(R.id.playlistList)
        tracksEmpty = findViewById(R.id.tracksEmpty)
        val btnBack: Button = findViewById(R.id.btnBack)
        val btnAdd: Button = findViewById(R.id.btnAddTracks)
        val btnCreate: Button = findViewById(R.id.btnCreatePlaylist)
        listOf(btnBack, btnAdd, btnCreate).forEach { it.addPressEffect() }

        btnBack.setOnClickListener { finish() }
        btnAdd.setOnClickListener {
            try {
                pickTracks.launch(arrayOf("audio/*"))
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось открыть выбор файлов", Toast.LENGTH_SHORT).show()
            }
        }
        btnCreate.setOnClickListener { createPlaylist() }
        render()
    }

    private fun displayName(uri: Uri): String {
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getString(0)
                    if (!n.isNullOrBlank()) return n.substringBeforeLast('.')
                }
            }
        } catch (_: Exception) {
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Трек"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun smallButton(text: String): AppCompatButton {
        val b = AppCompatButton(this, null, 0)
        b.setBackgroundResource(R.drawable.bg_button)
        b.setTextColor(Color.parseColor("#FF8A7A"))
        b.text = text
        b.textSize = 13f
        b.minHeight = 0
        b.minimumHeight = 0
        b.minWidth = 0
        b.minimumWidth = 0
        b.setPadding(dp(10), 0, dp(10), 0)
        b.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(34)).apply {
            marginStart = dp(6)
        }
        return b
    }

    private fun render() {
        val tracks = GameSettings.tracks(this)
        checked.retainAll(tracks.map { it.uri }.toSet())
        tracksEmpty.visibility = if (tracks.isEmpty()) View.VISIBLE else View.GONE
        trackList.removeAllViews()
        for (t in tracks) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, dp(2), 0, dp(2))
            val cb = CheckBox(this)
            cb.text = t.name
            cb.setTextColor(Color.parseColor("#EDE3C8"))
            cb.isChecked = t.uri in checked
            cb.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            cb.setOnCheckedChangeListener { _, on -> if (on) checked.add(t.uri) else checked.remove(t.uri) }
            val del = smallButton("✕")
            del.setOnClickListener {
                GameSettings.saveTracks(this, GameSettings.tracks(this).filter { it.uri != t.uri })
                // убираем трек и из плейлистов
                GameSettings.savePlaylists(this, GameSettings.playlists(this).map { p -> p.copy(uris = p.uris - t.uri) })
                try {
                    contentResolver.releasePersistableUriPermission(Uri.parse(t.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {
                }
                render()
            }
            row.addView(cb)
            row.addView(del)
            trackList.addView(row)
        }

        playlistList.removeAllViews()
        val active = GameSettings.activePlaylist(this)
        playlistList.addView(playlistRow(getString(R.string.game_all_tracks) + " (${tracks.size})", active == -1, -1, false))
        GameSettings.playlists(this).forEachIndexed { i, p ->
            playlistList.addView(playlistRow("${p.name} (${p.uris.size})", active == i, i, true))
        }
    }

    private fun playlistRow(title: String, selected: Boolean, index: Int, deletable: Boolean): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(3), 0, dp(3))
        val b = AppCompatButton(this, null, 0)
        b.setBackgroundResource(R.drawable.bg_button)
        b.setTextColor(Color.parseColor("#EDE3C8"))
        b.isAllCaps = false
        b.textSize = 13f
        b.text = if (selected) "▶  $title" else title
        b.isSelected = selected
        b.minHeight = 0
        b.minimumHeight = 0
        b.layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f)
        b.addPressEffect()
        b.setOnClickListener {
            GameSettings.setActivePlaylist(this, index)
            render()
        }
        row.addView(b)
        if (deletable) {
            val del = smallButton("✕")
            del.setOnClickListener {
                val list = GameSettings.playlists(this).toMutableList()
                if (index in list.indices) list.removeAt(index)
                GameSettings.savePlaylists(this, list)
                val a = GameSettings.activePlaylist(this)
                if (a == index) GameSettings.setActivePlaylist(this, -1) else if (a > index) GameSettings.setActivePlaylist(this, a - 1)
                render()
            }
            row.addView(del)
        }
        return row
    }

    private fun createPlaylist() {
        if (checked.isEmpty()) {
            Toast.makeText(this, "Сначала отметьте треки галочками слева", Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        input.hint = "Например: Для боя"
        input.setSingleLine()
        val pad = dp(20)
        val box = LinearLayout(this)
        box.setPadding(pad, dp(8), pad, 0)
        box.addView(input, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        AlertDialog.Builder(this)
            .setTitle("Название плейлиста")
            .setView(box)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString().trim().ifEmpty { "Плейлист ${GameSettings.playlists(this).size + 1}" }
                val list = GameSettings.playlists(this).toMutableList()
                list.add(GameSettings.Playlist(name, checked.toList()))
                GameSettings.savePlaylists(this, list)
                GameSettings.setActivePlaylist(this, list.size - 1)
                checked.clear()
                render()
            }
            .setNegativeButton("Отмена", null)
            .setOnDismissListener { hideSystemBars() }
            .show()
    }
}
