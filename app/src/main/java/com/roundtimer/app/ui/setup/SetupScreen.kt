package com.roundtimer.app.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roundtimer.app.R
import com.roundtimer.app.timer.TimerCommands
import com.roundtimer.app.timer.TimerStateHolder
import java.util.Locale

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onStartTimer: () -> Unit,
    onOpenPresets: () -> Unit,
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val timerState by TimerStateHolder.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }

    // Recarrega a config sempre que a tela volta ao primeiro plano.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            // Cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                )
                IconButton(onClick = onOpenPresets) {
                    Icon(Icons.Default.List, contentDescription = stringResource(R.string.presets_title))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Atalho para voltar ao timer rodando em 2º plano
            if (timerState.running) {
                OutlinedButton(
                    onClick = onStartTimer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.timer_running_return),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            ConfigStepper(
                label = stringResource(R.string.config_rounds),
                value = "${config.rounds}",
                onDecrease = { viewModel.updateConfig { it.copy(rounds = (it.rounds - 1).coerceAtLeast(1)) } },
                onIncrease = { viewModel.updateConfig { it.copy(rounds = (it.rounds + 1).coerceAtMost(99)) } },
            )
            Spacer(Modifier.height(12.dp))
            ConfigStepper(
                label = stringResource(R.string.config_round_duration),
                value = formatMmSs(config.roundDurationSec),
                onDecrease = { viewModel.updateConfig { it.copy(roundDurationSec = (it.roundDurationSec - 5).coerceAtLeast(5)) } },
                onIncrease = { viewModel.updateConfig { it.copy(roundDurationSec = (it.roundDurationSec + 5).coerceAtMost(3600)) } },
            )
            Spacer(Modifier.height(12.dp))
            ConfigStepper(
                label = stringResource(R.string.config_rest_duration),
                value = formatMmSs(config.restDurationSec),
                onDecrease = { viewModel.updateConfig { it.copy(restDurationSec = (it.restDurationSec - 5).coerceAtLeast(0)) } },
                onIncrease = { viewModel.updateConfig { it.copy(restDurationSec = (it.restDurationSec + 5).coerceAtMost(1800)) } },
            )
            Spacer(Modifier.height(12.dp))
            ConfigStepper(
                label = stringResource(R.string.config_prepare_duration),
                value = formatMmSs(config.prepareDurationSec),
                onDecrease = { viewModel.updateConfig { it.copy(prepareDurationSec = (it.prepareDurationSec - 5).coerceAtLeast(0)) } },
                onIncrease = { viewModel.updateConfig { it.copy(prepareDurationSec = (it.prepareDurationSec + 5).coerceAtMost(300)) } },
            )

            Spacer(Modifier.height(20.dp))

            // Alternância de som / vibração
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ToggleChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.settings_sound),
                    active = soundEnabled,
                    onToggle = { viewModel.setSound(!soundEnabled) },
                )
                ToggleChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.settings_vibration),
                    active = vibrationEnabled,
                    onToggle = { viewModel.setVibration(!vibrationEnabled) },
                )
            }

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = { showSaveDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.height(18.dp))
                Spacer(Modifier.padding(start = 8.dp))
                Text(stringResource(R.string.action_save_preset))
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.persistConfig()
                    TimerCommands.start(context, config)
                    onStartTimer()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            ) {
                Text(
                    text = stringResource(R.string.action_start),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                viewModel.savePreset(name)
                showSaveDialog = false
            },
        )
    }
}

@Composable
private fun ConfigStepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            FilledIconButton(
                onClick = onDecrease,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.action_decrease), tint = Color.White)
            }
            FilledIconButton(
                onClick = onIncrease,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_increase), tint = Color.White)
            }
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    active: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (active) Icons.Default.Notifications else Icons.Default.Refresh,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(start = 6.dp))
            Text(
                text = label,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SavePresetDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.preset_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.preset_dialog_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

fun formatMmSs(totalSeconds: Int): String =
    String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
