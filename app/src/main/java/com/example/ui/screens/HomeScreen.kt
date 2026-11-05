package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BluetoothEventEntity
import com.example.ui.components.ActiveAlarmBanner
import com.example.ui.components.DeviceIconBadge
import com.example.ui.components.EventStatusBadge
import com.example.ui.components.PulsingDot
import com.example.ui.components.QuickStatCard
import com.example.ui.dialogs.EventDetailDialog
import com.example.ui.theme.ConnectedGreen
import com.example.ui.theme.ConnectedGreenContainer
import com.example.ui.theme.DisconnectedRed
import com.example.ui.viewmodel.BluetoothTrackerViewModel

@Composable
fun HomeScreen(
  viewModel: BluetoothTrackerViewModel,
  onNavigateToHistory: () -> Unit,
  onNavigateToPeripherals: () -> Unit,
  onOpenSimulateDialog: () -> Unit,
  hasNotificationPermission: Boolean,
  hasBluetoothPermission: Boolean,
  onRequestPermissions: () -> Unit,
  modifier: Modifier = Modifier
) {
  val serviceEnabled by viewModel.serviceEnabled.collectAsStateWithLifecycle()
  val connectedCount by viewModel.connectedCount.collectAsStateWithLifecycle()
  val connectedPeripherals by viewModel.connectedPeripherals.collectAsStateWithLifecycle()
  val totalEventsCount by viewModel.totalEventsCount.collectAsStateWithLifecycle()
  val todayEventsCount by viewModel.todayEventsCount.collectAsStateWithLifecycle()
  val peripherals by viewModel.peripherals.collectAsStateWithLifecycle()
  val recentEvents by viewModel.recentEvents.collectAsStateWithLifecycle()
  val activeAlarm by viewModel.activeAlarm.collectAsStateWithLifecycle()

  var selectedEventForDetail by remember { mutableStateOf<BluetoothEventEntity?>(null) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 0. Active Anti-Lost Emergency Alarm Banner
    if (activeAlarm != null) {
      item {
        ActiveAlarmBanner(
          alarmState = activeAlarm,
          onDismiss = { viewModel.dismissActiveAlarm() },
          onSnooze = { viewModel.snoozeActiveAlarm(2) }
        )
      }
    }

    // 1. Service Status Hero Card
    item {
      ElevatedCard(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("service_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
          containerColor = if (serviceEnabled) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceVariant
          }
        )
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            PulsingDot(isActive = serviceEnabled)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (serviceEnabled) "MONITOR ACTIVE" else "MONITOR PAUSED",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (serviceEnabled) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
              checked = serviceEnabled,
              onCheckedChange = { viewModel.toggleService(it) },
              modifier = Modifier.testTag("toggle_service_switch")
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = if (serviceEnabled) {
              "Background service is watching for Bluetooth device connect & disconnect events with anti-lost alarm protection."
            } else {
              "Turn on monitoring to receive instant heads-up alerts and log peripheral events."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (serviceEnabled) {
              MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            }
          )
        }
      }
    }

    // 2. Permission Advisory Banner (if missing)
    if (!hasNotificationPermission || !hasBluetoothPermission) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_advisory_card")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
              Text(
                text = "Grant notification & Bluetooth permissions for background alert delivery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = onRequestPermissions,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
              ),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("grant_permission_button")
            ) {
              Text("Grant", fontSize = 12.sp)
            }
          }
        }
      }
    }

    // 3. Quick Metrics 2x2 Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          QuickStatCard(
            title = "Connected Now",
            value = "$connectedCount",
            icon = Icons.Default.BluetoothConnected,
            modifier = Modifier.weight(1f),
            iconTint = ConnectedGreen,
            testTag = "stat_connected_now"
          )
          QuickStatCard(
            title = "Today's Events",
            value = "$todayEventsCount",
            icon = Icons.Default.History,
            modifier = Modifier.weight(1f),
            iconTint = MaterialTheme.colorScheme.primary,
            testTag = "stat_today_events"
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          QuickStatCard(
            title = "Tracked Devices",
            value = "${peripherals.size}",
            icon = Icons.Default.Watch,
            modifier = Modifier.weight(1f),
            iconTint = MaterialTheme.colorScheme.tertiary,
            testTag = "stat_tracked_peripherals"
          )
          QuickStatCard(
            title = "Total Logs",
            value = "$totalEventsCount",
            icon = Icons.Default.Tune,
            modifier = Modifier.weight(1f),
            iconTint = MaterialTheme.colorScheme.secondary,
            testTag = "stat_total_events"
          )
        }
      }
    }

    // 4. Quick Actions
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onOpenSimulateDialog,
            modifier = Modifier
              .weight(1f)
              .testTag("open_simulate_dialog_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Simulate Event")
          }

          OutlinedButton(
            onClick = { viewModel.testNotificationAlert(isConnected = true) },
            modifier = Modifier
              .weight(1f)
              .testTag("quick_test_notification_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Test Alert")
          }
        }

        // Test Anti-Lost Alarm button
        OutlinedButton(
          onClick = {
            viewModel.testAntiLostAlarm("My Headphones", "00:11:22:33:44:55")
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("test_anti_lost_alarm_home_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = DisconnectedRed
          )
        ) {
          Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = null,
            tint = DisconnectedRed,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Test Anti-Lost Alarm (Continuous Sound & Snooze)")
        }
      }
    }

    // 5. Currently Connected Peripherals Section
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "CURRENTLY CONNECTED",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(
          onClick = onNavigateToPeripherals,
          contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
          Text("All Devices", fontSize = 12.sp)
          Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }

    if (connectedPeripherals.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
          ) {
            Icon(
              imageVector = Icons.Default.BluetoothSearching,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "No peripherals connected",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "When headphones, watches or accessories connect, they appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    } else {
      items(connectedPeripherals, key = { it.deviceAddress }) { peripheral ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (peripheral.isAntiLostEnabled) {
              DisconnectedRed.copy(alpha = 0.08f)
            } else {
              ConnectedGreenContainer.copy(alpha = 0.35f)
            }
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToPeripherals() }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
          ) {
            DeviceIconBadge(
              deviceType = peripheral.deviceType,
              tint = if (peripheral.isAntiLostEnabled) DisconnectedRed else ConnectedGreen,
              containerColor = if (peripheral.isAntiLostEnabled) DisconnectedRed.copy(alpha = 0.15f) else ConnectedGreenContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = peripheral.displayName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                PulsingDot(isActive = true)
                if (peripheral.isAntiLostEnabled) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DisconnectedRed
                  ) {
                    Text(
                      text = "Anti-Lost",
                      color = Color.White,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                  }
                }
              }
              Text(
                text = "${peripheral.deviceType} • ${peripheral.deviceAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = ConnectedGreen
            ) {
              Text(
                text = "Active",
                color = MaterialTheme.colorScheme.surface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }

    // 6. Recent Activity Section
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "RECENT ACTIVITY",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(
          onClick = onNavigateToHistory,
          contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
          Text("Full History", fontSize = 12.sp)
          Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }

    if (recentEvents.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No events logged yet",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Tap 'Simulate Event' above or connect a Bluetooth accessory to start logging.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 16.dp)
            )
          }
        }
      }
    } else {
      items(recentEvents, key = { it.id }) { event ->
        RecentEventRow(
          event = event,
          onClick = { selectedEventForDetail = event }
        )
      }
    }
  }

  // Event Details Modal
  selectedEventForDetail?.let { event ->
    EventDetailDialog(
      event = event,
      onDismiss = { selectedEventForDetail = null },
      onDelete = { viewModel.deleteEvent(event.id) }
    )
  }
}

@Composable
fun RecentEventRow(
  event: BluetoothEventEntity,
  onClick: () -> Unit
) {
  val timeElapsed = formatRelativeTime(event.timestamp)

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("recent_event_card_${event.id}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(12.dp)
    ) {
      DeviceIconBadge(
        deviceType = event.deviceType,
        modifier = Modifier.size(40.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = event.deviceName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "$timeElapsed • ${event.deviceType}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      EventStatusBadge(eventType = event.eventType)
    }
  }
}

fun formatRelativeTime(timestamp: Long): String {
  val diff = System.currentTimeMillis() - timestamp
  val seconds = diff / 1000
  val minutes = seconds / 60
  val hours = minutes / 60
  val days = hours / 24

  return when {
    seconds < 30 -> "Just now"
    seconds < 60 -> "${seconds}s ago"
    minutes < 60 -> "${minutes}m ago"
    hours < 24 -> "${hours}h ago"
    else -> "${days}d ago"
  }
}
