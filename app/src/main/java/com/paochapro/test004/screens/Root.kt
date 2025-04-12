package com.paochapro.test004.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.em
import com.paochapro.test004.MainActivity
import com.paochapro.test004.R
import com.paochapro.test004.ui.theme.Test004Theme

enum class Screen {
    MainScreen,
    ConfigureLesson,
    DevScreen,
    ImportWebsiteScreen
}

@Composable
fun Root(activity: MainActivity) {
    val screen = remember { mutableStateOf(Screen.MainScreen) }

    Test004Theme {
        Column(modifier = Modifier.fillMaxSize()) {
            when(screen.value) {
                Screen.MainScreen -> {
                    MainScreen(activity, Modifier
                        .fillMaxWidth()
                        .weight(1f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        Button(modifier = Modifier.fillMaxWidth(), onClick = { screen.value =
                            Screen.ConfigureLesson
                        }) { Text("Изменить расписание") }
                        Button(modifier = Modifier.fillMaxWidth(), onClick = { screen.value =
                            Screen.DevScreen
                        }) { Text("Тестирование") }
                        Button(modifier = Modifier.fillMaxWidth(), onClick = { screen.value =
                            Screen.ImportWebsiteScreen
                        }) { Text("Импорт") }
                    }
                }
                Screen.ConfigureLesson -> {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.hsl(0f, 0f, 0f, 0.2f))) {
                        Button({screen.value = Screen.MainScreen }) {
                            Text("Назад")
                        }
                    }

                    Column(modifier = Modifier.verticalScroll(ScrollState(0))) {
                        ConfigureLesson(activity)
                    }
                }
                Screen.DevScreen -> {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.hsl(0f, 0f, 0f, 0.2f))) {
                        Button({ screen.value = Screen.MainScreen }) {
                            Text("Назад")
                        }
                    }
                    DevScreen(activity)
                }
                Screen.ImportWebsiteScreen -> {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.hsl(0f, 0f, 0f, 0.2f))) {
                        Button({ screen.value = Screen.MainScreen; activity.hasLoginFailed.value = false }) {
                            Text("Назад")
                        }
                    }
                    ImportWebsiteScreen(activity)
                }
            }
        }
    }
}

@Composable
fun MainScreen(activity: MainActivity, modifier: Modifier) {
    //Creating strings that will show up in the center
    val generatedString = activity.timeString.value
    val centerTexts = mutableListOf("Нет урока")

    if(generatedString != null) {
        val lessonStrings = generatedString.split(' ')
        centerTexts.clear()

        if(lessonStrings.size == 3) {
            val lesson = lessonStrings[0]
            val cabinet = lessonStrings[1]
            val time = lessonStrings[2]

            centerTexts.add("$lesson $cabinet")
            centerTexts.add(time)
        }

        if(lessonStrings.size == 2) {
            val lesson = lessonStrings[0]
            val time = lessonStrings[1]

            centerTexts.add(lesson)
            centerTexts.add(time)
        }
    }

    val fontFamily = FontFamily(Font(R.font.jetbrains_mono))

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            for(i in centerTexts) {
                Text(
                    i,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = fontFamily,
                    fontSize = 5.em
                )
            }
        }
    }
}