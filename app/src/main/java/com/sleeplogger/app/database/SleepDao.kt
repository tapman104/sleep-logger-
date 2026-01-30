package com.sleeplogger.app.database

import androidx.room.*
import com.sleeplogger.app.SleepEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_entries ORDER BY id DESC")
    fun getAllEntries(): Flow<List<SleepEntry>>
    
    @Query("SELECT * FROM sleep_entries ORDER BY id DESC LIMIT 1")
    suspend fun getLatestEntrySync(): SleepEntry?
    
    @Query("SELECT * FROM sleep_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): SleepEntry?
    
    @Insert
    suspend fun insertEntry(entry: SleepEntry): Long
    
    @Insert
    suspend fun insertEntries(entries: List<SleepEntry>): List<Long>
    
    @Update
    suspend fun updateEntry(entry: SleepEntry)
    
    @Delete
    suspend fun deleteEntry(entry: SleepEntry)
    
    @Query("DELETE FROM sleep_entries")
    suspend fun deleteAllEntries()
}
