package com.example

import com.example.data.repository.BluetoothTrackerRepository
import org.junit.Assert.assertEquals
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
}
