package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bluetooth_events")
data class BluetoothEventEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val deviceName: String,
  val deviceAddress: String,
  val deviceType: String,
  val eventType: String, // "CONNECTED" or "DISCONNECTED"
  val timestamp: Long,
  val durationMs: Long? = null,
  val isSimulated: Boolean = false,
  val note: String? = null
)
