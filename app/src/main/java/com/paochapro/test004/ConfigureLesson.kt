package com.paochapro.test004

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlin.math.floor

//TODO: Sometimes saving to schedule doesn't work!

fun getTextFieldDataArray(dayIndex: Int, activity: MainActivity): List<List<String>> {
    val day = activity.schedule.getOrNull(dayIndex)

    if(day == null) {
        println("Couldn't find day! Returning  empty mutable list")
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

    //Should block button
    val isTimeIncorrectArray = remember { mutableStateListOf(false,false,false,false,false,false,false,false,
        /*This one is for lesson length text-field. Index 8*/ false) }

    //Day picker
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("День: ", color = MaterialTheme.colorScheme.onSurface)
        DayPicker(configureDay.value) { configureDay.value = it }
    }

    //Check if lesson length text-field is correct
    val minsInt = lessonTimeMinutes.value.toIntOrNull()
    var isLessonLengthError = true

    if(minsInt != null) {
        isLessonLengthError = minsInt >= 60
    }

    isTimeIncorrectArray[8] = isLessonLengthError

    //Lesson length
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Text("Длительность урока (мин): ", color = MaterialTheme.colorScheme.onSurface)
        TextFieldStylized(lessonTimeMinutes.value,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onValueChanged = {
                if(it.length <= 2)
                    lessonTimeMinutes.value = it
            },
            isError = isLessonLengthError
        )
    }

    //TextField grid
    val top = arrayOf("", "Предмет", "Время", "Кабинет")
    val weights = arrayOf(0.15f, 1.0f, 0.6f, 0.5f)
    val columnPadding = Modifier.padding(horizontal = 4.dp)

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

    //Check if every row is either fully filled or not filled at all. Then block save button if needed
    //Basically, block button if some row is partially filled
    var isSomeRowIsPartiallyFilled = false

    for(i in 0..<LESSON_COUNT) {
        val rowData = rowDataArray[i]

        if(rowData[0] == "" && rowData[1] == "" && rowData[2] == "")
            continue

        if(rowData[0] != "" && rowData[1] != "" && rowData[2] != "")
            continue

        isSomeRowIsPartiallyFilled = true
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
        val correctTimeFormat = isTimeIncorrectArray.all {i-> i == false}

        Button(onClick = getButtonSave(), enabled = correctTimeFormat && !isSomeRowIsPartiallyFilled) {
            Text("Сохранить")
        }

        Column {
            if (isSomeRowIsPartiallyFilled)
                ErrorText("Данные не заполнены.")

            if (!correctTimeFormat)
                ErrorText("Неправильный формат времени.")
        }
    }
}

@Composable fun ErrorText(text: String) {
    Text(text = text,
        color = Color(0xFFF33737),
        modifier = Modifier.padding(horizontal = 8.dp),
        fontSize = 2.em
    )
}