package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrackedPeripheralEntity
import com.example.ui.components.DeviceIconBadge
import com.example.ui.theme.ConnectedGreen
import com.example.ui.theme.DisconnectedRed

@Composable
fun DeviceAlarmConfigDialog(
  peripheral: TrackedPeripheralEntity,
  onDismiss: () -> Unit,
  onSave: (
    nickname: String?,
    alertMode: String,
    antiLostEnabled: Boolean,
    alarmDisconnect: Boolean,
    alarmConnect: Boolean
  ) -> Unit,
  onTestAlarm: () -> Unit
) {
  var nickname by remember { mutableStateOf(peripheral.customNickname ?: "") }
  var alertMode by remember { mutableStateOf(peripheral.alertMode) }
  var isAntiLostEnabled by remember { mutableStateOf(peripheral.isAntiLostEnabled) }
  var alarmOnDisconnect by remember { mutableStateOf(peripheral.alarmOnDisconnect) }
  var alarmOnConnect by remember { mutableStateOf(peripheral.alarmOnConnect) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        DeviceIconBadge(deviceType = peripheral.deviceType)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = peripheral.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = peripheral.deviceAddress,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // 1. Nickname
        OutlinedTextField(
          value = nickname,
          onValueChange = { nickname = it },
          label = { Text("Custom Nickname") },
          placeholder = { Text("e.g. Daily Headphones, Car Keys") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("config_nickname_input")
        )

        // 2. Alert Mode Policy
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "NOTIFICATION POLICY",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            AlertModeOption(
              label = "Follow App Default",
              description = "Uses the global setting in app settings",
              selected = alertMode == TrackedPeripheralEntity.ALERT_MODE_DEFAULT,
              onClick = { alertMode = TrackedPeripheralEntity.ALERT_MODE_DEFAULT },
              testTag = "alert_mode_default"
            )

            AlertModeOption(
              label = "Always Alert",
              description = "Always notify on connect/disconnect",
              selected = alertMode == TrackedPeripheralEntity.ALERT_MODE_ALWAYS_ALERT,
              onClick = { alertMode = TrackedPeripheralEntity.ALERT_MODE_ALWAYS_ALERT },
              testTag = "alert_mode_always"
            )

            AlertModeOption(
              label = "Mute Notifications",
              description = "Never show standard notifications for this device",
              selected = alertMode == TrackedPeripheralEntity.ALERT_MODE_MUTED,
              onClick = { alertMode = TrackedPeripheralEntity.ALERT_MODE_MUTED },
              testTag = "alert_mode_muted"
            )
          }
        }

        // 3. Anti-Lost Alarm Protection
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isAntiLostEnabled) {
              DisconnectedRed.copy(alpha = 0.1f)
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
          ),
          border = if (isAntiLostEnabled) androidx.compose.foundation.BorderStroke(1.dp, DisconnectedRed) else null,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = if (isAntiLostEnabled) DisconnectedRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Anti-Lost Protection",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isAntiLostEnabled) DisconnectedRed else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Never leave this item behind",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Switch(
                checked = isAntiLostEnabled,
                onCheckedChange = { isAntiLostEnabled = it },
                modifier = Modifier.testTag("anti_lost_toggle_switch")
              )
            }

            if (isAntiLostEnabled) {
              Spacer(modifier = Modifier.height(10.dp))
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = "ALARM TRIGGERS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )

              // Disconnect Checkbox
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { alarmOnDisconnect = !alarmOnDisconnect }
                  .padding(vertical = 4.dp)
              ) {
                Checkbox(
                  checked = alarmOnDisconnect,
                  onCheckedChange = { alarmOnDisconnect = it },
                  modifier = Modifier.testTag("trigger_disconnect_checkbox")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text(
                    text = "Alarm on Disconnect (Leaving area)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = "Sounds loud continuous alarm until you press Dismiss",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              // Connect Checkbox
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { alarmOnConnect = !alarmOnConnect }
                  .padding(vertical = 4.dp)
              ) {
                Checkbox(
                  checked = alarmOnConnect,
                  onCheckedChange = { alarmOnConnect = it },
                  modifier = Modifier.testTag("trigger_connect_checkbox")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text(
                    text = "Alarm on Connect (Item found / attached)",
                    style = MaterialTheme.typography.bodyMedium
                  )
                  Text(
                    text = "Sounds alarm when device is discovered/re-linked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              OutlinedButton(
                onClick = onTestAlarm,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("test_anti_lost_alarm_button"),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Anti-Lost Alarm Sound")
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            nickname.trim().ifEmpty { null },
            alertMode,
            isAntiLostEnabled,
            alarmOnDisconnect,
            alarmOnConnect
          )
          onDismiss()
        },
        modifier = Modifier.testTag("save_config_button")
      ) {
        Text("Save Settings")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_config_button")
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun AlertModeOption(
  label: String,
  description: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp)
      .testTag(testTag)
  ) {
    RadioButton(
      selected = selected,
      onClick = onClick
    )
    Spacer(modifier = Modifier.width(6.dp))
    Column {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
