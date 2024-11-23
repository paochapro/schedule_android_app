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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.paochapro.test004.ui.theme.Test004Theme
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Random

val fullWidth = Modifier.fillMaxWidth()
const val MSG = "MY_MESSAGE"
const val LESSON_COUNT = 8
const val DAY_COUNT = 7
const val DEFAULT_LESSON_TIME_MINS = 40
const val WIDGET_TEXT_LESSON_WASNT_FOUND = "Следующий предмет не найден"
const val SCHEDULE_FILE_NAME = "test2.json"

enum class Screen {
    UpdateLesson,
    ConfigureLesson
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

class TimeReceiver : BroadcastReceiver() {
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
    }
}

class MainActivity : ComponentActivity() {
    data class FileLesson(val lesson: Lesson?)
    data class FileDay(val lessons: Array<FileLesson>, val lessonTimeMinutes: Int)

    var schedule: Array<Day> = getEmptySchedule()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        schedule = readSchedule(this, SCHEDULE_FILE_NAME, getEmptySchedule(), shouldPrint = false)
        printSchedule(schedule)

        //Register minute change on clock to update widgets
        registerReceiver(TimeReceiver(), IntentFilter(Intent.ACTION_TIME_TICK))

        setContent { UpdateScreens(this) }
    }

    //Working with schedule
    fun saveSchedule(shouldPrint: Boolean = false) = saveSchedule(this, SCHEDULE_FILE_NAME, schedule, shouldPrint)

    fun createTemplateSchedule(addEightLesson: Boolean, addSunday: Boolean) {
        schedule = com.paochapro.test004.createTemplateSchedule(addEightLesson, addSunday)
    }

}

@Composable
fun UpdateScreens(activity: MainActivity) {
    val screen = remember { mutableStateOf(Screen.UpdateLesson) }

    Test004Theme { Column(modifier = Modifier.verticalScroll(ScrollState(0))) {
        when(screen.value) {
            Screen.UpdateLesson -> {
                Button({ screen.value = Screen.ConfigureLesson }) {
                    Text("Configure lesson")
                }
                LessonStatus(activity)
            }
            Screen.ConfigureLesson -> {
                Button({ screen.value = Screen.UpdateLesson }) {
                    Text("Go back")
                }
                ConfigureLesson(activity)
            }
        }
    } }
}