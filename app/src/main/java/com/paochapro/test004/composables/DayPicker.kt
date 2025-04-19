package com.paochapro.test004.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color

enum class DayName(val rusTranslation: String) {
    Monday("Понедельник"),
    Tuesday("Вторник"),
    Wednesday("Среда"),
    Thursday("Четверг"),
    Friday("Пятница"),
    Saturday("Суббота"),
    Sunday("Воскресенье");

    companion object {
        fun fromInt(int: Int) : DayName {
            return when(int) {
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
    Row {
        var next = value.ordinal + 1
        var previous = value.ordinal - 1

        if(value == DayName.Sunday) next = DayName.Monday.ordinal
        if(value == DayName.Monday) previous = DayName.Sunday.ordinal

        IconButton(
            onClick = {onDayPick(DayName.fromInt(previous))},
            ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "",
                tint = Color.White,
            )
        }

        Column {
            Button({ expanded.value = true }) {
                Box(contentAlignment = Alignment.Center) {
                    Text(value.rusTranslation)
                    Text("Понедельник", modifier = Modifier.alpha(0f))
                }
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

        IconButton(
            onClick = {onDayPick(DayName.fromInt(next))}) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "",
                tint = Color.White
            )
        }
    }
}