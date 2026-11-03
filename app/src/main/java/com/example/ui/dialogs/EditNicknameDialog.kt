package com.example.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.TrackedPeripheralEntity

@Composable
fun EditNicknameDialog(
  peripheral: TrackedPeripheralEntity,
  onDismiss: () -> Unit,
  onSave: (nickname: String?) -> Unit
) {
  var nickname by remember { mutableStateOf(peripheral.customNickname ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Edit Peripheral Nickname",
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Original Name: ${peripheral.deviceName}\nMAC: ${peripheral.deviceAddress}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
          value = nickname,
          onValueChange = { nickname = it },
          label = { Text("Custom Nickname") },
          placeholder = { Text("e.g. My Workout Buds") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("nickname_input_field")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(nickname.trim().ifEmpty { null })
          onDismiss()
        },
        modifier = Modifier.testTag("save_nickname_button")
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_nickname_button")
      ) {
        Text("Cancel")
      }
    }
  )
}
