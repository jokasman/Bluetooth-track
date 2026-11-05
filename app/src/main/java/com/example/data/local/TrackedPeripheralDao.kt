package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TrackedPeripheralEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedPeripheralDao {
  @Query("SELECT * FROM tracked_peripherals ORDER BY isConnected DESC, lastConnectedTimestamp DESC")
  fun getAllPeripherals(): Flow<List<TrackedPeripheralEntity>>

  @Query("SELECT * FROM tracked_peripherals WHERE isConnected = 1 ORDER BY lastConnectedTimestamp DESC")
  fun getConnectedPeripherals(): Flow<List<TrackedPeripheralEntity>>

  @Query("SELECT * FROM tracked_peripherals WHERE deviceAddress = :address LIMIT 1")
  suspend fun getPeripheralByAddress(address: String): TrackedPeripheralEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPeripheral(peripheral: TrackedPeripheralEntity)

  @Query("UPDATE tracked_peripherals SET isConnected = :connected, lastConnectedTimestamp = :timestamp, totalConnectionCount = totalConnectionCount + 1 WHERE deviceAddress = :address")
  suspend fun markConnected(address: String, connected: Boolean, timestamp: Long)

  @Query("UPDATE tracked_peripherals SET isConnected = 0, lastDisconnectedTimestamp = :timestamp, totalConnectedDurationMs = totalConnectedDurationMs + :sessionDurationMs WHERE deviceAddress = :address")
  suspend fun markDisconnected(address: String, timestamp: Long, sessionDurationMs: Long)

  @Query("UPDATE tracked_peripherals SET customNickname = :nickname WHERE deviceAddress = :address")
  suspend fun updateNickname(address: String, nickname: String?)

  @Query("UPDATE tracked_peripherals SET isFavorite = NOT isFavorite WHERE deviceAddress = :address")
  suspend fun toggleFavorite(address: String)

  @Query("UPDATE tracked_peripherals SET isMuted = NOT isMuted, alertMode = CASE WHEN isMuted = 1 THEN 'DEFAULT' ELSE 'MUTED' END WHERE deviceAddress = :address")
  suspend fun toggleMuted(address: String)

  @Query("UPDATE tracked_peripherals SET alertMode = :alertMode WHERE deviceAddress = :address")
  suspend fun updateAlertMode(address: String, alertMode: String)

  @Query("UPDATE tracked_peripherals SET isAntiLostEnabled = :enabled, alarmOnDisconnect = :alarmDisconnect, alarmOnConnect = :alarmConnect WHERE deviceAddress = :address")
  suspend fun updateAntiLostConfig(
    address: String,
    enabled: Boolean,
    alarmDisconnect: Boolean,
    alarmConnect: Boolean
  )

  @Query("DELETE FROM tracked_peripherals WHERE deviceAddress = :address")
  suspend fun deletePeripheral(address: String)

  @Query("SELECT COUNT(*) FROM tracked_peripherals WHERE isConnected = 1")
  fun getConnectedCount(): Flow<Int>
}
