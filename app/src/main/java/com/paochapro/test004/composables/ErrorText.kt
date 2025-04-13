package com.paochapro.test004.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

@Composable
fun ErrorText(text: String) {
    Text(text = text,
        color = Color(0xFFF33737),
        modifier = Modifier.padding(horizontal = 8.dp),
        fontSize = 2.em
    )
}