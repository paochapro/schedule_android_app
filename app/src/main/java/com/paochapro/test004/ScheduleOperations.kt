package com.paochapro.test004

import android.content.Context
import com.beust.klaxon.Klaxon
import java.io.File
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Random

data class FileLesson(val lesson: Lesson?)
data class FileDay(val lessons: Array<FileLesson>, val lessonTimeMinutes: Int)

//File I/O
fun readSchedule(
    context: Context,
    fileName: String,
    defaultValue: Array<Day>,
    shouldPrint: Boolean = true)
: Array<Day>
{
    val result: Array<Day> = defaultValue

    //Read the file
    val file = File(context.filesDir, fileName)

    if(!file.exists()) {
        println("No file to read")
        return defaultValue
    }

    //Get FileDay array
    val json = file.readText()
    val read: List<FileDay>?

    try {
        read = Klaxon().parseArray<FileDay>(json) //Quite slow!

        if(shouldPrint)
            println("Read: $json")
    }
    catch(ex: Exception) {
        println("Failed reading the schedule. Exception: " + ex.message)
        return defaultValue
    }

    if(read == null) {
        println("Failed reading the schedule, it is probably empty")
        return defaultValue
    }

    for(d in 0 until DAY_COUNT) {
        val readFileDay = read.getOrNull(d)

        if(readFileDay == null) {
            println("Failed reading the schedule, file doesn't have enough days")
            return defaultValue
        }

        val resultLessons: Array<Lesson?> = arrayOfNulls(LESSON_COUNT)

        for(l in 0 until LESSON_COUNT) {
            val readFileLesson = readFileDay.lessons.getOrNull(l)

            if(readFileLesson == null) {
                println("Failed reading the schedule, file doesn't have enough lessons")
                return defaultValue
            }

            resultLessons[l] = readFileLesson.lesson
        }

        result[d] = Day(resultLessons, readFileDay.lessonTimeMinutes)
    }

    return result
}

fun saveSchedule(
    context: Context,
    fileName: String,
    schedule: Array<Day>,
    shouldPrint: Boolean = true)
{
    //val nonNullableSchedule = schedule.filterNotNull().map { it.filterNotNull() }.map { FileDay(it) }

    val fileSchedule = schedule.map {
        FileDay( it.lessons.map { FileLesson(it) }.toTypedArray(), it.lessonTimeMinutes )
    }

    val json = Klaxon().toJsonString(fileSchedule)
    val file = File(context.filesDir, fileName)

    if(!file.exists()) {
        if (!file.createNewFile()) {
            println("Failed to create the schedule file when trying to save")
            return
        }
    }

    file.writeText(json)

    if(shouldPrint)
        println("Saved: $json")
}

//Helper functions
fun getEmptySchedule() = Array(DAY_COUNT) { Day(arrayOfNulls(LESSON_COUNT), DEFAULT_LESSON_TIME_MINS) }

fun createTemplateSchedule(addEighthLesson: Boolean, addSunday: Boolean) : Array<Day> {
    val subjects = arrayOf("Рус", "Инф", "Алгб", "Физ", "Био")

    val schedule = getEmptySchedule()

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
        schedule[dayIndex] = getRandDay(dayIndex)
    }

    if(addSunday)
        schedule[6] = getRandDay(6)

    return schedule
}

fun printSchedule(schedule: Array<Day>) {
    println("[Schedule]")
    schedule.forEachIndexed { di, d ->
        println("-- Day $di [Minutes:${d.lessonTimeMinutes}] --")

        d.lessons.forEachIndexed { li, l ->
            if (l != null)
                println("Lesson $li: ${l.subject} / ${l.startTime} / ${l.cabinet} ")
            else
                println("Lesson NULL")
        }
    }
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
fun generateWidgetString(context: Context, defaultString: String) : String {
    val schedule = readSchedule(context, SCHEDULE_FILE_NAME, getEmptySchedule(), shouldPrint = false)

    var currentLesson: Lesson? = null
    var lessonLength: Int = 1

    //Getting current lesson and its length
    val time = GregorianCalendar()
    val dayOfWeek = calendarDayToDayIndex(time.get(Calendar.DAY_OF_WEEK))
    val day = schedule.getOrNull(dayOfWeek)

    if(day != null) {
        currentLesson = getCurrentLesson(day, time)
        lessonLength = day.lessonTimeMinutes
    }
    else {
        println("Failed to get current day")
    }

    if(currentLesson != null) {
        val end = getLessonEndString(currentLesson, lessonLength)
        return "${currentLesson.startTime}-${end} ${currentLesson.subject} ${currentLesson.cabinet}"
    }

    return defaultString
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

fun getLessonEndString(lesson: Lesson, lessonTimeMinutes: Int) = utilCalendarToString(getLessonEndCalendar(lesson, lessonTimeMinutes))

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