package com.paochapro.test004

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.paochapro.test004.ui.theme.Test004Theme
import java.util.Calendar
import java.util.GregorianCalendar
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import com.beust.klaxon.Klaxon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

val fullWidth = Modifier.fillMaxWidth()
const val MSG = "MY_MESSAGE"
const val LESSON_COUNT = 8

enum class Screen {
    UpdateLesson,
    ConfigureLesson
}

class Schedule(var num: Int)

class MainActivity : ComponentActivity() {
    var schedule = Schedule(5)
    var lessons: Array<Lesson?> = arrayOfNulls(LESSON_COUNT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Read the file
        val file = File(this.filesDir, "test2.json")
        val json = file.readText()

        try {
            val readData = Klaxon().parseArray<Lesson>(json)
            println(json)

            if(readData != null) {
                val count = if(readData.size > LESSON_COUNT) LESSON_COUNT else readData.size

                for (n in 0..<count) {
                    lessons[n] = readData[n]
                }
            }
        }
        catch (ex: Exception) {
            println(ex.message)
        }

        setContent { UpdateScreens(this) }
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
                UpdateLesson(activity)
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

@Composable
fun UpdateLesson(activity: MainActivity) {
    val usingCustomTime = remember { mutableStateOf(false) }
    val customTime = remember { mutableStateOf(GregorianCalendar()) }
    val timeString = remember { mutableStateOf("") }
    val text = remember { mutableStateOf("") }
    val currentLesson = remember { mutableStateOf<Lesson?>(null) }

    TextField(text.value, {text.value = it})

    fun onButtonPress() {
        usingCustomTime.value = (text.value != "")

        if(usingCustomTime.value) {
            customTime.value = utilStringToCalendar(text.value)
            timeString.value = text.value
        }
        else
            timeString.value = ""
    }

    //Set current time
    val setCurrentTime = remember { mutableStateOf(false) }

    Row {
        Button(onClick = ::onButtonPress) { Text("Update time") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(setCurrentTime.value, {setCurrentTime.value = it})
            Text("Текущее время", modifier = Modifier.clickable {setCurrentTime.value = !setCurrentTime.value})
        }
    }

    if(usingCustomTime.value and !setCurrentTime.value) {
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            println("Launched effect! Custom time")

            coroutineScope.launch {
                while(true) {
                    timeString.value = utilCalendarToString(customTime.value)
                    currentLesson.value = getCurrentLesson(activity.lessons, customTime.value)
                    delay(1000)
                    customTime.value.add(Calendar.MINUTE,1)
                }
            }
        }
    }

    if(setCurrentTime.value) {
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            println("Launched effect! Current time")

            coroutineScope.launch {
                while(true) {
                    val today = GregorianCalendar()
                    val time = GregorianCalendar(0, 0, 0, today.get(Calendar.HOUR_OF_DAY), today.get(Calendar.MINUTE))
                    timeString.value = utilCalendarToString(time)
                    currentLesson.value = getCurrentLesson(activity.lessons, time)
                    delay(1000)
                }
            }
        }
    }

    Text(timeString.value)

    val les = currentLesson.value

    if(les != null) {
        Text("Кабинет: ${les.cabinet} / Время: ${les.startTime}-${getLessonEndString(les)} / Предмет: ${les.subject}")
    }
    else
        Text("SHOW CABINETS HERE")

}

fun getCurrentLesson(lessons: Array<Lesson?>, currentTime: GregorianCalendar): Lesson? {
    //val currentTime = utilStringToCalendar("10:00")

    //Get min
    var min: Lesson? = null
    for(lesson in lessons) {
        if(lesson == null)
            continue

        val end = getLessonEndCalendar(lesson)

        if(end.before(currentTime))
            continue

        if(min == null)
            min = lesson

        if(getLessonEndCalendar(min) > end)
            min = lesson
    }

    return min
}

fun getLessonEndCalendar(lesson: Lesson): GregorianCalendar {
    val lessonTimeMinutes = 45
    val calendar = utilStringToCalendar(lesson.startTime)
    calendar.add(Calendar.MINUTE , lessonTimeMinutes)
    return calendar
}

fun getLessonEndString(lesson: Lesson) = utilCalendarToString(getLessonEndCalendar(lesson))

class Lesson(val startTime: String, val subject: String, val cabinet: Int)