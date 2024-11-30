package com.paochapro.test004

import android.annotation.SuppressLint
import android.service.autofill.RegexValidator
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.text.isDigitsOnly
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
    val weights = arrayOf(0.15f, 1.0f, 0.6f, 0.5f)
    val columnPadding = Modifier.padding(horizontal = 4.dp)

    //Should block button
    val isTimeIncorrectArray = remember { mutableStateListOf(false,false,false,false,false,false,false,false) }

    @Composable
    fun SetupItem(row: Int, column: Int) {
        //Column names
        if (row == 0) {
            Text(top[column], color = MaterialTheme.colorScheme.onSurface, modifier = columnPadding)
            return
        }

        //Lesson number
        if (column == 0) {
            Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize()) {
                Text("$row", color = MaterialTheme.colorScheme.onSurface)
            }
            return
        }

        //TextFields
        val rowData = rowDataArray[row - 1]
        val index = column-1
        val text = rowData[index]

        //If its cabinet text field, make keyboard type for numbers
        val keyboardType = if(column == 3) KeyboardType.Number else KeyboardType.Text

        fun isTimeIncorrect() : Boolean {
            //If no time is entered, leave it as correct
            if(text == "")
                return false

            //String has to follow this format: 7:00 or 14:00
            if (Regex("^\\d{1,2}:\\d\\d\$").matches(text)) {
                val nums = text.split(":")

                //Hours and minutes must be in their normal range
                if(nums[0].toInt() <= 23 && nums[1].toInt() <= 59)
                    return false
            }

            return true
        }

        //If its time text field, check if time is correct. If its incorrect, then make text field glow in red
        var isError = false

        if(column == 2) {
            isError = isTimeIncorrect()
            isTimeIncorrectArray[row-1] = isError
        }

        //Max characters for subject, time and cabinet text fields
        val maxCharacters = arrayOf(12, 5, 3)

        TextFieldStylized(text, onValueChanged =  {
                //Wont use range, because maxCharacters could be Int.MAX_VALUE which would create very large range
                if(it.length <= maxCharacters[index])
                    rowData[index] = it
            },
            modifier = columnPadding,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = isError
        )
    }

    val colEven = MaterialTheme.colorScheme.surfaceContainerHigh
    val colOdd = MaterialTheme.colorScheme.surfaceContainer

    val rowModifier = { i: Int ->
        val padding = if(i == 0) 4.dp else 8.dp

        Modifier
            .background(if (i % 2 == 0) colEven else colOdd)
            .padding(vertical = padding)
    }

    NonlazyGrid(
        columns = 4,
        itemCount = (LESSON_COUNT + 1) * 4, //header and lessons for each column
        columnWeights = weights,
        rowModifier = rowModifier,
        modifier = Modifier.padding(horizontal = 4.dp)
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

        //Direct data change in activity's schedule
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

        //Stuff activity needs to do after we've changed schedule directly
        activity.onScheduleUpdate()
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        val canSave = isTimeIncorrectArray.all {i-> i == false}
        Button(onClick = getButtonSave(), enabled = canSave ) {
            Text("Сохранить")
        }

        if(!canSave) {
            Text("Неправильный формат времени.", color = Color(0xFFF33737), modifier = Modifier.padding(horizontal = 8.dp), fontSize = 2.em)
        }
    }
}