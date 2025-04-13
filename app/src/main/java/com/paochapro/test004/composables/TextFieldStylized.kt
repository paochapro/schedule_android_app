package com.paochapro.test004.composables

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TextFieldStylized(
    value: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onValueChanged: (String) -> Unit,
    isError: Boolean = false,
    enabled: Boolean = true
    ) {
    OutlinedTextField(value, onValueChanged,
        modifier = modifier,
        shape = CircleShape.copy(all = CornerSize(8.dp)),
        colors = TextFieldDefaults.colors().copy(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
        keyboardOptions = keyboardOptions,
        isError = isError,
        enabled = enabled)
}