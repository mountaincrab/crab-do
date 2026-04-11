package com.mountaincrab.crabdo.domain

import com.mountaincrab.crabdo.data.model.RecurrenceRule
import java.util.Calendar

object RecurrenceEngine {

    fun nextTriggerAfter(rule: RecurrenceRule, afterMillis: Long): Long? {
        return when (rule.type) {
            RecurrenceRule.RecurrenceType.DAILY -> nextDaily(rule, afterMillis, rule.interval)
            RecurrenceRule.RecurrenceType.EVERY_N_DAYS -> nextDaily(rule, afterMillis, rule.interval)
            RecurrenceRule.RecurrenceType.WEEKLY -> nextWeekly(rule, afterMillis)
            RecurrenceRule.RecurrenceType.MONTHLY -> nextMonthly(rule, afterMillis)
        }
    }

    private fun nextDaily(rule: RecurrenceRule, afterMillis: Long, intervalDays: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = afterMillis }
        cal.set(Calendar.HOUR_OF_DAY, rule.hour)
        cal.set(Calendar.MINUTE, rule.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= afterMillis) {
            cal.add(Calendar.DAY_OF_YEAR, intervalDays)
        }
        return cal.timeInMillis
    }

    private fun nextWeekly(rule: RecurrenceRule, afterMillis: Long): Long? {
        if (rule.daysOfWeek.isEmpty()) return null
        val sortedDays = rule.daysOfWeek.sorted()
        val cal = Calendar.getInstance().apply { timeInMillis = afterMillis }
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        cal.set(Calendar.HOUR_OF_DAY, rule.hour)
        cal.set(Calendar.MINUTE, rule.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        for (day in sortedDays) {
            if (day > currentDayOfWeek || (day == currentDayOfWeek && cal.timeInMillis > afterMillis)) {
                val daysAhead = day - currentDayOfWeek
                cal.add(Calendar.DAY_OF_YEAR, daysAhead)
                return cal.timeInMillis
            }
        }

        val daysUntilNextWeekFirstDay = (7 * rule.interval) - (currentDayOfWeek - sortedDays.first())
        cal.timeInMillis = afterMillis
        cal.set(Calendar.HOUR_OF_DAY, rule.hour)
        cal.set(Calendar.MINUTE, rule.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, daysUntilNextWeekFirstDay)
        return cal.timeInMillis
    }

    private fun nextMonthly(rule: RecurrenceRule, afterMillis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = afterMillis }
        cal.set(Calendar.DAY_OF_MONTH, rule.dayOfMonth.coerceAtMost(
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)))
        cal.set(Calendar.HOUR_OF_DAY, rule.hour)
        cal.set(Calendar.MINUTE, rule.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= afterMillis) {
            cal.add(Calendar.MONTH, 1)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, rule.dayOfMonth.coerceAtMost(maxDay))
        }
        return cal.timeInMillis
    }

    fun describe(rule: RecurrenceRule): String {
        val timeStr = String.format("%02d:%02d", rule.hour, rule.minute)
        return when (rule.type) {
            RecurrenceRule.RecurrenceType.DAILY ->
                if (rule.interval == 1) "Every day at $timeStr"
                else "Every ${rule.interval} days at $timeStr"
            RecurrenceRule.RecurrenceType.EVERY_N_DAYS ->
                "Every ${rule.interval} days at $timeStr"
            RecurrenceRule.RecurrenceType.WEEKLY -> {
                val days = rule.daysOfWeek.joinToString(", ") { dayName(it) }
                if (rule.interval == 1) "Every $days at $timeStr"
                else "Every ${rule.interval} weeks on $days at $timeStr"
            }
            RecurrenceRule.RecurrenceType.MONTHLY ->
                "Monthly on the ${ordinal(rule.dayOfMonth)} at $timeStr"
        }
    }

    private fun dayName(calDay: Int): String = when (calDay) {
        Calendar.SUNDAY -> "Sunday"
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
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
