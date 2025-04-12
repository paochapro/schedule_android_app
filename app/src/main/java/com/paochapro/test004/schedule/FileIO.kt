package com.paochapro.test004.schedule

import android.content.Context
import com.beust.klaxon.Klaxon
import com.paochapro.test004.DAY_COUNT
import com.paochapro.test004.LESSON_COUNT
import com.paochapro.test004.Day
import com.paochapro.test004.Lesson
import com.paochapro.test004.Schedule
import com.paochapro.test004.createEmptySchedule
import com.paochapro.test004.createEmptyWeek
import java.io.File

data class FileLesson(val lesson: Lesson?)
data class FileDay(val lessons: Array<FileLesson>, val lessonTimeMinutes: Int)
data class FileSchedule(val weekEven: Array<FileDay>, val weekUneven: Array<FileDay>)

fun readSchedule(
    context: Context,
    fileName: String,
    defaultValue: Schedule = createEmptySchedule(),
    shouldPrint: Boolean = true)
        : Schedule
{
    val result: Schedule = defaultValue

    //Read the file
    val file = File(context.filesDir, fileName)

    if(!file.exists()) {
        println("No file to read")
        return defaultValue
    }

    //Get FileSchedule
    val json = file.readText()
    val read: FileSchedule?

    try {
        read = Klaxon().parse<FileSchedule>(json) //Quite slow!

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

    fun readWeek(week: Array<FileDay>) : Array<Day> {
        val resultWeek = createEmptyWeek()

        for(d in 0 until DAY_COUNT) {
            val readFileDay = week.getOrNull(d)
                ?: throw Exception("Failed reading the schedule, file doesn't have enough days")

            val resultLessons: Array<Lesson?> = arrayOfNulls(LESSON_COUNT)

            for(l in 0 until LESSON_COUNT) {
                val readFileLesson = readFileDay.lessons.getOrNull(l)
                    ?: throw Exception("Failed reading the schedule, file doesn't have enough lessons")

                resultLessons[l] = readFileLesson.lesson
            }

            resultWeek[d] = Day(resultLessons, readFileDay.lessonTimeMinutes)
        }

        return resultWeek
    }

    val resultWeekEven: Array<Day> = readWeek(read.weekEven)
    val resultWeekUneven: Array<Day> = readWeek(read.weekUneven)

    return Schedule(weekEven = resultWeekEven, weekUneven = resultWeekUneven)
}

fun saveSchedule(
    context: Context,
    fileName: String,
    schedule: Schedule,
    shouldPrint: Boolean = true)
{
    val fileWeekEven = schedule.weekEven.map {
        FileDay( it.lessons.map { FileLesson(it) }.toTypedArray(), it.lessonTimeMinutes )
    }.toTypedArray()

    val fileWeekUneven = schedule.weekUneven.map {
        FileDay( it.lessons.map { FileLesson(it) }.toTypedArray(), it.lessonTimeMinutes )
    }.toTypedArray()

    val json = Klaxon().toJsonString(FileSchedule(weekEven = fileWeekEven, weekUneven = fileWeekUneven))
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