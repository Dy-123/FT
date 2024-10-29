package com.example.focustrack.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import com.example.focustrack.model.dataStats
import com.example.focustrack.database.SQLiteHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object StoreStats {

    private lateinit var sqliteHelper: SQLiteHelper

    fun init(context: Context) {
        sqliteHelper = SQLiteHelper(context)
        sqliteHelper.open()
    }

    fun closeDatabase(){
        sqliteHelper.closeDB()
    }

    fun saveStats() {
        val stats = dataStats.toStats()
        CoroutineScope(Dispatchers.IO).launch {
            // Check if the record exists
            val existingStats = sqliteHelper.getStatsByStartTime(stats.startTime)

            if (existingStats == null) {
                // Insert the new stats
                sqliteHelper.insertStats(stats)
                println("log: stats inserted")
            } else {
                // Update the existing stats
                sqliteHelper.updateStats(stats)
                println("log: stats updated")
            }
        }
    }

    fun getCompletedCountForToday(): String {
        return sqliteHelper.getCompletedCountForToday().toString()
    }

    fun printDatabaseTable() {
        val allStats = sqliteHelper.getAllStats()
        for (stat in allStats) {
            println("Stats: startTime=${stat.startTime}, todayCount=${stat.todayCount}, distraction=${stat.distraction}, completed=${stat.completed}, sessionCompletionStatus=${stat.sessionCompletionStatus}")
            // Optionally print distractionTimeList and completedTimeList
            println("Distraction Time List: ${stat.distractionTimeList}")
            println("Completed Time List: ${stat.completedTimeList}")
        }
    }

    fun getTotalForToday(): String{
        return sqliteHelper.getTotalCountForToday().toString()
    }


    // Export all data to JSON and database file
    fun exportAllData(context: Context): Pair<Uri?, Uri?> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Code for SDK 29 (Android 10) and above
            val jsonFilePath = sqliteHelper.exportAllStatsToJsonFileFromAPI29(context)
            val dbFilePath = sqliteHelper.exportDatabaseToFileFromAPI29(context)
            return Pair(jsonFilePath, dbFilePath)
        } else {
            // If required implement code for below SDK 29
            // If want below android 10 then change the min sdk version to lower like 24 and add export functionality
            // also modify android:requestLegacyExternalStorage="false" in manifest file
            return Pair(null, null)
        }
    }


    fun resetStats() {
        sqliteHelper.clearStats()
    }
}
