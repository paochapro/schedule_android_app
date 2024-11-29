package com.paochapro.test004.misc

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.paochapro.test004.Lesson
import com.paochapro.test004.MSG
import com.paochapro.test004.MainActivity
import com.paochapro.test004.getCurrentLesson
import com.paochapro.test004.getLessonEndString
import com.paochapro.test004.ui.theme.Test004Theme
import com.paochapro.test004.utilStringToCalendar
import java.io.File

val fullWidth = Modifier.fillMaxWidth()

fun saveFile(context: MainActivity, data: String) {
    val file = File(context.filesDir, "test.txt")

    fun write(file: File) {
        file.writeText(data)
        printFile(file)
    }

    if(file.exists()) {
        write(file)
        return
    }

    if(file.createNewFile()) {
        write(file)
    }
}

fun printFile(file: File) {
    val lines = file.readLines()
    for(line in lines)
        Log.d(MSG, line)
}

@Composable
fun StackedTextFields() {
    val fields = remember { mutableStateListOf<String>() }
    val getHandlerTextChange: (Int) -> (String) -> Unit = {index: Int ->
        { text: String ->
            fields[index] = text
            Log.d(MSG, text)
        }
    }

    Test004Theme {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = fullWidth) {
            TableLanguages();

            fields.forEachIndexed {index,field -> TextField(field, getHandlerTextChange(index), modifier = fullWidth) }

            Button({fields.add("")}) {
                Text("CLICK ME", color = Color.Black)
            }
        }
    }
}

@Composable
fun FileIoContent(activity: MainActivity) {
    val text = remember { mutableStateOf("") }
    val callSaveFile = {
        saveFile(activity, text.value)
    }

    val callPrintFile: () -> Unit = {
        Log.d(MSG, "TEST")
        val file = File(activity.filesDir, "test.txt")

        if(file.exists()) {
            Log.d(MSG, "PRINT FILE")
            printFile(file)
        }
        else
            Log.d(MSG, "FILE DOESNT EXISTS")
    }

    Log.d(MSG, "LAMBDA: ${text.value}")
    TextField(text.value, { newText: String -> text.value = newText })
    Button(callSaveFile) {
        Text("Save")
    }

    Button(callPrintFile) {
        Text("Print")
    }
}

@Composable fun ShowLesson() {
    val lessonString = remember { mutableStateOf("") }

    Button(onClick = ::onButtonPress) {
        Text("Show lesson")
    }

    Text(lessonString.value)
}

fun onButtonPress() {
//Deprecated
//    val lesson1 = Lesson("8:00", "Test", 106)
//    val lesson2 = Lesson("9:00", "Test", 215)
//    val lesson3 = Lesson("10:00", "Test",215)
//    val lesson4 = Lesson("11:00", "Test", 215)
//    val lessons = arrayOf<Lesson?>(lesson1, lesson2, lesson3, lesson4)
//
//    val currentTime = utilStringToCalendar("10:00")
//    val currentLesson = getCurrentLesson(lessons, currentTime)
//
//    if(currentLesson != null)
//        Log.d(MSG, "End: ${getLessonEndString(currentLesson)} ")
}