package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TrackedPeripheralEntity
import com.example.ui.components.ActiveAlarmBanner
import com.example.ui.components.DeviceIconBadge
import com.example.ui.components.PulsingDot
import com.example.ui.dialogs.DeviceAlarmConfigDialog
import com.example.ui.theme.ConnectedGreen
import com.example.ui.theme.DisconnectedRed
import com.example.ui.viewmodel.BluetoothTrackerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PeripheralsScreen(
  viewModel: BluetoothTrackerViewModel,
  onOpenSimulateDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val peripherals by viewModel.peripherals.collectAsStateWithLifecycle()
  val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
  val activeAlarm by viewModel.activeAlarm.collectAsStateWithLifecycle()

  var searchQuery by remember { mutableStateOf("") }
  var configuringPeripheral by remember { mutableStateOf<TrackedPeripheralEntity?>(null) }

  val filteredPeripherals = remember(peripherals, searchQuery) {
    if (searchQuery.isBlank()) peripherals
    else peripherals.filter {
      it.displayName.contains(searchQuery, ignoreCase = true) ||
        it.deviceAddress.contains(searchQuery, ignoreCase = true) ||
        it.deviceType.contains(searchQuery, ignoreCase = true)
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 0. Emergency Active Alarm Banner
    if (activeAlarm != null) {
      item {
        ActiveAlarmBanner(
          alarmState = activeAlarm,
          onDismiss = { viewModel.dismissActiveAlarm() },
          onSnooze = { viewModel.snoozeActiveAlarm(2) }
        )
      }
    }

    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Tracked Peripherals",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Tap any peripheral to configure alerts & anti-lost protection",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(
          onClick = { viewModel.refreshPairedDevices() },
          modifier = Modifier.testTag("refresh_paired_button")
        ) {
          Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Paired Devices")
        }
      }
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by name, type, or MAC...") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("peripheral_search_input"),
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
      )
    }

    if (filteredPeripherals.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Watch,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = if (searchQuery.isNotEmpty()) "No matching peripherals found" else "No peripherals logged yet",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Peripherals are automatically saved as soon as they connect to your device.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
              onClick = onOpenSimulateDialog,
              modifier = Modifier.testTag("simulate_from_empty_peripherals")
            ) {
              Text("Add Simulated Peripheral")
            }
          }
        }
      }
    } else {
      items(filteredPeripherals, key = { it.deviceAddress }) { peripheral ->
        PeripheralCard(
          peripheral = peripheral,
          onConfigure = { configuringPeripheral = peripheral },
          onToggleFavorite = { viewModel.toggleFavorite(peripheral.deviceAddress) },
          onToggleMute = { viewModel.toggleMuted(peripheral.deviceAddress) },
          onDelete = { viewModel.deletePeripheral(peripheral.deviceAddress) }
        )
      }
    }

    // Android System Paired Devices Section (if any detected)
    if (pairedDevices.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "PAIRED IN SYSTEM SETTINGS (${pairedDevices.size})",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      items(pairedDevices, key = { it.address }) { device ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
          ) {
            DeviceIconBadge(
              deviceType = device.type,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = device.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "${device.type} • ${device.address}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }

  // Configure Device Alert & Anti-Lost Dialog
  configuringPeripheral?.let { item ->
    DeviceAlarmConfigDialog(
      peripheral = item,
      onDismiss = { configuringPeripheral = null },
      onSave = { newNickname, alertMode, antiLostEnabled, alarmDisconnect, alarmConnect ->
        viewModel.updateNickname(item.deviceAddress, newNickname)
        viewModel.updateAlertMode(item.deviceAddress, alertMode)
        viewModel.updateAntiLostConfig(item.deviceAddress, antiLostEnabled, alarmDisconnect, alarmConnect)
      },
      onTestAlarm = {
        viewModel.testAntiLostAlarm(item.displayName, item.deviceAddress)
      }
    )
  }
}

@Composable
fun PeripheralCard(
  peripheral: TrackedPeripheralEntity,
  onConfigure: () -> Unit,
  onToggleFavorite: () -> Unit,
  onToggleMute: () -> Unit,
  onDelete: () -> Unit
) {
  val context = LocalContext.current
  val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

  val lastEventText = if (peripheral.isConnected) {
    "Connected now"
  } else if (peripheral.lastDisconnectedTimestamp > 0) {
    "Last seen: ${sdf.format(Date(peripheral.lastDisconnectedTimestamp))}"
  } else if (peripheral.lastConnectedTimestamp > 0) {
    "Last connected: ${sdf.format(Date(peripheral.lastConnectedTimestamp))}"
  } else {
    "Not recently active"
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (peripheral.isAntiLostEnabled) {
        DisconnectedRed.copy(alpha = 0.08f)
      } else if (peripheral.isConnected) {
        ConnectedGreen.copy(alpha = 0.08f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
      }
    ),
    border = if (peripheral.isAntiLostEnabled) {
      androidx.compose.foundation.BorderStroke(1.2.dp, DisconnectedRed.copy(alpha = 0.5f))
    } else null,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onConfigure)
      .testTag("peripheral_card_${peripheral.deviceAddress}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        DeviceIconBadge(
          deviceType = peripheral.deviceType,
          tint = if (peripheral.isAntiLostEnabled) DisconnectedRed else if (peripheral.isConnected) ConnectedGreen else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = peripheral.displayName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            if (peripheral.isConnected) {
              Spacer(modifier = Modifier.width(6.dp))
              PulsingDot(isActive = true)
            }
          }
          if (!peripheral.customNickname.isNullOrBlank()) {
            Text(
              text = "Original: ${peripheral.deviceName}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Favorite Toggle
        IconButton(
          onClick = onToggleFavorite,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = if (peripheral.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = "Favorite",
            tint = if (peripheral.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Mute Alert Toggle
        IconButton(
          onClick = onToggleMute,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = if (peripheral.isMuted || peripheral.alertMode == TrackedPeripheralEntity.ALERT_MODE_MUTED) {
              Icons.Default.NotificationsOff
            } else {
              Icons.Default.Notifications
            },
            contentDescription = "Toggle Mute Alerts",
            tint = if (peripheral.isMuted || peripheral.alertMode == TrackedPeripheralEntity.ALERT_MODE_MUTED) {
              MaterialTheme.colorScheme.error
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = peripheral.deviceAddress,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("MAC Address", peripheral.deviceAddress))
            Toast.makeText(context, "MAC address copied", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy MAC",
            modifier = Modifier.size(14.dp)
          )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = lastEventText,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = if (peripheral.isConnected) ConnectedGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Status Badges & Action Buttons
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Anti-Lost Badge
        if (peripheral.isAntiLostEnabled) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DisconnectedRed
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Anti-Lost Active",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Spacer(modifier = Modifier.width(6.dp))
        }

        // Alert Policy Badge
        when (peripheral.alertMode) {
          TrackedPeripheralEntity.ALERT_MODE_ALWAYS_ALERT -> {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = "Always Alert",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          TrackedPeripheralEntity.ALERT_MODE_MUTED -> {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.errorContainer
            ) {
              Text(
                text = "Muted",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          else -> {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = "${peripheral.totalConnectionCount} connects",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
          onClick = onConfigure,
          contentPadding = PaddingValues(horizontal = 8.dp),
          modifier = Modifier.testTag("configure_peripheral_${peripheral.deviceAddress}")
        ) {
          Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Configure", fontSize = 12.sp)
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}
