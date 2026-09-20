package app.orionmd.management.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateTimeUtils {

    fun monthYear(date: LocalDate, locale: Locale = Locale.US): String =
        date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))

    fun fullDate(date: LocalDate, locale: Locale = Locale.US): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", locale))

    fun shortDate(date: LocalDate, locale: Locale = Locale.US): String =
        date.format(DateTimeFormatter.ofPattern("MMM d", locale))

    fun mdy(date: LocalDate, locale: Locale = Locale.US): String =
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale))

    fun dayOfWeekLabel(date: LocalDate, locale: Locale = Locale.US): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

    /** Minutes since midnight -> "h:mm AM/PM" (localized). */
    fun formatMinute(minute: Int, locale: Locale = Locale.US): String {
        val time = LocalTime.of(minute / 60, minute % 60)
        return time.format(DateTimeFormatter.ofPattern("h:mm a", locale))
    }

    /** Total minutes -> "Xh Ym" (e.g. "3h 45m", "45m", "2h", "0m"). Used for Phone Time totals. */
    fun formatDuration(totalMinutes: Long): String {
        val safe = totalMinutes.coerceAtLeast(0)
        val hours = safe / 60
        val minutes = safe % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    fun timeRangeLabel(
        startMinute: Int?,
        endMinute: Int?,
        notApplicable: Boolean,
        notApplicableLabel: String = "Not Applicable",
        dash: String = "—",
        locale: Locale = Locale.US
    ): String {
        if (notApplicable) return notApplicableLabel
        if (startMinute == null) return dash
        val start = formatMinute(startMinute, locale)
        val end = endMinute?.let { formatMinute(it, locale) }
        return if (end != null) "$start – $end" else start
    }

    /** The Sunday that starts the week containing [date] (weeks run Sun-Sat per project spec). */
    fun weekStart(date: LocalDate): LocalDate {
        val daysFromSunday = (date.dayOfWeek.value % 7) // Mon=1..Sun=7 -> Sun=0
        return date.minusDays(daysFromSunday.toLong())
    }

    fun weekEnd(date: LocalDate): LocalDate = weekStart(date).plusDays(6)

    fun weekRangeLabel(weekStart: LocalDate, locale: Locale = Locale.US): String {
        val end = weekStart.plusDays(6)
        val shortFormatter = DateTimeFormatter.ofPattern("MMM d", locale)
        return if (weekStart.month == end.month) {
            "${weekStart.format(shortFormatter)} – ${end.dayOfMonth}, ${end.year}"
        } else {
            "${weekStart.format(shortFormatter)} – ${end.format(shortFormatter)}, ${end.year}"
        }
    }

    /** First day shown on a month grid (the Sunday on/before the 1st). */
    fun monthGridStart(monthAnchor: LocalDate): LocalDate {
        val firstOfMonth = monthAnchor.withDayOfMonth(1)
        return weekStart(firstOfMonth)
    }

    /** Last day shown on a month grid (the Saturday on/after the last day of the month). */
    fun monthGridEnd(monthAnchor: LocalDate): LocalDate {
        val lastOfMonth = monthAnchor.withDayOfMonth(monthAnchor.lengthOfMonth())
        return weekEnd(lastOfMonth)
    }
}
