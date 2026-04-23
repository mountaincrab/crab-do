package com.mountaincrab.crabdo.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Describes the recurrence pattern for a reminder. Time of day is stored separately
 * on the entity (reminderTime: String "HH:mm"), not here.
 *
 * Examples:
 *   Daily                    → type=DAILY, interval=1
 *   Every 3 days             → type=EVERY_N_DAYS, interval=3
 *   Every Monday             → type=WEEKLY, interval=1, daysOfWeek=[2]
 *   Mon+Wed+Fri              → type=WEEKLY, interval=1, daysOfWeek=[2,4,6]
 *   Every 3 weeks on Friday  → type=WEEKLY, interval=3, daysOfWeek=[6]
 *   Monthly on 1st           → type=MONTHLY, dayOfMonth=1
 *
 * daysOfWeek uses Calendar constants: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
 */
@Serializable
data class RecurrenceRule(
    val type: RecurrenceType,
    val interval: Int = 1,
    val daysOfWeek: List<Int> = emptyList(),
    val dayOfMonth: Int = 1
) {
    @Serializable
    enum class RecurrenceType {
        DAILY,
        WEEKLY,
        EVERY_N_DAYS,
        MONTHLY
    }

    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): RecurrenceRule = Json.decodeFromString(json)

        fun daily() = RecurrenceRule(RecurrenceType.DAILY)
        fun weekly(daysOfWeek: List<Int>, everyNWeeks: Int = 1) =
            RecurrenceRule(RecurrenceType.WEEKLY, interval = everyNWeeks, daysOfWeek = daysOfWeek)
        fun everyNDays(n: Int) = RecurrenceRule(RecurrenceType.EVERY_N_DAYS, interval = n)
        fun monthly(dayOfMonth: Int) =
            RecurrenceRule(RecurrenceType.MONTHLY, dayOfMonth = dayOfMonth.coerceIn(1, 28))
    }
}
