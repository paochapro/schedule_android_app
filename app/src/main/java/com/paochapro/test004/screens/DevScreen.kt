package com.paochapro.test004.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.paochapro.test004.Day
import com.paochapro.test004.Lesson
import com.paochapro.test004.MainActivity
import com.paochapro.test004.composables.DayName
import com.paochapro.test004.composables.DayPicker
import com.paochapro.test004.composables.TextCheckbox
import com.paochapro.test004.createEmptySchedule
import com.paochapro.test004.createRandomSchedule
import com.paochapro.test004.dayIndexToCalendarDay
import com.paochapro.test004.getCurrentLesson
import com.paochapro.test004.getLessonEndString
import com.paochapro.test004.readWebsiteAndStoreInSchedule
import com.paochapro.test004.utilCalendarToString
import com.paochapro.test004.utilStringToCalendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.GregorianCalendar

@Composable
fun DevScreen(activity: MainActivity) {
    //Custom
    val usingCustomTime = remember { mutableStateOf(false) }
    val customTime = remember { mutableStateOf(GregorianCalendar()) }
    val customTimeText = remember { mutableStateOf("") }
    val customDay = remember { mutableStateOf(DayName.Monday) }

    val timeString = remember { mutableStateOf("") }
    val currentLesson = remember { mutableStateOf<Lesson?>(null) }
    val currentDay = remember { mutableStateOf(Day(arrayOfNulls(8))) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("День: ", color = MaterialTheme.colorScheme.onSurface)
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
        Button(onClick = ::onButtonPress) { Text("Обновить") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(setCurrentTime.value, {setCurrentTime.value = it})
            Text("Текущее время",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable {setCurrentTime.value = !setCurrentTime.value})
        }
    }

    //Update lesson status
    suspend fun updateLessonStatusCustom() {
        val resultTime = customTime.value
        resultTime.set(Calendar.DAY_OF_WEEK, dayIndexToCalendarDay(customDay.value.ordinal))

        val day = activity.schedule.getCurrentDay(resultTime)

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
        val day = activity.schedule.getCurrentDay(today)

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
    Text(timeString.value, color = MaterialTheme.colorScheme.onSurface)

    val les = currentLesson.value

    if(les != null) {
        Text(
            color = MaterialTheme.colorScheme.onSurface,
            text = "Кабинет: ${les.cabinet} / Время: ${les.startTime}-${getLessonEndString(les, currentDay.value.lessonTimeMinutes)} / Предмет: ${les.subject}")
    }
    else
        Text(color = MaterialTheme.colorScheme.onSurface,
            text= "Нет урока")

    val addEigthLesson = remember { mutableStateOf(false) }
    val addSaturday = remember { mutableStateOf(false) }
    val addSunday = remember { mutableStateOf(false) }

    Button(onClick = {
        activity.schedule = createRandomSchedule(
            addEighthLesson = addEigthLesson.value,
            addSaturday = addSaturday.value,
            addSunday = addSunday.value,
        )
        activity.onScheduleUpdate()
    } )
    {
        Text("Сгенерировать расписание")
    }

    Button(onClick =
    {
        activity.schedule = createEmptySchedule()
        activity.onScheduleUpdate()
    })
    {
        Text("Отчистить расписание")
    }

    TextCheckbox("Добавить 8ой урок", addEigthLesson.value, { x -> addEigthLesson.value = x } )
    TextCheckbox("Добавить субботу", addSaturday.value, { x -> addSaturday.value = x } )
    TextCheckbox("Добавить воскресенье", addSunday.value, { x -> addSunday.value = x } )
}