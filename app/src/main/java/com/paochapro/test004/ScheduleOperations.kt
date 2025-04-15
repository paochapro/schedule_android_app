package com.paochapro.test004

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Random

//Helper functions
fun createEmptyWeek() = Array(DAY_COUNT) { Day(arrayOfNulls(LESSON_COUNT), DEFAULT_LESSON_TIME_MINS) }

fun createRandomWeek(addEighthLesson: Boolean, addSunday: Boolean, addSaturday: Boolean = false) : Array<Day> {
    val subjects = arrayOf("Рус", "Инф", "Алгб", "Физ", "Био")

    val week = createEmptyWeek()

    fun getRandLesson(lessonIndex: Int) : Lesson {
        val subjectIndex = Random().nextInt(subjects.size)
        val cab = (Random().nextInt(3) + 1) * 100 + (Random().nextInt(10) + 1 + 10)
        return Lesson("${lessonIndex+8}:00", subjects[subjectIndex], cab)
    }

    fun getRandDay(dayIndex: Int): Day {
        val randOffset = Random().nextInt(10)
        val lessonTime = 40 + randOffset
        val day = Day(arrayOfNulls(LESSON_COUNT), lessonTime)

        for(l in 0 until LESSON_COUNT - 2) {
            day.lessons[l] = getRandLesson(l)
        }

        if(addEighthLesson) {
            day.lessons[7] = getRandLesson(7)
        }

        return day
    }

    for(dayIndex in 0 until DAY_COUNT - 2) {
        week[dayIndex] = getRandDay(dayIndex)
    }

    if(addSunday)
        week[6] = getRandDay(6)

    if(addSaturday)
        week[5] = getRandDay(5)

    return week
}

fun createEmptySchedule() = Schedule(createEmptyWeek(), createEmptyWeek())

fun createRandomSchedule(
    addEighthLesson: Boolean = false,
    addSunday: Boolean = false,
    addSaturday: Boolean = false) : Schedule {
    return Schedule(
        createRandomWeek(addEighthLesson, addSunday, addSaturday),
        createRandomWeek(addEighthLesson, addSunday, addSaturday)
    )
}

fun printSchedule(schedule: Schedule) {
    println("[Schedule]")
    printWeek(schedule.weekEven, "[WeekEven]")
    printWeek(schedule.weekUneven, "[WeekUneven]")
}

fun printWeek(week: Array<Day>, startMessage: String) {
    println(startMessage)
    week.forEachIndexed { di, d ->
        println("-- Day $di [Minutes:${d.lessonTimeMinutes}] --")

        d.lessons.forEachIndexed { li, l ->
            if (l != null)
                println("Lesson $li: ${l.subject} / ${l.startTime} / ${l.cabinet} ")
            else
                println("Lesson NULL")
        }
    }
}

fun getLessonString(lesson: Lesson?): String {
    return if(lesson != null) "Lesson: ${lesson.subject} / ${lesson.startTime} / ${lesson.cabinet}" else "null lesson"
}

//Working with lesson
/**Returns new string for widget, based on current time.
 *
 * If no lesson for current time was found, function returns [defaultString]
 *
 * Automatically reads schedule file, looks up current day and lesson, and returns a string with current lesson info that is formatted for a widge
 *
 * Format: time lesson cabinet
 *
 * Example: 8:00-8:45 Maths 204
 */
fun generateWidgetString(schedule: Schedule) : String? {
    var currentLesson: Lesson? = null
    var lessonLength: Int = 1

    //Getting current lesson and its length
    val time = GregorianCalendar()
    val day = schedule.getCurrentDay(time)

    if(day != null) {
        currentLesson = getCurrentLesson(day, time)
        lessonLength = day.lessonTimeMinutes
    }
    else {
        println("Failed to get current day")
    }

    if(currentLesson != null) {
        val end = getLessonEndString(currentLesson, lessonLength)

        return if(currentLesson.cabinet != -1)
            "${currentLesson.subject} ${currentLesson.cabinet} ${currentLesson.startTime}-${end}"
        else
            "${currentLesson.subject} ${currentLesson.startTime}-${end}"
    }

    return null
}

fun getCurrentLessonFromSchedule(schedule: Schedule) : Lesson? {
    var result: Lesson? = null
    var lessonLength: Int = 1

    //Getting current lesson and its length
    val time = GregorianCalendar()
    val day = schedule.getCurrentDay(time)

    if(day != null) {
        result = getCurrentLesson(day, time)
        lessonLength = day.lessonTimeMinutes
    }
    else {
        println("Failed to get current day")
    }

    return result
}

fun getLessonEnd(schedule: Schedule, lesson: Lesson) : String {
    var lessonLength: Int = 1

    //Getting current lesson and its length
    val time = GregorianCalendar()
    val day = schedule.getCurrentDay(time)

    if(day != null) {
        lessonLength = day.lessonTimeMinutes
    }
    else {
        println("Failed to get current day")
    }

    return getLessonEndString(lesson, lessonLength)
}


/**Returns current lesson (or null) from [day] based on what [time] is right now.*/
fun getCurrentLesson(day: Day, time: GregorianCalendar): Lesson? {
    val currentTime = GregorianCalendar(0, 0, 0, time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE))

    //Get min
    var min: Lesson? = null
    for(lesson in day.lessons) {
        if(lesson == null)
            continue

        val end = getLessonEndCalendar(lesson, day.lessonTimeMinutes)

        if(end.before(currentTime))
            continue

        if(min == null)
            min = lesson

        if(getLessonEndCalendar(min, day.lessonTimeMinutes) > end)
            min = lesson
    }

    return min
}

//Helper functions
fun getLessonEndCalendar(lesson: Lesson, lessonTimeMinutes: Int): GregorianCalendar {
    val calendar = utilStringToCalendar(lesson.startTime)
    calendar.add(Calendar.MINUTE , lessonTimeMinutes)
    return calendar
}

fun getLessonEndString(lesson: Lesson, lessonTimeMinutes: Int) = utilCalendarToString(
    getLessonEndCalendar(lesson, lessonTimeMinutes)
)

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