package com.example.myapplication1

/**
 * Аватарки лежат в assets/avatars/ (анимированные GIF).
 *  Игрок:   m1..m4 (мужские), f1..f4 (женские) — у каждой 3 состояния: _idle, _hurt, _dead
 *  Экипаж:  mihailo, smirnov, natalson, crew (неизвестный член экипажа), crew_group (разговор), abrams (противник)
 *  События: ev_track, ev_fire, ev_wreck, ev_shot, ev_explosion, ev_victory
 */
object Avatars {
    val MALE = listOf("m1", "m2", "m3", "m4")
    val FEMALE = listOf("f1", "f2", "f3", "f4")

    fun forGender(female: Boolean): List<String> = if (female) FEMALE else MALE

    fun player(id: String, state: String = "idle") = "file:///android_asset/avatars/${id}_$state.gif"
    fun asset(name: String) = "file:///android_asset/avatars/$name.gif"
}

/** ending = "victory" / "defeat" в концовках (для финальной музыки), иначе null. */
data class SceneAvatar(val uri: String, val caption: String, val ending: String? = null)

/**
 * Решает, кого показать в углу на текущем ходу.
 *
 * Можно указать явно в F1(...): speaker = "mihailo" / "smirnov" / "natalson" / "abrams" / "crew" / "group" / "player",
 * event = "track" / "fire" / "wreck" / "shot" / "explosion" / "hurt" / "victory" / "defeat" / "none".
 * Если не указано — угадывается по тексту хода:
 *  - строка вида "Тов. Михайло: ..." → говорит Михайло;
 *  - несколько подписанных реплик разных членов экипажа → общая аватарка «экипаж переговаривается»;
 *  - "Командир, ..." без подписи и без <скобок> (доклад экипажа) → Натальсон;
 *  - текст в <скобках> — рассказчик, показывается командир;
 *  - слова «гусениц», «пылает», «догорает», «взрыв», «выстрел»… → картинка события;
 *  - обе кнопки «ГГ» → победа или гибель;
 *  - иначе — аватар самого игрока (командира).
 */
object SceneDirector {

    private val speakerLine = Regex(
        """^\s*(?:Тов\.\s*)?(Михайло|Смирнов|Натальсон|K\.\s*Abrams|Абрамс)\s*(?:<[^>]*>)?\s*:"""
    )
    private val crewCall = Regex("""^\s*Командир\s*[,!]""")

    private val defeatWords = Regex("погиб|провален|проиграл|умира|смерт")
    private val victoryWords = Regex("выполнен|подмог|подкрепл|сорван|победа|спасло")

    fun pick(
        text: String,
        left: String,
        right: String,
        speaker: String?,
        event: String?,
        playerAvatar: String,
        playerName: String,
    ): SceneAvatar {
        val commander = if (playerName.isBlank()) "Командир" else "Командир $playerName"
        val ev = when (event) {
            null -> autoEvent(text, left, right)
            "none" -> null
            else -> event
        }
        when (ev) {
            "victory" -> return SceneAvatar(Avatars.asset("ev_victory"), "Победа!", "victory")
            "defeat" -> return SceneAvatar(Avatars.player(playerAvatar, "dead"), "$commander — конец пути", "defeat")
        }
        when (speaker ?: autoSpeaker(text)) {
            "mihailo" -> return SceneAvatar(Avatars.asset("mihailo"), "Мехвод Михайло")
            "smirnov" -> return SceneAvatar(Avatars.asset("smirnov"), "Наводчик Смирнов")
            "natalson" -> return SceneAvatar(Avatars.asset("natalson"), "Заряжающий Натальсон")
            "abrams" -> return SceneAvatar(Avatars.asset("abrams"), "Крейтон Абрамс")
            "crew" -> return SceneAvatar(Avatars.asset("crew"), "Экипаж")
            "group" -> return SceneAvatar(Avatars.asset("crew_group"), "Экипаж переговаривается")
            "player" -> return SceneAvatar(Avatars.player(playerAvatar), commander)
        }
        return when (ev) {
            "track" -> SceneAvatar(Avatars.asset("ev_track"), "Гусеница сбита!")
            "fire" -> SceneAvatar(Avatars.asset("ev_fire"), "Пожар!")
            "wreck" -> SceneAvatar(Avatars.asset("ev_wreck"), "Танк догорает…")
            "explosion" -> SceneAvatar(Avatars.asset("ev_explosion"), "Взрыв!")
            "shot" -> SceneAvatar(Avatars.asset("ev_shot"), "Выстрел!")
            "hurt" -> SceneAvatar(Avatars.player(playerAvatar, "hurt"), commander)
            else -> SceneAvatar(Avatars.player(playerAvatar), commander)
        }
    }

    private fun isGG(s: String) = s.trim().lowercase() in setOf("гг", "gg")

    private fun autoSpeaker(text: String): String? {
        val who = LinkedHashSet<String>()
        for (line in text.lines()) {
            val m = speakerLine.find(line) ?: continue
            val name = m.groupValues[1]
            who += when {
                name.startsWith("Михайло") -> "mihailo"
                name.startsWith("Смирнов") -> "smirnov"
                name.startsWith("Натальсон") -> "natalson"
                else -> "abrams"
            }
        }
        // в одном ходе говорят несколько членов экипажа — показываем общую аватарку
        if (who.size >= 2) return "group"
        if (who.size == 1) return who.first()
        if (crewCall.containsMatchIn(text)) return "natalson" // доклады экипажа озвучивает Натальсон
        return null
    }

    private fun autoEvent(text: String, left: String, right: String): String? {
        val t = text.lowercase()
        if (isGG(left) && isGG(right)) {
            return if (victoryWords.containsMatchIn(t) && !Regex("погиб|провален").containsMatchIn(t)) "victory"
            else "defeat"
        }
        return when {
            "гусениц" in t -> "track"
            Regex("пылает|полыха|горит|пожар").containsMatchIn(t) -> "fire"
            Regex("догора|(?<!не )уничтож").containsMatchIn(t) -> "wreck"
            Regex("взрыв|детонац").containsMatchIn(t) -> "explosion"
            Regex("оглуш|рикошет|не пробит|фарш|в ваш танк попали").containsMatchIn(t) -> "hurt"
            Regex("выстрел|стреля|снаряд").containsMatchIn(t) -> "shot"
            else -> null
        }
    }
}
