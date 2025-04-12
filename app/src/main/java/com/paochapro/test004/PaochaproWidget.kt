package com.paochapro.test004

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.paochapro.test004.schedule.readSchedule

/**
 * Implementation of App Widget functionality.
 */
class PaochaproWidget : AppWidgetProvider() {
    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager
                .getAppWidgetIds(ComponentName(context, PaochaproWidget::class.java))

            for(id in ids) {
                updateWidgetContents(context, manager, id)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent != null && context != null)
            if (intent.action == null) {
                println("Update all")
                updateAll(context)
            }

        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAll(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        if(context == null || appWidgetManager == null || newOptions == null) {
            println("No context or appWidgetManager or newOptions was found when updating widget size (or options)")
            return
        }

        val height = newOptions.getInt(OPTION_APPWIDGET_MIN_HEIGHT)
        val width = newOptions.getInt(OPTION_APPWIDGET_MIN_WIDTH)
        val layout = getWidgetLayout(width, height)

        updateWidgetContents(context, appWidgetManager, appWidgetId)
    }
}


internal fun updateWidgetContents(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    //println("Update widget contents")

    val schedule = readSchedule(context, SCHEDULE_FILE_NAME, shouldPrint = false)
    val currentLesson = getCurrentLessonFromSchedule(schedule)

    val timeStr = if(currentLesson != null) "${currentLesson.startTime}-${getLessonEnd(schedule, currentLesson)}" else ""

    // Construct the RemoteViews object
    val info = appWidgetManager.getAppWidgetOptions(appWidgetId)
    val layout = getWidgetLayout(info.getInt(OPTION_APPWIDGET_MIN_WIDTH), info.getInt(OPTION_APPWIDGET_MIN_HEIGHT))

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, PaochaproWidget::class.java),
        PendingIntent.FLAG_IMMUTABLE)

    val views = RemoteViews(context.packageName, layout).apply {
        setOnClickPendingIntent(R.id.main_text, pendingIntent)
    }

    val normalWidgetText = if(currentLesson != null)
        if(currentLesson.cabinet != -1)
            "${currentLesson.subject} ${currentLesson.cabinet} $timeStr"
        else
            "${currentLesson.subject} $timeStr"
    else
        WIDGET_TEXT_LESSON_WASNT_FOUND

    //Updating according to layout
    when(layout) {
        R.layout.paochapro_widget -> views.setTextViewText(R.id.main_text, normalWidgetText)
        R.layout.paochapro_widget_2x1 -> {
            val subjectAndCabinet = if(currentLesson != null)
                "${currentLesson.subject} ${currentLesson.cabinet}"
            else
                WIDGET_TEXT_LESSON_WASNT_FOUND

            views.setTextViewText(R.id.subject_cabinet, subjectAndCabinet)
            views.setTextViewText(R.id.time, timeStr)
        }
        R.layout.paochapro_widget_1x1 -> {
            var subject = WIDGET_TEXT_LESSON_WASNT_FOUND
            var time_start = ""
            var time_end = ""
            var cabinet = ""

            if(currentLesson != null) {
                subject = currentLesson.subject
                time_start = timeStr.split('-')[0]
                time_end = timeStr.split('-')[1]
                //'-' means no cabinet
                cabinet = if(currentLesson.cabinet != -1) "${currentLesson.cabinet}" else "-"
            }

            views.setTextViewText(R.id.subject_1x1, subject)
            views.setTextViewText(R.id.cabinet_1x1, cabinet)
            views.setTextViewText(R.id.time_start_1x1, time_start)
            views.setTextViewText(R.id.time_end_1x1, time_end)
        }
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

internal fun getWidgetLayout(width: Int, height: Int) : Int {
    var layout = R.layout.paochapro_widget

    //I decided to not use 2x1, simple text wrap looks nicer
//    if(width < (254 - 74 / 2) && height < (194 - 89/2) ) {
//        layout = R.layout.paochapro_widget_2x1
//    }

    if(width < (164 - 74 / 2)) {
        layout = R.layout.paochapro_widget_1x1
    }

    return layout
}

