package com.sleeplogger.app

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Serializable
@Entity(tableName = "sleep_entries")
data class SleepEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val sleepTime: String,
    val fallAsleepMin: String,
    val wakeTime: String,
    val totalSleep: String = "",
    val tags: List<String> = emptyList()
) {
    fun getTotalMinutes(): Int {
        if (totalSleep.isEmpty()) return 0
        return try {
            // Robust parsing: matches "7h 10m", "7h", "10m", "7 h 10 m" etc.
            val clean = totalSleep.lowercase().replace(" ", "")
            var total = 0
            if (clean.contains("h")) {
                val hPart = clean.substringBefore("h")
                total += hPart.toIntOrNull()?.let { it * 60 } ?: 0
            }
            if (clean.contains("m") && !clean.contains("min")) {
                val mPart = if (clean.contains("h")) clean.substringAfter("h").substringBefore("m") else clean.substringBefore("m")
                total += mPart.toIntOrNull() ?: 0
            } else if (clean.contains("min")) {
                // Fallback for cases where 'min' might be used instead of 'm'
                val mPart = if (clean.contains("h")) clean.substringAfter("h").substringBefore("min") else clean.substringBefore("min")
                total += mPart.toIntOrNull() ?: 0
            }
            total
        } catch (e: Exception) {
            0
        }
    }

    fun getBedtimeMinutes(): Int {
        return parseTimeToMinutes(sleepTime)
    }

    fun getWakeTimeMinutes(): Int {
        return parseTimeToMinutes(wakeTime)
    }

    fun getDateTime(): Date? {
        val formats = listOf("dd-MM-yyyy", "dd,MM,yyyy", "d-M-yy", "M-d-yy", "MM-dd-yyyy", "yyyy-MM-dd")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false
                val parsed = sdf.parse(date)
                if (parsed != null) return parsed
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    companion object {
        fun parseBulkEntry(line: String): SleepEntry? {
            try {
                val parts = line.split("-----")
                if (parts.size != 2) return null
                
                val datePart = parts[0].trim()
                val timePart = parts[1].trim()
                
                val timeSubParts = timePart.split("-------")
                if (timeSubParts.size != 2) return null
                
                val sleepPart = timeSubParts[0].trim()
                val wakeTime = timeSubParts[1].trim()
                
                val sleepSubParts = sleepPart.split("+")
                if (sleepSubParts.size != 2) return null
                
                val sleepTime = sleepSubParts[0].trim()
                val fallAsleepMin = sleepSubParts[1].trim()
                
                return SleepEntry(
                    date = datePart,
                    sleepTime = sleepTime,
                    fallAsleepMin = fallAsleepMin,
                    wakeTime = wakeTime,
                    totalSleep = calculateTotalSleep(sleepTime, fallAsleepMin, wakeTime)
                )
            } catch (e: Exception) {
                return null
            }
        }
        
        fun calculateTotalSleep(sleepTime: String, fallAsleepMin: String, wakeTime: String): String {
            try {
                val sleepMinutes = parseTimeToMinutes(sleepTime)
                val fallAsleepMinutes = parseDurationToMinutes(fallAsleepMin)
                val wakeMinutes = parseTimeToMinutes(wakeTime)
                
                val actualSleepStart = sleepMinutes + fallAsleepMinutes
                var totalMinutes = wakeMinutes - actualSleepStart
                
                if (totalMinutes < 0) {
                    totalMinutes += 24 * 60 // Handle overnight
                }
                
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                
                return "${hours}h ${minutes}m"
            } catch (e: Exception) {
                return "0h 0m"
            }
        }
        
        fun parseTimeToMinutes(timeStr: String): Int {
            try {
                val cleanTime = timeStr.lowercase().replace(" ", "")
                val isPM = cleanTime.contains("pm")
                val isAM = cleanTime.contains("am")
                
                val timeOnly = cleanTime.replace("am", "").replace("pm", "")
                val parts = timeOnly.split(":")
                
                var hours = parts[0].toInt()
                val minutes = if (parts.size > 1) parts[1].toInt() else 0
                
                if (isPM && hours != 12) hours += 12
                if (isAM && hours == 12) hours = 0
                
                return hours * 60 + minutes
            } catch (e: Exception) {
                return 0
            }
        }
        
        private fun parseDurationToMinutes(durationStr: String): Int {
            try {
                val clean = durationStr.lowercase().replace(" ", "").replace("+", "")
                return when {
                    clean.contains("min") -> clean.replace("min", "").toInt()
                    clean.contains("h") -> {
                        val parts = clean.split("h")
                        val hours = parts[0].toInt()
                        val minutesStr = if (parts.size > 1) parts[1].replace("min", "") else ""
                        val minutes = if (minutesStr.isNotEmpty()) minutesStr.toInt() else 0
                        hours * 60 + minutes
                    }
                    else -> clean.toIntOrNull() ?: 0
                }
            } catch (e: Exception) {
                return 0
            }
        }

        fun formatMinutesToTime(totalMinutes: Int): String {
            val hours = (totalMinutes / 60) % 24
            val minutes = totalMinutes % 60
            val amPm = if (hours >= 12) "pm" else "am"
            val displayHour = if (hours == 0) 12 else if (hours > 12) hours - 12 else hours
            return "$displayHour:${minutes.toString().padStart(2, '0')}$amPm"
        }

        fun formatMinutesToDuration(totalMinutes: Int): String {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return "${hours}h ${minutes}m"
        }
    }
}
