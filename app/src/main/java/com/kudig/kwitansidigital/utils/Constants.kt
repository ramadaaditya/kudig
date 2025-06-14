package com.kudig.kwitansidigital.utils

import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

const val LOCATION_PERMISSION_REQUEST_CODE = 1
const val STORAGE_PERMISSION_REQUEST_CODE = 2
const val PREFS_NAME = "MyPrefsFile"
const val FIRST_INSTALL_KEY = "first_install"
const val PERMISSION_BLUETOOTH = 1
const val PERMISSION_BLUETOOTH_ADMIN = 1
const val PERMISSION_BLUETOOTH_CONNECT = 1
const val PERMISSION_BLUETOOTH_SCAN = 1
val locale = Locale("id", "ID")
val df: DateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a", locale)
val nf = NumberFormat.getCurrencyInstance(locale)
const val SPLASH_DURATION: Long = 1500