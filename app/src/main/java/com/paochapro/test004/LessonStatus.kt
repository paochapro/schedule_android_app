package com.paochapro.test004

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val usingCustomTime = remember { mutableStateOf(false) }
    val customTime = remember { mutableStateOf(GregorianCalendar()) }
    val customTimeText = remember { mutableStateOf("") }

    val timeString = remember { mutableStateOf("") }
    val currentLesson = remember { mutableStateOf<Lesson?>(null) }

    //Set custom time and day
    val customDay = remember { mutableStateOf(Day.Monday) }

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
    suspend fun UpdateLessonStatusCustom() {
        timeString.value = utilCalendarToString(customTime.value)
        val resultTime = customTime.value
        resultTime.set(Calendar.DAY_OF_WEEK, dayIndexToCalendarDay(customDay.value.ordinal))

        currentLesson.value = getCurrentLesson(activity.schedule, resultTime)
        delay(1000)
        customTime.value.add(Calendar.MINUTE,1)
    }

    suspend fun UpdateLessonStatus() {
        val today = GregorianCalendar()
        timeString.value = utilCalendarToString(today)
        currentLesson.value = getCurrentLesson(activity.schedule, today)
        delay(1000)
    }

    if(usingCustomTime.value and !setCurrentTime.value) {
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            println("Launched effect! Custom time")
            coroutineScope.launch { while(true) {UpdateLessonStatusCustom()} }
        }
    }

    if(setCurrentTime.value) {
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            println("Launched effect! Current time")
            coroutineScope.launch { while(true) {UpdateLessonStatus()} }
        }
    }

    //Show time string and current lesson
    Text(timeString.value)

    val les = currentLesson.value

    if(les != null) {
        Text("Кабинет: ${les.cabinet} / Время: ${les.startTime}-${getLessonEndString(les)} / Предмет: ${les.subject}")
    }
    else
        Text("SHOW CABINETS HERE")

}

fun calendarDayToDayIndex(day: Int) =
    when(day) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> -1
    }

fun dayIndexToCalendarDay(dayIndex: Int) =
    when(dayIndex) {
        0 -> Calendar.MONDAY
        1 -> Calendar.TUESDAY
        2 -> Calendar.WEDNESDAY
        3 -> Calendar.THURSDAY
        4 -> Calendar.FRIDAY
        5 -> Calendar.SATURDAY
        6 -> Calendar.SUNDAY
        else -> Calendar.MONDAY
    }

fun getCurrentLesson(schedule: Array<Array<Lesson?>?>, today: GregorianCalendar): Lesson? {
    val currentTime = GregorianCalendar(0, 0, 0, today.get(Calendar.HOUR_OF_DAY), today.get(Calendar.MINUTE))
    val dayOfWeek = calendarDayToDayIndex(today.get(Calendar.DAY_OF_WEEK))
    val lessons = schedule.getOrNull(dayOfWeek) ?: return null

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