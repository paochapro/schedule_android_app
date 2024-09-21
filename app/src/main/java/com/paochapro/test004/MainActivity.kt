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
import com.beust.klaxon.Klaxon
import com.paochapro.test004.ui.theme.Test004Theme
import java.io.File
import java.util.Random

val fullWidth = Modifier.fillMaxWidth()
const val MSG = "MY_MESSAGE"
const val LESSON_COUNT = 8
const val DAY_COUNT = 7

enum class Screen {
    UpdateLesson,
    ConfigureLesson
}

enum class Day(val rusTranslation: String) {
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
                0 -> Day.Monday
                1 -> Day.Tuesday
                2 -> Day.Wednesday
                3 -> Day.Thursday
                4 -> Day.Friday
                5 -> Day.Saturday
                6 -> Day.Sunday
                else -> {
                    println("Integer $int isnt matching any day. Returning Day.Monday")
                    Day.Monday
                }
            }
        }
    }
}

class Lesson(val startTime: String, val subject: String, val cabinet: Int)

class MainActivity : ComponentActivity() {
    data class FileLesson(val lesson: Lesson?)
    data class FileDay(val lessons: Array<FileLesson>?)

    val SCHEDULE_FILE_NAME = "test2.json"
    var schedule: Array<Array<Lesson?>?> = arrayOfNulls(DAY_COUNT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readSchedule()
        printSchedule()

        setContent { UpdateScreens(this) }
    }

    private fun readSchedule(): Boolean {
        //Read the file
        val file = File(this.filesDir, SCHEDULE_FILE_NAME)

        if(!file.exists()) {
            println("No file to read")
            schedule = arrayOfNulls(DAY_COUNT)
            return false
        }

        val json = file.readText()

        try {
            val read = Klaxon().parseArray<FileDay>(json)//Quite slow!
            println("Read: $json")

            if(read == null) {
                println("Failed reading the schedule, it is probably empty. No exception was thrown")
                return false
            }

            //Start copying with nulls
            schedule = arrayOfNulls(DAY_COUNT)

            for(d in 0 until DAY_COUNT) {
                val readFileDay = read.getOrNull(d)

                if(readFileDay == null) {
                    println("File contains out of bounds day! Skipping")
                    continue
                }

                if(readFileDay.lessons == null) {
                    schedule[d] = null
                    continue
                }

                val resultDay: Array<Lesson?> = arrayOfNulls(LESSON_COUNT)

                for(l in 0 until LESSON_COUNT) {
                    val readFileLesson = readFileDay.lessons.getOrNull(l)

                    if(readFileLesson == null) {
                        println("File contains out of bounds lesson! Skipping")
                        continue
                    }

                    resultDay[l] = readFileLesson.lesson
                }

                schedule[d] = resultDay
            }
        }
        catch(ex: Exception) {
            println("Failed reading the schedule. Exception: " + ex.message)
        }

        return true
    }

    fun saveSchedule() {
        //val nonNullableSchedule = schedule.filterNotNull().map { it.filterNotNull() }.map { FileDay(it) }
        val fileSchedule = schedule.map {
            FileDay( it?.map { FileLesson(it) }?.toTypedArray() )
        }

        val json = Klaxon().toJsonString(fileSchedule)
        val file = File(this.filesDir, SCHEDULE_FILE_NAME)

        if(!file.exists()) {
            if (!file.createNewFile()) {
                println("Failed to create the schedule file when trying to save")
                return
            }
        }

        file.writeText(json)
        println("Saved: $json")
    }

    private fun createTemplateSchedule(addEighthLesson: Boolean, addSunday: Boolean) {
        val subjects = arrayOf("Рус", "Инф", "Алгб", "Физ", "Био")

        schedule = arrayOfNulls(DAY_COUNT)

        fun addDay(dayIndex: Int) {
            val day = arrayOfNulls<Lesson?>(LESSON_COUNT)

            for(l in 0 until LESSON_COUNT - 2) {
                val subjectIndex = Random().nextInt(subjects.size)
                val cab = (Random().nextInt(3) + 1) * 100 + (Random().nextInt(10) + 1 + 10)
                day[l] = Lesson("${l+8}:00", subjects[subjectIndex], cab)
            }

            if(addEighthLesson) {
                val subjectIndex = Random().nextInt(subjects.size)
                val cab = (Random().nextInt(3) + 1) * 100 + (Random().nextInt(10) + 1 + 10)
                day[7] = Lesson("15:00", subjects[subjectIndex], cab)
            }

            schedule[dayIndex] = day
        }

        for(dayIndex in 0 until DAY_COUNT - 2) {
            addDay(dayIndex)
        }

        if(addSunday)
            addDay(6)
    }

    private fun printSchedule() {
        println("[Schedule]")
        schedule.forEachIndexed { di, d ->
            if(d != null) {
                println("-- Day $di --")

                d.forEachIndexed { li, l ->
                    if (l != null)
                        println("Lesson $li: ${l.subject} / ${l.startTime} / ${l.cabinet} ")
                    else
                        println("Lesson NULL")
                }
            }
            else
                println("-- Day NULL--")
        }
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