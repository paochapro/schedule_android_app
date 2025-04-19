package com.paochapro.test004

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.paochapro.test004.schedule.readSchedule
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
    var hasLoginFailedMsg = mutableStateOf("")
    var schedule: Schedule = Schedule(createEmptyWeek(), createEmptyWeek())

    private var havePermissionToUseAlarmManager: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val barColor = ContextCompat.getColor(this, R.color.statusBar)
        window.statusBarColor = barColor
        window.navigationBarColor = barColor

        schedule = readSchedule(this, SCHEDULE_FILE_NAME, shouldPrint = false)
        registerReceiver(LessonStatusUpdate(), IntentFilter(Intent.ACTION_TIME_TICK))
        updateWidgetsAndTimeString("App start")

        havePermissionToUseAlarmManager = isAllowedToUseAlarmManager()

        if(havePermissionToUseAlarmManager)
            println("Have permission to use alarm manager")
        else
            println("Dont have permission to use alarm manager")

        try {
            startWidgetUpdateCycle()
        }
        catch (ex: Exception) {
            println("Exception when trying to set an alarm manager: ${ex.message}")
        }

        setContent { Root(this) }
    }

    private fun isAllowedToUseAlarmManager() : Boolean {
        try {
            val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms())
                return true

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                return true
        }
        catch (ex: SecurityException) {
            println("SecurityException when scheduling exact alarm: ${ex.message}")
        }
        catch (ex: Exception) {
            println("Exception when scheduling exact alarm: ${ex.message}")
        }

        return false
    }

    private fun startWidgetUpdateCycle() {
        if(havePermissionToUseAlarmManager) {
            val intent = Intent(this, WidgetUpdateCycle::class.java)
            intent.action = ACTION_UPDATE_WIDGET_CYCLE
            this.sendBroadcast(intent)
        }
    }

    fun updateWidgetsAndTimeString(reasonMessage: String) {
        PaochaproWidget.updateAll(this, reasonMessage)

        val widgetText = generateWidgetString(schedule)

        //Updating global time string, which means it updates things like main screen
        timeString.value = widgetText
    }

    //Working with schedule
    fun onScheduleUpdate() {
        saveSchedule(this, SCHEDULE_FILE_NAME, schedule, false)
        updateWidgetsAndTimeString("Schedule update")
        startWidgetUpdateCycle()
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

const val ACTION_UPDATE_WIDGET_CYCLE = "updateWidgetCycle"
const val ACTION_CANCEL_PENDING_INTENT= "cancelPendingIntent"

class WidgetUpdateCycle : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action == ACTION_UPDATE_WIDGET_CYCLE)
            updateCycle(context)

        if(intent?.action == ACTION_CANCEL_PENDING_INTENT) {
            println("Tried to cancel pending intent in AlarmManager, but canceling isnt implemented. No canceling will happen")
//            val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            alarmManager.cancel(alarmManager.nextAlarmClock.showIntent)
        }
    }

    private fun updateCycle(context: Context?) {
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        PaochaproWidget.updateAll(context, "Widget update cycle")

        val intent = Intent(context, WidgetUpdateCycle::class.java)
        intent.action = ACTION_UPDATE_WIDGET_CYCLE
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        var secondsDiff = getCurrentLessonEndSeconds(
            readSchedule(
                context,
                SCHEDULE_FILE_NAME,
                shouldPrint = false
            )
        )

        if(secondsDiff > 0) {
            secondsDiff += 2 //Adding two seconds for safety

            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() - (System.currentTimeMillis() % 1000) + secondsDiff * 1000,
                pendingIntent
            )

            println("WidgetUpdateCycle called. Next call should be after $secondsDiff seconds")
        }
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

        context.updateWidgetsAndTimeString("Action time tick")
    }
}