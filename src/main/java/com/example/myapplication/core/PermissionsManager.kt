package com.example.myapplication.core

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionsManager {

    const val REQUEST_CODE = 5001

    private fun requiredPermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        return perms.toTypedArray()
    }

    fun hasAll(context: Context): Boolean {
        return requiredPermissions().all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun request(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            requiredPermissions(),
            REQUEST_CODE
        )
    }
}
