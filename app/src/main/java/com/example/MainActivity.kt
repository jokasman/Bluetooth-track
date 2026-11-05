package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.BluetoothMonitorService
import com.example.ui.components.PulsingDot
import com.example.ui.dialogs.SimulateEventDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PeripheralsScreen
import com.example.ui.screens.SettingsScreen
import android.content.Intent
import com.example.ui.theme.ConnectedGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BluetoothTrackerViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val initialTab = if (intent?.getStringExtra("EXTRA_OPEN_TAB") == "HISTORY") 2 else 0

    setContent {
      MyApplicationTheme {
        BlueTrackMainApp(initialTab = initialTab)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueTrackMainApp(
  initialTab: Int = 0,
  viewModel: BluetoothTrackerViewModel = viewModel()
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(initialTab) }
  var showSimulateDialog by remember { mutableStateOf(false) }

  // Permissions state
  var hasNotificationPermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }

  var hasBluetoothPermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }

  // Permissions Request Launcher
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { results ->
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      hasNotificationPermission =
        results[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val btGranted = results[Manifest.permission.BLUETOOTH_CONNECT] ?: hasBluetoothPermission
      hasBluetoothPermission = btGranted
      if (btGranted && viewModel.serviceEnabled.value) {
        BluetoothMonitorService.startService(context)
      }
    }
    viewModel.refreshPairedDevices()
  }

  val requestAppPermissions = {
    val permsToRequest = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
      permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission) {
      permsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (permsToRequest.isNotEmpty()) {
      permissionLauncher.launch(permsToRequest.toTypedArray())
    }
  }

  // Start service if permission is already granted and enabled
  LaunchedEffect(hasBluetoothPermission) {
    if (hasBluetoothPermission && viewModel.serviceEnabled.value) {
      BluetoothMonitorService.startService(context)
    }
  }

  // Proactively request permissions on first load if missing
  LaunchedEffect(Unit) {
    if (!hasNotificationPermission || !hasBluetoothPermission) {
      requestAppPermissions()
    }
  }

  // Handle Android back button
  BackHandler(enabled = selectedTab != 0) {
    selectedTab = 0
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = when (selectedTab) {
              0 -> stringResource(R.string.app_name)
              1 -> "Peripherals"
              2 -> "History"
              3 -> "Settings"
              else -> stringResource(R.string.app_name)
            },
            fontWeight = FontWeight.Bold
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface
        )
      )
    },
    bottomBar = {
      NavigationBar(
        modifier = Modifier
          .windowInsetsPadding(WindowInsets.navigationBars)
          .testTag("main_bottom_nav_bar")
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = {
            Icon(
              imageVector = if (selectedTab == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
              contentDescription = "Dashboard"
            )
          },
          label = { Text("Dashboard") },
          modifier = Modifier.testTag("nav_item_dashboard")
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = {
            Icon(
              imageVector = if (selectedTab == 1) Icons.Default.Watch else Icons.Outlined.Watch,
              contentDescription = "Peripherals"
            )
          },
          label = { Text("Peripherals") },
          modifier = Modifier.testTag("nav_item_peripherals")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = {
            Icon(
              imageVector = if (selectedTab == 2) Icons.Default.History else Icons.Outlined.History,
              contentDescription = "History"
            )
          },
          label = { Text("History") },
          modifier = Modifier.testTag("nav_item_history")
        )
        NavigationBarItem(
          selected = selectedTab == 3,
          onClick = { selectedTab = 3 },
          icon = {
            Icon(
              imageVector = if (selectedTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
              contentDescription = "Settings"
            )
          },
          label = { Text("Settings") },
          modifier = Modifier.testTag("nav_item_settings")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Crossfade(targetState = selectedTab, label = "tab_fade") { tab ->
        when (tab) {
          0 -> HomeScreen(
            viewModel = viewModel,
            onNavigateToHistory = { selectedTab = 2 },
            onNavigateToPeripherals = { selectedTab = 1 },
            onOpenSimulateDialog = { showSimulateDialog = true },
            hasNotificationPermission = hasNotificationPermission,
            hasBluetoothPermission = hasBluetoothPermission,
            onRequestPermissions = requestAppPermissions
          )
          1 -> PeripheralsScreen(
            viewModel = viewModel,
            onOpenSimulateDialog = { showSimulateDialog = true }
          )
          2 -> HistoryScreen(
            viewModel = viewModel
          )
          3 -> SettingsScreen(
            viewModel = viewModel,
            hasNotificationPermission = hasNotificationPermission,
            hasBluetoothPermission = hasBluetoothPermission,
            onRequestPermissions = requestAppPermissions
          )
        }
      }
    }
  }

  // Simulation Dialog
  if (showSimulateDialog) {
    SimulateEventDialog(
      onDismiss = { showSimulateDialog = false },
      onSimulate = { name, address, type, isConnect ->
        viewModel.simulateEvent(name, address, type, isConnect)
      }
    )
  }
}
