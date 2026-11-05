package com.example

import com.example.data.model.TrackedPeripheralEntity
import com.example.data.repository.BluetoothTrackerRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTestLogic {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun parseDeviceType_nullClass_returnsDefault() {
    val result = BluetoothTrackerRepository.parseDeviceType(null)
    assertEquals("Bluetooth Device", result)
  }

  @Test
  fun trackedPeripheral_defaultAlertModeAndAntiLost() {
    val peripheral = TrackedPeripheralEntity(
      deviceAddress = "AA:BB:CC:DD:EE:FF",
      deviceName = "Sony Headphones",
      deviceType = "Headphones"
    )
    assertEquals(TrackedPeripheralEntity.ALERT_MODE_DEFAULT, peripheral.alertMode)
    assertFalse(peripheral.isAntiLostEnabled)
    assertTrue(peripheral.alarmOnDisconnect)
    assertFalse(peripheral.alarmOnConnect)
    assertEquals("Sony Headphones", peripheral.displayName)
  }

  @Test
  fun trackedPeripheral_customNicknameDisplay() {
    val peripheral = TrackedPeripheralEntity(
      deviceAddress = "AA:BB:CC:DD:EE:FF",
      deviceName = "WH-1000XM5",
      customNickname = "My Daily Driver",
      deviceType = "Headphones",
      isAntiLostEnabled = true
    )
    assertEquals("My Daily Driver", peripheral.displayName)
    assertTrue(peripheral.isAntiLostEnabled)
  }

  @Test
  fun pairedDevice_deduplicationLogic_filtersTrackedAddresses() {
    val trackedAddresses = setOf("AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02")
    val allPaired = listOf(
      "AA:BB:CC:DD:EE:01",
      "aa:bb:cc:dd:ee:02",
      "AA:BB:CC:DD:EE:03"
    )
    val unrecorded = allPaired.filter { it.uppercase() !in trackedAddresses }
    assertEquals(1, unrecorded.size)
    assertEquals("AA:BB:CC:DD:EE:03", unrecorded.first())
  }

  @Test
  fun bluetoothErrorLog_crashSeverity_preservesStackTraceAndDevice() {
    val log = com.example.data.model.BluetoothErrorLogEntity(
      id = 1L,
      timestamp = 1000L,
      tag = "CrashHandler",
      message = "NullPointerException in test",
      stackTrace = "java.lang.NullPointerException\n\tat com.example.Test.run(Test.kt:10)",
      severity = com.example.data.model.BluetoothErrorLogEntity.SEVERITY_CRASH,
      deviceAddress = "AA:BB:CC:DD:EE:FF",
      deviceName = "My Headphones"
    )
    assertEquals(com.example.data.model.BluetoothErrorLogEntity.SEVERITY_CRASH, log.severity)
    assertEquals("CrashHandler", log.tag)
    assertTrue(log.stackTrace?.contains("NullPointerException") == true)
    assertEquals("My Headphones", log.deviceName)
  }
}
