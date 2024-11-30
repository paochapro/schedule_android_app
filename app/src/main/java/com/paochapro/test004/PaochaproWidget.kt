package com.paochapro.test004

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import java.io.File

/**
 * Implementation of App Widget functionality.
 */
class PaochaproWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    println("updateAppWidget was called")

    val widgetText = generateWidgetString(getScheduleFromFile(context))

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.paochapro_widget)
    views.setTextViewText(R.id.appwidget_text, widgetText ?: WIDGET_TEXT_LESSON_WASNT_FOUND)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}