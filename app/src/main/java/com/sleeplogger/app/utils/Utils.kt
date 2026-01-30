package com.sleeplogger.app.utils

import com.sleeplogger.app.SleepEntry

fun parseBulkInput(text: String): List<SleepEntry> {
    return text.lines().mapNotNull { line ->
        if (line.isBlank()) return@mapNotNull null
        try {
            val parts = line.split("-----")
            if (parts.size != 2) return@mapNotNull null

            val datePart = parts[0].trim()
            val timePart = parts[1].trim()

            val timeSubParts = timePart.split("-------")
            if (timeSubParts.size != 2) return@mapNotNull null

            val sleepPart = timeSubParts[0].trim()
            val wakeTime = timeSubParts[1].trim()

            val sleepSubParts = sleepPart.split("+")
            if (sleepSubParts.size != 2) return@mapNotNull null

            val sleepTime = sleepSubParts[0].trim()
            val fallAsleepMin = sleepSubParts[1].trim()

            SleepEntry(
                date = datePart,
                sleepTime = sleepTime,
                fallAsleepMin = fallAsleepMin,
                wakeTime = wakeTime
            )
        } catch (e: Exception) {
            null
        }
    }
}
