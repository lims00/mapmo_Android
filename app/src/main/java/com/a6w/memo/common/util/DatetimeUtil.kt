package com.a6w.memo.common.util

import android.text.format.DateUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Datetime Util
 * - Contains methods for date or time
 */
object DatetimeUtil {
    /**
     * Convert Long millis time to Date String for UI
     */
    fun getUiDateStringFromMillis(targetMillis: Long): String {
        // Current Time and Diff Time, One day in millis
        val currentTimeMillis = System.currentTimeMillis()
        val diffTimeMillis = currentTimeMillis - targetMillis
        val oneDayMillis = DateUtils.DAY_IN_MILLIS

        return if (diffTimeMillis in 1..<oneDayMillis) {
            // If diff is less than one day, return relative time
            DateUtils.getRelativeTimeSpanString(
                targetMillis,
                currentTimeMillis,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        } else {
            // If diff is more than one day, return formatted date (absolute)
            val instant = Instant.ofEpochMilli(targetMillis)
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("yyyy. MM. dd.", Locale.getDefault())
            dateTime.format(formatter)
        }
    }
}