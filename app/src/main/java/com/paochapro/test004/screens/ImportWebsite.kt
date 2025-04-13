package com.paochapro.test004.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.paochapro.test004.MainActivity
import com.paochapro.test004.readWebsiteAndStoreInSchedule

//Dont commit these values!
private const val LOGIN_OVERRIDE = ""
private const val PASSWORD_OVERRIDE = ""

@Composable
fun ImportWebsiteScreen(activity: MainActivity)
{
    val login = remember { mutableStateOf(LOGIN_OVERRIDE) }
    val password = remember { mutableStateOf(PASSWORD_OVERRIDE) }

    Row {
        Text("Логин:")
        TextField(login.value, onValueChange = { login.value = it })
    }

    Row {
        Text("Пароль:")
        TextField(password.value, onValueChange = { password.value = it })
    }

    if(activity.hasLoginFailed.value) {
        ErrorText("Не удалось войти в систему. Проверьте ваш пароль и логин")
    }

    Button(onClick = {
        readWebsiteAndStoreInSchedule(activity, login.value, password.value)
    } ) {
        Text("Импортировать")
    }
}