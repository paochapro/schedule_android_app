package com.paochapro.test004

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import com.paochapro.test004.schedule.saveSchedule
import com.paochapro.test004.screens.Root

//TODO: Show lesson schedule for the whole day before first lesson starts (8:00)
//Like this:
//1. Русский    5. Физика
//2. Русский    6. Биология
//3. Математика 7. География
//4. Математика 8. Химия

//TODO: Scrollable text field with subjects

//TODO: Optimize requests and retrieving html from them

//TODO: Be careful with credentials

const val SCHEDULE_FILE_NAME = "test2.json"

class MainActivity : ComponentActivity() {
    var timeString = mutableStateOf<String?>(null)
    var schedule: Schedule = Schedule(createEmptyWeek(), createEmptyWeek())

    var hasLoginFailed = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //schedule = readSchedule(this, SCHEDULE_FILE_NAME)

        //registerReceiver(LessonStatusUpdate(), IntentFilter(Intent.ACTION_TIME_TICK))
        //updateWidgetsAndTimeString()

        try {
            setAlarmManager()
        }
        catch (ex: Exception) {
            println(ex.message)
        }

        setContent { Root(this) }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setAlarmManager() {
        val intent = Intent(this, TestReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val currentLesson = getCurrentLessonFromSchedule(schedule)
        val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if(alarmManager.canScheduleExactAlarms()) {
                println("Have permission")

                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 20000, //20 seconds from now
                    pendingIntent
                )
            }
            else {
                println("Dont have permission")
            }
        }
        catch (ex: SecurityException) {
            println("SecurityException when scheduling exact alarm: ${ex.message}")
        }
        catch (ex: Exception) {
            println("Exception when scheduling exact alarm: ${ex.message}")
        }
    }

    fun updateWidgetsAndTimeString() {
        PaochaproWidget.updateAll(this)

        val widgetText = generateWidgetString(schedule)

        //Updating global time string, which means it updates things like main screen
        timeString.value = widgetText
    }

    //Working with schedule
    fun onScheduleUpdate() {
        saveSchedule(this, SCHEDULE_FILE_NAME, schedule, false)
        updateWidgetsAndTimeString()
        printSchedule(schedule)
    }

    //Time settings
    fun copyTime(week: Array<Day>, fromDayIndex: Int, toDayIndex: Int) {
        val fromLessons = week[fromDayIndex].lessons

        // Copy to even and uneven weeks
        for(toWeek in arrayOf(schedule.weekEven, schedule.weekUneven))
            for(i in fromLessons.indices) {
                val toDay = toWeek[toDayIndex]
                val toLessons = toDay.lessons

                val toLesson = toLessons[i]
                val fromLesson = fromLessons[i]

                //Assign a modified lesson that keeps subject and cabinet but copies startTime
                if(toLesson != null && fromLesson != null) {
                    val modifiedLesson = Lesson(
                        subject = toLesson.subject,
                        startTime = fromLesson.startTime,
                        cabinet = toLesson.cabinet)

                    toLessons[i] = modifiedLesson
                }
            }
    }
}

class TestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        println("Test receiver")
    }
}

class LessonStatusUpdate : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action != Intent.ACTION_TIME_TICK) {
            return
        }

        //println("Minute changed: ${Calendar.getInstance().get(Calendar.MINUTE)}")

        if(context == null || context !is MainActivity) {
            println("No context was found in TimeReceiver, or it is not a MainActivity")
            return
        }

        context.updateWidgetsAndTimeString()
    }
}