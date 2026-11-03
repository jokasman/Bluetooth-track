package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DeviceIconBadge
import com.example.ui.theme.ConnectedGreen
import com.example.ui.theme.DisconnectedRed

data class PresetPeripheral(
  val name: String,
  val address: String,
  val type: String
)

val PRESET_PERIPHERALS = listOf(
  PresetPeripheral("AirPods Pro 2", "1A:2B:3C:4D:5E:6F", "Headphones"),
  PresetPeripheral("Sony WH-1000XM5", "44:55:66:77:88:99", "Headphones"),
  PresetPeripheral("Apple Watch Ultra", "AA:BB:CC:11:22:33", "Smartwatch / Wearable"),
  PresetPeripheral("Logitech MX Master 3S", "00:11:22:33:44:55", "Mouse / Pointer"),
  PresetPeripheral("Tesla Model 3 Audio", "CC:DD:EE:FF:00:11", "Car Audio"),
  PresetPeripheral("Pixel Buds Pro", "66:77:88:99:AA:BB", "Headset / Earbuds"),
  PresetPeripheral("Bose SoundLink Flex", "88:99:AA:BB:CC:DD", "Speaker")
)

@Composable
fun SimulateEventDialog(
  onDismiss: () -> Unit,
  onSimulate: (name: String, address: String, type: String, isConnect: Boolean) -> Unit
) {
  var selectedPreset by remember { mutableStateOf(PRESET_PERIPHERALS.first()) }
  var customName by remember { mutableStateOf(selectedPreset.name) }
  var customAddress by remember { mutableStateOf(selectedPreset.address) }
  var customType by remember { mutableStateOf(selectedPreset.type) }
  var isConnectAction by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Simulate Peripheral Event",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Test instant notifications and event logs without needing a physical Bluetooth device nearby.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "QUICK PRESETS",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(PRESET_PERIPHERALS) { preset ->
            val isSelected = preset.address == customAddress
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  selectedPreset = preset
                  customName = preset.name
                  customAddress = preset.address
                  customType = preset.type
                }
                .testTag("preset_${preset.name.replace(" ", "_")}"),
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                DeviceIconBadge(
                  deviceType = preset.type,
                  modifier = Modifier.size(24.dp),
                  containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = preset.name,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = customName,
          onValueChange = { customName = it },
          label = { Text("Device Name") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("simulate_device_name"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = customAddress,
          onValueChange = { customAddress = it },
          label = { Text("MAC Address") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("simulate_device_address"),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "EVENT ACTION",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable { isConnectAction = true }
              .testTag("simulate_action_connect"),
            color = if (isConnectAction) ConnectedGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = if (isConnectAction) androidx.compose.foundation.BorderStroke(1.5.dp, ConnectedGreen) else null
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.padding(vertical = 10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.BluetoothConnected,
                contentDescription = null,
                tint = if (isConnectAction) ConnectedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Connect",
                fontWeight = FontWeight.Bold,
                color = if (isConnectAction) ConnectedGreen else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable { isConnectAction = false }
              .testTag("simulate_action_disconnect"),
            color = if (!isConnectAction) DisconnectedRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = if (!isConnectAction) androidx.compose.foundation.BorderStroke(1.5.dp, DisconnectedRed) else null
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.padding(vertical = 10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.BluetoothDisabled,
                contentDescription = null,
                tint = if (!isConnectAction) DisconnectedRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Disconnect",
                fontWeight = FontWeight.Bold,
                color = if (!isConnectAction) DisconnectedRed else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSimulate(customName, customAddress, customType, isConnectAction)
          onDismiss()
        },
        modifier = Modifier.testTag("confirm_simulate_button")
      ) {
        Text("Trigger Event")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_simulate_button")
      ) {
        Text("Cancel")
      }
    }
  )
}
