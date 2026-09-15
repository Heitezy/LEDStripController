package heitezy.ledstripcontroller.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import heitezy.ledstripcontroller.ble.LedPattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternSelector(
    selectedPattern: LedPattern,
    patternSpeedMs: Long,
    onPatternSelected: (LedPattern) -> Unit,
    onSpeedChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    // Local draft so the field stays editable while the user types
    var speedText by remember(patternSpeedMs) { mutableStateOf(patternSpeedMs.toString()) }
    val focusManager = LocalFocusManager.current

    fun commitSpeed() {
        val ms = speedText.toLongOrNull()?.coerceIn(10L, 5000L)
        if (ms != null) {
            onSpeedChanged(ms)
            speedText = ms.toString()
        } else {
            speedText = patternSpeedMs.toString()
        }
        focusManager.clearFocus()
    }

    Column(modifier = modifier) {
        Text(stringResource(heitezy.ledstripcontroller.R.string.label_pattern), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = stringResource(selectedPattern.displayNameRes),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                LedPattern.entries.forEach { pattern ->
                    DropdownMenuItem(
                        text = { Text(stringResource(pattern.displayNameRes)) },
                        onClick = {
                            onPatternSelected(pattern)
                            expanded = false
                        },
                        leadingIcon = if (pattern == selectedPattern) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else null,
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        AnimatedVisibility(visible = selectedPattern != LedPattern.SOLID) {
            Column {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = speedText,
                    onValueChange = { speedText = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(heitezy.ledstripcontroller.R.string.label_delay_ms)) },
                    supportingText = { Text(stringResource(heitezy.ledstripcontroller.R.string.label_delay_range_ms)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { commitSpeed() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}