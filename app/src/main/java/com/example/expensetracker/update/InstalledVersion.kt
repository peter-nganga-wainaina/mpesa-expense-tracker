package com.example.expensetracker.update

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat

/** The version code of the app as installed, which a release must exceed to be offered. */
fun installedVersionCode(context: Context): Long =
    PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
