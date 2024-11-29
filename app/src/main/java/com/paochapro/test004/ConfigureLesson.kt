package com.paochapro.test004

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import kotlin.math.floor

//TODO: Sometimes saving to schedule doesn't work!

fun getTextFieldDataArray(dayIndex: Int, activity: MainActivity): List<List<String>> {
    val day = activity.schedule.getOrNull(dayIndex)

    if(day == null) {
        println("Couldn't find day! Returning empty mutable list")
        return List(LESSON_COUNT) { listOf("", "", "") }
    }

    return List(LESSON_COUNT) {
        val lesson: Lesson? = day.lessons[it]
        if(lesson != null)
            listOf(lesson.subject, lesson.startTime, "${lesson.cabinet}")
        else
            listOf("", "", "")
    }
}

fun getLessonTimeMinutes(dayIndex: Int, activity: MainActivity): String {
    val day = activity.schedule.getOrNull(dayIndex)

    if(day != null)
        return day.lessonTimeMinutes.toString()
    else
        return 333.toString()
}

@Composable
fun ConfigureLesson(activity: MainActivity) {
    //Day
    val configureDay = remember { mutableStateOf(DayName.Monday) }
    val configureDayIndex = configureDay.value.ordinal

    //Pure data
    val lessonTimeMinutes = remember(configureDayIndex) {
        mutableStateOf(getLessonTimeMinutes(configureDayIndex, activity))
    }

    val rowDataArray = remember(configureDayIndex) {
        getTextFieldDataArray(configureDayIndex, activity).map {
            mutableStateListOf(it[0], it[1], it[2])
        }
    }

    //Day picker
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("День: ", color = MaterialTheme.colorScheme.onSurface)
        DayPicker(configureDay.value) { configureDay.value = it }
    }

    //Lesson time
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Длительность урока (мин): ", color = MaterialTheme.colorScheme.onSurface)
        TextField(lessonTimeMinutes.value, onValueChange = {lessonTimeMinutes.value = it})
    }

    //TextField grid
    val top = arrayOf("", "Предмет", "Время", "Кабинет")
    val weights = arrayOf(0.2f, 1.0f, 0.5f, 0.4f)

    @Composable
    fun SetupItem(row: Int, column: Int) {
        //Column names
        if (row == 0) {
            Text(top[column], color = MaterialTheme.colorScheme.onSurface)
            return
        }

        //Lesson number
        if (column == 0) {
            Text("$row", color = MaterialTheme.colorScheme.onSurface)
            return
        }

        //TextFields
        val rowData = rowDataArray[row - 1]
        TextField(rowData[column - 1], { rowData[column - 1] = it })
    }

    NonlazyGrid(
        columns = 4,
        itemCount = 9 * 4,
        columnWeights = weights
    ) {
        val column = it % 4
        val row = floor(it.toFloat() / 4f).toInt()
        SetupItem(row, column)
    }

    //Save button
    //TODO: check array oob
    fun getButtonSave(): () -> Unit = {
        val day = activity.schedule.getOrNull(configureDayIndex)

        if(day == null) {
            println("Couldn't find day!")
        }

        //Saving
        if(day != null) {
            val time = lessonTimeMinutes.value.toIntOrNull()

            if(time != null)
                day.lessonTimeMinutes = time
            else
                day.lessonTimeMinutes = DEFAULT_LESSON_TIME_MINS

            for(n in 0..< LESSON_COUNT) {
                val row = rowDataArray.getOrElse(n) { listOf("","","") }

                if(row[0] == "" || row[1] == "" || row[2] == "") {
                    day.lessons[n] = null
                    continue
                }

                day.lessons[n] = Lesson(row[1], row[0], row[2].toInt())
            }
        }

        activity.saveSchedule()
    }

    Button(onClick = getButtonSave()) {
        Text("Save")
    }
}