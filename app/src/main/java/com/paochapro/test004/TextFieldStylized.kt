package com.paochapro.test004

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TextFieldStylized(
    value: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onValueChanged: (String) -> Unit,
    isError: Boolean = false
    ) {
    OutlinedTextField(value, onValueChanged,
        modifier = modifier,
        shape = CircleShape.copy(all = CornerSize(8.dp)),
        colors = TextFieldDefaults.colors().copy(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
        keyboardOptions = keyboardOptions,
        isError = isError)
}