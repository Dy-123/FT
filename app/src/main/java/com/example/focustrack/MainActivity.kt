package com.example.focustrack

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.example.focustrack.utils.Utils.enterFullScreen
import com.example.focustrack.utils.Utils.exitFullScreen
import com.example.focustrack.model.dataStats
import com.example.focustrack.permissions.RuntimePermissions
import com.example.focustrack.repository.StoreStats
import com.example.focustrack.utils.Utils
import com.example.focustrack.utils.Utils.disableDndMode
import com.example.focustrack.utils.Utils.enableDndMode
import com.example.focustrack.viewmodel.TimerViewModel

class MainActivity : AppCompatActivity() {

    private var minimalView = false
    private var startStatus = false
    private val timerViewModel: TimerViewModel by viewModels()
    private val timeLimit : Long = 60*60*1000

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        StoreStats.init(this)


        val showHideButton: ImageButton = findViewById(R.id.showHide)
        val optionsButton: ImageButton = findViewById(R.id.Options)
        val startStopButton: ImageButton = findViewById(R.id.startStopButton)
        val focusModeButton: ImageButton = findViewById(R.id.focusModeButton)
        val completedButton: ImageButton = findViewById(R.id.completedButton)
        val statsView: LinearLayout = findViewById(R.id.stats)
        val todayCountTextView: TextView = findViewById(R.id.todayCount)
        val distractionTextView: TextView = findViewById(R.id.distraction)
        val completedTextView: TextView = findViewById(R.id.completed)
        val timeTextView: TextView = findViewById(R.id.timeView)
        val rootLayout: ConstraintLayout = findViewById(R.id.main)

        // Set initial transparency (0.0 is fully transparent, 1.0 is fully opaque)
        showHideButton.alpha = 0.05f

        val timeViewObserveUpdate = Observer { millisUntilFinished: Long ->
            if (millisUntilFinished > 0) {
                timeTextView.text = Utils.formatTime(millisUntilFinished)
            } else {
                timeTextView.text = Utils.formatTime(timeLimit)
                dataStats.sessionCompleted()
                StoreStats.saveStats()
                startStatus=false
                val currentCount = todayCountTextView.text.toString().removePrefix("Total: ").toIntOrNull() ?: 0
                val newCount = currentCount + 1
                todayCountTextView.text = "Total: $newCount"
            }
        }

        rootLayout.setOnClickListener { view ->
            if (view != showHideButton && view != optionsButton && view != startStopButton && view != focusModeButton && view!= completedButton) {
                if(startStatus) {
                    dataStats.updateDistractionCount()
                    distractionTextView.text = "Ignore : ${dataStats.distraction}"
                }
            }
        }

        todayCountTextView.text = "Total: " + StoreStats.getTotalForToday()

//        StoreStats.resetStats()
//        StoreStats.printDatabaseTable()

        showHideButton.setOnClickListener {
            Utils.toggleVisibility(optionsButton, startStopButton, focusModeButton, statsView)
            if(!minimalView){
                enterFullScreen()
                showHideButton.setImageResource(R.drawable.visibility_48px)
                minimalView = true
            }else{
                exitFullScreen()
                showHideButton.setImageResource(R.drawable.visibility_off_48px)
                minimalView = false
            }
        }

        startStopButton.setOnClickListener{
            if(!startStatus){
                dataStats.initStats()
                timerViewModel.startTimer(timeLimit)
                timerViewModel.timeRemaining.observe(this, timeViewObserveUpdate)
                startStopButton.setImageResource(R.drawable.restart)
                distractionTextView.text = "Ignore : 0"
                completedTextView.text = "Completed : 0"
                startStatus=true
            }else{
                timerViewModel.stopTimer()
                timerViewModel.timeRemaining.removeObserver(timeViewObserveUpdate)
                startStopButton.setImageResource(R.drawable.play_arrow_48px)
                dataStats.resetStats()
                timeTextView.text = Utils.formatTime(timeLimit)
                startStatus=false
            }
        }

        completedButton.setOnClickListener {
            if(startStatus) {
                dataStats.updateCompletedCount()
                completedTextView.text = "Completed : ${dataStats.completed}"
            }else{
                Toast.makeText(this, "Start a session first", Toast.LENGTH_SHORT).show()
            }
        }

        optionsButton.setOnClickListener {
            val (jsonFileUri, dbFileUri) = StoreStats.exportAllData(this)
            val successMessage = StringBuilder()
            if (jsonFileUri != null) {
                successMessage.append("Exported stats to JSON: $jsonFileUri\n")
            } else {
                successMessage.append("Failed to export stats to JSON\n")
            }

            if (dbFileUri != null) {
                successMessage.append("Exported database: $dbFileUri\n")
            } else {
                successMessage.append("Failed to export database\n")
            }

            Toast.makeText(this, successMessage.toString(), Toast.LENGTH_LONG).show()
        }

        focusModeButton.setOnClickListener {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (!notificationManager.isNotificationPolicyAccessGranted) {
                // Request DND permission
                RuntimePermissions.requestDndPermission(this)
            }else {
                if (notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
                    disableDndMode(this)
                    focusModeButton.setImageResource(R.drawable.do_not_disturb_on_48px)
                } else {
                    enableDndMode(this)
                    focusModeButton.setImageResource(R.drawable.do_not_disturb_off_48px)
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        StoreStats.closeDatabase()
    }
}