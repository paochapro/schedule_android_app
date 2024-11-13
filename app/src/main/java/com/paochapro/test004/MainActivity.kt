package com.paochapro.test004

import android.os.Bundle
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
import java.util.Random

val fullWidth = Modifier.fillMaxWidth()
const val MSG = "MY_MESSAGE"
const val LESSON_COUNT = 8
const val DAY_COUNT = 7
const val DEFAULT_LESSON_TIME_MINS = 40

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

class MainActivity : ComponentActivity() {
    data class FileLesson(val lesson: Lesson?)
    data class FileDay(val lessons: Array<FileLesson>, val lessonTimeMinutes: Int)

    val SCHEDULE_FILE_NAME = "test2.json"
    var schedule: Array<Day> = getEmptySchedule()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createTemplateSchedule(false, false)
        //schedule = readSchedule(this, SCHEDULE_FILE_NAME, getEmptySchedule(), shouldPrint = false)
        printSchedule()

        setContent { UpdateScreens(this) }
    }

    fun saveSchedule(shouldPrint: Boolean = false) = saveSchedule(this, SCHEDULE_FILE_NAME, schedule, shouldPrint)

    private fun createTemplateSchedule(addEighthLesson: Boolean, addSunday: Boolean) {
        val subjects = arrayOf("Рус", "Инф", "Алгб", "Физ", "Био")

        schedule = getEmptySchedule()

        fun getRandLesson(lessonIndex: Int) : Lesson {
            val subjectIndex = Random().nextInt(subjects.size)
            val cab = (Random().nextInt(3) + 1) * 100 + (Random().nextInt(10) + 1 + 10)
            return Lesson("${lessonIndex+8}:00", subjects[subjectIndex], cab)
        }

        fun getRandDay(dayIndex: Int): Day {
            val randOffset = Random().nextInt(10)
            val lessonTime = 40 + randOffset
            val day = Day(arrayOfNulls(LESSON_COUNT), lessonTime)

            for(l in 0 until LESSON_COUNT - 2) {
                day.lessons[l] = getRandLesson(l)
            }

            if(addEighthLesson) {
                day.lessons[7] = getRandLesson(7)
            }

            return day
        }

        for(dayIndex in 0 until DAY_COUNT - 2) {
            schedule[dayIndex] = getRandDay(dayIndex)
        }

        if(addSunday)
            schedule[6] = getRandDay(6)
    }

    private fun printSchedule() {
        println("[Schedule]")
        schedule.forEachIndexed { di, d ->
            println("-- Day $di [Minutes:${d.lessonTimeMinutes}] --")

            d.lessons.forEachIndexed { li, l ->
                if (l != null)
                    println("Lesson $li: ${l.subject} / ${l.startTime} / ${l.cabinet} ")
                else
                    println("Lesson NULL")
            }
        }
    }

    private fun getEmptySchedule() = Array(DAY_COUNT) { Day(arrayOfNulls(LESSON_COUNT), DEFAULT_LESSON_TIME_MINS) }
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