package com.sleeplogger.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sleeplogger.app.database.SleepDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SleepWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, SleepWidget::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int) {
        
        val views = RemoteViews(context.packageName, R.layout.sleep_widget_layout)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SleepDatabase.getDatabase(context)
                val entries = db.sleepDao().getLatestEntrySync()
                
                withContext(Dispatchers.Main) {
                    if (entries != null) {
                        views.setTextViewText(R.id.widget_sleep_duration, entries.totalSleep)
                        views.setTextViewText(R.id.widget_sleep_date, entries.date)
                    } else {
                        views.setTextViewText(R.id.widget_sleep_duration, "--h --m")
                        views.setTextViewText(R.id.widget_sleep_date, "No data")
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                // Silently handle widget update failures
            }
        }
    }
}
