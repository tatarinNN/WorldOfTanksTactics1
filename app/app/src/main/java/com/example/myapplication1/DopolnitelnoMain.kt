package com.example.myapplication1

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.bumptech.glide.Glide

class DopolnitelnoMain : BaseActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dopolnitelno_main)
        val dopimage: ImageView= findViewById(R.id.dopimage)
        var teg=""
        val spisok: LinearLayout = findViewById(R.id.spisok1)
        val exittomenu: Button= findViewById(R.id.ExitToMenub)
        var opisanie: TextView= findViewById(R.id.Opisanie)
        val imagenames = listOf(
            "Карта Колорадо 1948года",
            "Современная Карта БД",
            "Современный город Лидвилл",
            "Разъезд по левой стороне",
            "Разъезд по правой стороне",
            "Птички на подлете",
            "Номера на танках",
            "Основные разъезды",
            "Немного истории",
            "Виды из игры",
            "О создании игры",
            "Смирнов как концепт игры"
        )
        exittomenu.addPressEffect()
        exittomenu.setOnClickListener { finish() }





        //Список кнопок для просмотра фото
        for (i in imagenames){
            val imagename = AppCompatButton(this, null, 0)
            imagename.setBackgroundResource(R.drawable.bg_button)
            imagename.setTextColor(Color.parseColor("#EDE3C8"))
            imagename.textSize = 13f
            imagename.isAllCaps = false
            imagename.minHeight = 0
            imagename.minimumHeight = 0
            val dens = resources.displayMetrics.density
            imagename.setPadding((10 * dens).toInt(), (8 * dens).toInt(), (10 * dens).toInt(), (8 * dens).toInt())
            imagename.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (6 * dens).toInt() }
            imagename.addPressEffect()
            imagename.text=i
            imagename.setOnClickListener {
                if (i=="Карта Колорадо 1948года"){
                    teg="Допка/Колорадо1948.jpg"
                    opisanie.text="Настоящая историчная военная тактическая карта США 1948года. Линии сражений, вымышленые. Источник:  \thttp://www.etomesto.ru/map-america_usa_1948"
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
                else if (i=="Современная Карта БД"){
                    teg="Допка/ГуглКартыКолорадо.jpg"
                    opisanie.text="Google Maps. Координаты(Колорадо, США). Leadville 39°04'56.2N 106°22'46.3W"
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
                else if (i=="Современный город Лидвилл"){
                    teg="Допка/ЛэдвиллГуглКарты.png"
                    opisanie.text="Google Maps. Координаты(Колорадо, г. Лидвилл США). Leadville 39°04'56.2N 106°22'46.3W"
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
                else if (i=="Разъезд по левой стороне"){
                    teg="Допка/СпойлерКарта разъезда1рут.jpg"
                    opisanie.text="Самый легкий рут игры(Не считая скрытые)"
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
                else if (i=="Разъезд по правой стороне"){
                    teg="Допка/СпойлерКарта разъезда2рут.jpg"
                    opisanie.text="Объем всех фотографий  только второго рута занимает 400МБ, это 2/3 объема игры(Из 700МБ)"
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
                else if (i=="Основные разъезды"){
                    teg="Допка/КартаРазъездов1часть.jpg"
                    opisanie.text="Основные разъезды игры, которые ведут к основным концовкам"
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
                else if (i=="Птички на подлете"){
                    teg="Допка/ИЛ-2НаПодлете.jpg"
                    opisanie.text=" Одна из секретных концовок игры. На фото Звено советских штурмовиков Ил-2 на боевом задании во время Великой Отечественной войны. Фото: ТАСС."
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
                else if (i=="Номера на танках"){
                    teg="Допка/shot_006.jpg"
                    opisanie.text="Номера машин полуисторичные, такая нумерация была у немецких танков по большой степени. США использовали преимущественно буквенно-номерное обозначение "
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
                else if (i=="Немного истории"){
                    teg="Допка/shot_166.jpg"
                    opisanie.text="Игра построена на основе игры World of tanks, где карта Лассвиль была вдохновлена настоящим городом  leadville, в США. Здесь и происходят действия моей игры"
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
                else if (i=="Смирнов как концепт игры"){
                    teg="Допка/shot_157.jpg"
                    opisanie.text="Была идея, что Смирнов — это ПТСР у командиров ИС-2, что становится ясно из матчасти. После этого понимаешь, что ты в дурке, сбегаешь, но всё оказывается игрой, а ты студент КГЭУ."
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
                else if (i=="О создании игры"){
                    teg="Допка/shot_001Ч.jpg"
                    opisanie.text="Были идеи: ВОВ, нападение СССР на США ради русских меньшинств и революции чернокожих. Другая идея: игра в духе Dark Pictures: Little Hope."
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
                else if (i=="Виды из игры"){
                    teg="Допка/shot_157.jpg"
                    opisanie.text="Просто красивый вид. Кстати, все вражеские танки имеют свой личный порядковый номер  в своем взводе, от 751-755. Где 7- номер роты, 5-номер взвода, последнее число- номер танка во взводе"
                    Glide.with(this).load("file:///android_asset/$teg").into(dopimage)
                }
            }
            spisok.addView(imagename)
        }

    }
}