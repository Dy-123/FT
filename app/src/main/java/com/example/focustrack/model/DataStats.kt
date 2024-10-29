package com.example.focustrack.model

object dataStats {
    var startTime = System.currentTimeMillis()
    var todayCount = 0
//    val countTimeList: List<Long> = emptyList()
    var distraction = 0
    val distractionTimeList: MutableList<Long> = mutableListOf()
    var completed = 0
    val completedTimeList: MutableList<Long> = mutableListOf()
    var sessionCompletionStatus = false

    fun updateTodayCount() {
        todayCount++
    }

    fun updateDistractionCount() {
        distraction++
        distractionTimeList.add(System.currentTimeMillis())
    }

    fun updateCompletedCount() {
        completed++
        completedTimeList.add(System.currentTimeMillis())
    }

    fun initStats(){
        startTime = System.currentTimeMillis()
        todayCount = 0
        distraction = 0
        completed = 0
        distractionTimeList.clear()
        completedTimeList.clear()
        sessionCompletionStatus = false
    }

    fun sessionCompleted(){
        sessionCompletionStatus = true
    }

    fun resetStats(){
        todayCount = 0
        distraction = 0
        completed = 0
        distractionTimeList.clear()
        completedTimeList.clear()
        sessionCompletionStatus = false
    }

    fun toStats(): Stats {
        return Stats(
            startTime = startTime,
            todayCount = todayCount,
            distraction = distraction,
            distractionTimeList = distractionTimeList,
            completed = completed,
            completedTimeList = completedTimeList,
            sessionCompletionStatus = sessionCompletionStatus
        )
    }

}