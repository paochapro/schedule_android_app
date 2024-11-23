package com.paochapro.test004

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.GregorianCalendar

@Composable
fun LessonStatus(activity: MainActivity) {
    //Custom
    val usingCustomTime = remember { mutableStateOf(false) }
    val customTime = remember { mutableStateOf(GregorianCalendar()) }
    val customTimeText = remember { mutableStateOf("") }
    val customDay = remember { mutableStateOf(DayName.Monday) }

    val timeString = remember { mutableStateOf("") }
    val currentLesson = remember { mutableStateOf<Lesson?>(null) }
    val currentDay = remember { mutableStateOf(Day(arrayOfNulls(8))) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("День: ")
        DayPicker(customDay.value) { customDay.value = it }
    }

    TextField(customTimeText.value, {customTimeText.value = it})

    fun onButtonPress() {
        usingCustomTime.value = (customTimeText.value != "")

        if(usingCustomTime.value) {
            customTime.value = utilStringToCalendar(customTimeText.value)
            timeString.value = customTimeText.value
        }
        else
            timeString.value = ""
    }

    val setCurrentTime = remember { mutableStateOf(false) }

    Row {
        Button(onClick = ::onButtonPress) { Text("Update time") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(setCurrentTime.value, {setCurrentTime.value = it})
            Text("Текущее время", modifier = Modifier.clickable {setCurrentTime.value = !setCurrentTime.value})
        }
    }

    //Update lesson status
    suspend fun updateLessonStatusCustom() {
        val resultTime = customTime.value
        resultTime.set(Calendar.DAY_OF_WEEK, dayIndexToCalendarDay(customDay.value.ordinal))

        val dayOfWeek = calendarDayToDayIndex(resultTime.get(Calendar.DAY_OF_WEEK))
        val day = activity.schedule.getOrNull(dayOfWeek)

        timeString.value = utilCalendarToString(customTime.value)

        if(day != null) {
            currentLesson.value = getCurrentLesson(day, resultTime)
            currentDay.value = day
        }
        else
            currentLesson.value = null

        delay(1000)

        customTime.value.add(Calendar.MINUTE,1)
    }

    suspend fun updateLessonStatus() {
        val today = GregorianCalendar()
        val dayOfWeek = calendarDayToDayIndex(today.get(Calendar.DAY_OF_WEEK))
        val day = activity.schedule.getOrNull(dayOfWeek)

        timeString.value = utilCalendarToString(today)

        if(day != null) {
            currentLesson.value = getCurrentLesson(day, today)
            currentDay.value = day
        }
        else
            currentLesson.value = null

        

        delay(1000)
    }

    if(usingCustomTime.value and !setCurrentTime.value) {
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            println("Launched effect! Custom time")
            coroutineScope.launch { while(true) {updateLessonStatusCustom()} }
        }
    }

    if(setCurrentTime.value) {
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            println("Launched effect! Current time")
            coroutineScope.launch { while(true) {updateLessonStatus()} }
        }
    }

    //Show time string and current lesson
    Text(timeString.value)

    val les = currentLesson.value

    if(les != null) {
        Text("Кабинет: ${les.cabinet} / Время: ${les.startTime}-${getLessonEndString(les, currentDay.value.lessonTimeMinutes)} / Предмет: ${les.subject}")
    }
    else
        Text("SHOW CABINETS HERE")

    val addEigthLesson = remember { mutableStateOf(false) }
    val addSunday = remember { mutableStateOf(false) }

    Button(onClick = { activity.createTemplateSchedule(addEigthLesson.value, addSunday.value) }) { Text("Generate new schedule") }

    TextCheckbox("Добавить 8ой урок", addEigthLesson.value, { x -> addEigthLesson.value = x } )
    TextCheckbox("Добавить воскресенье", addSunday.value, { x -> addSunday.value = x } )
}