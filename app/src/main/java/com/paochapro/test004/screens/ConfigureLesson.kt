package com.paochapro.test004.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.paochapro.test004.DEFAULT_LESSON_TIME_MINS
import com.paochapro.test004.Day
import com.paochapro.test004.LESSON_COUNT
import com.paochapro.test004.Lesson
import com.paochapro.test004.MainActivity
import com.paochapro.test004.composables.DayName
import com.paochapro.test004.composables.DayPicker
import com.paochapro.test004.composables.NonlazyGrid
import com.paochapro.test004.composables.TextFieldStylized
import com.paochapro.test004.composables.ValuePicker
import java.util.Locale
import kotlin.math.exp
import kotlin.math.floor

//TODO: Sometimes saving to schedule doesn't work!

fun getTextFieldDataArray(week: Array<Day>, dayIndex: Int): List<List<String>> {
    val day = week.getOrNull(dayIndex)

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

fun getLessonTimeMinutes(week: Array<Day>, dayIndex: Int): String {
    val day = week.getOrNull(dayIndex)

    if(day != null)
        return day.lessonTimeMinutes.toString()
    else
        return 333.toString()
}

@Composable
fun ConfigureLesson(activity: MainActivity) {
    //Week
    val weekValues = arrayOf("Чётная", "Нечётная")
    val configureWeek = remember { mutableStateOf(weekValues[0]) }

    //Get schedule to work with, based on chosen week
    val week =
        if(configureWeek.value == weekValues[0])
            activity.schedule.weekEven
        else
            activity.schedule.weekUneven

    //Day
    val configureDay = remember { mutableStateOf(DayName.Monday) }
    val configureDayIndex = configureDay.value.ordinal

    //Pure data
    val lessonTimeMinutes = remember(configureDayIndex, week) {
        mutableStateOf(getLessonTimeMinutes(week, configureDayIndex))
    }

    val rowDataArray = remember(configureDayIndex, week) {
        getTextFieldDataArray(week, configureDayIndex).map {
            mutableStateListOf(it[0], it[1], it[2])
        }
    }

    //Should block button
    val isTimeIncorrectArray = remember { mutableStateListOf(false,false,false,false,false,false,false,false,
        /*This one is for lesson length text-field. Index 8*/ false) }

    //Week picker
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Неделя: ", color = MaterialTheme.colorScheme.onSurface)
        ValuePicker(weekValues, configureWeek.value) { configureWeek.value = it }
    }
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
            //Time header
            if(column == 2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(top[column], color = MaterialTheme.colorScheme.onSurface, modifier = columnPadding)
                    TimeSettings(activity, week, configureDay.value)
                }

                return
            }

            //Title or cabinet
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
        val maxCharacters = arrayOf(99, 5, 3)

        TextFieldStylized(text, onValueChanged =  {
                //Wont use range, because maxCharacters could be Int.MAX_VALUE which would create very large range
                if(it.length <= maxCharacters[index])
                    rowData[index] = it
            },
            modifier = columnPadding.fillMaxHeight(),
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
        //modifier = Modifier.padding(horizontal = 4.dp)
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
        val day = week.getOrNull(configureDayIndex)

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

@Composable
fun ErrorText(text: String) {
    Text(text = text,
        color = Color(0xFFF33737),
        modifier = Modifier.padding(horizontal = 8.dp),
        fontSize = 2.em
    )
}

@Composable
fun TimeSettings(activity: MainActivity, week: Array<Day>, dayPicked: DayName) {
    val isMenuExpanded = remember { mutableStateOf(false) }

    Column {
        IconButton(
            onClick = { isMenuExpanded.value = true }, modifier = Modifier
                .width(Icons.Filled.Settings.defaultWidth)
                .height(Icons.Filled.Settings.defaultHeight)
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Опции для времени",
                tint = Color.White
            )
        }
        DropdownMenu(
            expanded = isMenuExpanded.value,
            onDismissRequest = { isMenuExpanded.value = false })
        {
            val nameExceptions = arrayOf("", "", "среду", "", "пятницу", "субботу", "")

            for(dayName in DayName.entries) {
                val resultDayName =
                    if(nameExceptions[dayName.ordinal] == "")
                        dayName.rusTranslation.lowercase(Locale.ROOT)
                    else
                        nameExceptions[dayName.ordinal]

                val text = "Скопировать в $resultDayName"

                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        activity.copyTime(week, fromDayIndex = dayPicked.ordinal, toDayIndex = dayName.ordinal)
                        isMenuExpanded.value = false
                    },
                    enabled = dayPicked != dayName
                )
            }
        }
    }
}