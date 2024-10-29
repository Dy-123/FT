package com.example.focustrack.viewmodel

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TimerViewModel : ViewModel() {

    private val _timeRemaining = MutableLiveData<Long>()
    val timeRemaining: LiveData<Long> get() = _timeRemaining
    private var timer: CountDownTimer? = null

    fun startTimer(timeInMilliSec: Long) {
        timer?.cancel()     // cancel any existing timer
        timer = object : CountDownTimer(timeInMilliSec, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timeRemaining.value = millisUntilFinished
            }

            override fun onFinish() {
                _timeRemaining.value = 0

            }
        }.start()
    }

    fun resumeTimer() {
        _timeRemaining.value?.let {
            startTimer(it)
        }
    }

    fun stopTimer() {
        timer?.cancel()
    }

}
