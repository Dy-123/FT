package com.example.focustrack.permissions

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

class RuntimePermissions {
    companion object {
        // Static-like function to request DND permission
        fun requestDndPermission(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                context.startActivity(intent)
            }
        }
    }
}
