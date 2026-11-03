package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_peripherals")
data class TrackedPeripheralEntity(
  @PrimaryKey
  val deviceAddress: String,
  val deviceName: String,
  val customNickname: String? = null,
  val deviceType: String,
  val isConnected: Boolean = false,
  val lastConnectedTimestamp: Long = 0L,
  val lastDisconnectedTimestamp: Long = 0L,
  val totalConnectionCount: Int = 0,
  val totalConnectedDurationMs: Long = 0L,
  val isFavorite: Boolean = false,
  val isMuted: Boolean = false
) {
  val displayName: String
    get() = if (!customNickname.isNullOrBlank()) customNickname else deviceName
}
