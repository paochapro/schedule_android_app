package com.paochapro.test004.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle

enum class DayName(val rusTranslation: String) {
    Monday("Понедельник"),
    Tuesday("Вторник"),
    Wednesday("Среда"),
    Thursday("Четверг"),
    Friday("Пятница"),
    Saturday("Суббота"),
    Sunday("Воскресенье");

    companion object {
        fun fromInt(int: Int) {
            when(int) {
                0 -> Monday
                1 -> Tuesday
                2 -> Wednesday
                3 -> Thursday
                4 -> Friday
                5 -> Saturday
                6 -> Sunday
                else -> {
                    println("Integer $int isnt matching any day. Returning DayName.Monday")
                    Monday
                }
            }
        }
    }
}

@Composable
fun DayPicker(value: DayName, onDayPick: (DayName) -> Unit)
{
    val expanded = remember { mutableStateOf(false) }
    Column {
        Button({ expanded.value = true }) {
            Text(value.rusTranslation, style = TextStyle.Default)
        }
        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            for (day in DayName.entries) {
                DropdownMenuItem(text = { Text(day.rusTranslation) }, onClick = {
                    expanded.value = false
                    onDayPick(day)
                })
            }
        }
    }
}