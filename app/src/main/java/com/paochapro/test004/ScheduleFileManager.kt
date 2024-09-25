package com.paochapro.test004

import android.content.Context
import com.beust.klaxon.Klaxon
import com.paochapro.test004.MainActivity.FileDay
import com.paochapro.test004.MainActivity.FileLesson
import java.io.File

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