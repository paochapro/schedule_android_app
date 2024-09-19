package com.paochapro.test004

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beust.klaxon.Klaxon
import java.io.File

@Composable
fun ConfigureLesson(activity: MainActivity) {
    val expanded = remember { mutableStateOf(false) }
    val day = remember { mutableStateOf("Понедельник") }

    //Day
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("День: ")
        Column  {
            Button({expanded.value = true}) {
                Text(day.value)
            }
            DropdownMenu(expanded = expanded.value, onDismissRequest = {expanded.value = false}) {
                DropdownMenuItem(text = { Text("Понедельник") }, onClick = {
                    expanded.value = false
                    day.value = "Понедельник"
                })
                DropdownMenuItem(text = { Text("Вторник") }, onClick = {
                    expanded.value = false
                    day.value = "Вторник"
                })
                DropdownMenuItem(text = { Text("Среда") }, onClick = {
                    expanded.value = false
                    day.value  = "Среда"
                })
            }
        }
    }

    val top = listOf("", "Предмет", "Время", "Кабинет")

    val texts = remember {
        List(LESSON_COUNT)  {
            val result: SnapshotStateList<String>

            val lesson = activity.lessons.getOrNull(it)
            result = if(lesson != null)
                mutableStateListOf(lesson.subject, lesson.startTime, "${lesson.cabinet}")
            else
                mutableStateListOf("", "", "")

            result
        }
    }

    //TODO: Replace grid with lazyColumn/rows to get rid of scrolling
    //Lessons
    Column {
    LazyVerticalGrid (columns = GridCells.Fixed(4), modifier = Modifier.height(700.dp))
    {
        items(top) {
            Text(it)
        }

        //Huge performance drop!
        for (n in 0..<LESSON_COUNT) {
            val row = texts[n]
            item { Text("${n + 1}") }
            item { TextField(row[0], { row[0] = it }) }
            item { TextField(row[1], { row[1] = it }) }
            item { TextField(row[2], { row[2] = it }) }
        }
    } }

    //TODO: check array oob
    Button({
        val writeLessons = mutableListOf<Lesson>()

        //Saving
        for(n in 0..< LESSON_COUNT) {
            val row = texts.getOrElse(n) { listOf("","","") }

            if(row[0] == "" || row[1] == "" || row[2] == "")
                continue

            val lesson = Lesson(row[1], row[0], row[2].toInt())

            writeLessons.add(lesson)
            activity.lessons[n] = lesson
        }

        //Save to file
        val file = File(activity.filesDir, "test2.json")

        var success = true
        if(!file.exists())
            success = file.createNewFile()

        if(success) {
            val json = Klaxon().toJsonString(writeLessons.toTypedArray())
            file.writeText(json)
        }
        else
            println("Couldn't save the file. Failed to create the new one")

        println(file.readText())

    }) {
        Text("Save")
    }
}