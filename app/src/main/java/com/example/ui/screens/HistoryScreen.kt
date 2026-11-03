package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BluetoothEventEntity
import com.example.ui.components.DeviceIconBadge
import com.example.ui.components.EventStatusBadge
import com.example.ui.dialogs.EventDetailDialog
import com.example.ui.viewmodel.BluetoothTrackerViewModel
import com.example.ui.viewmodel.HistoryFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
  viewModel: BluetoothTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val filteredEvents by viewModel.filteredEvents.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val currentFilter by viewModel.historyFilter.collectAsStateWithLifecycle()

  var selectedEventForDetail by remember { mutableStateOf<BluetoothEventEntity?>(null) }
  var showClearConfirmDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header Row with Actions
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Event History",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Complete log of all connect & disconnect events",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Export button
        IconButton(
          onClick = {
            val csvData = viewModel.generateExportData()
            val sendIntent = Intent().apply {
              action = Intent.ACTION_SEND
              putExtra(Intent.EXTRA_TEXT, csvData)
              putExtra(Intent.EXTRA_TITLE, "BlueTrack_Export_${System.currentTimeMillis()}.csv")
              type = "text/csv"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Export Bluetooth Event Logs")
            context.startActivity(shareIntent)
          },
          modifier = Modifier.testTag("export_logs_button")
        ) {
          Icon(imageVector = Icons.Default.Share, contentDescription = "Export Logs")
        }

        // Clear All button
        IconButton(
          onClick = { showClearConfirmDialog = true },
          modifier = Modifier.testTag("clear_all_logs_button")
        ) {
          Icon(
            imageVector = Icons.Default.DeleteSweep,
            contentDescription = "Clear All Logs",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
          )
        }
      }
    }

    // Search bar
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { viewModel.setSearchQuery(it) },
        placeholder = { Text("Filter by name, MAC, or type...") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.setSearchQuery("") }) {
              Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear Search")
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("history_search_input"),
        singleLine = true,
        shape = RoundedCornerShape(14.dp)
      )
    }

    // Filter Chips Row
    item {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        item {
          FilterChip(
            selected = currentFilter == HistoryFilter.ALL,
            onClick = { viewModel.setHistoryFilter(HistoryFilter.ALL) },
            label = { Text("All") },
            modifier = Modifier.testTag("filter_all")
          )
        }
        item {
          FilterChip(
            selected = currentFilter == HistoryFilter.CONNECTED,
            onClick = { viewModel.setHistoryFilter(HistoryFilter.CONNECTED) },
            label = { Text("Connected Only") },
            modifier = Modifier.testTag("filter_connected")
          )
        }
        item {
          FilterChip(
            selected = currentFilter == HistoryFilter.DISCONNECTED,
            onClick = { viewModel.setHistoryFilter(HistoryFilter.DISCONNECTED) },
            label = { Text("Disconnected Only") },
            modifier = Modifier.testTag("filter_disconnected")
          )
        }
        item {
          FilterChip(
            selected = currentFilter == HistoryFilter.TODAY,
            onClick = { viewModel.setHistoryFilter(HistoryFilter.TODAY) },
            label = { Text("Today") },
            modifier = Modifier.testTag("filter_today")
          )
        }
      }
    }

    // Event Items
    if (filteredEvents.isEmpty()) {
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
              .padding(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No log records found",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = if (searchQuery.isNotEmpty()) "Try adjusting your search query or filters." else "Peripherals will appear here automatically as they connect.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(filteredEvents, key = { it.id }) { event ->
        HistoryEventCard(
          event = event,
          onClick = { selectedEventForDetail = event }
        )
      }
    }
  }

  // Event Detail Dialog
  selectedEventForDetail?.let { event ->
    EventDetailDialog(
      event = event,
      onDismiss = { selectedEventForDetail = null },
      onDelete = { viewModel.deleteEvent(event.id) }
    )
  }

  // Clear All Confirmation Dialog
  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = { Text("Clear All Logs?") },
      text = { Text("This will permanently remove all event history from the local database. Tracked peripherals will remain.") },
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
          modifier = Modifier.testTag("confirm_clear_all_button")
        ) {
          Text("Clear All")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showClearConfirmDialog = false },
          modifier = Modifier.testTag("cancel_clear_all_button")
        ) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun HistoryEventCard(
  event: BluetoothEventEntity,
  onClick: () -> Unit
) {
  val sdf = SimpleDateFormat("MMM d, yyyy • h:mm:ss a", Locale.getDefault())
  val formattedDate = sdf.format(Date(event.timestamp))

  val durationText = event.durationMs?.let { ms ->
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    when {
      hours > 0 -> "Session: ${hours}h ${minutes % 60}m"
      minutes > 0 -> "Session: ${minutes}m ${seconds % 60}s"
      else -> "Session: ${seconds}s"
    }
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("history_event_${event.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        DeviceIconBadge(
          deviceType = event.deviceType,
          modifier = Modifier.size(38.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = event.deviceName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${event.deviceType} • ${event.deviceAddress}",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        EventStatusBadge(eventType = event.eventType)
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = formattedDate,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        if (durationText != null) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = durationText,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        if (event.isSimulated) {
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer
          ) {
            Text(
              text = "Simulated",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }
      }
    }
  }
}
