package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BluetoothErrorLogEntity
import com.example.data.model.BluetoothEventEntity
import com.example.data.model.TrackedPeripheralEntity

@Database(
  entities = [
    BluetoothEventEntity::class,
    TrackedPeripheralEntity::class,
    BluetoothErrorLogEntity::class
  ],
  version = 3,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun bluetoothEventDao(): BluetoothEventDao
  abstract fun trackedPeripheralDao(): TrackedPeripheralDao
  abstract fun bluetoothErrorLogDao(): BluetoothErrorLogDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "bluetrack_database"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
