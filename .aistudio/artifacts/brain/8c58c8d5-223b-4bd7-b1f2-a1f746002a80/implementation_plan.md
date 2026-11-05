# Per-Device Alerts & Anti-Lost Alarm System

Provide per-device alert customization, global notification defaults (opt-out vs. opt-in), and an unmissable persistent Anti-Lost alarm system with continuous sound, vibration, 2-minute snooze, and explicit button dismissal to prevent leaving peripherals behind.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following product decisions were confirmed during the planning interview:
> - **Global Default Policy**: Alert by default for all devices (opt-out per device), with a global setting to switch to opt-in mode.
> - **Anti-Lost Alarm Behavior**: Continuous looping alarm sound and vibration that only ceases when the user taps "Dismiss" or "Snooze (2 min)".
> - **Alarm Trigger Flexibility**: Configurable per device to trigger on disconnect (primary use-case for leaving items behind), on connect, or both.

---

### 1. Overview & Core Concept

When a user walks away from their headphones, smartwatch, or accessory, Bluetooth signal drops and an `ACL_DISCONNECTED` event occurs. Standard Android notifications can easily be missed in a pocket or bag. 

**BlueTrack Anti-Lost** introduces:
1. **Per-Device Alert Rule Engine**: Each device can follow global defaults, be strictly muted, or be designated as a protected **Critical Anti-Lost Peripheral**.
2. **Global Default Switch**: In Settings, users choose whether newly detected peripherals alert by default (opt-out) or remain silent unless explicitly configured (opt-in).
3. **Continuous Anti-Lost Alarm**: A high-priority audible alarm using the Android `USAGE_ALARM` audio stream, continuous pulsing vibration, and full-screen heads-up notification with action buttons:
   - **Dismiss**: Stops the alarm immediately.
   - **Snooze (2 min)**: Silences the alarm for 2 minutes and re-triggers if the device has not reconnected.

---

### 2. User Experience & Visual Design

#### Key User Flows

1. **Configuring a Critical Peripheral**:
   - In the **Peripherals** tab, tap any accessory (e.g., "AirPods Pro").
   - Open the **Device Alert & Anti-Lost Settings** sheet.
   - Enable **"🛡️ Anti-Lost Protection (Never Leave Behind)"**.
   - Select triggers: **On Disconnect** (default checked) and/or **On Connect**.
   - Tap **"Test Alarm"** to hear the exact sound and verify dismissal.

2. **Leaving an Item Behind (Real-World Trigger)**:
   - The user steps away from their headphones. The Bluetooth link disconnects.
   - The phone immediately initiates a continuous alarm ringtone with strong vibration.
   - A heads-up notification appears:
     - **Title**: `🚨 ANTI-LOST ALERT: AirPods Pro Left Behind!`
     - **Subtitle**: `Device disconnected. Tap Dismiss or Snooze.`
     - **Buttons**: `[ Dismiss ]` and `[ Snooze 2m ]`
   - If the user opens the app, a prominent **Active Alarm Banner** pulses at the top with large Dismiss and Snooze buttons.

3. **Global Policy Setting**:
   - In the **Settings** tab, under **Alert Policy**, toggle **"Alert by Default for All Devices"**.
   - When switched to OFF, untracked devices stay quiet, alerting only for explicitly marked peripherals.

---

### 3. Key Product Decisions & Trade-Offs

- **Audio Stream**: Use `AudioAttributes.USAGE_ALARM` (or `USAGE_NOTIFICATION_RINGTONE`) with looping `RingtoneManager.TYPE_ALARM` so that critical separation alerts cut through background noise and low media volume settings.
- **Alarm Dismissal Guarantee**: Standard notifications disappear on swipe, which would risk missing a separation alert. The anti-lost alarm notification is marked `setOngoing(true)` with non-dismissible flags until the user clicks **Dismiss** or **Snooze**.
- **Snooze Architecture**: Uses Android `AlarmManager` (or coroutine delay in background service) with a precise 2-minute timer that re-checks if the peripheral has re-connected before sounding again.
- **Database Schema Migration**: Update `TrackedPeripheralEntity` in Room with `alertMode`, `isAntiLostEnabled`, `alarmOnDisconnect`, and `alarmOnConnect` using `fallbackToDestructiveMigration` or version 2 migration.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   Bluetooth Events                     │
│        (ACL_CONNECTED / ACL_DISCONNECTED)              │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│              BluetoothTrackerRepository                │
│  - Checks Global Default Policy (Opt-In vs Opt-Out)    │
│  - Queries TrackedPeripheralEntity configuration       │
└──────────┬─────────────────────────────────┬───────────┘
           │ (Standard Alert)                │ (Anti-Lost Alert)
           ▼                                 ▼
┌──────────────────────────┐   ┌─────────────────────────┐
│    NotificationHelper    │   │   AntiLostAlarmService  │
│ - Standard Notification  │   │ - Looping Ringtone      │
│ - Vibration (1 pulse)    │   │ - Continuous Vibration  │
└──────────────────────────┘   │ - Ongoing Notification  │
                               │ - [Dismiss] [Snooze 2m] │
                               └─────────────┬───────────┘
                                             │
                                             ▼
                               ┌─────────────────────────┐
                               │       AlarmManager      │
                               │ - 2-Minute Snooze Timer │
                               └─────────────────────────┘
```

#### Data Model Extensions

**`TrackedPeripheralEntity`**:
- `alertMode: String = "DEFAULT"` (`"DEFAULT"`, `"ALWAYS_ALERT"`, `"MUTED"`)
- `isAntiLostEnabled: Boolean = false`
- `alarmOnDisconnect: Boolean = true`
- `alarmOnConnect: Boolean = false`

**`PreferencesManager`**:
- `globalAlertByDefault: StateFlow<Boolean>` (default: `true`)
- `antiLostAlarmVolume: StateFlow<Float>` (default: `1.0f`)

#### Components to Build / Modify

1. **`service/AntiLostAlarmService.kt`**:
   - Manages audio loop playback (`Ringtone` / `MediaPlayer`) with `USAGE_ALARM`.
   - Manages repeating vibration pattern.
   - Posts ongoing heads-up notification with action intents for `ACTION_DISMISS` and `ACTION_SNOOZE`.
   - Tracks active alarm state in a singleton `StateFlow` so the UI can show a live dismiss banner.
2. **`receiver/AlarmActionReceiver.kt`**:
   - Handles `ACTION_DISMISS` and `ACTION_SNOOZE` broadcasts from notification buttons.
   - If Snooze is tapped, schedules re-trigger in 120,000ms.
3. **`data/model/TrackedPeripheralEntity.kt` & `AppDatabase.kt`**:
   - Add anti-lost and alert mode properties.
4. **`ui/dialogs/DeviceAlarmConfigDialog.kt`**:
   - Per-device configuration modal with anti-lost switch, disconnect/connect checkboxes, and live alarm sound test.
5. **`ui/screens/SettingsScreen.kt`**:
   - Global default notification policy switch (Opt-out vs. Opt-in).
6. **`ui/screens/HomeScreen.kt` & `PeripheralsScreen.kt`**:
   - Anti-lost shield indicators and active ringing banner with one-tap dismiss.
