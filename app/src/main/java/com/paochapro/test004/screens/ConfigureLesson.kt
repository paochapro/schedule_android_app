package com.paochapro.test004.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import com.paochapro.test004.DEFAULT_LESSON_TIME_MINS
import com.paochapro.test004.Day
import com.paochapro.test004.LESSON_COUNT
import com.paochapro.test004.Lesson
import com.paochapro.test004.MainActivity
import com.paochapro.test004.calendarDayToDayIndex
import com.paochapro.test004.composables.DayName
import com.paochapro.test004.composables.DayPicker
import com.paochapro.test004.composables.ErrorText
import com.paochapro.test004.composables.NonlazyGrid
import com.paochapro.test004.composables.TextFieldStylized
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import kotlin.math.floor

//TODO: Sometimes saving to schedule doesn't work!
@Composable
fun ConfigureLesson(activity: MainActivity) {
    val dayOfWeek = calendarDayToDayIndex(GregorianCalendar().get(Calendar.DAY_OF_WEEK))
    val todayWeekIndex = GregorianCalendar().get(Calendar.WEEK_OF_YEAR)

    //Week
    val weekStrings = arrayOf("Текущая неделя", "Следующая неделя")
    val nextWeekChosen = remember { mutableStateOf(false) }

    //Configure week and other week
    var chosenWeekIndex = todayWeekIndex

    if(nextWeekChosen.value) {
        chosenWeekIndex += 1 //TODO: might be problems when its the last week in year
    }

    val configureWeek =
        if(chosenWeekIndex % 2 == 0)
            activity.schedule.weekEven
        else
            activity.schedule.weekUneven

    val otherWeek =
        if(chosenWeekIndex % 2 == 0)
            activity.schedule.weekUneven
        else
            activity.schedule.weekEven

    //Day
    val configureDay = remember { mutableStateOf(DayName.fromInt(dayOfWeek)) }
    val configureDayIndex = configureDay.value.ordinal

    val rowDataArray = remember(configureDayIndex, configureWeek) {
        getTextFieldDataArray(configureWeek, configureDayIndex).map {
            mutableStateListOf(it[0], it[1], it[2])
        }
    }

    //Lesson length
    val lessonTimeMinutes = remember(configureDayIndex, configureWeek) {
        mutableStateOf(getLessonTimeMinutes(configureWeek, configureDayIndex))
    }

    val lessonTimeMinutesInt = lessonTimeMinutes.value.toIntOrNull()
    val isLessonLengthError = lessonTimeMinutesInt == null || lessonTimeMinutesInt >= 60

    Column(Modifier.padding(horizontal = 8.dp)) {
        WeekPicker(nextWeekChosen, weekStrings)
        DayPickerRow(configureDay)
        LessonLength(lessonTimeMinutes, nextWeekChosen, isLessonLengthError)
    }

    val isDataIncorrectArray = remember { Array(2) { MutableList(8) { false } } }

    LessonGrid(
        activity,
        configureWeek,
        configureDay,
        nextWeekChosen,
        rowDataArray,
        isDataIncorrectArray
    )

    BottomPanel(
        isDataIncorrectArray,
        isLessonLengthError,
        rowDataArray,
        configureDayIndex,
        lessonTimeMinutes,
        activity,
        configureWeek,
        otherWeek,
        nextWeekChosen.value
    )
}

@Composable
private fun DayPickerRow(configureDay: MutableState<DayName>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("День: ", color = MaterialTheme.colorScheme.onSurface)
        DayPicker(configureDay.value) { configureDay.value = it }
    }
}

@Composable
private fun WeekPicker(
    nextWeekChosen: MutableState<Boolean>,
    weekStrings: Array<String>
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Неделя: ", color = MaterialTheme.colorScheme.onSurface)
        Button(onClick = { nextWeekChosen.value = !nextWeekChosen.value }) {
            Text(if (nextWeekChosen.value) weekStrings[1] else weekStrings[0])
        }
    }
}

@Composable
private fun BottomPanel(
    isDataIncorrectArray: Array<MutableList<Boolean>>,
    isLessonLengthError: Boolean,
    rowDataArray: List<SnapshotStateList<String>>,
    configureDayIndex: Int,
    lessonTimeMinutes: MutableState<String>,
    activity: MainActivity,
    configureWeek: Array<Day>,
    otherWeek: Array<Day>,
    nextWeekChosen: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
        val correctDataFormat =
            isDataIncorrectArray.all { column -> column.all { !it } } && !isLessonLengthError
        val isSomeRowIsPartiallyFilled = isSomeRowIsPartiallyFilled(rowDataArray)

        val onClick: () -> Unit

        if (!nextWeekChosen) {

            onClick = getButtonSaveForFirstWeek(
                firstWeek = configureWeek,
                nextWeek = otherWeek,
                configureDayIndex = configureDayIndex,
                lessonTimeMinutes = lessonTimeMinutes.value.toIntOrNull(),
                rowDataArray = rowDataArray,
                activity = activity
            )
        } else {

            onClick = saveConfigureWeek(
                configureWeek = configureWeek,
                configureDayIndex = configureDayIndex,
                lessonTimeMinutes = lessonTimeMinutes.value.toIntOrNull(),
                rowDataArray = rowDataArray,
                activity = activity
            )
        }

        if (isSomeRowIsPartiallyFilled)
            ErrorText("Данные не заполнены.")

        if (!correctDataFormat) {
            ErrorText("Неправильный формат данных.")
            ErrorText("Время заполняется так: 7:00, 14:00")
            ErrorText("Кабинет должен быть числом")
        }

        Button(
            modifier = Modifier.fillMaxWidth(0.8f),
            onClick = onClick,
            enabled = correctDataFormat && !isSomeRowIsPartiallyFilled
        )
        {
            Text("Сохранить")
        }
    }
}

@Composable
private fun LessonGrid(
    activity: MainActivity,
    configureWeek: Array<Day>,
    configureDay: MutableState<DayName>,
    nextWeekChosen: MutableState<Boolean>,
    rowDataArray: List<SnapshotStateList<String>>,
    isDataIncorrectArray: Array<MutableList<Boolean>>
) {
    val top = arrayOf("", "Предмет", "Время", "Кабинет")
    val weights = arrayOf(0.15f, 1.0f, 0.6f, 0.5f)
    val columnPadding = Modifier.padding(horizontal = 4.dp)

    val colEven = MaterialTheme.colorScheme.surfaceContainer
    val colOdd = MaterialTheme.colorScheme.surfaceContainer

    val nextWeekColEven = MaterialTheme.colorScheme.primary
    val nextWeekColOdd = MaterialTheme.colorScheme.secondary

    val rowModifier = { i: Int ->
        val padding = if (i == 0) 4.dp else 4.dp

        Modifier
            .background(if (i % 2 == 0) colEven else colOdd)
            .padding(vertical = padding)
    }

    NonlazyGrid(
        columns = 4,
        itemCount = (LESSON_COUNT + 1) * 4, //header and lessons for each column
        columnWeights = weights,
        rowModifier = rowModifier,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
    ) {
        val column = it % 4
        val row = floor(it.toFloat() / 4f).toInt()
        LessonGridItem(
            row,
            column,
            top,
            columnPadding,
            activity,
            configureWeek,
            configureDay,
            nextWeekChosen,
            rowDataArray,
            isDataIncorrectArray
        )
    }
}

@Composable
private fun LessonGridItem(
    row: Int,
    column: Int,
    top: Array<String>,
    columnPadding: Modifier,
    activity: MainActivity,
    configureWeek: Array<Day>,
    configureDay: MutableState<DayName>,
    nextWeekChosen: MutableState<Boolean>,
    rowDataArray: List<SnapshotStateList<String>>,
    isDataIncorrectArray: Array<MutableList<Boolean>>
) {
    //Column names
    if (row == 0) {
        //Time header
        if (column == 2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    top[column],
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = columnPadding
                )
                TimeSettings(
                    activity,
                    configureWeek,
                    configureDay.value,
                    enabled = !nextWeekChosen.value
                )
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
    val index = column - 1
    val text = rowData[index]

    //If its cabinet text field, make keyboard type for numbers
    val keyboardType = if (column == 3) KeyboardType.Number else KeyboardType.Text

    fun isTimeIncorrect(): Boolean {
        //If no time is entered, leave it as correct
        if (text == "")
            return false

        //String has to follow this format: 7:00 or 14:00
        if (Regex("^\\d{1,2}:\\d\\d\$").matches(text)) {
            val nums = text.split(":")

            //Hours and minutes must be in their normal range
            if (nums[0].toInt() <= 23 && nums[1].toInt() <= 59)
                return false
        }

        return true
    }

    //If its time or cabinet text field, check if they're correct.
    //If someone is incorrect, make text field glow in red, and block saving
    var isError = false

    if (column == 2) {
        isError = isTimeIncorrect()
        isDataIncorrectArray[0][row - 1] = isError
    }

    if (column == 3) {
        isError = !text.isDigitsOnly()
        isDataIncorrectArray[1][row - 1] = isError
    }

    //If its time and the next week is chosen, disable time
    //Because we shouldn't change time on the next week
    val enabled = !(column == 2 && nextWeekChosen.value)

    //Max characters for subject, time and cabinet text fields
    val maxCharacters = arrayOf(99, 5, 3)

    TextFieldStylized(
        text, onValueChanged = {
            //Wont use range, because maxCharacters could be Int.MAX_VALUE which would create very large range
            if (it.length <= maxCharacters[index])
                rowData[index] = it
        },
        modifier = columnPadding.fillMaxHeight(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        isError = isError,
        enabled = enabled
    )
}

@Composable
private fun LessonLength(
    lessonTimeMinutes: MutableState<String>,
    nextWeekChosen: MutableState<Boolean>,
    isError: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text("Длительность урока (мин): ", color = MaterialTheme.colorScheme.onSurface)
        TextFieldStylized(
            lessonTimeMinutes.value,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onValueChanged = {
                if (it.length <= 2)
                    lessonTimeMinutes.value = it
            },

            //Check if lesson length text-field is correct
            isError = isError,
            enabled = !nextWeekChosen.value
        )
    }
}

@Composable
private fun TimeSettings(activity: MainActivity, week: Array<Day>, dayPicked: DayName, enabled: Boolean) {
    val isMenuExpanded = remember { mutableStateOf(false) }

    Column {
        IconButton(
            onClick = { isMenuExpanded.value = true }, modifier = Modifier
                .width(Icons.Filled.Settings.defaultWidth)
                .height(Icons.Filled.Settings.defaultHeight),
            enabled = enabled
        ) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Опции для времени",
                tint = if(enabled) Color.White else Color.Gray,
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

private fun isSomeRowIsPartiallyFilled(rowDataArray: List<List<String>>) : Boolean {
    //Check if every row is either fully filled or not filled at all (not including cabinet).
    //Then block save button if needed
    //Basically, block button if some row is partially filled
    for(i in 0..<LESSON_COUNT) {
        val rowData = rowDataArray[i]

        //If all are empty, its fine
        if (rowData[0] == "" && rowData[1] == "" && rowData[2] == "")
            continue

        //If some row is not empty (subject or/and lesson or/and cabinet)
        //Check if necessary components, subject and time, are present
        if (rowData[0] != "" && rowData[1] != "")
            continue

        return true
    }

    return false
}

//TODO: Fix this function calling onScheduleUpdate two times
private fun getButtonSaveForFirstWeek(
    firstWeek: Array<Day>,
    nextWeek: Array<Day>,
    configureDayIndex: Int,
    lessonTimeMinutes: Int?,
    rowDataArray: List<List<String>>,
    activity: MainActivity
): () -> Unit = {
    fun firstAndNextLessonEqual(first: Lesson?, next: Lesson?) : Boolean {
        if(first == null && next == null)
            return false

        if(first != null && next != null) {
            return first.subject == next.subject
                    && first.cabinet == next.cabinet
        }

        
        return false
    }

    val nextDayIndeciesToOverwrite = mutableListOf<Int>()
    val firstDay = firstWeek[configureDayIndex].lessons
    val nextDay = nextWeek[configureDayIndex].lessons

    for(lessonIndex in firstDay.indices) {
        //If next day lesson is the same as first day lesson, or if its empty
        //Then the lesson should be overwritten by the new one in first day
        if(firstAndNextLessonEqual(firstDay[lessonIndex], nextDay[lessonIndex]) ||
            nextDay[lessonIndex] == null) {
            nextDayIndeciesToOverwrite.add(lessonIndex)
        }
    }

    saveConfigureWeek(
        configureWeek = firstWeek,
        configureDayIndex,
        lessonTimeMinutes,
        rowDataArray,
        activity)
        .invoke()

    //Copy lesson time minutes to other day
    nextWeek[configureDayIndex].lessonTimeMinutes = firstWeek[configureDayIndex].lessonTimeMinutes

    //Overwrite next day lessons
    val newFirstDay = firstWeek[configureDayIndex].lessons

    for(lessonIndex in nextDay.indices) {
        if(lessonIndex in nextDayIndeciesToOverwrite) {
            nextDay[lessonIndex] = newFirstDay[lessonIndex]
            continue
        }

        //Copy time
        val firstLesson = newFirstDay[lessonIndex]
        val nextLesson = nextDay[lessonIndex]

        if(nextLesson != null && firstLesson != null) {
            nextDay[lessonIndex] = Lesson(
                startTime = firstLesson.startTime,
                cabinet = nextLesson.cabinet,
                subject = nextLesson.subject)
        }
    }

    activity.onScheduleUpdate()
}

private fun saveConfigureWeek(
    configureWeek: Array<Day>,
    configureDayIndex: Int,
    lessonTimeMinutes: Int?,
    rowDataArray: List<List<String>>,
    activity: MainActivity
): () -> Unit = {
    val day = configureWeek.getOrNull(configureDayIndex)

    if(day == null) {
        println("Couldn't find day!")
    }

    //Direct data change in activity's schedule
    if(day != null) {
        if(lessonTimeMinutes != null)
            day.lessonTimeMinutes = lessonTimeMinutes
        else
            day.lessonTimeMinutes = DEFAULT_LESSON_TIME_MINS

        for(n in 0..< LESSON_COUNT) {
            val row = rowDataArray.getOrElse(n) { listOf("","","") }

            //If time or/and subject are empty, don't save the lesson
            if(row[0] == "" || row[1] == "") {
                day.lessons[n] = null
                continue
            }

            val cabinet = if(row[2] != "" && row[2].isDigitsOnly()) row[2].toInt() else -1
            day.lessons[n] = Lesson(row[1], row[0], cabinet)
        }
    }

    //Stuff activity needs to do after we've changed schedule directly
    activity.onScheduleUpdate()
}

private fun getTextFieldDataArray(week: Array<Day>, dayIndex: Int): List<List<String>> {
    val day = week.getOrNull(dayIndex)

    if(day == null) {
        println("Couldn't find day! Returning  empty mutable list")
        return List(LESSON_COUNT) { listOf("", "", "") }
    }

    return List(LESSON_COUNT) {
        val lesson: Lesson? = day.lessons[it]
        if(lesson != null)
            listOf(lesson.subject, lesson.startTime, if(lesson.cabinet != -1) "${lesson.cabinet}" else "")
        else
            listOf("", "", "")
    }
}

private fun getLessonTimeMinutes(week: Array<Day>, dayIndex: Int): String {
    val day = week.getOrNull(dayIndex)

    if(day != null)
        return day.lessonTimeMinutes.toString()
    else
        return 333.toString()
}