package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BluetoothErrorLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BluetoothErrorLogDao {

  @Query("SELECT * FROM bluetooth_error_logs ORDER BY timestamp DESC")
  fun getAllLogs(): Flow<List<BluetoothErrorLogEntity>>

  @Query("SELECT * FROM bluetooth_error_logs ORDER BY timestamp DESC LIMIT :limit")
  fun getRecentLogs(limit: Int): Flow<List<BluetoothErrorLogEntity>>

  @Query("SELECT * FROM bluetooth_error_logs WHERE severity = :severity ORDER BY timestamp DESC")
  fun getLogsBySeverity(severity: String): Flow<List<BluetoothErrorLogEntity>>

  @Query("SELECT COUNT(*) FROM bluetooth_error_logs")
  fun getErrorCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLog(log: BluetoothErrorLogEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertLogSync(log: BluetoothErrorLogEntity): Long

  @Query("DELETE FROM bluetooth_error_logs WHERE id = :id")
  suspend fun deleteLogById(id: Long)

  @Query("DELETE FROM bluetooth_error_logs")
  suspend fun clearAllLogs()
}
