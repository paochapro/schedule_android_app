package com.paochapro.test004

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle

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