package com.paochapro.test004

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.em
import com.paochapro.test004.ui.theme.Test004Theme
import java.util.Calendar

//print lesson character count
//        val lessonName = arrayOf("Математика", "Алгебра", "Геометрия", "Русский", "Литература", "География", "Башкирский", "Биология", "Английский", "Информатика", "История", "Обществознание", "Обж", "Химия")
//        lessonName.forEach { println("$it: ${it.length}") }
//        println("Среднее: ${lessonName.map { it.length }.average()}")
//        println("---Сокращённые---")
//        println(lessonName.map { it.getOrNull(0).toString() + it.getOrNull(1).toString() + it.getOrNull(2).toString() })

const val MSG = "MY_MESSAGE"
const val LESSON_COUNT = 8
const val DAY_COUNT = 7
const val DEFAULT_LESSON_TIME_MINS = 40
const val WIDGET_TEXT_LESSON_WASNT_FOUND = "Следующий предмет не найден"
const val SCHEDULE_FILE_NAME = "test2.json"
val CONTENT_COLOR = Color.White

enum class Screen {
    MainScreen,
    ConfigureLesson,
    DevScreen
}

enum class DayName(val rusTranslation: String) {
    Monday("Понедельник"),
    Tuesday("Вторник"),
    Wednesday("Среда"),
    Thursday("Четверг"),
    Friday("Пятница"),
    Saturday("Суббота"),
    Sunday("Воскресенье");

    companion object {
        fun fromInt(int: Int) {
            when(int) {
                0 -> DayName.Monday
                1 -> DayName.Tuesday
                2 -> DayName.Wednesday
                3 -> DayName.Thursday
                4 -> DayName.Friday
                5 -> DayName.Saturday
                6 -> DayName.Sunday
                else -> {
                    println("Integer $int isnt matching any day. Returning DayName.Monday")
                    DayName.Monday
                }
            }
        }
    }
}

class Lesson(val startTime: String, val subject: String, val cabinet: Int)

class Day(val lessons: Array<Lesson?>, var lessonTimeMinutes: Int = DEFAULT_LESSON_TIME_MINS)

class LessonStatusUpdate : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action != Intent.ACTION_TIME_TICK) {
            return
        }

        println("Minute changed: ${Calendar.getInstance().get(Calendar.MINUTE)}")

        if(context == null || context !is MainActivity) {
            println("No context was found in TimeReceiver, or it is not a MainActivity")
            return
        }

        //Update widgets
        val widgetText = generateWidgetString(context, WIDGET_TEXT_LESSON_WASNT_FOUND)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, PaochaproWidget::class.java))

        for(id in widgetIds) {
            val views = RemoteViews(context.packageName, R.layout.paochapro_widget)
            views.setTextViewText(R.id.appwidget_text, widgetText)
            appWidgetManager.updateAppWidget(id, views)
        }

        //Updating global time string, which means it updates things like main screen
        context.timeString.value = widgetText
    }
}

class MainActivity : ComponentActivity() {
    var timeString = mutableStateOf("no time")

    var schedule: Array<Day> = getEmptySchedule()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        schedule = readSchedule(this, SCHEDULE_FILE_NAME, getEmptySchedule(), shouldPrint = false)

        //Register minute change on clock to update widgets, main screen
        registerReceiver(LessonStatusUpdate(), IntentFilter(Intent.ACTION_TIME_TICK))

        setContent { Root(this) }
    }

    //Working with schedule
    fun saveSchedule(shouldPrint: Boolean = false) = saveSchedule(this, SCHEDULE_FILE_NAME, schedule, shouldPrint)

    fun createTemplateSchedule(addEightLesson: Boolean, addSunday: Boolean) {
        schedule = com.paochapro.test004.createTemplateSchedule(addEightLesson, addSunday)
    }
}

@Composable
fun Root(activity: MainActivity) {
    val screen = remember { mutableStateOf(Screen.MainScreen) }

    Test004Theme {
    Column(modifier = Modifier.fillMaxSize()) {
        when(screen.value) {
            Screen.MainScreen -> {
                Row(modifier = Modifier
                    .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { screen.value = Screen.ConfigureLesson }) { Text("Изменить расписание") }
                    Button(onClick = { screen.value = Screen.DevScreen }) { Text("Тестирование") }
                }
                MainScreen(activity)
            }
            Screen.ConfigureLesson -> {
                Row(modifier = Modifier.fillMaxWidth().background(Color.hsl(0f,0f,0f,0.2f))) {
                    Button({screen.value = Screen.MainScreen}) {
                        Text("Назад")
                    }
                }

                Column(modifier = Modifier.verticalScroll(ScrollState(0))) {
                    ConfigureLesson(activity)
                }
            }
            Screen.DevScreen -> {
                Row(modifier = Modifier.fillMaxWidth().background(Color.hsl(0f,0f,0f,0.2f))) {
                    Button({screen.value = Screen.MainScreen}) {
                        Text("Назад")
                    }
                }
                DevScreen(activity)
            }
        }
    }
    }
}

@Composable
fun MainScreen(activity: MainActivity) {
    //Creating strings that will show up in the center
    val defaultString = "No lesson"
    val generatedString = activity.timeString.value
    val centerTexts = mutableListOf(defaultString)

    if(generatedString != defaultString) {
        val lessonStrings = generatedString.split(' ')
        if(lessonStrings.size >= 3) {
            val time = lessonStrings[0]
            val lesson = lessonStrings[1]
            val cabinet = lessonStrings[2]

            centerTexts.clear()
            centerTexts.add("$lesson $cabinet")
            centerTexts.add(time)
        }
    }

    val fontFamily = FontFamily(Font(R.font.jetbrains_mono))

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            for(i in centerTexts) {
                Text(
                    i,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = fontFamily,
                    fontSize = 5.em
                )
            }
        }
    }
}