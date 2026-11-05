package com.example.util

import android.app.Application
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.BluetoothErrorLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter

object BluetoothLogger {
  private const val LOG_TAG = "BlueTrackLogger"
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private var database: AppDatabase? = null
  private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null
  private var isInitialized = false

  fun init(application: Application) {
    if (isInitialized) return
    isInitialized = true

    database = AppDatabase.getInstance(application)
    defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    // Install uncaught crash handler to capture crashes to Room before application termination
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      try {
        val stackTrace = getStackTraceString(throwable)
        val errorMessage = throwable.message ?: throwable.javaClass.simpleName
        val crashLog = BluetoothErrorLogEntity(
          timestamp = System.currentTimeMillis(),
          tag = "UncaughtCrash:${thread.name}",
          message = "Fatal Exception: $errorMessage",
          stackTrace = stackTrace,
          severity = BluetoothErrorLogEntity.SEVERITY_CRASH,
          deviceAddress = null,
          deviceName = null
        )
        // Synchronous insert to guarantee persistence before process death
        database?.bluetoothErrorLogDao()?.insertLogSync(crashLog)
        Log.e(LOG_TAG, "Recorded fatal crash to Room: $errorMessage", throwable)
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Failed to persist crash log to Room", e)
      } finally {
        defaultExceptionHandler?.uncaughtException(thread, throwable)
      }
    }

    logInfo("BlueTrackLogger", "Diagnostic error logging initialized")
  }

  fun logError(
    tag: String,
    message: String,
    throwable: Throwable? = null,
    deviceName: String? = null,
    deviceAddress: String? = null
  ) {
    val stackTrace = throwable?.let { getStackTraceString(it) }
    Log.e(tag, message, throwable)
    persistLog(
      tag = tag,
      message = message,
      stackTrace = stackTrace,
      severity = BluetoothErrorLogEntity.SEVERITY_ERROR,
      deviceName = deviceName,
      deviceAddress = deviceAddress
    )
  }

  fun logWarning(
    tag: String,
    message: String,
    throwable: Throwable? = null,
    deviceName: String? = null,
    deviceAddress: String? = null
  ) {
    val stackTrace = throwable?.let { getStackTraceString(it) }
    Log.w(tag, message, throwable)
    persistLog(
      tag = tag,
      message = message,
      stackTrace = stackTrace,
      severity = BluetoothErrorLogEntity.SEVERITY_WARNING,
      deviceName = deviceName,
      deviceAddress = deviceAddress
    )
  }

  fun logInfo(
    tag: String,
    message: String,
    deviceName: String? = null,
    deviceAddress: String? = null
  ) {
    Log.i(tag, message)
    persistLog(
      tag = tag,
      message = message,
      stackTrace = null,
      severity = BluetoothErrorLogEntity.SEVERITY_INFO,
      deviceName = deviceName,
      deviceAddress = deviceAddress
    )
  }

  fun logCrash(
    thread: Thread,
    throwable: Throwable,
    deviceName: String? = null,
    deviceAddress: String? = null
  ) {
    val stackTrace = getStackTraceString(throwable)
    val errorMessage = throwable.message ?: throwable.javaClass.simpleName
    Log.e("BluetoothCrash", "Crash in ${thread.name}: $errorMessage", throwable)
    persistLog(
      tag = "Crash:${thread.name}",
      message = "Crash: $errorMessage",
      stackTrace = stackTrace,
      severity = BluetoothErrorLogEntity.SEVERITY_CRASH,
      deviceName = deviceName,
      deviceAddress = deviceAddress
    )
  }

  private fun persistLog(
    tag: String,
    message: String,
    stackTrace: String?,
    severity: String,
    deviceName: String?,
    deviceAddress: String?
  ) {
    val db = database ?: return
    scope.launch {
      try {
        db.bluetoothErrorLogDao().insertLog(
          BluetoothErrorLogEntity(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            stackTrace = stackTrace,
            severity = severity,
            deviceAddress = deviceAddress,
            deviceName = deviceName
          )
        )
      } catch (e: Exception) {
        Log.e(LOG_TAG, "Could not insert log into Room", e)
      }
    }
  }

  private fun getStackTraceString(throwable: Throwable): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    throwable.printStackTrace(pw)
    pw.flush()
    return sw.toString()
  }
}
