package com.paochapro.test004

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import kotlin.math.floor

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


    val rowDataArray = remember {
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


    val top = arrayOf("", "Предмет", "Время", "Кабинет")
    val weights = arrayOf(0.2f, 1.0f, 0.5f, 0.4f)

//    @Composable
//    fun Boxed(weight: Modifier, content: @Composable () -> Unit) {
//        Box(modifier = weight.fillMaxWidth()) {
//            content()
//        }
//    }
//
//    Column {
//        //Column names
//        Row {
//            Boxed(Modifier.weight(weights[0])) {}
//            Boxed(Modifier.weight(weights[1])) { Text("Предмет") }
//            Boxed(Modifier.weight(weights[2])) { Text("Время") }
//            Boxed(Modifier.weight(weights[3])) { Text("Кабинет") }
//        }
//
//        for(y in 0 until LESSON_COUNT) {
//            Row {
//                //Lesson name
//                Boxed(Modifier.weight(weights[0])) {
//                    Text("${y+1}")
//                }
//
//
//                //TextFields
//                for(x in 0..2) {
//                    Box(modifier = Modifier.fillMaxWidth().weight(weights[x+1])) {
//                        val rowData = rowDataArray[y]
//                        TextField(rowData[x], {rowData[x]=it})
//                    }
//                }
//            }
//        }
//    }

    @Composable
    fun SetupItem(row: Int, column: Int) {
        //Column names
        if(row == 0) {
            Text(top[column])
            return
        }

        //Lesson number
        if(column == 0) {
            Text("$row")
            return
        }

        //TextFields
        val rowData = rowDataArray[row-1]
        TextField(rowData[column-1], {rowData[column-1]=it})
    }

    NonlazyGrid(
        columns = 4,
        itemCount = 9*4,
        columnWeights = weights
    ) {
        val column = it % 4
        val row = floor(it.toFloat() / 4f).toInt()
        SetupItem(row, column)
    }

    //TODO: check array oob
    Button({
        val writeLessons = mutableListOf<Lesson>()

        //Saving
        for(n in 0..< LESSON_COUNT) {
            val row = rowDataArray.getOrElse(n) { listOf("","","") }

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