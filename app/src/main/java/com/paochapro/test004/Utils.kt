package com.paochapro.test004

import android.util.Log
import java.util.GregorianCalendar
import java.util.Calendar

fun utilStringToCalendar(string: String): GregorianCalendar
{
    val data = string.split(":")

    if(data.size != 2) {
        Log.e("MY_MESSAGE","Incorrect time string format!")
        return GregorianCalendar()
    }

    val hour = data[0].toInt()
    val minute = data[1].toInt()

    return GregorianCalendar(0,0,0, hour, minute)
}

fun utilCalendarToString(calendar: GregorianCalendar): String
{
    val minutes = calendar.get(Calendar.MINUTE)
    val minutesString: String = if(minutes < 10) "0$minutes" else "$minutes"
    return "${calendar.get(Calendar.HOUR_OF_DAY)}:$minutesString"
}