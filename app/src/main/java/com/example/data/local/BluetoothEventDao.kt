package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BluetoothEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BluetoothEventDao {
  @Query("SELECT * FROM bluetooth_events ORDER BY timestamp DESC")
  fun getAllEvents(): Flow<List<BluetoothEventEntity>>

  @Query("SELECT * FROM bluetooth_events ORDER BY timestamp DESC LIMIT :limit")
  fun getRecentEvents(limit: Int): Flow<List<BluetoothEventEntity>>

  @Query("SELECT * FROM bluetooth_events WHERE deviceAddress = :address ORDER BY timestamp DESC")
  fun getEventsForDevice(address: String): Flow<List<BluetoothEventEntity>>

  @Query("SELECT * FROM bluetooth_events WHERE eventType = :type ORDER BY timestamp DESC")
  fun getEventsByType(type: String): Flow<List<BluetoothEventEntity>>

  @Query("SELECT * FROM bluetooth_events WHERE deviceName LIKE '%' || :query || '%' OR deviceAddress LIKE '%' || :query || '%' ORDER BY timestamp DESC")
  fun searchEvents(query: String): Flow<List<BluetoothEventEntity>>

  @Query("SELECT * FROM bluetooth_events WHERE deviceAddress = :address AND eventType = 'CONNECTED' ORDER BY timestamp DESC LIMIT 1")
  suspend fun getLastConnectedEvent(address: String): BluetoothEventEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvent(event: BluetoothEventEntity): Long

  @Query("DELETE FROM bluetooth_events WHERE id = :id")
  suspend fun deleteEventById(id: Long)

  @Query("DELETE FROM bluetooth_events")
  suspend fun clearAllEvents()

  @Query("SELECT COUNT(*) FROM bluetooth_events")
  fun getTotalEventCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM bluetooth_events WHERE timestamp >= :sinceTimestamp")
  fun getEventCountSince(sinceTimestamp: Long): Flow<Int>
}
