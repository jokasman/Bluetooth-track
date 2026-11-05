package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ActiveAlarmState
import com.example.ui.theme.DisconnectedRed
import com.example.ui.theme.DisconnectedRedContainer

@Composable
fun ActiveAlarmBanner(
  alarmState: ActiveAlarmState?,
  onDismiss: () -> Unit,
  onSnooze: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "alarm_flash")
  val alpha by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(400),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alarm_alpha"
  )

  AnimatedVisibility(
    visible = alarmState != null,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically()
  ) {
    alarmState?.let { state ->
      Card(
        modifier = modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .testTag("active_alarm_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (state.isSnoozed) {
            MaterialTheme.colorScheme.tertiaryContainer
          } else {
            DisconnectedRed.copy(alpha = alpha)
          }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                  if (state.isSnoozed) MaterialTheme.colorScheme.tertiary
                  else Color.White
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (state.isSnoozed) Icons.Default.Snooze else Icons.Default.Warning,
                contentDescription = null,
                tint = if (state.isSnoozed) MaterialTheme.colorScheme.onTertiary else DisconnectedRed,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (state.isSnoozed) "ALARM SNOOZED" else "ANTI-LOST ALARM RINGING",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (state.isSnoozed) MaterialTheme.colorScheme.onTertiaryContainer else Color.White
              )
              Text(
                text = state.deviceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.isSnoozed) MaterialTheme.colorScheme.onTertiaryContainer else Color.White
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = if (state.isSnoozed) {
              "Alarm paused. Will sound again in 2 minutes if item remains disconnected."
            } else {
              "Device was disconnected! Sound and vibration will continue until dismissed."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (state.isSnoozed) {
              MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
            } else {
              Color.White.copy(alpha = 0.9f)
            }
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onDismiss,
              modifier = Modifier
                .weight(1f)
                .testTag("dismiss_alarm_banner_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isSnoozed) MaterialTheme.colorScheme.primary else Color.White,
                contentColor = if (state.isSnoozed) MaterialTheme.colorScheme.onPrimary else DisconnectedRed
              )
            ) {
              Icon(imageVector = Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Dismiss", fontWeight = FontWeight.Bold)
            }

            if (!state.isSnoozed) {
              OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                  .weight(1f)
                  .testTag("snooze_alarm_banner_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)
              ) {
                Icon(imageVector = Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Snooze (2m)", fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }
    }
  }
}
