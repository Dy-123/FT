package com.example.focustrack.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.focustrack.model.Stats
import com.google.gson.Gson
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStreamWriter

class SQLiteHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "stats_database.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_STATS = "stats"
        private const val COLUMN_START_TIME = "startTime"
        private const val COLUMN_TODAY_COUNT = "todayCount"
        private const val COLUMN_DISTRACTION = "distraction"
        private const val COLUMN_DISTRACTION_TIME_LIST = "distractionTimeList"
        private const val COLUMN_COMPLETED = "completed"
        private const val COLUMN_COMPLETED_TIME_LIST = "completedTimeList"
        private const val COLUMN_SESSION_COMPLETION_STATUS = "sessionCompletionStatus"
    }

    private var db: SQLiteDatabase? = null

    fun open() {
        if (db == null || !db!!.isOpen) {
            db = writableDatabase
        }
    }

    fun closeDB() {
        db?.close()
        db = null
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_STATS (" +
                "$COLUMN_START_TIME INTEGER PRIMARY KEY, " +
                "$COLUMN_TODAY_COUNT INTEGER, " +
                "$COLUMN_DISTRACTION INTEGER, " +
                "$COLUMN_DISTRACTION_TIME_LIST TEXT, " +
                "$COLUMN_COMPLETED INTEGER, " +
                "$COLUMN_COMPLETED_TIME_LIST TEXT, " +
                "$COLUMN_SESSION_COMPLETION_STATUS INTEGER)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Empty onUpgrade for now
    }

    // Method to insert a new Stats record
    fun insertStats(stats: Stats) {
        val values = ContentValues().apply {
            put(COLUMN_START_TIME, stats.startTime)
            put(COLUMN_TODAY_COUNT, stats.todayCount)
            put(COLUMN_DISTRACTION, stats.distraction)
            put(COLUMN_DISTRACTION_TIME_LIST, Gson().toJson(stats.distractionTimeList))
            put(COLUMN_COMPLETED, stats.completed)
            put(COLUMN_COMPLETED_TIME_LIST, Gson().toJson(stats.completedTimeList))
            put(COLUMN_SESSION_COMPLETION_STATUS, if (stats.sessionCompletionStatus) 1 else 0)
        }
        db?.insert(TABLE_STATS, null, values)
    }

    // Method to update an existing Stats record
    fun updateStats(stats: Stats) {
        val values = ContentValues().apply {
            put(COLUMN_TODAY_COUNT, stats.todayCount)
            put(COLUMN_DISTRACTION, stats.distraction)
            put(COLUMN_DISTRACTION_TIME_LIST, Gson().toJson(stats.distractionTimeList))
            put(COLUMN_COMPLETED, stats.completed)
            put(COLUMN_COMPLETED_TIME_LIST, Gson().toJson(stats.completedTimeList))
            put(COLUMN_SESSION_COMPLETION_STATUS, if (stats.sessionCompletionStatus) 1 else 0)
        }
        db?.update(TABLE_STATS, values, "$COLUMN_START_TIME = ?", arrayOf(stats.startTime.toString()))
    }

    fun getStatsByStartTime(startTime: Long): Stats? {
        val cursor = db?.query(
            TABLE_STATS,
            null,
            "$COLUMN_START_TIME = ?",
            arrayOf(startTime.toString()),
            null,
            null,
            null
        )

        var stats: Stats? = null
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                stats = Stats(
                    startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_TIME)),
                    todayCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TODAY_COUNT)),
                    distraction = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DISTRACTION)),
                    distractionTimeList = Gson().fromJson(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DISTRACTION_TIME_LIST)),
                        Array<Long>::class.java
                    ).toMutableList(),
                    completed = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)),
                    completedTimeList = Gson().fromJson(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED_TIME_LIST)),
                        Array<Long>::class.java
                    ).toMutableList(),
                    sessionCompletionStatus = cursor.getInt(cursor.getColumnIndexOrThrow(
                        COLUMN_SESSION_COMPLETION_STATUS
                    )) == 1
                )
            }
        }
        cursor?.close()
        return stats
    }


    fun getCompletedCountForDate(dateMillis: Long): Int {
        val startOfDay = dateMillis - (dateMillis % (24 * 60 * 60 * 1000)) // Start of the given date
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000) // Start of the next day

        val cursor = db?.query(
            TABLE_STATS,
            arrayOf("SUM($COLUMN_COMPLETED) AS totalCompleted"), // Sum the completed counts
            "$COLUMN_START_TIME >= ? AND $COLUMN_START_TIME < ?",
            arrayOf(startOfDay.toString(), endOfDay.toString()),
            null,
            null,
            null
        )

        var totalCompleted = 0
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                totalCompleted = cursor.getInt(cursor.getColumnIndexOrThrow("totalCompleted"))
            }
        }
        cursor?.close()
        return totalCompleted
    }

    fun getCompletedCountForToday(): Int {
        return getCompletedCountForDate(System.currentTimeMillis()) // Pass current time
    }

    fun getAllStats(): List<Stats> {
        val statsList = mutableListOf<Stats>()
        val cursor = db?.query(TABLE_STATS, null, null, null, null, null, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val stats = Stats(
                        startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_TIME)),
                        todayCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TODAY_COUNT)),
                        distraction = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DISTRACTION)),
                        distractionTimeList = Gson().fromJson(
                            cursor.getString(cursor.getColumnIndexOrThrow(
                                COLUMN_DISTRACTION_TIME_LIST
                            )),
                            Array<Long>::class.java
                        ).toMutableList(),
                        completed = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED)),
                        completedTimeList = Gson().fromJson(
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPLETED_TIME_LIST)),
                            Array<Long>::class.java
                        ).toMutableList(),
                        sessionCompletionStatus = cursor.getInt(cursor.getColumnIndexOrThrow(
                            COLUMN_SESSION_COMPLETION_STATUS
                        )) == 1
                    )
                    statsList.add(stats)
                } while (cursor.moveToNext())
            }
        }
        cursor?.close()
        return statsList
    }

    fun clearStats() {
        db?.execSQL("DELETE FROM $TABLE_STATS") // Removes all rows
    }

    // Method to get the count where sessionCompletionStatus is true for a given day
    fun getTotalCountForDate(dateMillis: Long): Int {
        val startOfDay = dateMillis - (dateMillis % (24 * 60 * 60 * 1000)) // Start of the given date
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000) // Start of the next day

        val cursor = db?.query(
            TABLE_STATS,
            arrayOf("COUNT(*) AS totalCount"), // Sum the completed counts
            "$COLUMN_START_TIME >= ? AND $COLUMN_START_TIME < ? AND $COLUMN_SESSION_COMPLETION_STATUS = 1",
            arrayOf(startOfDay.toString(), endOfDay.toString()),
            null,
            null,
            null
        )

        var totalCount = 0
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                totalCount = cursor.getInt(cursor.getColumnIndexOrThrow("totalCount"))
            }
        }
        cursor?.close()
        // db.close()
        return totalCount
    }

    fun getTotalCountForToday(): Int {
        return getTotalCountForDate(System.currentTimeMillis())
    }

    // Method to get the distracted count for a given day
    fun getDistractedCountForDate(dateMillis: Long): Int {
        val startOfDay = dateMillis - (dateMillis % (24 * 60 * 60 * 1000)) // Start of the given date
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000) // Start of the next day

        val cursor = db?.query(
            TABLE_STATS,
            arrayOf("SUM($COLUMN_DISTRACTION) AS totalDistracted"), // Sum the completed counts
            "$COLUMN_START_TIME >= ? AND $COLUMN_START_TIME < ?",
            arrayOf(startOfDay.toString(), endOfDay.toString()),
            null,
            null,
            null
        )

        var totalDistracted = 0
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                totalDistracted = cursor.getInt(cursor.getColumnIndexOrThrow("totalDistracted"))
            }
        }
        cursor?.close()
        return totalDistracted
    }

    // Method to get today's total distracted count
    fun getDistractedCountForToday(): Int {
        return getDistractedCountForDate(System.currentTimeMillis())
    }

    // Export using MediaStore which works for android 10 and above
    fun exportDatabaseToFileFromAPI29(context: Context): Uri? {
        val currentDBPath = context.getDatabasePath(DATABASE_NAME).absolutePath

        // Prepare the content values for MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, DATABASE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) // Scoped storage path
        }

        // Insert the file into the MediaStore and get the URI
        val uri: Uri =
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?:
                return null

        return try {
            // Open the input stream from the database file
            FileInputStream(currentDBPath).use { inputStream ->
                // Open the output stream from the MediaStore URI
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    // Transfer data from input stream to output stream
                    inputStream.copyTo(outputStream)
                } ?: throw IOException("Failed to open output stream.")
            }
            uri
        } catch (e: Exception) {
            null
        }
    }

    // Export all stats to JSON and save as file using scoped storage
    fun exportAllStatsToJsonFileFromAPI29(context: Context): Uri? {
        val statsList = getAllStats()  // Retrieve all records from the database
        val jsonData = Gson().toJson(statsList)  // Convert list to JSON format
        val fileName = "FocusTrackStatsExport.json"

        // Prepare content values for the file
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) // Scoped storage path
        }

        // Insert the file into the MediaStore
        val uri: Uri? = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        return if (uri != null) {
            try {
                // Write to the output stream
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonData)
                    }
                }
                uri // Return the URI of the saved file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
}
