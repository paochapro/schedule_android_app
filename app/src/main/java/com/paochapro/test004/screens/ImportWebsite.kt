package com.paochapro.test004.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.paochapro.test004.LOGIN_FAIL_STATUS_EMPTY_SCHEDULE
import com.paochapro.test004.LOGIN_FAIL_STATUS_INCORRECT_LOGIN_OR_PASSWORD
import com.paochapro.test004.LOGIN_FAIL_STATUS_REQUEST_FAIL
import com.paochapro.test004.LOGIN_FAIL_STATUS_RESPONSE_FAIL
import com.paochapro.test004.LOGIN_STATUS_WAIT
import com.paochapro.test004.LOGIN_SUCCESS_STATUS
import com.paochapro.test004.MainActivity
import com.paochapro.test004.composables.ErrorText
import com.paochapro.test004.composables.TextFieldStylized
import com.paochapro.test004.readWebsiteAndStoreInSchedule

//Dont commit these values!
private const val LOGIN_OVERRIDE = ""
private const val PASSWORD_OVERRIDE = ""
private const val GRADE_OVERRIDE = ""

@Composable
fun ImportWebsiteScreen(activity: MainActivity) {
    val login = remember { mutableStateOf(LOGIN_OVERRIDE) }
    val password = remember { mutableStateOf(PASSWORD_OVERRIDE) }
    val grade = remember { mutableStateOf(GRADE_OVERRIDE) }

    @Composable
    fun Data(
        text: String,
        state: MutableState<String>, ) {
        Row(Modifier.padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(0.2f)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd
            )
            {
                Text(text, color = MaterialTheme.colorScheme.onSurface)
            }
            TextFieldStylized(state.value, onValueChanged = { state.value = it },
                modifier = Modifier
                    .weight(0.6f)
                    .padding(end = 14.dp))
        }
    }

    Column {
        Data("Логин: ", login)
        Data("Пароль: ", password)
        Data("Класс: ", grade)
    }

    //Wait
    if(activity.hasLoginFailedMsg.value == LOGIN_STATUS_WAIT) {
        MyText("Подождите...")
    }

    //Success
    if(activity.hasLoginFailedMsg.value == LOGIN_SUCCESS_STATUS) {
        SuccessText("Успех. Расписание было импортировано")
    }

    //Failure
    if(activity.hasLoginFailedMsg.value == LOGIN_FAIL_STATUS_INCORRECT_LOGIN_OR_PASSWORD) {
        ErrorText("Не удалось войти в систему")
        ErrorText("Проверьте ваш пароль и логин")
    }

    if( activity.hasLoginFailedMsg.value == LOGIN_FAIL_STATUS_REQUEST_FAIL ||
        activity.hasLoginFailedMsg.value == LOGIN_FAIL_STATUS_RESPONSE_FAIL) {
        ErrorText("Не удалось войти в систему")
        ErrorText("Что-то пошло не так при попытке подключиться к сайту")
        ErrorText("Попробуйте войти позже")
    }

    if(activity.hasLoginFailedMsg.value == LOGIN_FAIL_STATUS_EMPTY_SCHEDULE) {
        ErrorText("Импортирование не удалось")
        ErrorText("Расписание в дневнике пустое")
    }

    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)){
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
            readWebsiteAndStoreInSchedule(activity, login.value, password.value, grade.value)
        } ) {
            Text("Импортировать")
        }
    }

    MyText("Класс заполнятеся так: 9Б, 10А")
}

@Composable
fun SuccessText(text: String) {
    Text(text = text,
        color = Color(0xFF4CAF50),
        modifier = Modifier.padding(horizontal = 8.dp),
        fontSize = 3.em
    )
}

@Composable
fun MyText(text: String) {
    Text(text = text,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 8.dp),
        fontSize = 3.em
    )
}