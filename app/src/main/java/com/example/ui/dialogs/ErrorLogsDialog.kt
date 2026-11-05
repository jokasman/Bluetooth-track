package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BluetoothErrorLogEntity
import com.example.ui.theme.ConnectedGreen
import com.example.ui.theme.DisconnectedRed
import com.example.ui.viewmodel.BluetoothTrackerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ErrorLogsDialog(
  viewModel: BluetoothTrackerViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val errorLogs by viewModel.errorLogs.collectAsStateWithLifecycle()

  var searchQuery by remember { mutableStateOf("") }
  var selectedSeverityFilter by remember { mutableStateOf("ALL") }
  var showClearConfirm by remember { mutableStateOf(false) }

  val filteredLogs = remember(errorLogs, searchQuery, selectedSeverityFilter) {
    errorLogs.filter { log ->
      val matchesSeverity = when (selectedSeverityFilter) {
        "ALL" -> true
        "CRASH" -> log.severity == BluetoothErrorLogEntity.SEVERITY_CRASH
        "ERROR" -> log.severity == BluetoothErrorLogEntity.SEVERITY_ERROR
        "WARNING" -> log.severity == BluetoothErrorLogEntity.SEVERITY_WARNING
        else -> true
      }
      val matchesQuery = searchQuery.isBlank() ||
        log.tag.contains(searchQuery, ignoreCase = true) ||
        log.message.contains(searchQuery, ignoreCase = true) ||
        (log.deviceName?.contains(searchQuery, ignoreCase = true) == true) ||
        (log.deviceAddress?.contains(searchQuery, ignoreCase = true) == true) ||
        (log.stackTrace?.contains(searchQuery, ignoreCase = true) == true)

      matchesSeverity && matchesQuery
    }
  }

  val crashCount = remember(errorLogs) {
    errorLogs.count { it.severity == BluetoothErrorLogEntity.SEVERITY_CRASH }
  }
  val errorCount = remember(errorLogs) {
    errorLogs.count { it.severity == BluetoothErrorLogEntity.SEVERITY_ERROR }
  }
  val warningCount = remember(errorLogs) {
    errorLogs.count { it.severity == BluetoothErrorLogEntity.SEVERITY_WARNING }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 12.dp, vertical = 24.dp)
        .testTag("error_logs_dialog_surface"),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Top Header Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = null,
            tint = if (crashCount > 0) DisconnectedRed else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Crash & Error Diagnostics",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${errorLogs.size} recorded in local Room database",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Share / Export Button
          IconButton(
            onClick = {
              val exportText = viewModel.generateErrorLogsExport()
              val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, exportText)
                putExtra(Intent.EXTRA_TITLE, "BlueTrack_Error_Logs.txt")
                type = "text/plain"
              }
              context.startActivity(Intent.createChooser(sendIntent, "Export Diagnostic Logs"))
            },
            modifier = Modifier.testTag("export_error_logs_button")
          ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = "Export Logs")
          }

          // Clear All Button
          IconButton(
            onClick = { showClearConfirm = true },
            modifier = Modifier.testTag("clear_error_logs_button")
          ) {
            Icon(
              imageVector = Icons.Default.DeleteSweep,
              contentDescription = "Clear All Logs",
              tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
          }

          // Close Button
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_error_logs_button")
          ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Input
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search logs, exceptions, or tags...") },
          leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null)
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("error_logs_search_input"),
          singleLine = true,
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Severity Filter Chips
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          item {
            FilterChip(
              selected = selectedSeverityFilter == "ALL",
              onClick = { selectedSeverityFilter = "ALL" },
              label = { Text("All (${errorLogs.size})") },
              modifier = Modifier.testTag("filter_errors_all")
            )
          }
          item {
            FilterChip(
              selected = selectedSeverityFilter == "CRASH",
              onClick = { selectedSeverityFilter = "CRASH" },
              label = { Text("Crashes ($crashCount)") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = DisconnectedRed.copy(alpha = 0.2f),
                selectedLabelColor = DisconnectedRed
              ),
              modifier = Modifier.testTag("filter_errors_crash")
            )
          }
          item {
            FilterChip(
              selected = selectedSeverityFilter == "ERROR",
              onClick = { selectedSeverityFilter = "ERROR" },
              label = { Text("Errors ($errorCount)") },
              modifier = Modifier.testTag("filter_errors_error")
            )
          }
          item {
            FilterChip(
              selected = selectedSeverityFilter == "WARNING",
              onClick = { selectedSeverityFilter = "WARNING" },
              label = { Text("Warnings ($warningCount)") },
              modifier = Modifier.testTag("filter_errors_warning")
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Logs List
        if (filteredLogs.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
              ),
              modifier = Modifier.fillMaxWidth(0.9f)
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = ConnectedGreen,
                  modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = if (searchQuery.isNotEmpty()) "No matching log entries" else "No error events logged",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Any future Bluetooth issues or crashes will be captured and visible here.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                  onClick = { viewModel.logTestError() },
                  modifier = Modifier.testTag("trigger_test_error_empty_button")
                ) {
                  Text("Trigger Test Diagnostic Error")
                }
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            itemsIndexed(
              filteredLogs,
              key = { index, log -> "error_log_${log.id}_$index" }
            ) { _, log ->
              ErrorLogCard(
                log = log,
                onDelete = { viewModel.deleteErrorLog(log.id) }
              )
            }
          }
        }

        // Bottom Actions Bar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
        ) {
          OutlinedButton(
            onClick = { viewModel.logTestError() },
            modifier = Modifier
              .weight(1f)
              .testTag("trigger_test_error_bottom_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Simulate Test Error", fontSize = 12.sp)
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .testTag("dismiss_error_logs_dialog_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Done", fontSize = 12.sp)
          }
        }
      }
    }
  }

  // Clear Confirmation
  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text("Clear All Diagnostic Logs?") },
      text = { Text("This will permanently remove all crash and error logs from the local database.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearAllErrorLogs()
            showClearConfirm = false
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          ),
          modifier = Modifier.testTag("confirm_clear_errors_button")
        ) {
          Text("Clear All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun ErrorLogCard(
  log: BluetoothErrorLogEntity,
  onDelete: () -> Unit
) {
  val context = LocalContext.current
  var expandedStackTrace by remember { mutableStateOf(false) }
  val sdf = SimpleDateFormat("MMM d, yyyy • h:mm:ss a", Locale.getDefault())
  val formattedDate = sdf.format(Date(log.timestamp))

  val isCrash = log.severity == BluetoothErrorLogEntity.SEVERITY_CRASH
  val isWarning = log.severity == BluetoothErrorLogEntity.SEVERITY_WARNING

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCrash) {
        DisconnectedRed.copy(alpha = 0.1f)
      } else if (isWarning) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
      } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
      }
    ),
    border = if (isCrash) {
      androidx.compose.foundation.BorderStroke(1.2.dp, DisconnectedRed)
    } else null,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("error_log_card_${log.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header Row: Severity badge, Tag, Time, Delete
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = when {
            isCrash -> DisconnectedRed
            isWarning -> Color(0xFFE65100)
            else -> MaterialTheme.colorScheme.error
          }
        ) {
          Text(
            text = log.severity,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
          text = log.tag,
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f)
        )

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete Log",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = formattedDate,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (!log.deviceName.isNullOrBlank() || !log.deviceAddress.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Device: ${log.deviceName ?: "Unknown"} (${log.deviceAddress ?: "N/A"})",
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Error Message
      Text(
        text = log.message,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
      )

      // Expandable Stack Trace
      if (!log.stackTrace.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { expandedStackTrace = !expandedStackTrace }
        ) {
          Text(
            text = if (expandedStackTrace) "Hide Stack Trace" else "View Stack Trace",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Icon(
            imageVector = if (expandedStackTrace) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.weight(1f))

          // Copy Stack Trace Button
          IconButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("BlueTrack StackTrace", log.stackTrace))
              Toast.makeText(context, "Stack trace copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copy Stack Trace",
              modifier = Modifier.size(14.dp)
            )
          }
        }

        AnimatedVisibility(visible = expandedStackTrace) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 6.dp)
          ) {
            Text(
              text = log.stackTrace,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              color = Color(0xFFE0E0E0),
              lineHeight = 14.sp,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    }
  }
}
