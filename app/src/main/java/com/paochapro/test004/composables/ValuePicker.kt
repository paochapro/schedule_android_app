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

@Composable
fun ValuePicker(values: Array<String>, currentValue: String, onValuePick: (String) -> Unit)
{
    val expanded = remember { mutableStateOf(false) }
    Column {
        Button({ expanded.value = true }) {
            Text(currentValue, style = TextStyle.Default)
        }
        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            for (value in values) {
                DropdownMenuItem(text = { Text(value) }, onClick = {
                    expanded.value = false
                    onValuePick(value)
                })
            }
        }
    }
}