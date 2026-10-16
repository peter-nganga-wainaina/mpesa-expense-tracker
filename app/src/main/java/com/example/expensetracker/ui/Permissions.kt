package com.example.expensetracker.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Receive new M-Pesa SMS, read past ones for backfill, and prompt for categories. */
val REQUIRED_PERMISSIONS: Array<String> = buildList {
    add(Manifest.permission.RECEIVE_SMS)
    add(Manifest.permission.READ_SMS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.hasRequiredPermissions() = REQUIRED_PERMISSIONS.all { isGranted(it) }
