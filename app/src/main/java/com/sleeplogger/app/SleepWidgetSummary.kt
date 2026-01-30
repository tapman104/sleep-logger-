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
import java.util.Calendar

class SleepWidgetSummary : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.sleep_widget_summary_layout)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SleepDatabase.getDatabase(context)
                val entries = db.sleepDao().getLatestEntrySync() // Re-using existing DAO method for simplicity
                
                // Fetch all for weekly avg
                val allEntriesFlow = db.sleepDao().getAllEntries()
                // Room Flow doesn't work well in Widget Sync, we'd need a non-flow query for production
                // But for now let's assume we show streak or latest
                
                withContext(Dispatchers.Main) {
                    if (entries != null) {
                        views.setTextViewText(R.id.widget_summary_duration, entries.totalSleep)
                        views.setTextViewText(R.id.widget_summary_date, "Last: ${entries.date}")
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {}
        }
    }
}
