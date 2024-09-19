package com.paochapro.test004.misc

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TableLanguages() {
    val c_langs = arrayOf("C#", "C++", "C")
    val java_langs = arrayOf("Java", "Kotlin", "Javascript")
    val lang_groups = arrayOf(c_langs, java_langs)

    Column(
        modifier = Modifier.background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (group in lang_groups) {
            Row {
                val border = BorderStroke(1.dp, Color.Red)
                for (lang in group) {
                    Text(
                        lang,
                        modifier = Modifier
                            .border(border)
                            .width(width = 100.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}