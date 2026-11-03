package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ConnectedGreen
import com.example.ui.viewmodel.BluetoothTrackerViewModel

@Composable
fun SettingsScreen(
  viewModel: BluetoothTrackerViewModel,
  hasNotificationPermission: Boolean,
  hasBluetoothPermission: Boolean,
  onRequestPermissions: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val serviceEnabled by viewModel.serviceEnabled.collectAsStateWithLifecycle()
  val notifyOnConnect by viewModel.notifyOnConnect.collectAsStateWithLifecycle()
  val notifyOnDisconnect by viewModel.notifyOnDisconnect.collectAsStateWithLifecycle()
  val vibrateOnAlert by viewModel.vibrateOnAlert.collectAsStateWithLifecycle()
  val autoStartOnBoot by viewModel.autoStartOnBoot.collectAsStateWithLifecycle()

  var showClearConfirmDialog by remember { mutableStateOf(false) }

  val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
  val isBluetoothEnabled = bluetoothManager?.adapter?.isEnabled ?: false

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Column {
        Text(
          text = "Settings & Alerts",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Configure background monitoring and notification preferences",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Notification Preferences
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "NOTIFICATION ALERTS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          Spacer(modifier = Modifier.height(12.dp))

          SettingToggleRow(
            title = "Alert on Connect",
            subtitle = "Show notification when a peripheral connects",
            icon = Icons.Default.NotificationsActive,
            checked = notifyOnConnect,
            onCheckedChange = { viewModel.setNotifyOnConnect(it) },
            testTag = "toggle_notify_connect"
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

          SettingToggleRow(
            title = "Alert on Disconnect",
            subtitle = "Show notification and active duration when disconnected",
            icon = Icons.Default.Notifications,
            checked = notifyOnDisconnect,
            onCheckedChange = { viewModel.setNotifyOnDisconnect(it) },
            testTag = "toggle_notify_disconnect"
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

          SettingToggleRow(
            title = "Vibrate on Alert",
            subtitle = "Vibrate device when a peripheral connects or disconnects",
            icon = Icons.Default.Vibration,
            checked = vibrateOnAlert,
            onCheckedChange = { viewModel.setVibrateOnAlert(it) },
            testTag = "toggle_vibrate_alert"
          )

          Spacer(modifier = Modifier.height(14.dp))

          OutlinedButton(
            onClick = { viewModel.testNotificationAlert(isConnected = true) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("send_test_notification_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Test Notification Now")
          }
        }
      }
    }

    // Background Service Settings
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "BACKGROUND MONITORING",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          Spacer(modifier = Modifier.height(12.dp))

          SettingToggleRow(
            title = "Foreground Monitor Service",
            subtitle = "Keeps connection monitor active even when the app is closed",
            icon = Icons.Default.Bluetooth,
            checked = serviceEnabled,
            onCheckedChange = { viewModel.toggleService(it) },
            testTag = "toggle_monitor_service"
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

          SettingToggleRow(
            title = "Auto-Start on Boot",
            subtitle = "Automatically restart peripheral monitoring when phone reboots",
            icon = Icons.Default.Security,
            checked = autoStartOnBoot,
            onCheckedChange = { viewModel.setAutoStartOnBoot(it) },
            testTag = "toggle_auto_boot"
          )
        }
      }
    }

    // Android Permissions Status Card
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "SYSTEM PERMISSIONS & STATUS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          Spacer(modifier = Modifier.height(12.dp))

          StatusRow(
            label = "Notification Permission",
            isGranted = hasNotificationPermission,
            testTag = "status_notif_permission"
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

          StatusRow(
            label = "Bluetooth Connect Permission",
            isGranted = hasBluetoothPermission,
            testTag = "status_bt_permission"
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

          StatusRow(
            label = "Bluetooth Hardware Adapter",
            isGranted = isBluetoothEnabled,
            grantedText = "Turned On",
            deniedText = "Turned Off",
            testTag = "status_bt_hardware"
          )

          if (!hasNotificationPermission || !hasBluetoothPermission) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onRequestPermissions,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("request_permissions_settings_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Request Missing Permissions")
            }
          }
        }
      }
    }

    // Data Management
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "DATA MANAGEMENT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Export Logs",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Share history logs as a CSV file",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            IconButton(
              onClick = {
                val csvData = viewModel.generateExportData()
                val sendIntent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, csvData)
                  type = "text/csv"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Export BlueTrack Logs"))
              },
              modifier = Modifier.testTag("settings_export_button")
            ) {
              Icon(imageVector = Icons.Default.Share, contentDescription = "Export")
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Clear All Event Logs",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
              )
              Text(
                text = "Permanently delete event history database",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            IconButton(
              onClick = { showClearConfirmDialog = true },
              modifier = Modifier.testTag("settings_clear_all_button")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Clear",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        }
      }
    }

    // About BlueTrack Card
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "BlueTrack v1.0",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Smart Bluetooth peripheral connection monitor powered by local Room persistence and real-time Android ACL event dispatching.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }

  // Clear All Confirmation
  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = { Text("Clear All Event Logs?") },
      text = { Text("This will permanently erase all connection and disconnection logs.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearAllEvents()
            showClearConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          ),
          modifier = Modifier.testTag("confirm_clear_settings")
        ) {
          Text("Delete All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun SettingToggleRow(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Spacer(modifier = Modifier.width(8.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag)
    )
  }
}

@Composable
fun StatusRow(
  label: String,
  isGranted: Boolean,
  grantedText: String = "Granted",
  deniedText: String = "Missing / Denied",
  testTag: String = ""
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.weight(1f)
    )
    Icon(
      imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
      contentDescription = null,
      tint = if (isGranted) ConnectedGreen else MaterialTheme.colorScheme.error,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = if (isGranted) grantedText else deniedText,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
      color = if (isGranted) ConnectedGreen else MaterialTheme.colorScheme.error
    )
  }
}
