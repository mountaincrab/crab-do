package com.mountaincrab.crabdo.domain

import com.mountaincrab.crabdo.data.model.RecurrenceRule
import java.util.Calendar

object RecurrenceEngine {

    fun nextTriggerAfter(rule: RecurrenceRule, afterMillis: Long, hour: Int, minute: Int): Long? {
        return when (rule.type) {
            RecurrenceRule.RecurrenceType.DAILY -> nextDaily(rule.interval, afterMillis, hour, minute)
            RecurrenceRule.RecurrenceType.EVERY_N_DAYS -> nextDaily(rule.interval, afterMillis, hour, minute)
            RecurrenceRule.RecurrenceType.WEEKLY -> nextWeekly(rule, afterMillis, hour, minute)
            RecurrenceRule.RecurrenceType.MONTHLY -> nextMonthly(rule.dayOfMonth, afterMillis, hour, minute)
        }
    }

    private fun nextDaily(intervalDays: Int, afterMillis: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = afterMillis }
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= afterMillis) {
            cal.add(Calendar.DAY_OF_YEAR, intervalDays)
        }
        return cal.timeInMillis
    }

    private fun nextWeekly(rule: RecurrenceRule, afterMillis: Long, hour: Int, minute: Int): Long? {
        if (rule.daysOfWeek.isEmpty()) return null
        val sortedDays = rule.daysOfWeek.sorted()
        val cal = Calendar.getInstance().apply { timeInMillis = afterMillis }
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        for (day in sortedDays) {
            if (day > currentDayOfWeek || (day == currentDayOfWeek && cal.timeInMillis > afterMillis)) {
                cal.add(Calendar.DAY_OF_YEAR, day - currentDayOfWeek)
                return cal.timeInMillis
            }
        }

        val daysUntilNextWeekFirstDay = (7 * rule.interval) - (currentDayOfWeek - sortedDays.first())
        cal.timeInMillis = afterMillis
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, daysUntilNextWeekFirstDay)
        return cal.timeInMillis
    }

    private fun nextMonthly(dayOfMonth: Int, afterMillis: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = afterMillis }
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= afterMillis) {
            cal.add(Calendar.MONTH, 1)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceAtMost(cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
        return cal.timeInMillis
    }

    fun describe(rule: RecurrenceRule, reminderTime: String): String {
        return when (rule.type) {
            RecurrenceRule.RecurrenceType.DAILY ->
                if (rule.interval == 1) "Every day at $reminderTime"
                else "Every ${rule.interval} days at $reminderTime"
            RecurrenceRule.RecurrenceType.EVERY_N_DAYS ->
                "Every ${rule.interval} days at $reminderTime"
            RecurrenceRule.RecurrenceType.WEEKLY -> {
                val days = rule.daysOfWeek.joinToString(", ") { dayName(it) }
                if (rule.interval == 1) "Every $days at $reminderTime"
                else "Every ${rule.interval} weeks on $days at $reminderTime"
            }
            RecurrenceRule.RecurrenceType.MONTHLY ->
                "Monthly on the ${ordinal(rule.dayOfMonth)} at $reminderTime"
        }
    }

    private fun dayName(calDay: Int): String = when (calDay) {
        Calendar.SUNDAY -> "Sun"
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        else -> "?"
    }

    private fun ordinal(n: Int): String = when {
        n in 11..13 -> "${n}th"
        n % 10 == 1 -> "${n}st"
        n % 10 == 2 -> "${n}nd"
        n % 10 == 3 -> "${n}rd"
        else -> "${n}th"
    }
}
