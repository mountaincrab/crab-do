package com.mountaincrab.crabdo.ui.reminders.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mountaincrab.crabdo.data.model.RecurrenceRule
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrencePicker(
    rule: RecurrenceRule?,
    onRuleChanged: (RecurrenceRule) -> Unit,
    modifier: Modifier = Modifier
) {
    var intervalStr by remember { mutableStateOf(rule?.interval?.toString() ?: "1") }
    var periodIndex by remember {
        mutableStateOf(
            when (rule?.type) {
                RecurrenceRule.RecurrenceType.MONTHLY -> 2
                RecurrenceRule.RecurrenceType.WEEKLY -> 1
                else -> 0
            }
        )
    }
    var selectedDays by remember { mutableStateOf(rule?.daysOfWeek?.toSet() ?: emptySet<Int>()) }
    var dayOfMonthStr by remember { mutableStateOf(rule?.dayOfMonth?.toString() ?: "1") }

    val periods = listOf("days", "weeks", "months")
    val dayLabels = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    val calDays = listOf(
        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
        Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    fun emitRule() {
        val interval = intervalStr.toIntOrNull()?.coerceIn(1, 99) ?: 1
        val newRule = when (periodIndex) {
            0 -> if (interval == 1) RecurrenceRule.daily() else RecurrenceRule.everyNDays(interval)
            1 -> RecurrenceRule.weekly(
                daysOfWeek = if (selectedDays.isEmpty()) listOf(Calendar.MONDAY) else selectedDays.toList(),
                everyNWeeks = interval
            )
            2 -> RecurrenceRule.monthly(dayOfMonthStr.toIntOrNull()?.coerceIn(1, 28) ?: 1)
            else -> RecurrenceRule.daily()
        }
        onRuleChanged(newRule)
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Every", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = intervalStr,
                    onValueChange = { intervalStr = it; emitRule() },
                    modifier = Modifier.width(64.dp),
                    singleLine = true
                )
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = periods[periodIndex],
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().width(100.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        periods.forEachIndexed { idx, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { periodIndex = idx; expanded = false; emitRule() }
                            )
                        }
                    }
                }
            }

            if (periodIndex == 1) {
                Text("On:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayLabels.forEachIndexed { idx, label ->
                        val calDay = calDays[idx]
                        FilterChip(
                            selected = calDay in selectedDays,
                            onClick = {
                                selectedDays = if (calDay in selectedDays) selectedDays - calDay else selectedDays + calDay
                                emitRule()
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (periodIndex == 2) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Day of month:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = dayOfMonthStr,
                        onValueChange = { dayOfMonthStr = it; emitRule() },
                        modifier = Modifier.width(64.dp),
                        singleLine = true
                    )
                }
            }
        }
    }
}
