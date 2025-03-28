package com.paochapro.test004

import android.app.AlarmManager
import android.app.PendingIntent
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
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
const val SCHEDULE_UPDATE_INTENT = "com.paochapro.test004.SCHEDULE_UPDATE"

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

        context.updateWidgetsAndTimeString()
    }
}

class MainActivity : ComponentActivity() {
    var timeString = mutableStateOf<String?>(null)

    var schedule: Array<Day> = getEmptySchedule()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        schedule = readSchedule(this, SCHEDULE_FILE_NAME, getEmptySchedule(), shouldPrint = false)

        //Register minute change on clock to update widgets, main screen
        registerReceiver(LessonStatusUpdate(), IntentFilter(Intent.ACTION_TIME_TICK))
        //setAlarmManager

        //Show lesson schedule for the whole day before first lesson starts (8:00)
        //Like this:
        //1. Русский    5. Физика
        //2. Русский    6. Биология
        //3. Математика 7. География
        //4. Математика 8. Химия

        updateWidgetsAndTimeString()

        setContent { Root(this) }
    }

    fun setAlarmManager() {
        val intent = Intent(this, PaochaproWidget::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val currentLesson = getCurrentLessonFromSchedule(schedule)

        val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        //alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 20000, pendingIntent)
    }

    fun updateWidgetsAndTimeString() {
        PaochaproWidget.updateAll(this)

        val widgetText = generateWidgetString(schedule)

        //Updating global time string, which means it updates things like main screen
        timeString.value = widgetText
    }

    //Working with schedule
    fun onScheduleUpdate() {
        saveSchedule(this, SCHEDULE_FILE_NAME, schedule, false)
        updateWidgetsAndTimeString()
    }

    fun devCreateTemplateSchedule(addEightLesson: Boolean, addSunday: Boolean, addSaturday: Boolean = false) {
        schedule = com.paochapro.test004.createTemplateSchedule(addEightLesson, addSunday, addSaturday)
        onScheduleUpdate()
    }

    fun clearSchedule() {
        schedule = getEmptySchedule()
        onScheduleUpdate()
    }
}

@Composable
fun Root(activity: MainActivity) {
    val screen = remember { mutableStateOf(Screen.MainScreen) }

    Test004Theme {
    Column(modifier = Modifier.fillMaxSize()) {
        when(screen.value) {
            Screen.MainScreen -> {
                MainScreen(activity, Modifier.fillMaxWidth().weight(1f))
                Column(modifier = Modifier
                    .fillMaxWidth(),) {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { screen.value = Screen.ConfigureLesson }) { Text("Изменить расписание") }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { screen.value = Screen.DevScreen }) { Text("Тестирование") }
                }
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
fun MainScreen(activity: MainActivity, modifier: Modifier) {
    //Creating strings that will show up in the center
    val generatedString = activity.timeString.value
    val centerTexts = mutableListOf("Нет урока")

    if(generatedString != null) {
        val lessonStrings = generatedString.split(' ')
        if(lessonStrings.size >= 3) {
            val lesson = lessonStrings[0]
            val cabinet = lessonStrings[1]
            val time = lessonStrings[2]

            centerTexts.clear()
            centerTexts.add("$lesson $cabinet")
            centerTexts.add(time)
        }
    }

    val fontFamily = FontFamily(Font(R.font.jetbrains_mono))

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
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