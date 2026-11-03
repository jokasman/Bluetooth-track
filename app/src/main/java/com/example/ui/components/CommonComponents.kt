package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ConnectedGreen
import com.example.ui.theme.ConnectedGreenContainer
import com.example.ui.theme.DisconnectedRed
import com.example.ui.theme.DisconnectedRedContainer

@Composable
fun getDeviceIcon(deviceType: String): ImageVector {
  val lower = deviceType.lowercase()
  return when {
    lower.contains("headphone") || lower.contains("earbud") -> Icons.Default.Headphones
    lower.contains("headset") -> Icons.Default.Headset
    lower.contains("watch") || lower.contains("wearable") -> Icons.Default.Watch
    lower.contains("car") || lower.contains("auto") -> Icons.Default.DirectionsCar
    lower.contains("mouse") -> Icons.Default.Mouse
    lower.contains("keyboard") -> Icons.Default.Keyboard
    lower.contains("gamepad") || lower.contains("controller") -> Icons.Default.Gamepad
    lower.contains("speaker") -> Icons.Default.Speaker
    lower.contains("computer") || lower.contains("laptop") -> Icons.Default.Computer
    lower.contains("phone") -> Icons.Default.PhoneAndroid
    else -> Icons.Default.Bluetooth
  }
}

@Composable
fun DeviceIconBadge(
  deviceType: String,
  modifier: Modifier = Modifier,
  tint: Color = MaterialTheme.colorScheme.primary,
  containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
  Box(
    modifier = modifier
      .size(44.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(containerColor),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = getDeviceIcon(deviceType),
      contentDescription = deviceType,
      tint = tint,
      modifier = Modifier.size(24.dp)
    )
  }
}

@Composable
fun EventStatusBadge(
  eventType: String,
  modifier: Modifier = Modifier
) {
  val isConnected = eventType.equals("CONNECTED", ignoreCase = true)
  val bgColor = if (isConnected) ConnectedGreenContainer else DisconnectedRedContainer
  val textColor = if (isConnected) ConnectedGreen else DisconnectedRed
  val label = if (isConnected) "Connected" else "Disconnected"

  Surface(
    modifier = modifier.clip(RoundedCornerShape(16.dp)),
    color = bgColor
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(textColor)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = label,
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
fun QuickStatCard(
  title: String,
  value: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  iconTint: Color = MaterialTheme.colorScheme.primary,
  containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
  testTag: String = ""
) {
  Card(
    modifier = modifier.testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(iconTint.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = value,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun PulsingDot(
  isActive: Boolean,
  modifier: Modifier = Modifier,
  color: Color = ConnectedGreen
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val alpha by if (isActive) {
    infiniteTransition.animateFloat(
      initialValue = 0.3f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(800),
        repeatMode = RepeatMode.Reverse
      ),
      label = "pulse_alpha"
    )
  } else {
    animateColorAsState(targetValue = Color.Gray, label = "inactive_dot")
    infiniteTransition.animateFloat(
      initialValue = 0.4f,
      targetValue = 0.4f,
      animationSpec = infiniteRepeatable(animation = tween(1000)),
      label = "inactive_alpha"
    )
  }

  Box(
    modifier = modifier
      .size(10.dp)
      .clip(CircleShape)
      .background((if (isActive) color else Color.Gray).copy(alpha = alpha))
  )
}
