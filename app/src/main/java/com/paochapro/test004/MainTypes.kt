package com.paochapro.test004

import java.util.Calendar
import java.util.GregorianCalendar

const val LESSON_COUNT = 8
const val DAY_COUNT = 7
const val DEFAULT_LESSON_TIME_MINS = 40
const val WIDGET_TEXT_LESSON_WASNT_FOUND = "Следующий предмет не найден"

class Lesson(val startTime: String, val subject: String, val cabinet: Int)

class Day(val lessons: Array<Lesson?>, var lessonTimeMinutes: Int = DEFAULT_LESSON_TIME_MINS)

class Schedule(val weekEven: Array<Day>, val weekUneven: Array<Day>) {
    private fun getCurrentWeek(time: GregorianCalendar) : Array<Day> {
        return if(time.get(Calendar.WEEK_OF_YEAR) % 2 == 0) weekEven else weekUneven
    }

    fun getCurrentDay(time: GregorianCalendar) : Day? {
        val dayOfWeek = calendarDayToDayIndex(time.get(Calendar.DAY_OF_WEEK))
        return getCurrentWeek(time).getOrNull(dayOfWeek)
    }
}