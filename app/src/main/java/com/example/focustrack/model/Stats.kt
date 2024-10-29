package com.example.focustrack.model

data class Stats(
    val startTime: Long,
    val todayCount: Int,
    val distraction: Int,
    val distractionTimeList: MutableList<Long>,
    val completed: Int,
    val completedTimeList: MutableList<Long>,
    val sessionCompletionStatus: Boolean
)
