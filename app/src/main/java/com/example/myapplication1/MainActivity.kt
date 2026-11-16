package com.example.myapplication1

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class MainActivity : BaseActivity() {

    private lateinit var audio: StepAudioPlayer
    private lateinit var music: MusicPlayer
    private var restartMusicOnResume = false
    private var currentStep = "S000"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val opisanie: TextView = findViewById(R.id.Opisanie)
        val mapb: Button = findViewById(R.id.mapbutton)//Карта
        val tankb: Button = findViewById(R.id.TankButton)//Информация по танку
        val exitb: Button = findViewById(R.id.ExitButton)//Выходих игры
        val shot_1A = findViewById<ImageView>(R.id.shot_1A)
        val tankimage = findViewById<ImageView>(R.id.tankinfo)
        val mapimage1 = findViewById<ImageView>(R.id.mapimage)
        val bl: Button = findViewById(R.id.buttonleft)
        val br: Button = findViewById(R.id.buttonright)
        var root = 0
        var vrtext=""
        var waitcnt = 0
        var cnt=1
        var solution = "-1"

        // ---------- аватар говорящего ----------
        val avatarFrame: View = findViewById(R.id.avatarFrame)
        val avatarView: ImageView = findViewById(R.id.avatarView)
        val speakerName: TextView = findViewById(R.id.speakerName)
        val textScroll: ScrollView = findViewById(R.id.textScroll)
        val playerName = GameSettings.playerName(this)
        val playerAvatar = GameSettings.playerAvatar(this)
        var lastAvatarUri = ""

        // ---------- озвучка ----------
        val audioBar: View = findViewById(R.id.audioBar)
        val audioPlay: ImageButton = findViewById(R.id.audioPlay)
        val audioSeek: SeekBar = findViewById(R.id.audioSeek)
        val audioTime: TextView = findViewById(R.id.audioTime)
        val audioMute: ImageButton = findViewById(R.id.audioMute)
        var currentStepId = ""
        var userSeeking = false

        fun fmt(ms: Int): String {
            val s = ms / 1000
            return "%d:%02d".format(s / 60, s % 60)
        }

        // ---------- музыка ----------
        val musicButton: ImageButton = findViewById(R.id.musicButton)
        val nowPlaying: TextView = findViewById(R.id.nowPlaying)
        music = MusicPlayer(this)
        music.onTrackChanged = { title ->
            nowPlaying.text = "♪ $title"
            nowPlaying.visibility = View.VISIBLE
            nowPlaying.alpha = 0f
            nowPlaying.animate().alpha(1f).setDuration(300).withEndAction {
                nowPlaying.animate().alpha(0f).setStartDelay(2800).setDuration(600).start()
            }.start()
        }

        audio = StepAudioPlayer(this) { st ->
            music.setDucked(st.playing) // под звуки хода музыка тише
            audioBar.visibility = if (st.available) View.VISIBLE else View.GONE
            val voiceOn = GameSettings.voiceEnabled(this)
            audioMute.setImageResource(if (voiceOn) R.drawable.ic_game_volume_on else R.drawable.ic_game_volume_off)
            audioPlay.setImageResource(if (st.playing) R.drawable.ic_game_pause else R.drawable.ic_game_play)
            audioPlay.isEnabled = st.active
            audioPlay.alpha = if (st.active) 1f else 0.35f
            audioSeek.isEnabled = st.active
            audioSeek.max = st.durationMs.coerceAtLeast(1)
            if (!userSeeking) audioSeek.progress = st.positionMs
            audioTime.text = if (st.active && st.durationMs > 0) "${fmt(st.positionMs)} / ${fmt(st.durationMs)}" else "—"
        }
        audioPlay.setOnClickListener { audio.toggle() }
        audioMute.setOnClickListener {
            val on = !GameSettings.voiceEnabled(this)
            GameSettings.setVoiceEnabled(this, on)
            if (on) audio.play(currentStepId, true) else audio.stop()
        }
        audioSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) audioTime.text = "${fmt(progress)} / ${fmt(sb.max)}"
            }
            override fun onStartTrackingTouch(sb: SeekBar) { userSeeking = true }
            override fun onStopTrackingTouch(sb: SeekBar) {
                userSeeking = false
                audio.seekTo(sb.progress)
            }
        })

        listOf(mapb, tankb, exitb, bl, br).forEach { it.addPressEffect() }

        /**
         * Показать ход.
         * id      — уникальный код хода (имя файла озвучки: assets/audio/<id>.mp3)
         * speaker — кто говорит (необязательно): "mihailo", "smirnov", "natalson", "abrams", "crew", "group", "player"
         * event   — событие вместо лица (необязательно): "track", "fire", "wreck", "shot", "explosion",
         *           "hurt", "victory", "defeat", "none". Если не указать — угадывается по тексту.
         */
        fun F1(id: String, text: String, leftbuttontext: String, rightbuttontext: String, imagePath: String,
               speaker: String? = null, event: String? = null) {
            currentStepId = id
            currentStep = id
            opisanie.text = text
            textScroll.scrollTo(0, 0)
            opisanie.alpha = 0f
            opisanie.animate().alpha(1f).setDuration(250).start()
            bl.text = leftbuttontext
            br.text = rightbuttontext
            // пустая кнопка не показывается, вторая растягивается на всю ширину
            bl.visibility = if (leftbuttontext.isBlank()) View.GONE else View.VISIBLE
            br.visibility = if (rightbuttontext.isBlank()) View.GONE else View.VISIBLE
            Glide.with(this).load("file:///android_asset/$imagePath")
                .transition(DrawableTransitionOptions.withCrossFade(250))
                .into(shot_1A)

            val scene = SceneDirector.pick(text, leftbuttontext, rightbuttontext, speaker, event, playerAvatar, playerName)
            speakerName.text = scene.caption
            if (scene.uri != lastAvatarUri) {
                lastAvatarUri = scene.uri
                Glide.with(this).load(scene.uri).into(avatarView)
                avatarFrame.pop()
            }
            // предыдущая дорожка всегда останавливается — звук не накладывается
            audio.play(id, GameSettings.voiceEnabled(this))
            // фоновая музыка: смена трека по настроению хода, в концовке — мелодия победы/поражения
            music.onStep(id, scene.ending)
        }

        // ---------- меню музыки (кнопка ♪ в углу картинки) ----------
        fun dialog(b: AlertDialog.Builder) {
            b.setOnDismissListener { hideSystemBars() }
            b.show()
        }
        fun chooseTrack() {
            val q = music.customQueue()
            if (q.isEmpty()) {
                startActivity(Intent(this, MusicLibraryActivity::class.java)); restartMusicOnResume = true; return
            }
            dialog(AlertDialog.Builder(this).setTitle("Выбрать трек")
                .setSingleChoiceItems(q.map { it.name }.toTypedArray(), music.currentIndex()) { d, i -> music.playTrack(i); d.dismiss() })
        }
        fun choosePlaylist() {
            val lists = GameSettings.playlists(this)
            val names = listOf(getString(R.string.game_all_tracks)) + lists.map { "${it.name} (${it.uris.size})" }
            dialog(AlertDialog.Builder(this).setTitle("Выбрать плейлист")
                .setSingleChoiceItems(names.toTypedArray(), GameSettings.activePlaylist(this) + 1) { d, i ->
                    GameSettings.setActivePlaylist(this, i - 1)
                    music.restart(currentStepId)
                    d.dismiss()
                })
        }
        fun showMusicMenu() {
            val on = music.enabled
            val custom = music.isCustom
            val items = ArrayList<Pair<String, () -> Unit>>()
            if (on) items += "Выключить музыку" to { GameSettings.setMusicEnabled(this, false); music.restart(currentStepId) }
            else items += "Включить музыку" to { GameSettings.setMusicEnabled(this, true); music.restart(currentStepId) }
            if (on && custom) {
                items += "⏭  Следующий трек" to { music.next() }
                items += "⏮  Предыдущий трек" to { music.previous() }
                items += "Выбрать трек…" to { chooseTrack() }
                items += "Выбрать плейлист…" to { choosePlaylist() }
                items += "Режим: рекомендуемая музыка" to {
                    GameSettings.setMusicMode(this, GameSettings.MODE_RECOMMENDED); music.restart(currentStepId)
                }
            } else if (on) {
                items += "Режим: свои треки" to {
                    GameSettings.setMusicMode(this, GameSettings.MODE_CUSTOM)
                    if (GameSettings.tracks(this).isEmpty()) {
                        restartMusicOnResume = true
                        startActivity(Intent(this, MusicLibraryActivity::class.java))
                    } else music.restart(currentStepId)
                }
            }
            items += "Мои треки и плейлисты…" to {
                restartMusicOnResume = true
                startActivity(Intent(this, MusicLibraryActivity::class.java))
            }
            val title = if (on) "♪ ${music.title.ifBlank { "Музыка" }}" else "Музыка выключена"
            dialog(AlertDialog.Builder(this).setTitle(title)
                .setItems(items.map { it.first }.toTypedArray()) { _, i -> items[i].second.invoke() })
        }
        musicButton.addPressEffect()
        musicButton.setOnClickListener { showMusicMenu() }

        tankb.setOnClickListener {
            if (cnt%2==1)
            {
                vrtext=opisanie.text.toString()
                val text = SpannableString("<Вы открыли мат часть по танку. Чтобы прочесть, Кликните по ссылке https://drive.google.com/file>")
                text.setSpan(URLSpan("https://drive.google.com/file/d/111sjvvSuWP6ML5Ba9-itvWFy-0LNNceh/view?usp=drive_link"), text.indexOf("https"), text.length-1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                opisanie.text = text; opisanie.movementMethod = LinkMovementMethod.getInstance()
                Glide.with(this).load("file:///android_asset/Допка/Руководство по танку.jpg").into(tankimage); cnt++}
            else {cnt++;opisanie.movementMethod = null; Glide.with(this).clear(tankimage); opisanie.text=vrtext;}
        }
        mapb.setOnClickListener {
            if (cnt%2==1)
            { Glide.with(this).load("file:///android_asset/РУТ1/mapimageorig.jpg").into(mapimage1); vrtext=opisanie.text.toString(); opisanie.text="<Вы открыли карту>";cnt++}
            else {cnt++; opisanie.text=vrtext;Glide.with(this).clear(mapimage1);}
        }
        exitb.setOnClickListener {
            // если меню уже открыто под игрой — просто возвращаемся в него, иначе открываем
            if (isTaskRoot) startActivity(android.content.Intent(this, MainActivityMenu::class.java))
            finish()
        }

        F1("S000", if (playerName.isBlank()) "Привет, Танкист! Прочитай сводку" else "Привет, Танкист $playerName! Прочитай сводку",
            "Дальше", "", "Допка/Сводка.jpg")

        bl.setOnClickListener {
            if (solution == "-1") {
                F1("L001", "У тебя есть выбор: поехать налево или направо",
                    "Налево",
                    "Направо",
                    "РУТ1/shot_!!!.jpg"
                ); solution = "0"
            }
            else if (waitcnt >= 50) {
                F1("L002", "Ты ТАК ДОЛГО ждал, что пришла подмога! Союзные танковые дивизии на подходе, а сверху летят наши орлы на ИЛ-2!",
                    "ГГ",
                    "ГГ",
                    "Допка/ИЛ-2НаПодлете.jpg"
                ); solution = "ГГ"
            }
            else if (root == 0 && solution == "0") {
                F1("L003", "Вы решили ехать в сторону ущелья", "Дальше", "", "РУТ1/1-3/shot_1.jpg")
                root = 1; solution = "1.0.0"
            }
            else if (root == 1 && solution == "1.0.0") {
                F1("L004", "У вас выбор: ехать налево или направо", "Налево", "Направо", "РУТ1/1-3/shot_002.jpg")
                solution = "2.0.0"
            }
            else if (root == 1 && solution == "2.0.0") {
                F1("L005", "Вы решили ехать налево", "Дальше", "", "РУТ1/1-3/shot_002.1.0.jpg")
                solution = "2.1.0"
            }
            else if (root == 1 && solution == "2.1.0") {
                F1("L006", "У вас выбор: ждать или ехать дальше", "Ждать", "Дальше", "РУТ1/1-3/shot_002.1.1.jpg")
                solution = "2.1.1"
            }
            else if (root == 1 && solution == "2.1.1") {
                waitcnt++
                F1("L007", "Вы ждете $waitcnt раз. Может следует выбрать другое действие? Вы играете в танки, а не симулятор камня:D", "Ждать", "Дальше", "РУТ1/1-3/shot_002.1.1.jpg")
            }
            else if (root == 1 && solution == "2.2.1") {
                waitcnt++
                F1("L008", "Вы ждете $waitcnt раз. Может следует выбрать другое действие? Вы играете в танки, а не симулятор камня:D", "Ждать", "Дальше", "РУТ1/1-3/shot_002.2.1.jpg")
            }
            else if (root == 1 && solution == "3.3.0") {
                F1("L009", "Вы поехали налево", "Дальше", "", "РУТ1/4/shot_004.0.0.jpg")
                solution = "4"
            }
            else if (root == 1 && solution == "4") {
                F1("L010", "Вы поехали дальше", "Дальше", "", "РУТ1/4/shot_004.1.0.jpg")
                solution = "4.1.0"
            }
            else if (root == 1 && solution == "4.1.0") {
                F1("L011", "Командир, мы подъезжаем к позиции врага", "Дальше", "", "РУТ1/4/shot_004.2.0.jpg")
                solution = "4.2.0"
            }
            else if (root == 1 && solution == "4.2.0") {
                F1("L012", "<Вы уже у позиции вражеского танка, Наводчик уже наводит орудие>", "Дальше", "", "РУТ1/4/shot_004.3.0.jpg")
                solution = "4.3.0"
            }
            else if (root == 1 && solution == "4.3.0") {
                F1("L013", "Командир, Вражеский Шерман на 9 часов! Что делать?", "Да поможет нам Господь..", "Включить реактивные двигатели, попробуем улететь< Cтоп, ЧТО?!>", "РУТ1/4/shot_004.4.0.jpg")
                solution = "4.4.0"
            }
            else if (root == 1 && solution == "4.4.0") {
                F1("L014", "<Понимая неизбежность смерти, вы молитесь, экипаж все понимает, и умирает с миром. Вы проиграли, но это не самый плохой конец..>", "ГГ", "ГГ", "РУТ1/4/shot_004.5.0.jpg")
                solution = "4.5.0"
            }
            else if (root == 1 && solution == "6.7.0") {
                F1("L015", "<Для разнообразия Гейминга, вы наблюдаете от лица Заряжающего, вашей правой руки.>", "", "Дальше", "РУТ1/5-6/shot_006.8.0.jpg")
                solution = "6.8.0Z"
            }
            else if (root == 1 && solution == "6.10.0Z") {
                F1("L016", "<Вы оборачиваетесь и видите аллею из растительности и домов по левому флангу и хорошо простреливаемое поле по правому флангу>", "", "Вернуться к машине", "РУТ1/5-6/shot_006.11.0.jpg")
                solution = "6.11.0Z"
            }
            else if (root == 1 && solution == "6.10.0ZW") {
                F1("L017", "Вы поступаете с умом, высаживая Смирнова у наблюдательной вышки для слежки за радиосообщениями противника и в качестве наблюдателя за полем боя. Вы едете окапываться для обороны захваченного опорного пунтка противника", "Дальше", "", "РУТ1/7-Оборона/shot_7.0.0.jpg")
                solution = "7.0.0"
            }
            else if (root == 1 && (solution == "6.10.0SW" || solution == "6.10.0ZC")) {
                F1("L018", "<У вас есть признаки разума, раз вы дошли до этого рута. Но хватит ли это вам для победы? Вы выбрали напасть на противников через аллею.>", "", "Дальше", "РУТ1/8-Скрытность/shot_8.0.0.jpg")
                solution = "8.0.0"
            }
            else if (root == 1 && solution == "7.0.0") {
                F1("L019", "<Смирнов докладывает о приближающемся танке с левого фланга>", "Навести орудие на 10 часов", "", "РУТ1/7-Оборона/shot_7.1.0.jpg")
                solution = "7.1.0"
            }
            else if (root == 1 && solution == "7.1.0") {
                F1("L020", "<Вы навели орудие на предполагаемое расположение вражеского танка>", "Посмотреть через триплекс", "", "РУТ1/7-Оборона/shot_7.2.0.jpg")
                solution = "7.2.0"
            }
            else if (root == 1 && solution == "7.2.0") {
                F1("L021", "<Вы видите вражеский танк>", "Сесть на место заряжающего", "", "РУТ1/7-Оборона/shot_7.3.0.jpg")
                solution = "7.3.0"
            }
            else if (root == 1 && solution == "7.3.0") {
                F1("L022", "<Сев за место заряжающего, вы навели орудие>", "ВЫСТРЕЛ!", "", "РУТ1/7-Оборона/shot_7.4.0.jpg")
                solution = "7.4.0"
            }
            else if (root == 1 && solution == "7.4.0") {
                F1("L023", "<Бронебойный с искрами входит в танк>", "Дальше", "", "РУТ1/7-Оборона/shot_7.5.0.jpg")
                solution = "7.5.0"
            }
            else if (root == 1 && solution == "7.5.0") {
                F1("L024", "<Танк пылает>", "Дальше", "", "РУТ1/7-Оборона/shot_7.6.0.jpg")
                solution = "7.6.0"
            }
            else if (root == 1 && solution == "7.6.0") {
                F1("L025", "Противник, осознав факт перехвата связи, принял решение на отход. Прибыло долгожданное союзное подкрепление. Личный состав экипажа представлен к награждению орденами Красного Знамени. Документы переданы командованию. Благодаря вашим действиям сорвано готовившееся контрнаступление противника, что спасло группу армий «Центр»", "GG", "GG", "РУТ1/7-Оборона/VictoryRoute1.jpg")
                solution = "7.7.0"
            }
            else if (root == 1 && solution == "9.7.0") {
                F1("L026", "<Бля... у тебя НЕТ ЭНЕРГЕТИЧЕСКОГО ПОЛЯ!>", "", "Шнейне((", "РУТ1/9-На яйцах/shot_9.7.0.jpg")
                solution = "9.7.0"
            }
            else if (root == 1 && solution == "6.0.0") {
                F1("L027", "<Не нужно было надеяться на благоразумие Смирнова...>", "ГГ", "ГГ", "РУТ1/4/shot_004.5.0.jpg")
                solution = "6.0.0rip"
            }
            //===============================РУТ 3 LEFT BUTTON===================
            //===============================||==================================
            //===============================||==================================
            //===============================\/==================================
            else if(solution=="0" && root==23)
            {
                F1("L028", "Вы выбрали играть на неожиданности, поехав вдоль левого берега озера","","Дальше","РУТ3/1/shot_001.1.0.jpg")
                root=3
                solution="1.0.0"
            }
            else if(solution=="1.10.0" && root==3)
            {
                F1("L029", "<Вы приказываете экипажу эвакуироваться. Через мгновение происхдит вспышка и танк подбивают. Выбрался лишь тов. Михайло и Вы. Вы корите себя в смерти Натальсона, ну и Смирнова. В дыме горящего танка вы отступаете, задание провалено>","ГГ","ГГ","РУТ3/1/shot_001.11.0.jpg")
                solution="1.11.0"
            }
            else if(solution=="1.8.0" && root==3)
            {
                F1("L030", "<Вы приказываете механику водителю развернуться. Интуиция подсказывает вам, что это был правильный выбор>","Дальше","","РУТ3/3-ОтступлениеДлинное/shot_003.0.0.jpg")
                solution="3.0.0"
            }
            else if((solution=="3.0.0"|| solution=="1.6.0") && root==3)
            {
                F1("L031", "<Вы все дальше отдаляетесь от того злополучного места>","Дальше","","РУТ3/3-ОтступлениеДлинное/shot_003.1.0.jpg")
                solution="3.1.0"
            }
            else if(solution=="3.1.0" && root==3)
            {
                F1("L032", "<Вы едете дальше>","Ну што-ш, дальше","","РУТ3/4-7/shot_004.0.0.jpg")
                solution="4.0.0"
            }
            else if(solution=="4.0.0" && root==3)
            {
                F1("L033", "<У вас выбор: ехать в город или в ехать ущелье>","В город","В ущелье","РУТ3/4-7/shot_004.1.0.jpg")
                solution="4.1.0"
            }

            //===============================РУТ 2 LEFT BUTTON===================
            //===============================||==================================
            //===============================||==================================
            //===============================\/==================================



            else if((solution=="4.1.0" && root==3) || (root==2 && solution=="1.0.0"))
            {
                F1("L034", "<Вы поехали в сторону города, у вас выбор: ехать по прямой, или поехать в обход>","По прямой","В обход","РУТ3/4-7/shot_5-6.jpg")
                solution="2-3"
                root=2
            }
            else if(root==2 && solution=="2-3")
            {
                F1("L035", "<Вы поехали в сторону города по прямой>","Дальше","","РУТ2/1-2/shot_002.0.0.jpg")
                solution="2.0.0"
                root=2
            }
            else if(root==2 && solution=="2.0.0")
            {
                F1("L036", "<Вы открываете личное дело экипажа, по нему вы узнаете имена братьев по оружию: механик-водитель- Михайло В.П., наводчик- Смирнов Ю.П, заряжающий- Натальсон Л.В.>","Далее","","РУТ2/1-2/shot_002.0.1.jpg")
                solution="2.0.0ч1"
            }
            else if(root==2 && solution=="2.0.0ч1")
            {
                F1("L037", "Тов. Михайло: Командир, куда едем?","Заворачивай левее, Михайло","Едем прямо","РУТ2/1-2/shot_002.0.1.jpg")
                solution="2.0.0ч2"
            }
            else if(root==2 && solution=="2.0.0ч2")
            {
                F1("L038", "Тов. Михайло: Так точно!\n<Вы сворачиваете налево, в сторону береговой части города>","Дальше","","РУТ2/2.1/shot_002.1.0.jpg")
                solution="2.1.0"
            }
            else if(root==2 && solution=="2.1.0")
            {
                F1("L039", "<Читая дело экипажа, вы узнаёте: все — тигры, закалённые в боях, кроме наводчика Смирнова. У него отклонения, это нужно учитывать. При принятии решений прислушивайтесь к заряжающему Натальсону — уроженцу Литвы и просто хорошему человеку.>","Дальше","","РУТ2/2.1/shot_002.1.1.jpg")
                solution="2.1.1"
            }
            else if(root==2 && solution=="2.1.1")
            {
                F1("L040", "<Вам открываются прекрасные виды на озеро ТуркВойс и горы Хомстейк и ХолиКросс. Экипаж воодушевлен, но не стоит забывать, где вы находитесь>","Дальше","","РУТ2/2.1/shot_002.1.2.jpg")
                solution="2.1.2"
            }
            else if(root==2 && solution=="2.1.2")
            {
                F1("L041", "Тов. Натальсон: Командир, может свернем направо?","Дальше","Михайло, поварачивай на 2 часа","РУТ2/2.1/shot_002.1.3-!!!.jpg")
                solution="2.1.3"
            }
            else if(root==2 && solution=="2.1.3")
            {
                F1("L042", "<Вы поехали дальше>","Дальше","","РУТ2/2.1/shot_002.1.4.jpg")
                solution="2.1.4"
            }
            else if(root==2 && solution=="2.1.4")
            {
                F1("L043", "<Летит снаряд>","Дальше","","РУТ2/2.1/shot_002.1.5.jpg")
                solution="2.1.5"
            }
            else if(root==2 && solution=="2.1.5")
            {
                F1("L044", "<Somewhere on the other side of the lake>\nK.Abrams: Оne less commie!","Next","","РУТ2/2.1/shot_002.1.6.jpg")
                solution="2.1.6"
            }
            else if(root==2 && solution=="2.1.6")
            {
                F1("L045", "<Это была ловушка, вы вышли хорошей мишенью. Весь экипаж погиб, задание провалено.>","ГГ","ГГ","РУТ2/2.1/shot_002.1.7.jpg")
                solution="2.1.7"
            }
            else if(root==2 && solution=="3.1.1")
            {
                F1("L046", "<Вы едете дальше по объездной дороге>","Михайло, сбавь обороты, заедем через другие переулки","Дальше","РУТ2/3/shot_003.2.0.jpg")
                solution="3.2.0"
            }
            else if(root==2 && solution=="3.0.7")
            {
                F1("L047", "Вы выбрали ехать в город через переулки","Дальше","","РУТ2/3/shot_003.1.1.0.jpg")
                solution="3.1.1.0"
            }
            else if(root==2 && solution=="3.1.1.0")
            {
                F1("L048", "Вы объезжаете дом за домом","Дальше","","РУТ2/3/shot_003.1.1.1.jpg")
                solution="3.1.1.1"
            }
            else if(root==2 && solution=="3.1.1.1")
            {
                F1("L049", "У вас выбор: ехать налево или ехать направо","Налево","Направо","РУТ2/3/shot_003.1.1.2-!!!.jpg")
                solution="3.1.1.2"
            }
            else if(root==2 && solution=="3.1.1.2")
            {
                F1("L050", "Вы поехали налево","Дальше","","РУТ2/3/shot_003.1.1.3.jpg")
                solution="3.1.1.3"
            }
            else if(root==2 && solution=="3.1.1.3")
            {
                F1("L051", "Вы все дальше продвигаетесь по городу","","Дальше","РУТ2/3/shot_003.1.1.4.jpg")
                solution="3.1.1.4"
            }
            else if(root==2 && solution=="3.4.1")
            {
                F1("L052", "<Звонит Смерть в нищете, нельзя отклонить>\n Тут была засада, что могу сказать, надо было не брать этот рут, я же говорил. Экипаж погиб, задание провалено бла,бла,бла","ГГ","ГГ","РУТ2/2.1/shot_002.1.7.jpg")
                solution="Смерть"
            }
            else if(root==2 && solution=="4.0.6")
            {
                F1("L053", "<Михайло резко останавливается, и очень вовремя, Шерман выстрелил, рикошет о башню танка всех оглушает>","","Смирнов, ВЫСТРЕЛ!","РУТ2/4/shot_004.0.7.jpg")
            }
            else if(root==2 && solution=="4.0.10")
            {
                F1("L054", "<Вы заезжаете на левую улицу>","Дальше","","РУТ2/4/shot_004.1.0.jpg")
                solution="4.1.0"
            }
            else if(root==2 && solution=="4.1.0")
            {
                F1("L055", "<Вы выезжаете на перекресток>","Смирнов, орудие на 3 часа, Михайло, будь готов остановиться!","","РУТ2/4/shot_004.1.1.jpg")
                solution="4.1.1"
            }
            else if(root==2 && solution=="4.1.1")
            {
                F1("L056", "<Михайло своё дело знает — машина встала точно для выстрела. К сожалению, про Смирнова такого не сказать, он ******>","Смирнов, ты при*****!","","РУТ2/4/shot_004.1.2.jpg")
                solution="4.1.2"
            }
            else if(root==2 && solution=="4.1.2")
            {
                F1("L057", "<К счастью, шерман смотрит в другую сторону, Смирнов доварачивает орудие>","Смирнов, быстрее бля!","","РУТ2/4/shot_004.1.3.jpg")
                solution="4.1.3"
            }
            else if(root==2 && solution=="4.1.3")
            {
                F1("L058", "Тов. Смирнов: Командир, орудие наведе..","Смирнов, ВЫСТРЕЛ!!","","РУТ2/4/shot_004.1.4.jpg")
                solution="4.1.4"
            }
            else if(root==2 && solution=="4.1.4")
            {
                F1("L059", "<Смирнов делает выстрел>","Дальше","","РУТ2/4/shot_004.1.5.jpg")
                solution="4.1.5"
            }
            else if(root==2 && solution=="4.1.5")
            {
                F1("L060", "<Сквозь едкий дым выстрела вы различаете догорающий танк>","Михайло, выезжаем из города, полный вперед!","","РУТ2/4/shot_004.1.6.jpg")
                solution="4.1.6"
            }
            else if(root==2 && solution=="4.1.6")
            {
                F1("L061", "<Вы выезжаете из города>","Дальше","","РУТ2/4/shot_004.1.7.jpg")
                solution="4.1.7"
            }
            else if(root==2 && (solution=="4.1.7" ||solution=="5.3.0"))
            {
                F1("L062", "<Выехав из города, вы приказываете Михайло>","Михайло, едем налево!","Не сбавляй обороты Михайло, едем прямо!","РУТ2/4/shot_004.1.8.jpg")
                solution="4.1.8"
            }
            else if(root==2 && solution=="4.1.8")
            {
                F1("L063", "<Ловко потянув рычаги, Михайло сворачивает налево. Вы едете по полю, это истинный рут Рэмбо!>","Дальше","","РУТ2/4/shot_004.3.0.jpg")
                solution="4.3.0"
            }
            else if(root==2 && solution=="4.3.0")
            {
                F1("L064", "<Едете дальше>","Дальше","","РУТ2/4/shot_004.3.1.jpg")
                solution="4.3.1"
            }
            else if(root==2 && solution=="4.3.1")
            {
                F1("L065", "<Едете дальше>","Дальше","","РУТ2/4/shot_004.3.2.jpg")
                solution="4.3.2"
            }
            else if(root==2 && solution=="4.3.2")
            {
                F1("L066", "<Едете дальше>","Дальше","","РУТ2/4/shot_004.3.3.jpg")
                solution="4.3.3"
            }
            else if(root==2 && solution=="4.3.3")
            {
                F1("L067", "<Едете дальше>","Дальше","","РУТ2/4/shot_004.3.4.jpg")
                solution="4.3.4"
            }
            else if(root==2 && solution=="4.3.4")
            {
                F1("L068", "<Вы замечаете вспышку со стороны штаба противника>","Дальше","","РУТ2/4/shot_004.3.6.jpg")
                solution="4.3.6"
            }
            else if(root==2 && solution=="4.3.6")
            {
                F1("L069", "<Противник сбивает вам левую гусеницу>","Стоп машина! Смирнов орудие,направо!","","РУТ2/4/shot_004.3.7.jpg")
                solution="4.3.7"
            }
            else if(root==2 && solution=="4.3.7")
            {
                F1("L070", "<Михайло останавливает машину, однако машину занесло! Смирнов доводит орудие>","Дальше","","РУТ2/4/shot_004.3.8.jpg")
                solution="4.3.8"
            }
            else if(root==2 && solution=="4.3.8")
            {
                F1("L071", "<Тандерболт делает контрольный выстрел, Вам не избежать смерти>","Принять смерть достойно","Включить систему защиты 'Шотный совок'","РУТ2/4/shot_004.3.9.jpg")
                solution="4.3.9"
            }
            else if(root==2 && solution=="4.3.9")
            {
                F1("L072", "<Вы умираете с миром. Экипаж погибает, задание провалено, вы награждены посмертно орденами Красного знамени 1 степени.>\n~Истинная концовка Рэмбо","ГГ","ГГ","РУТ2/4/shot_004.3.10.jpg")
                solution="4.3.10"
            }
            else if(root==2 && solution=="5.0.4") {
                F1("L073", "<Вы приказали свернуть к центральным улицам, но это была засада>","Дальше","","РУТ2/5/shot_005.1.0.jpg")
                solution="5.1.0"
            }
            else if(root==2 && solution=="5.1.0") {
                F1("L074", "<Экипаж погиб. Задание провалено>","ГГ","ГГ","РУТ2/4/shot_004.2.2.jpg")
                solution="Смерть"
            }
            else if(root==2 && solution=="5.2.10") {
                F1("L075", "<Не нужно было надеяться на благоразумие Смирнова...>\nЭкипаж погиб. Задание провалено", "ГГ", "ГГ", "РУТ1/4/shot_004.5.0.jpg")
                solution="Смерть"
            }
            else if(root==2 && solution=="5.2.13") {
                F1("L076", "<Вы едете сворачиваете в сторону полей>","Дальше","","РУТ2/5/shot_005.3.0.jpg")
                solution="5.3.0"
            }
            else if(root==2 && solution=="6.2.2") {
                F1("L077", "<Вы навелись на тандерболт. Шанс пробития не высок!>","Стрелять!","Ждать","РУТ2/6/shot_006.2.3.jpg")
                solution="6.2.3"
            }
            else if(root==2 && solution=="6.2.3") {
                F1("L078", "<Вы выстрелили преждевременно. Тандерболт стреляет и уничтожает ваш танк. Экипаж погибает, задание провалено>","ГГ", "ГГ", "РУТ1/4/shot_004.5.0.jpg")
                solution="Смерть"
            }

            else if(root==2 && solution=="6.2.4") {
                F1("L079", "<Вы навелись на уязвимость тандерболта-пулеметное гнездо>","Стрелять!","","РУТ2/6/shot_006.2.5.jpg")
                solution="6.2.5"
            }
            else if(root==2 && solution=="6.2.5") {
                F1("L080", "<Выстрел>","Дальше","","РУТ2/6/shot_006.2.6.jpg")
                solution="6.2.6"
            }
            else if(root==2 && solution=="6.2.6") {
                F1("L081", "<Вы видите прямое попадание по танку. Танк уничтожен! Вы пересаживаетесь на место командира>","Дальше.","Отъехать назад","РУТ2/6/shot_006.2.7.jpg")
                solution="6.2.7"
            }
            else if(root==2 && solution=="6.2.7") {
                F1("L082", "<Вы едете дальше>","Дальше","","РУТ2/6/shot_006.3.0.jpg")
                solution="6.3.0"
            }
            else if(root==2 && solution=="6.3.0") {
                F1("L083", "<Это засада!>","Дальше","","РУТ2/6/shot_006.3.1.jpg")
                solution="6.3.1"
            }
            else if(root==2 && solution=="6.3.1") {
                F1("L084", "<Выстрел!>","Дальше","","РУТ2/6/shot_006.3.2.jpg")
                solution="6.3.2"
            }
            else if(root==2 && solution=="6.3.2") {
                F1("L085", "<Последнее что вы увидели, это снаряд летящий в вас! Экипаж погиб, задание провалено>","ГГ","ГГ","РУТ2/6/shot_006.3.3.jpg")
                solution="Смерть"
            }
            else if(root==2 && solution=="6.4.1") {
                F1("L086", "<Вы подъезжаете к сараю>","Дальше", "", "РУТ2/6/shot_006.4.2.jpg")
                solution="6.4.2"
            }
            else if(root==2 && solution=="6.4.2") {
                F1("L087", "<Немного присмотревшись, вы замечаете вражеский танк!>","Дальше", "", "РУТ2/6/shot_006.4.3.jpg")
                solution="6.4.3"
            }
            else if(root==2 && solution=="6.4.3") {
                F1("L088", "<Вы садитесь на место наводчика и наводите орудие>","ВЫСТРЕЛ!", "", "РУТ2/6/shot_006.4.4.jpg")
                solution="6.4.4"
            }
            else if(root==2 && solution=="6.4.4") {
                F1("L089", "<Вы стреляете>","Дальше", "", "РУТ2/6/shot_006.4.5.jpg")
                solution="6.4.5"
            }
            else if(root==2 && solution=="6.4.5") {
                F1("L090", "<Взрыв охватывает вражеский танк!>","Дальше", "", "РУТ2/6/shot_006.4.6.jpg")
                solution="6.4.6"
            }
            else if(root==2 && solution=="6.4.6") {
                F1("L091", "<Танк уничтожен!>","Дальше", "", "РУТ2/6/shot_006.4.7.jpg")
                solution="6.4.7"
            }
            else if(root==2 && solution=="6.4.7") {
                F1("L092", "<Вы заезжаете на вражеский опорный пункт! Вы докладываете о полной зачистке города Лидвилл. Вскоре подъезжает подкрепление. Все живы кроме младшего офицера Михайло А.П. Экипаж награжден медалями за отвагу и орденами красного знамени. Задание выполнено>\n~Канон концовка","ГГ", "ГГ", "РУТ2/6/shot_006.4.8.jpg")
                solution="6.4.8"
            }








        }







        br.setOnClickListener {
            if (root == 1 && solution == "2.0.0") {
                F1("R001", "Вы решили ехать направо", "", "Дальше", "РУТ1/1-3/shot_002.2.0.jpg")
                solution = "2.2.0"
            }
            else if (root == 1 && solution == "2.2.0") {
                F1("R002", "У вас выбор: ждать или ехать дальше", "Ждать", "Дальше", "РУТ1/1-3/shot_002.2.1.jpg")
                solution = "2.2.1"
            }
            else if (root == 1 && solution == "2.2.1") {
                F1("R003", "Вы выбрали ехать дальше", "", "Дальше", "РУТ1/1-3/shot_003.jpg")
                solution = "3"
            }
            else if (root == 1 && solution == "2.1.1") {
                F1("R004", "Вы выбрали ехать дальше", "", "Дальше", "РУТ1/1-3/shot_002.1.2.jpg")
                solution = "2.1.2"
            }
            else if (root == 1 && solution == "2.1.2") {
                F1("R005", "Вы выбрали ехать дальше", "", "Дальше", "РУТ1/1-3/shot_002.1.3.jpg")
                solution = "2.1.3"
            }
            else if (root == 1 && solution == "2.1.3") {
                F1("R006", "Вы выбрали ехать дальше", "", "Дальше", "РУТ1/1-3/shot_003.jpg")
                solution = "3"
            }
            else if (root == 1 && solution == "3") {
                F1("R007", "<Летит снаряд>", "", "Дальше", "РУТ1/1-3/shot_003.1.0.jpg")
                solution = "3.1.0"
            }
            else if (root == 1 && solution == "3.1.0") {
                F1("R008", "Командир, снаряд на 13 часов!", "", "Дальше", "РУТ1/1-3/shot_003.1.1.jpg")
                solution = "3.1.1"
            }
            else if (root == 1 && solution == "3.1.1") {
                F1("R009", "Командир, Вражеский танк скрылся, едем дальше?", "", "Да, черт возьми, нечего **** мять!", "РУТ1/1-3/shot_003.2.0.jpg")
                solution = "3.2.0"
            }
            else if (root == 1 && solution == "3.2.0") {
                F1("R010", "У вас выбор: поехать налево или направо", "Едем налево, заедем к нему в тыл", "Едем направо, встретим танк влоб", "РУТ1/1-3/shot_003.3.0.jpg")
                solution = "3.3.0"
            }
            else if (root == 1 && solution == "3.3.0") {
                F1("R011", "Вы поехали направо", "", "Дальше", "РУТ1/5-6/shot_005.0.0.jpg")
                solution = "5.0.0"
            }
            else if (root == 1 && solution == "4.4.0") {
                F1("R012", "У нас не было реактивных двигателей. Мог бы и помолиться перед смертью..", "ГГ", "ГГ", "РУТ1/4/shot_004.5.0.jpg")
                solution = "4.5.0"
            }
            else if (root == 1 && solution == "5.0.0") {
                F1("R013", "Вы едете дальше.", "", "Смирнов, наводи орудие на 10 часов, будь готов к выстрелу!", "РУТ1/5-6/shot_005.1.0.jpg")
                solution = "5.1.0"
            }
            else if (root == 1 && solution == "5.1.0") {
                F1("R014", "Командир, вражеский танк прямо по курсу!", "", "Смирнов, блять, ВЫСТРЕЛ!", "РУТ1/5-6/shot_005.2.0.jpg")
                solution = "5.2.0"
            }
            else if (root == 1 && solution == "5.2.0") {
                F1("R015", "<Смирнов делает выстрел, еще бы немного, и вам была бы пизда.>", "", "Твою мать, Смирнов! Молись, что ты попал", "РУТ1/5-6/shot_005.3.0.jpg")
                solution = "5.3.0"
            }
            else if (root == 1 && solution == "5.3.0") {
                F1("R016", "<Вы видите уничтоженный шерман. Экипаж с облегчением вздыхает. Смирнов успел...>", "", "Бляя, Смирнов, любишь же ты играть на нервах.. Едем дальше", "РУТ1/5-6/shot_005.4.0.jpg")
                solution = "5.4.0"
            }
            else if (root == 1 && solution == "5.4.0") {
                F1("R017", "<Вы сбавляете скорость перед выездом из-за ущелья. Интуиция подсказывает, что рядом есть вражеский танк, вы приказываете быть наготове>", "", "Дальше", "РУТ1/5-6/shot_005.5.0.jpg")
                solution = "5.5.0"
            }
            else if (root == 1 && solution == "5.5.0") {
                F1("R018", "<Интуиция не подвела вас, вы наблюдаете в триплексе Эмчу на 9 часов>", "Довериться Смирнову", "Смирнов, съебался!", "РУТ1/5-6/shot_006.0.0.jpg")
                solution = "6.0.0"
            }
            else if (root == 1 && solution == "6.0.0") {
                F1("R019", "<Вы навелись на вражеский танк>", "", "ВЫСТРЕЛ!", "РУТ1/5-6/shot_006.1.0.jpg")
                solution = "6.1.0"
            }
            else if (root == 1 && solution == "6.1.0") {
                F1("R020", "<Выстрел из 122 миллиметрового оглушает весь экипаж>", "", "Дальше", "РУТ1/5-6/shot_006.2.0.jpg")
                solution = "6.2.0"
            }
            else if (root == 1 && solution == "6.2.0") {
                F1("R021", "<Вы наблюдаете плоды своего прекрасного выстрела>", "", "Дальше", "РУТ1/5-6/shot_006.3.0.jpg")
                solution = "6.3.0"
            }
            else if (root == 1 && solution == "6.3.0") {
                F1("R022", "<Вы едете дальше>", "", "Дальше", "РУТ1/5-6/shot_006.4.0.jpg")
                solution = "6.4.0"
            }
            else if (root == 1 && solution == "6.4.0") {
                F1("R023", "<Вы подъезжаете к вражеской базе>", "", "Дальше", "РУТ1/5-6/shot_006.5.0.jpg")
                solution = "6.5.0"
            }
            else if (root == 1 && solution == "6.5.0") {
                F1("R024", "<Вы приказываете мехводу остановиться у наблюдательной башни.>", "", "Дальше", "РУТ1/5-6/shot_006.6.0.jpg")
                solution = "6.6.0"
            }
            else if (root == 1 && solution == "6.6.0") {
                F1("R025", "<Подъехав к вышке, вы выбираете, кто пойдет на вылазку>", "Выбрать Заряжающего", "Выбрать Смирнова", "РУТ1/5-6/shot_006.7.0.jpg")
                solution = "6.7.0"
            }
            else if (root == 1 && solution == "6.7.0") {
                F1("R026", "<Для разнообразия Гейминга, вы наблюдаете от лица Смирнова.>", "", "Дальше", "РУТ1/5-6/shot_006.8.0.jpg")
                solution = "6.8.0S"
            }
            else if (root == 1 && solution == "6.8.0Z") {
                F1("R027", "<Вы видите лестницу>", "", "Подняться", "РУТ1/5-6/shot_006.9.0.jpg")
                solution = "6.9.0Z"
            }
            else if (root == 1 && solution == "6.8.0S") {
                F1("R028", "<Вы видите лестницу>", "", "Подняться", "РУТ1/5-6/shot_006.9.0.jpg")
                solution = "6.9.0S"
            }
            else if (root == 1 && solution == "6.9.0S") {
                F1("R029", "<Поднявшись, вы видите радио, работающее на частоте противника и документы, похожие на те, что могут повлиять на исход боя>", "", "Забрать документы и вернуться к танку", "РУТ1/5-6/shot_006.10.0.jpg")
                solution = "6.10.0S"
            }
            else if (root == 1 && solution == "6.9.0Z") {
                F1("R030", "<Поднявшись, вы видите радио, работающее на частоте противника и документы, похожие на те, что могут повлиять на исход боя>", "Забрать документы, но осмотреться перед возвращением", "Забрать документы и вернуться к танку", "РУТ1/5-6/shot_006.10.0.jpg")
                solution = "6.10.0Z"
            }
            else if (root == 1 && solution == "6.10.0S" && waitcnt > 0) {
                F1("R031", "<Смирнов отдал документы. По ним вы узнаете, что город доверили подразделению 7-5(7-ая рота 5 взвод) под командованием Легендарного Аса Крейтона Абрамса. Оценив вышеизложенную информацию и тот факт, что союзное подкрепление в скором времени прибудет к вам, вы объявляете свое решение команде>", "Нападем по-умному. Едем по левому флангу", "Нападаем в лоб, они такого точно не ожидают", "РУТ1/5-6/shot_006.7.0.jpg")
                solution = "6.10.0SW"
            }
            else if (root == 1 && solution == "6.10.0S" && waitcnt == 0) {
                F1("R032", "<Смирнов отдал документы. По ним вы узнаете, что город доверили подразделению 7-5(7-ая рота 5 взвод) под командованием Легендарного Аса Крейтона Абрамса. Оценив вышеизложенную информацию, вы объявляете свое решение команде>", "", "Нападаем в лоб, они такого точно не ожидают", "РУТ1/5-6/shot_006.7.0.jpg")
                solution = "6.10.0SC"
            }
            else if (root == 1 && solution == "6.11.0Z" && waitcnt > 0) {
                F1("R033", "<Заряжающий отдал документы и доложил обстановку. По ним вы узнаете, что город доверили подразделению 7-5(7-ая рота 5 взвод) под командованием Легендарного Аса Крейтона Абрамса. Оценив вышеизложенную информацию и тот факт, что союзное подкрепление в скором времени прибудет к вам, вы объявляете свое решение команде>", "Сыграем в оборону. Нечего рисковать", "", "РУТ1/5-6/shot_006.7.0.jpg")
                solution = "6.10.0ZW"
            }
            else if (root == 1 && solution == "6.11.0Z" && waitcnt == 0) {
                F1("R034", "<Заряжающий отдал документы и доложил обстановку. По ним вы узнаете, что город доверили подразделению 7-5(7-ая рота 5 взвод) под командованием Легендарного Аса Крейтона Абрамса. Оценив вышеизложенную информацию и тот факт, что союзное подкрепление в скором времени прибудет к вам, вы объявляете свое решение команде>", "Нападем по-умному. Едем по левому флангу", "Нападаем в лоб, они такого точно не ожидают", "РУТ1/5-6/shot_006.7.0.jpg")
                solution = "6.10.0ZC"
            }
            else if (root == 1 && solution == "6.10.0Z" && waitcnt > 0) {
                F1("R035", "<Заряжающий отдал документы. По ним вы узнаете, что город доверили подразделению 7-5(7-ая рота 5 взвод) под командованием Легендарного Аса Крейтона Абрамса. Оценив вышеизложенную информацию и тот факт, что союзное подкрепление в скором времени прибудет к вам, вы объявляете свое решение команде>", "Сыграем в оборону. Нечего рисковать", "", "РУТ1/5-6/shot_006.7.0.jpg")
                solution = "6.10.0ZW"
            }
            else if (root == 1 && solution == "6.10.0Z" && waitcnt == 0) {
                F1("R036", "<Заряжающий отдал документы. По ним вы узнаете, что город доверили подразделению 7-5(7-ая рота 5 взвод) под командованием Легендарного Аса Крейтона Абрамса. Оценив вышеизложенную информацию и тот факт, что союзное подкрепление в скором времени прибудет к вам, вы объявляете свое решение команде>", "Нападем по-умному. Едем по левому флангу", "Нападаем в лоб, они такого точно не ожидают", "РУТ1/5-6/shot_006.7.0.jpg")
                solution = "6.10.0ZC"
            }
            else if (root == 1 && solution == "8.0.0") {
                F1("R037", "Командир, засада!", "", "Стоп машина!", "РУТ1/8-Скрытность/shot_8.1.0.jpg")
                solution = "8.1.0"
            }
            else if (root == 1 && solution == "8.1.0") {
                F1("R038", "<Машина не успела вовремя остановиться. Выстрел Шермана!>", "", "Дальше", "РУТ1/8-Скрытность/shot_8.2.0.jpg")
                solution = "8.2.0"
            }
            else if (root == 1 && solution == "8.2.0") {
                F1("R039", "<Броня не пробита!>", "", "Наводись!", "РУТ1/8-Скрытность/shot_8.3.0.jpg")
                solution = "8.3.0"
            }
            else if (root == 1 && solution == "8.3.0") {
                F1("R040", "Командир, мы наведены на Шермана!", "", "Выстрел!", "РУТ1/8-Скрытность/shot_8.4.0.jpg")
                solution = "8.4.0"
            }
            else if (root == 1 && solution == "8.4.0") {
                F1("R041", "<Выстрел попал точно в цель!>", "", "Проверить, чтобы наверняка", "РУТ1/8-Скрытность/shot_8.5.0.jpg")
                solution = "8.5.0"
            }
            else if (root == 1 && solution == "8.5.0") {
                F1("R042", "<Шерман уничтожен!>", "", "Ехать дальше", "РУТ1/8-Скрытность/shot_8.6.0.jpg")
                solution = "8.6.0"
            }
            else if (root == 1 && solution == "8.6.0") {
                F1("R043", "<Вы объезжаете свежевзорванный танк противника>", "", "Ехать дальше", "РУТ1/8-Скрытность/shot_8.7.0.jpg")
                solution = "8.7.0"
            }
            else if (root == 1 && solution == "8.7.0") {
                F1("R044", "<Вы едете дальше>", "", "Што-ш, едем дальше", "РУТ1/8-Скрытность/shot_8.8.0.jpg")
                solution = "8.8.0"
            }
            else if (root == 1 && solution == "8.8.0") {
                F1("R045", "<Вы выезжаете из аллеи растительности и домов>", "", "Едем дальше", "РУТ1/8-Скрытность/shot_8.9.0.jpg")
                solution = "8.9.0"
            }
            else if (root == 1 && solution == "8.9.0") {
                F1("R046", "<Черт, танк на 3 часа!>", "", "Смирнов, наводись!", "РУТ1/8-Скрытность/shot_8.10.0.jpg")
                solution = "8.10.0"
            }
            else if (root == 1 && solution == "8.10.0") {
                F1("R047", "<Не успел навестись товарищ Смирнов, как яркая вспышка выстрела уничто..>", "", "Автор, пж, расскажи че-нить, не хочу помирать..", "РУТ1/8-Скрытность/shot_8.11.0.jpg")
                solution = "8.11.0"
            }
            else if (root == 1 && solution == "8.11.0") {
                F1("R048", "<Што-ш. Ладно, отмотал время. Тебя уже поджидал Тандерболт легендарного Крайтона Абрамса. Он был на окраине города>", "", "Ну вот..", "РУТ1/8-Скрытность/shot_8.12.0.jpg")
                solution = "8.12.0"
            }
            else if (root == 1 && solution == "8.12.0") {
                F1("R049", "<Твой танк через смотровой прибор Тандерболта>", "", "Я понял", "РУТ1/8-Скрытность/shot_8.13.0.jpg")
                solution = "8.13.0"
            }
            else if (root == 1 && solution == "8.13.0") {
                F1("R050", "<А вот ты под прицелом наводчика>", "", "...", "РУТ1/8-Скрытность/shot_8.14.0.jpg")
                solution = "8.14.0"
            }
            else if (root == 1 && solution == "8.14.0") {
                F1("R051", "<Знаешь, вот смотришь на подбитый танк — вылитый СССР: броня вроде толстая, а внутри всё давно сгнило, и хватило одного хорошего пинка, чтобы развалиться на металлолом>", "", "Автор, иди, проспись", "РУТ1/8-Скрытность/shot_8.15.0.jpg")
                solution = "8.15.0"
            }
            else if (root == 1 && solution == "8.15.0") {
                F1("R052", "<Да, время уже позднее.. В любом случае я должен завершить твою историю. Твой танк был подбит, никто не выжил. Изначально планировался сюжет, но я устал делать руты и сделал его конечным. Если будешь проходить игру повторно, советую играть аккуратнее. Впрочем, и везение играет немаловажную роль.>", "ГГ", "ГГ", "РУТ1/8-Скрытность/shot_8.16.0.jpg")
                solution = "8.16.0"
            }
            else if (solution == "6.10.0SW" || root == 1 && solution == "6.10.0ZC" || solution == "6.10.0SC") {
                F1("R053", "<У вас стальные яйца, вы выбрали ехать по хорошо обстреливаемому полю прямо в город! Вот это отвага, вот это слабоумие!>", "", "Дальше", "РУТ1/9-На яйцах/shot_9.0.0.jpg")
                solution = "9.0.0"
            }
            else if (root == 1 && solution == "9.0.0") {
                F1("R054", "<Вы едете дальше. Думаю, Полуфабрикат-Дамир был бы поумнее>", "", "Бегите, я конченый", "РУТ1/9-На яйцах/shot_9.1.0.jpg")
                solution = "9.1.0"
            }
            else if (root == 1 && solution == "9.1.0") {
                F1("R055", "<Вы едете дальше. Нечего добавить..>", "", "Six-seven", "РУТ1/9-На яйцах/shot_9.2.0.jpg")
                solution = "9.2.0"
            }
            else if (root == 1 && solution == "9.2.0") {
                F1("R056", "<Чекай, ща тебе пизда>", "", "Шнейне?!?!", "РУТ1/9-На яйцах/shot_9.3.0.jpg")
                solution = "9.3.0"
            }
            else if (root == 1 && solution == "9.3.0") {
                F1("R057", "<Сам Крайтон Абрамс на легендарном Тандерболте 7 выехал из-за улиц города>", "", "Шнейне-пэпэ?!?!", "РУТ1/9-На яйцах/shot_9.4.0.jpg")
                solution = "9.4.0"
            }
            else if (root == 1 && solution == "9.4.0") {
                F1("R058", "<Тандерболт производит выстрел>", "", "Дальше", "РУТ1/9-На яйцах/shot_9.5.0.jpg")
                solution = "9.5.0"
            }
            else if (root == 1 && solution == "9.5.0") {
                F1("R059", "<Вам сбили правую гусеницу. Танк слегка подвернуло в сторону озера. Вам повезло..>", "", "Дальше", "РУТ1/9-На яйцах/shot_9.6.0.jpg")
                solution = "9.6.0"
            }
            else if (root == 1 && solution == "9.6.0") {
                F1("R060", "<..ненадолго. С северо-востока карты Т1 Хэви выстрелил по вам.>", "Включить Энергетическое поле", "Шнейне-пэпэ-ВАТАФАААА?!?!", "РУТ1/9-На яйцах/shot_9.7.0.jpg")
                solution = "9.7.0"
            }
            else if (root == 1 && solution == "9.7.0") {
                F1("R061", "<Ну, что могу сказать, ты полный шнеле-пэпэ>", "ГГ", "ГГ", "РУТ1/9-На яйцах/shot_9.9.0.jpg")
                solution = "9.9.0"
            }

            //===============================РУТ 3 RIGHT BUTTON==================
            //===============================||==================================
            //===============================||==================================
            //===============================\/==================================
            else if(solution=="0" && root==0)
            {
                F1("R062", "Линдвилл впереди. Выберите маршрут: вдоль озера (правый берег у гор) или через город.","Раш по центру","В город, твердо и четко","РУТ3/1/shot_001.0.0.jpg")
                root=23
                solution="0"
            }
            else if(solution=="1.0.0" && root==3)
            {
                F1("R063", "Вы едете дальше","","Ну ок","РУТ3/1/shot_001.2.0.jpg")
                solution="1.2.0"
            }
            else if(solution=="1.2.0" && root==3)
            {
                F1("R064", "<Читая личное дело вашего экпижа, вы узнаете, что каждый член экипажа, все как на подбор, тигры, закаленные в боях, исключая наводчика,  товарища-рядового Смирнова. Судя по его делу, у него есть некие отклонения, это нужно учитывать. При принятии решения, рекомендуется прислушиваться заряжающему, вашей правой руке- товарищу Натальсону. Судя по всему, он уроженец из Литвы и просто хороший...>","","Дальше","РУТ3/1/shot_001.4.0.jpg")
                solution="1.4.0"
            }
            else if(solution=="1.4.0" && root==3)
            {
                F1("R065", "Вы наблюдаете прекрыне виды на озеро и город, вдали наблюдается жд мост проходящий через город. Весь экипаж воодушевлен видами, но не стоит забывать, где вы находитесь","","Дальше","РУТ3/1/shot_001.5.0.jpg")
                solution="1.5.0"
            }
            else if(solution=="1.5.0" && root==3)
            {
                F1("R066", "Тов. Натальсон: Командир, судя по карте, данная дорога отлично простреливается с направления противника! Может отступим?","Послушать Натальсона","Отставить, Натальсон, едем дальше","РУТ3/1/shot_001.6.0-!!!.jpg")
                solution="1.6.0"
            }
            else if(solution=="1.6.0" && root==3)
            {
                F1("R067", "Тов. Натальсон: Так точно, Командир!\n <Вы едете дальше>","","Ну да","РУТ3/1/shot_001.7.0.jpg")
                solution="1.7.0"
            }
            else if(solution=="1.7.0" && root==3)
            {
                F1("R068", "<Экипаж замечает, что, вас окружает смертельная тишина.>\nТов. Натальсон:<вполголоса> Не хорошо...\n<Лишь Смирнов сидит спокойным, возможно у него проблемы со слухом или чего похуже, думаете вы..>","Оступить","Ехать дальше","РУТ3/1/shot_001.8.0-!!!.jpg")
                solution="1.8.0"
            }
            else if(solution=="1.8.0" && root==3)
            {
                F1("R069", "<Проехав немного дальше..>\nТов. Натальсон:Командир, Выстрел на 2 часа!\n<На миг вы замечаете выстрел>","","Молиться","РУТ3/1/shot_001.9.0.jpg")
                solution="1.9.0"
            }
            else if(solution=="1.9.0" && root==3)
            {
                F1("R070", "<Танк не уничтожен, но по-видмому задело отсек МТО. Танк горит, У вас есть Выбор: Приказать экипажу эвакироваться или приготовиться к бою>","Эвакуироваться","Отставить Панику! Приготовиться к бою!","РУТ3/1/shot_001.10.0.jpg")
                solution="1.10.0"
            }
            else if(solution=="1.10.0" && root==3)
            {
                F1("R071", "<Не успев вы договорить приказ, Следующий выстрел вспышка с аллеи была для вас последней, может нужно было послушать Натальсона? Экипаж погиб. Вы не выполнили Задание>","ГГ","ГГ","РУТ3/1/shot_001.11.0.jpg")
                solution="1.11.0"
            }
            else if(solution=="4.1.0" && root==3)
            {
                F1("R072", "<Вы выбрали ехать в ущелье>","Дальше","","РУТ3/4-7/shot_007.0.0.jpg")
                root=1
                solution = "1.0.0"
            }

            //===============================РУТ 2 RIGHT BUTTON==================
            //===============================||==================================
            //===============================||==================================
            //===============================\/==================================
            else if(solution=="0" && root==23)
            {
                F1("R073", "Вы выбрали ехать в город. Твердо и четко.","Дальше","","РУТ2/1-2/shot_001.0.0.jpg")
                root=2
                solution="1.0.0"
            }
            else if(root==2 && solution=="2.0.0ч2")
            {
                F1("R074", "<Вы едете по прямой>","","Дальше","РУТ2/2.2/shot_002.2.0.jpg")
                solution="2.2.0"
            }
            else if(root==2 && solution=="2.2.0")
            {
                F1("R075", "<Читая дело экипажа, вы узнаёте: все — тигры, закалённые в боях, кроме наводчика Смирнова. У него отклонения, это нужно учитывать. При принятии решений прислушивайтесь к заряжающему Натальсону — уроженцу Литвы и просто хорошему человеку.>","","Дальше","РУТ2/2.2/shot_002.2.1.jpg")
                solution="2.2.1"
            }
            else if(root==2 && solution=="2.2.1")
            {
                F1("R076", "<Вы едете дальше>","","Дальше","РУТ2/2.2/shot_002.2.2.jpg")
                solution="2.2.2"
            }
            else if(root==2 && solution=="2.2.2")
            {
                F1("R077", "<Вы вошли в город>","","Дальше","РУТ2/2.2/shot_002.2.3.jpg")
                solution="2.2.3"
            }
            else if(root==2 && solution=="2.1.3")
            {
                F1("R078", "<Механик свернул в сторону города>","","Дальше","РУТ2/3/shot_003.3.0.jpg")
                solution="3.3.0"
            }
            else if(root==2 && solution=="3.3.0")
            {
                F1("R079", "<Вы подъезжаете к центру города>","","Дальше","РУТ2/3/shot_003.3.1.jpg")
                solution="3.3.1"
            }
            else if(root==2 && solution=="2-3")
            {
                F1("R080", "<Вы решили ехать в город по объездной дороге>","","Дальше","РУТ2/3/shot_003.0.0.jpg")
                solution="3.0.0"
            }
            else if(root==2 && solution=="3.0.0")
            {
                F1("R081", "<Вы открываете личное дело экипажа, по нему вы узнаете имена братьев по оружию: механик-водитель- Михайло В.П., наводчик- Смирнов Ю.П, заряжающий- Натальсон Л.В.>","","Дальше","РУТ2/3/shot_003.0.1.jpg")
                solution="3.0.1"
            }
            else if(root==2 && solution=="3.0.1")
            {
                F1("R082", "<Вам открываются прекрасные виды на озеро ТуркВойс и горы Хомстейк и Вейл Скай. Экипаж воодушевлен, но не стоит забывать, где вы находитесь>","","Дальше","РУТ2/3/shot_003.0.2.jpg")
                solution="3.0.2"
            }
            else if(root==2 && solution=="3.0.2")
            {
                F1("R083", "<Продолжив читать дело экпипажа, вы узнаёте: все — тигры, закалённые в боях, кроме наводчика Смирнова. У него отклонения, это нужно учитывать. При принятии решений прислушивайтесь к заряжающему Натальсону — уроженцу Литвы и просто хорошему человеку.>","","Дальше","РУТ2/3/shot_003.0.3.jpg")
                solution="3.0.3"
            }
            else if(root==2 && solution=="3.0.3")
            {
                F1("R084", "<Не смотря на то, что машина едет по грунтовке, машина едет плавно>\n Тов. Михайло: Це мне дорогу до мого краю нагадуе... таку, знаете, грунтову...","","Михайло, ты откуда будешь? Акцент не обычный..","РУТ2/3/shot_003.0.4.jpg")
                solution="3.0.4ч"
            }
            else if(root==2 && solution=="3.0.4ч")
            {
                F1("R085", "Тов. Михайло: Так точно, командире, я з деревні Слобожанське, що під Харковом.","","Добре, Михайло. Техника на тебе. За Смирновим приглядай — голова не така, як у людей.","РУТ2/3/shot_003.0.4.jpg")
                solution="3.0.4ч1"
            }
            else if(root==2 && solution=="3.0.4ч1")
            {
                F1("R086", "Тов. Михайло: Служу советскому Союзу!\nТов. Смирнов<недоумённо>: Командир... А почему я наводчик? У меня ж подготовка другая...","","Не нравится — пиши рапорт. Исполняй.","РУТ2/3/shot_003.0.4.jpg")
                solution="3.0.4ч2"
            }
            else if(root==2 && solution=="3.0.4ч2")
            {
                F1("R087", "Натальсон<мягко, с усмешкой>: Смирнов, ты это... Командир из казанских татар. Он если взглянет — малохольным себя почувствуешь.\nТов. Смирнов: Да я ничего... Просто спросил...\nТов. Михайло<с акцентом>: Ото, Смирнов, не кобенись. Я довезу, Натальсон зарядить. Твоє — бачити ціль. \nТов. Натальсон: Слышал? Механик дело говорит. Будешь плохо стрелять — я сам тебя малохольным назову. По-доброму.","","Добре, Михайло. Техника на тебе. За Смирновим приглядай — голова не така, як у людей.","РУТ2/3/shot_003.0.5.jpg")
                solution="3.0.5"
            }
            else if(root==2 && solution=="3.0.5")
            {
                F1("R088", "Тов. Смирнов: А вы вообще кто такой, чтоб меня называть?\nТов. Натальсон<спокойно>:  Я — правая рука командира. А ты — наводчик. Усе. Понял, малохольный?","","<коротко> Натальсон, хватит.","РУТ2/3/shot_003.0.5.jpg")
                solution="3.0.5ч"
            }
            else if(root==2 && solution=="3.0.5ч")
            {
                F1("R089", "Тов. Михайло<вздыхает>: Ну от, знову почали... Смирнов, просто стріляй, а?\nТов. Натальсон<Смирнову>:  Ладно, не дуйся. Командир сказал — значит, мир. По машинам.","","Отставить разговоры, приготовиться к бою","РУТ2/3/shot_003.0.6.jpg")
                solution="3.0.6"
            }
            else if(root==2 && solution=="3.0.6")
            {
                F1("R090", "<Экипаж замолкает>\nТов. Натальсон: Командир, въезжаем в город","Михайло, сбавь обороты, заедем через другие переулки","Михайло, сбавь обороты. Едем дальше","РУТ2/3/shot_003.0.7!!!.jpg")
                solution="3.0.7"
            }
            else if(root==2 && solution=="3.0.7")
            {
                F1("R091", "<Вы едете дальше по объездной дороге>","","Дальше","РУТ2/3/shot_003.2.0.jpg")
                solution="3.2.0"
            }
            else if(root==2 && solution=="3.2.0")
            {
                F1("R092", "<Вы подъезжаете к центру города>","","Дальше","РУТ2/3/shot_003.2.1.jpg")
                solution="3.2.1"
            }
            else if(root==2 && solution=="3.1.1.2")
            {
                F1("R093", "<Вы поехали направо>","","Дальше","РУТ2/3/shot_003.1.2.0.jpg")
                solution="3.1.2.0"
            }
            else if(root==2 && solution=="3.1.2.0")
            {
                F1("R094", "<Вы выезжаете к историческому центру города>","","Дальше","РУТ2/3/shot_003.1.2.1.jpg")
                solution="3.1.2.1"
            }
            else if(root==2 && (solution=="3.1.1.4" ||solution=="2.2.3"))
            {
                F1("R095", "Проехав улицы города, вы выезжаете подъезжаете к центральной площади города","","Дальше","РУТ2/3/shot_003.4.0.jpg")
                solution="3.4.0"
            }
            else if(root==2 && solution=="3.4.0")
            {
                F1("R096", "Выехав на площадь, у вас есть вариант ехать по сюжету Рембо или помереть в нищете","Смерть в нищете звонит","Дальше","РУТ2/3/shot_003.4.1!!!.jpg")
                solution="3.4.1"
            }
            else if(root==2 && solution=="3.4.1")
            {
                F1("R097", "<Вы решаете не испытывать судьбу, и ехать по пути Рэмбо, кто знает, куда он вас приведет>","","Дальше","РУТ2/3/shot_003.4.2.jpg")
                solution="3.4.2"
            }
            else if(root==2 && (solution=="3.1.2.1" ||solution=="3.4.2" || solution=="3.3.1"))
            {
                F1("R098", "<Вы  выехали центральную площадь города>","","Дальше","РУТ2/4/shot_004.0.0.jpg")
                solution="4.0.0"
            }
            else if(root==2 && solution=="4.0.0")
            {
                F1("R099", "<Вы приказываете Михайло ехать на пониженных скоростях, Смирнов на готове>","","Дальше","РУТ2/4/shot_004.0.1.jpg")
                solution="4.0.1"
            }
            else if(root==2 && solution=="4.0.1")
            {
                F1("R100", "<Вы замечаете машину на 10 часов, механик-водиитель останавливается. Смирнов наводит орудие>","","Дальше","РУТ2/4/shot_004.0.2.jpg")
                solution="4.0.2"
            }
            else if(root==2 && solution=="4.0.2")
            {
                F1("R101", "<Через триплекс вы видете машину, Смирнов тормозит с выстрелом>","","Смирнов, выстрел!!!","РУТ2/4/shot_004.0.3.jpg")
                solution="4.0.3"
            }
            else if(root==2 && solution=="4.0.3")
            {
                F1("R102", "<Выстрел 122-мм орудия оглушает весь экипаж>","","Проверить","РУТ2/4/shot_004.0.4.jpg")
                solution="4.0.4"
            }
            else if(root==2 && solution=="4.0.4")
            {
                F1("R103", "<Вы видите уничтоженного Хэви>","","Михайло, на 1 передаче, Смирнов, орудие, на 2 часа","РУТ2/4/shot_004.0.5.jpg")
                solution="4.0.5"
            }
            else if(root==2 && solution=="4.0.5")
            {
                F1("R104", "<Интуиция вас не подвела, Шерман  наведен прямо на вас!>","Стоп-машина! Выстрел по команде!","Смирнов, ВЫСТРЕЛ!","РУТ2/4/shot_004.0.6.jpg")
                solution="4.0.6"
            }
            else if(root==2 && solution=="4.0.6")
            {
                F1("R105", "<Смирнов делает выстрел>","","Дальше","РУТ2/4/shot_004.0.8.jpg")
                solution="4.0.8"
            }
            else if(root==2 && solution=="4.0.8")
            {
                F1("R106", "<Вы видите догорающего Шермана>","","Дальше","РУТ2/4/shot_004.0.9.jpg")
                solution="4.0.9"
            }
            else if(root==2 && solution=="4.0.9")
            {
                F1("R107", "<Немного проехав вы командуете Михайло ехать>","Михайло, едем по левой улице","Михайло едем по правой улице","РУТ2/4/shot_004.0.10-!!!.jpg")
                solution="4.0.10"
            }
            else if(root==2 && solution=="4.0.10") {
                F1("R108", "<Вы выбрали ехать по правой улице>", "", "Дальше", "РУТ2/4/shot_004.2.0.jpg")
                solution = "4.2.0"
            }
            else if(root==2 && solution=="4.2.0") {
                F1("R109", "<Подъезжая к въезду вы мельком видите силует машины>", "", "Стоп-маши..", "РУТ2/4/shot_004.2.1.jpg")
                solution = "4.2.1"
            }
            else if(root==2 && solution=="4.2.1") {
                F1("R110", "<Не успев договорить приказ, Шерман делает свой ход. Экипаж погиб, задание провалено>", "ГГ", "ГГ", "РУТ2/4/shot_004.2.2.jpg")
                solution = "4.2.2"
            }
            else if(root==2 && solution=="4.3.9") {
                F1("R111", "<Как бывалый игрок танков, вы включаете тумблер защиты шотного совка, вы получаете многочисленные рикошеты и не пробития от танка. Удача благоволит вам, прилетает подмога. Вы продеражались сколько нужно, задание выполнено!>","ГГ","ГГ","Допка/ИЛ-2НаПодлете.jpg")
                solution="ГГ"
            }
            else if(root==2 && solution=="4.1.8") {
                F1("R112", "<Вы едете в сторону объездной улицы>","","Дальше","РУТ2/4/shot_004.4.0.jpg")
                solution="4.4.0"
            }
            else if(root==2 && solution=="4.4.0") {
                F1("R113", "<Вы едете дальше>","","Дальше","РУТ2/4/shot_004.4.1.jpg")
                solution="4.4.1"
            }
            else if(root==2 && solution=="4.4.1") {
                F1("R114", "<Вы едете дальше>","","Дальше","РУТ2/4/shot_004.4.2.jpg")
                solution="4.4.2"
            }

            else if(root==2 && solution=="3.2.1") {
                F1("R115", "<Вы уже выезжаете на центральную площадь города, как замечаете засаду, Смирнов уже наводит орудие>","","Смирнов, быстрее бля!","РУТ2/5/shot_005.0.0.jpg")
                solution="5.0.0"
            }
            else if(root==2 && solution=="5.0.0") {
                F1("R116", "<Орудие наведено>","","ВЫСТРЕЛ!","РУТ2/5/shot_005.0.1.jpg")
                solution="5.0.1"
            }
            else if(root==2 && solution=="5.0.1") {
                F1("R117", "<Снаряд попал точно в цель>","","Дальше","РУТ2/5/shot_005.0.2.jpg")
                solution="5.0.2"
            }
            else if(root==2 && solution=="5.0.2") {
                F1("R118", "Тов. Натальсон: Командир, танк уничтожен!\n<Вы видите полыхающие танки шермана, судя по оторванной башне, произошла детоанция боекомплекта>","","Дальше","РУТ2/5/shot_005.0.3.jpg")
                solution="5.0.3"
            }
            else if(root==2 && solution=="5.0.3") {
                F1("R119", "<У вас есть выбор: поехать и дальше по объездной дороге или свернуть к центральным улицам>","Михайло, держи левее","Михайло, полный вперед!","РУТ2/5/shot_005.0.4.jpg")
                solution="5.0.4"
            }
            else if(root==2 && solution=="5.0.4") {
                F1("R120", "<Вы приказали ехать вперед, по объездной дороге>","","Дальше","РУТ2/5/shot_005.2.0.jpg")
                solution="5.2.0"
            }
            else if(root==2 && solution=="5.2.0") {
                F1("R121", "<Проехав немного, вы говорите Михайло сбавить обороты>","","Дальше","РУТ2/5/shot_005.2.1.jpg")
                solution="5.2.1"
            }
            else if(root==2 && solution=="5.2.1") {
                F1("R122", "<Вы видите шерман на 9 часов!>","","Михайло, стоп-машина! Смирнов Орудие на 9 часов!","РУТ2/5/shot_005.2.2.jpg")
                solution="5.2.2"
            }
            else if(root==2 && solution=="5.2.2") {
                F1("R123", "<Машина остановилась, Смирнов наводит орудие>","","<Подгонять Смирнова> Смирнов еп твою мать!","РУТ2/5/shot_005.2.3.jpg")
                solution="5.2.3"
            }
            else if(root==2 && solution=="5.2.3") {
                F1("R124", "<Смирнов навел орудие>","","ВЫСТРЕЛ!","РУТ2/5/shot_005.2.4.jpg")
                solution="5.2.4"
            }
            else if(root==2 && solution=="5.2.4") {
                F1("R125", "<Смирнов выстрелил!>","","Дальше","РУТ2/5/shot_005.2.6.jpg")
                solution="5.2.6"
            }
            else if(root==2 && solution=="5.2.6") {
                F1("R126", "<В триплексе вы видите догорающую Эмчу>","","Едем дальше, Смирнов, орудие на изготовку!","РУТ2/5/shot_005.2.6.jpg")
                solution="5.2.7"
            }
            else if(root==2 && solution=="5.2.7") {
                F1("R127", "<Вы едете дальше>","","Дальше","РУТ2/5/shot_005.2.8.jpg")
                solution="5.2.8"
            }
            else if(root==2 && solution=="5.2.8") {
                F1("R128", "<Вы видите мельком вражеский танк. Михайло останавливается. Вас окончательно заебал Смирнов>","<С терпением> Довериться Смирнову","Смирнов, съебался!","РУТ2/5/shot_005.2.10.jpg")
                solution="5.2.10"
            }
            else if(root==2 && solution=="5.2.10") {
                F1("R129", "<Смирнов съебался. Вы навели орудие>","","ВЫСТРЕЛ!","РУТ2/5/shot_005.2.11.jpg")
                solution="5.2.11"
            }
            else if(root==2 && solution=="5.2.11") {
                F1("R130", "<Через наблюдательные приборы вы видите  догорающий Т1 Хэви>","","Пересесть на свое место","РУТ2/5/shot_005.2.12.jpg")
                solution="5.2.12"
            }
            else if(root==2 && solution=="5.2.12") {
                F1("R131", "<Сев за свое место вы приказыаете Михайло>","Михайло, сворачиваем налево, к полям","Михайло, едем дальше по объездной","РУТ2/5/shot_005.2.13.jpg")
                solution="5.2.13"
            }
            else if(root==2 && solution=="5.2.13") {
                F1("R132", "<Вы едете по объездной>","","Михайло, едем дальше по объездной","РУТ2/5/shot_005.4.0.jpg")
                solution="5.4.0"
            }
            else if(root==2 && solution=="5.4.0") {
                F1("R133", "<Вы едете дальше>","","Дальше","РУТ2/5/shot_005.4.1.jpg")
                solution="5.4.1"
            }
            else if(root==2 && solution=="5.4.1") {
                F1("R134", "<Вы едете дальше>","","Дальше","РУТ2/5/shot_005.4.2.jpg")
                solution="5.4.2"
            }
            else if(root==2 && solution=="5.4.2") {
                F1("R135", "<Вы едете дальше>","","Дальше","РУТ2/5/shot_005.4.3.jpg")
                solution="5.4.3"
            }
            else if(root==2 && solution=="5.4.3" || solution=="4.4.2") {
                F1("R136", "<Вы приказываете Михайло ехать через аллею, сойдя с дороги>","","Дальше","РУТ2/5/shot_005.4.4.jpg")
                solution="5.4.4"
            }
            else if(root==2 && solution=="5.4.4") {
                F1("R137", "<Вы едете дальше>","","Дальше","РУТ2/5/shot_005.4.5.jpg")
                solution="5.4.5"
            }
            else if(root==2 && solution=="5.4.5") {
                F1("R138", "<Вы проезжаете амбар>","","Дальше","РУТ2/5/shot_005.4.6.jpg")
                solution="5.4.6"
            }
            else if(root==2 && solution=="5.4.6") {
                F1("R139", "<В ваш танк попали! Звук оглушил всех, вы замечаете, что Михайло больше походит на фарш, чем на механика>","","Смирнов, противник на 11 часов, стреляй!","РУТ2/6/shot_006.0.0.jpg")
                solution="6.0.0"
            }
            else if(root==2 && solution=="6.0.0") {
                F1("R140", "<Весь экипаж потихоньку очухивается, кроме Михайло. Но сейчас нет времени для скорби, дым выветривается, это чудо что боекомплект не задет. Смирнов делает выстрел>","","Дальше","РУТ2/6/shot_006.0.1.jpg")
                solution="6.0.1"
            }
            else if(root==2 && solution=="6.0.1") {
                F1("R141", "<Мимо>","","Дальше","РУТ2/6/shot_006.0.2.jpg")
                solution="6.0.2"
            }
            else if(root==2 && solution=="6.0.2") {
                F1("R142", "<Шерман стреляет по вам! Рикошет!>","","Натальсон, перезаряжай!","РУТ2/6/shot_006.0.3.jpg")
                solution="6.0.3"
            }
            else if(root==2 && solution=="6.0.3") {
                F1("R143", "<Натальсон перезарядил орудие, Смирнов делает выстрел>","","Дальше","РУТ2/6/shot_006.0.4.jpg")
                solution="6.0.4"
            }
            else if(root==2 && solution=="6.0.4") {
                F1("R144", "Тов. Смирнов: Попадание по танку!\n","","Дальше","РУТ2/6/shot_006.0.5.jpg")
                solution="6.0.5"
            }
            else if(root==2 && solution=="6.0.5") {
                F1("R145", "Тов. Смирнов: Командир, вражеский танк отступает!","","Смирнов, на место Михайло! Едем дальше!","РУТ2/6/shot_006.0.6.jpg")
                solution="6.0.6"
            }
            else if(root==2 && solution=="6.0.6") {
                F1("R146", "<Проехав немного,  вы приказываете Смирнову>","Смирнов, выезжаем с левой стороны дома!","Смирнов, подъезжай с правой стороны дома!","РУТ2/6/shot_006.0.7!!!.jpg")
                solution="6.0.7"
            }
            else if(root==2 && solution=="6.0.7") {
                F1("R147", "<Вы решаете заехать с правой стороны>","","Дальше","РУТ2/6/shot_006.2.0.jpg")
                solution="6.2.0"
            }
            else if(root==2 && solution=="6.2.0") {
                F1("R148", "<Подъехав к дому, вы приказываете Смирнову остановиться. В триплексе виден Тандерболт!>","","Сесть на место заряжающего","РУТ2/6/shot_006.2.1.jpg")
                solution="6.2.1"
            }
            else if(root==2 && solution=="6.2.1") {
                F1("R149", "<Вы наводитесь на противника>","Ждать","Стрелять!","РУТ2/6/shot_006.2.2.jpg")
                solution="6.2.2"
            }
            else if(root==2 && solution=="6.2.2") {
                F1("R150", "<Вы выстрелили преждевременно. Тандерболт стреляет и уничтожает ваш танк. Экипаж погибает, задание провалено>","ГГ", "ГГ", "РУТ1/4/shot_004.5.0.jpg")
                solution="Смерть"
            }
            else if(root==2 && solution=="6.2.3") {
                F1("R151", "<Вы наводитесь на противника>","Ждать","Стрелять","РУТ2/6/shot_006.2.4.jpg")
                solution="6.2.4"
            }
            else if(root==2 && solution=="6.2.4") {
                F1("R152", "<Вы выстрелили преждевременно. Тандерболт стреляет и уничтожает ваш танк. Экипаж погибает, задание провалено>","ГГ", "ГГ", "РУТ1/4/shot_004.5.0.jpg")
                solution="Смерть"
            }
            else if(root==2 && solution=="6.2.7") {
                F1("R153", "<Вы решаете отъехать назад>","", "Смирнов, держись левее", "РУТ2/6/shot_006.4.0.jpg")
                solution="6.4.0"
            }
            else if(root==2 && solution=="6.4.0") {
                F1("R154", "<Вы приказываете Смирнову>","Смирнов, подъедь к сараю слева!", "Смирнов, объезжаем сарай справа", "РУТ2/6/shot_006.4.1-!!!.jpg")
                solution="6.4.1"
            }
            else if(root==2 && solution=="6.4.1") {
                F1("R155", "<Здесь была засада. Экипаж погибает, задание провалено>","ГГ", "ГГ", "РУТ2/6/shot_006.3.2.jpg")
                solution="Смерть"
            }


        }
    }

    override fun onPause() {
        super.onPause()
        if (::audio.isInitialized) audio.pause()
        if (::music.isInitialized) music.pause()
    }

    override fun onResume() {
        super.onResume()
        if (::music.isInitialized) {
            music.setVolume(GameSettings.musicVolume(this))
            if (restartMusicOnResume) {
                restartMusicOnResume = false
                music.resume()
                music.restart(currentStep) // свои треки / плейлисты могли измениться
            } else {
                music.resume()
            }
        }
    }

    override fun onDestroy() {
        if (::audio.isInitialized) audio.release()
        if (::music.isInitialized) music.release()
        super.onDestroy()
    }
}
