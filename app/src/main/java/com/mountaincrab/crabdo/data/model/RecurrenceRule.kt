package com.mountaincrab.crabdo.data.model

import com.google.gson.Gson

/**
 * Represents a recurrence rule for a reminder.
 * Serialized to/from JSON for storage in Room.
 *
 * Examples:
 *   Daily at 09:00           → type=DAILY, interval=1, hour=9, minute=0
 *   Every 3 days at 08:00    → type=EVERY_N_DAYS, interval=3, hour=8, minute=0
 *   Every Monday at 10:00    → type=WEEKLY, interval=1, daysOfWeek=[2], hour=10, minute=0
 *   Mon+Wed+Fri at 07:00     → type=WEEKLY, interval=1, daysOfWeek=[2,4,6], hour=7, minute=0
 *   Every 3 weeks on Friday  → type=WEEKLY, interval=3, daysOfWeek=[6], hour=9, minute=0
 *   Monthly on 1st at 09:00  → type=MONTHLY, dayOfMonth=1, hour=9, minute=0
 *
 * daysOfWeek uses Calendar constants: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
 */
data class RecurrenceRule(
    val type: RecurrenceType,
    val interval: Int = 1,
    val daysOfWeek: List<Int> = emptyList(),
    val dayOfMonth: Int = 1,
    val hour: Int,
    val minute: Int
) {
    enum class RecurrenceType {
        DAILY,
        WEEKLY,
        EVERY_N_DAYS,
        MONTHLY
    }

    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): RecurrenceRule = Gson().fromJson(json, RecurrenceRule::class.java)

        fun daily(hour: Int, minute: Int) =
            RecurrenceRule(RecurrenceType.DAILY, interval = 1, hour = hour, minute = minute)

        fun weekly(daysOfWeek: List<Int>, hour: Int, minute: Int, everyNWeeks: Int = 1) =
            RecurrenceRule(RecurrenceType.WEEKLY, interval = everyNWeeks,
                daysOfWeek = daysOfWeek, hour = hour, minute = minute)

        fun everyNDays(n: Int, hour: Int, minute: Int) =
            RecurrenceRule(RecurrenceType.EVERY_N_DAYS, interval = n, hour = hour, minute = minute)

        fun monthly(dayOfMonth: Int, hour: Int, minute: Int) =
            RecurrenceRule(RecurrenceType.MONTHLY, dayOfMonth = dayOfMonth.coerceIn(1, 28),
                hour = hour, minute = minute)
    }
}
