package com.example.myapplication1

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Всё, что нужно запомнить между запусками: профиль игрока, звук, музыка, сложность. */
object GameSettings {
    private const val PREFS = "game_settings"
    private const val KEY_VOICE = "voice_enabled"
    private const val KEY_NAME = "player_name"
    private const val KEY_FEMALE = "player_female"
    private const val KEY_AVATAR = "player_avatar"
    private const val KEY_DIFFICULTY = "difficulty_text"

    private fun prefs(c: Context): SharedPreferences = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun voiceEnabled(c: Context): Boolean = prefs(c).getBoolean(KEY_VOICE, true)
    fun setVoiceEnabled(c: Context, value: Boolean) = prefs(c).edit().putBoolean(KEY_VOICE, value).apply()

    fun playerName(c: Context): String = prefs(c).getString(KEY_NAME, "") ?: ""
    fun playerFemale(c: Context): Boolean = prefs(c).getBoolean(KEY_FEMALE, false)
    fun playerAvatar(c: Context): String {
        val saved = prefs(c).getString(KEY_AVATAR, null)
        val list = Avatars.forGender(playerFemale(c))
        return if (saved != null && saved in list) saved else list.first()
    }

    fun saveProfile(c: Context, name: String, female: Boolean, avatar: String) {
        prefs(c).edit()
            .putString(KEY_NAME, name)
            .putBoolean(KEY_FEMALE, female)
            .putString(KEY_AVATAR, avatar)
            .apply()
    }

    fun difficultyText(c: Context): String =
        prefs(c).getString(KEY_DIFFICULTY, "Выбрана сложность: нормальная") ?: "Выбрана сложность: нормальная"
    fun setDifficultyText(c: Context, value: String) = prefs(c).edit().putString(KEY_DIFFICULTY, value).apply()

    // ---------------------------------------------------------------- музыка
    const val MODE_RECOMMENDED = "recommended"
    const val MODE_CUSTOM = "custom"
    private const val KEY_MUSIC_ON = "music_enabled"
    private const val KEY_MUSIC_MODE = "music_mode"
    private const val KEY_MUSIC_VOLUME = "music_volume"
    private const val KEY_TRACKS = "music_tracks"
    private const val KEY_PLAYLISTS = "music_playlists"
    private const val KEY_ACTIVE_PLAYLIST = "music_active_playlist"

    data class Track(val uri: String, val name: String)
    data class Playlist(val name: String, val uris: List<String>)

    fun musicEnabled(c: Context): Boolean = prefs(c).getBoolean(KEY_MUSIC_ON, true)
    fun setMusicEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_MUSIC_ON, v).apply()

    /** MODE_RECOMMENDED — наши треки, расставленные по событиям; MODE_CUSTOM — свои треки игрока. */
    fun musicMode(c: Context): String = prefs(c).getString(KEY_MUSIC_MODE, MODE_RECOMMENDED) ?: MODE_RECOMMENDED
    fun setMusicMode(c: Context, v: String) = prefs(c).edit().putString(KEY_MUSIC_MODE, v).apply()

    fun musicVolume(c: Context): Float = prefs(c).getFloat(KEY_MUSIC_VOLUME, 0.6f)
    fun setMusicVolume(c: Context, v: Float) = prefs(c).edit().putFloat(KEY_MUSIC_VOLUME, v.coerceIn(0f, 1f)).apply()

    fun tracks(c: Context): List<Track> {
        val arr = try { JSONArray(prefs(c).getString(KEY_TRACKS, "[]")) } catch (e: Exception) { JSONArray() }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Track(o.optString("uri"), o.optString("name"))
        }
    }

    fun saveTracks(c: Context, list: List<Track>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("uri", it.uri).put("name", it.name)) }
        prefs(c).edit().putString(KEY_TRACKS, arr.toString()).apply()
    }

    fun playlists(c: Context): List<Playlist> {
        val arr = try { JSONArray(prefs(c).getString(KEY_PLAYLISTS, "[]")) } catch (e: Exception) { JSONArray() }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val u = o.optJSONArray("uris") ?: JSONArray()
            Playlist(o.optString("name"), (0 until u.length()).map { u.optString(it) })
        }
    }

    fun savePlaylists(c: Context, list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach { p ->
            val u = JSONArray()
            p.uris.forEach { u.put(it) }
            arr.put(JSONObject().put("name", p.name).put("uris", u))
        }
        prefs(c).edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    /** -1 — играть все свои треки по порядку, иначе номер плейлиста. */
    fun activePlaylist(c: Context): Int = prefs(c).getInt(KEY_ACTIVE_PLAYLIST, -1)
    fun setActivePlaylist(c: Context, v: Int) = prefs(c).edit().putInt(KEY_ACTIVE_PLAYLIST, v).apply()

    /** Очередь своих треков с учётом выбранного плейлиста. */
    fun customQueue(c: Context): List<Track> {
        val all = tracks(c)
        val idx = activePlaylist(c)
        val pl = playlists(c).getOrNull(idx) ?: return all
        val byUri = all.associateBy { it.uri }
        return pl.uris.mapNotNull { byUri[it] }.ifEmpty { all }
    }
}
