package com.sleeplogger.app.repository

import com.sleeplogger.app.SleepEntry
import com.sleeplogger.app.database.SleepDao
import kotlinx.coroutines.flow.Flow

class SleepRepository(
    private val sleepDao: SleepDao
) {
    fun getAllEntries(): Flow<List<SleepEntry>> = sleepDao.getAllEntries()
    
    suspend fun getEntryById(id: Long): SleepEntry? = sleepDao.getEntryById(id)
    
    suspend fun insertEntry(entry: SleepEntry): Long {
        val entryWithTotalSleep = entry.copy(
            totalSleep = SleepEntry.calculateTotalSleep(
                entry.sleepTime,
                entry.fallAsleepMin,
                entry.wakeTime
            )
        )
        return sleepDao.insertEntry(entryWithTotalSleep)
    }
    
    suspend fun insertEntries(entries: List<SleepEntry>): List<Long> {
        val entriesWithTotalSleep = entries.map { entry ->
            entry.copy(
                totalSleep = SleepEntry.calculateTotalSleep(
                    entry.sleepTime,
                    entry.fallAsleepMin,
                    entry.wakeTime
                )
            )
        }
        return sleepDao.insertEntries(entriesWithTotalSleep)
    }
    
    suspend fun updateEntry(entry: SleepEntry) {
        val entryWithTotalSleep = entry.copy(
            totalSleep = SleepEntry.calculateTotalSleep(
                entry.sleepTime,
                entry.fallAsleepMin,
                entry.wakeTime
            )
        )
        sleepDao.updateEntry(entryWithTotalSleep)
    }
    
    suspend fun deleteEntry(entry: SleepEntry) = sleepDao.deleteEntry(entry)
    
    suspend fun deleteAllEntries() = sleepDao.deleteAllEntries()
}
