package com.paochapro.test004

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import kotlin.math.floor

//TODO: Sometimes saving to schedule doesn't work!

fun getTextFieldDataArray(dayIndex: Int, activity: MainActivity): List<SnapshotStateList<String>> {
    val day = activity.schedule.getOrNull(dayIndex)

    if(day == null) {
        return List(LESSON_COUNT) { mutableStateListOf("", "", "") }
    }
    else {
        return List(LESSON_COUNT) {
            val lesson = day.getOrNull(it)
            if(lesson != null)
                mutableStateListOf(lesson.subject, lesson.startTime, "${lesson.cabinet}")
            else
                mutableStateListOf("", "", "")
        }
    }
}

@Composable
fun ConfigureLesson(activity: MainActivity) {
    val configureDay = remember { mutableStateOf(Day.Monday) }
    val configureDayIndex = configureDay.value.ordinal

    //Day
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("День: ")
        DayPicker(configureDay.value) { configureDay.value = it }
    }

    val rowDataArray = getTextFieldDataArray(configureDayIndex, activity)

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
        if (row == 0) {
            Text(top[column])
            return
        }

        //Lesson number
        if (column == 0) {
            Text("$row")
            return
        }

        //TextFields
        val rowData = rowDataArray[row - 1]
        TextField(rowData[column - 1], { rowData[column - 1] = it })
    }

    NonlazyGrid(
        columns = 4,
        itemCount = 9 * 4,
        columnWeights = weights
    ) {
        val column = it % 4
        val row = floor(it.toFloat() / 4f).toInt()
        SetupItem(row, column)
    }

    //TODO: check array oob
    fun getButtonSave(): () -> Unit = {
        val day = activity.schedule[configureDayIndex]

        if(day == null)
            activity.schedule[configureDayIndex] = arrayOfNulls<Lesson?>(LESSON_COUNT)

        if(day != null){
            //Saving
            for(n in 0..< LESSON_COUNT) {
                val row = rowDataArray.getOrElse(n) { listOf("","","") }

                if(row[0] == "" || row[1] == "" || row[2] == "") {
                    day[n] = null
                    continue
                }

                day[n] = Lesson(row[1], row[0], row[2].toInt())
            }
        }

        activity.saveSchedule()
    }

    Button(onClick = getButtonSave()) {
        Text("Save")
    }
}