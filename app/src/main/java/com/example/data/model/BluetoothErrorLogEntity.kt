package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bluetooth_error_logs")
data class BluetoothErrorLogEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val tag: String,
  val message: String,
  val stackTrace: String? = null,
  val severity: String = SEVERITY_ERROR,
  val deviceAddress: String? = null,
  val deviceName: String? = null
) {
  companion object {
    const val SEVERITY_CRASH = "CRASH"
    const val SEVERITY_ERROR = "ERROR"
    const val SEVERITY_WARNING = "WARNING"
    const val SEVERITY_INFO = "INFO"
  }
}
