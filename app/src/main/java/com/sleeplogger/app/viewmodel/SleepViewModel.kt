package com.sleeplogger.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sleeplogger.app.SleepEntry
import com.sleeplogger.app.repository.SleepRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class SortCriteria { DATE, DURATION, BEDTIME, WAKETIME }
enum class SortOrder { ASCENDING, DESCENDING }
enum class DatePreset { ALL, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH }
enum class SleepMetric { DURATION, BEDTIME, WAKETIME, CONSISTENCY }
enum class GraphViewMode { BAR, LINE, RANGE, DISTRIBUTION }

data class SleepFilters(
    val query: String = "",
    val startDate: Date? = null,
    val endDate: Date? = null,
    val minDuration: Int? = null,
    val maxDuration: Int? = null,
    val sortCriteria: SortCriteria = SortCriteria.DATE,
    val sortOrder: SortOrder = SortOrder.DESCENDING
)

data class SleepStats(
    val avgDuration7Days: String = "0h 0m",
    val avgDuration30Days: String = "0h 0m",
    val avgDurationAllTime: String = "0h 0m",
    val avgBedtime: String = "--:--",
    val avgWakeTime: String = "--:--",
    val consistencyScore: Int = 0,
    val bestNight: SleepEntry? = null,
    val worstNight: SleepEntry? = null,
    val currentViewData: List<ChartPoint> = emptyList(),
    val distributionData: List<ChartPoint> = emptyList(),
    val contextLabel: String = "",
    val totalAvailableDays: Int = 0
)

data class ChartPoint(
    val label: String,
    val value: Float,
    val date: String,
    val rawEntry: SleepEntry? = null,
    val minValue: Float? = null,
    val maxValue: Float? = null
)

data class GroupedSummary(
    val title: String,
    val avgDuration: String,
    val entriesCount: Int,
    val totalMinutes: Int,
    val sortDate: Date
)

class SleepViewModel(private val repository: SleepRepository) : ViewModel() {

    private val _entries = MutableStateFlow<List<SleepEntry>>(emptyList())
    private val _filters = MutableStateFlow(SleepFilters())
    val filters = _filters.asStateFlow()

    private val _selectedMetric = MutableStateFlow(SleepMetric.DURATION)
    val selectedMetric = _selectedMetric.asStateFlow()

    private val _graphMode = MutableStateFlow(GraphViewMode.BAR)
    val graphMode = _graphMode.asStateFlow()

    private val _viewPageOffset = MutableStateFlow(0) 
    val viewPageOffset = _viewPageOffset.asStateFlow()

    private val _inspectedDateIndex = MutableStateFlow(0)
    val inspectedDateIndex = _inspectedDateIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    val searchQuery: StateFlow<String> = _filters
        .map { it.query }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val allEntries: StateFlow<List<SleepEntry>> = combine(
        _entries, 
        _filters
    ) { entries: List<SleepEntry>, filters: SleepFilters ->
        var filtered = entries
        
        if (filters.query.isNotBlank()) {
            filtered = filtered.filter {
                it.date.contains(filters.query, ignoreCase = true) ||
                it.sleepTime.contains(filters.query, ignoreCase = true) ||
                it.wakeTime.contains(filters.query, ignoreCase = true) ||
                it.totalSleep.contains(filters.query, ignoreCase = true)
            }
        }
        
        if (filters.startDate != null || filters.endDate != null) {
            filtered = filtered.filter { entry ->
                val entryDate = entry.getDateTime() ?: return@filter false
                val afterStart = filters.startDate?.let { !entryDate.before(it) } ?: true
                val beforeEnd = filters.endDate?.let { !entryDate.after(it) } ?: true
                afterStart && beforeEnd
            }
        }
        
        if (filters.minDuration != null) {
            filtered = filtered.filter { it.getTotalMinutes() >= filters.minDuration }
        }
        if (filters.maxDuration != null) {
            filtered = filtered.filter { it.getTotalMinutes() <= filters.maxDuration }
        }
        
        val sorted = when (filters.sortCriteria) {
            SortCriteria.DATE -> filtered.sortedBy { it.getDateTime() ?: Date(0) }
            SortCriteria.DURATION -> filtered.sortedBy { it.getTotalMinutes() }
            SortCriteria.BEDTIME -> filtered.sortedBy { 
                var mins = it.getBedtimeMinutes()
                if (mins < 720) mins += 1440 
                mins
            }
            SortCriteria.WAKETIME -> filtered.sortedBy { it.getWakeTimeMinutes() }
        }
        
        if (filters.sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<SleepStats> = combine(_entries, _viewPageOffset, _selectedMetric) { entries, offset, metric ->
        calculateStats(entries, offset, metric)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SleepStats()
    )

    val weeklySummaries: StateFlow<List<GroupedSummary>> = _entries.map { entries ->
        groupEntriesByPeriod(entries, Calendar.WEEK_OF_YEAR)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySummaries: StateFlow<List<GroupedSummary>> = _entries.map { entries ->
        groupEntriesByPeriod(entries, Calendar.MONTH)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            repository.getAllEntries().collect { entryList ->
                _entries.value = entryList
            }
        }
    }

    private fun groupEntriesByPeriod(entries: List<SleepEntry>, periodField: Int): List<GroupedSummary> {
        val calendar = Calendar.getInstance()
        return entries.groupBy {
            val date = it.getDateTime() ?: return@groupBy "Unknown"
            calendar.time = date
            if (periodField == Calendar.WEEK_OF_YEAR) {
                "Week ${calendar.get(Calendar.WEEK_OF_YEAR)}, ${calendar.get(Calendar.YEAR)}"
            } else {
                val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US)
                "$month ${calendar.get(Calendar.YEAR)}"
            }
        }.mapNotNull { (title, periodEntries) ->
            if (title == "Unknown") return@mapNotNull null
            val totalMins = periodEntries.sumOf { it.getTotalMinutes() }
            val avg = if (periodEntries.isNotEmpty()) totalMins / periodEntries.size else 0
            val sortDate = periodEntries.mapNotNull { it.getDateTime() }.minOrNull() ?: Date(0)
            GroupedSummary(
                title = title,
                avgDuration = SleepEntry.formatMinutesToDuration(avg),
                entriesCount = periodEntries.size,
                totalMinutes = totalMins,
                sortDate = sortDate
            )
        }.sortedByDescending { it.sortDate }
    }

    private fun calculateStats(entries: List<SleepEntry>, pageOffset: Int, metric: SleepMetric): SleepStats {
        if (entries.isEmpty()) return SleepStats()

        val sortedEntries = entries.sortedBy { it.getDateTime() ?: Date(0) }
        val calendar = Calendar.getInstance()
        
        val totalDays = sortedEntries.size
        val endIdx = (totalDays - (pageOffset * 7)).coerceIn(0, totalDays)
        val startIdx = (endIdx - 7).coerceIn(0, totalDays)
        val currentWindowEntries = sortedEntries.subList(startIdx, endIdx)

        val now = calendar.time
        val sevenDaysAgo = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }.time
        val thirtyDaysAgo = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -30) }.time

        val last7DaysEntries = sortedEntries.filter { it.getDateTime()?.after(sevenDaysAgo) == true }
        val last30DaysEntries = sortedEntries.filter { it.getDateTime()?.after(thirtyDaysAgo) == true }

        val avg7 = if (last7DaysEntries.isNotEmpty()) last7DaysEntries.map { it.getTotalMinutes() }.average().toInt() else 0
        val avg30 = if (last30DaysEntries.isNotEmpty()) last30DaysEntries.map { it.getTotalMinutes() }.average().toInt() else 0
        val avgAll = entries.map { it.getTotalMinutes() }.average().toInt()

        val bedtimeList = entries.map { 
            var mins = it.getBedtimeMinutes()
            if (mins < 720) mins += 1440
            mins
        }
        val avgBedtimeMinutes = if (bedtimeList.isNotEmpty()) bedtimeList.average().toInt() % 1440 else 0
        val avgWakeMinutes = if (entries.isNotEmpty()) entries.map { it.getWakeTimeMinutes() }.average().toInt() else 0

        var totalVariance = 0.0
        if (bedtimeList.size > 1) {
            val avg = bedtimeList.average()
            totalVariance = bedtimeList.sumOf { abs(it.toDouble() - avg) } / bedtimeList.size
        }
        val consistency = (100 - (totalVariance.toInt() / 2)).coerceIn(0, 100)

        val bestNight = entries.maxByOrNull { it.getTotalMinutes() }
        val worstNight = entries.filter { it.getTotalMinutes() > 0 }.minByOrNull { it.getTotalMinutes() }

        val currentViewData = currentWindowEntries.map { entry ->
            val value = when(metric) {
                SleepMetric.DURATION -> entry.getTotalMinutes().toFloat() / 60f
                SleepMetric.BEDTIME -> {
                    var mins = entry.getBedtimeMinutes().toFloat()
                    if (mins < 720) mins += 1440
                    mins / 60f
                }
                SleepMetric.WAKETIME -> entry.getWakeTimeMinutes().toFloat() / 60f
                SleepMetric.CONSISTENCY -> {
                    val avgBed = bedtimeList.average()
                    var mins = entry.getBedtimeMinutes().toDouble()
                    if (mins < 720) mins += 1440
                    val score = (100f - abs(mins - avgBed).toFloat()).coerceAtLeast(0f)
                    score
                }
            }
            ChartPoint(getShortDayName(entry.getDateTime()), value, entry.date, entry)
        }

        // Distribution Data
        val distribution = entries.groupBy { (it.getTotalMinutes() / 60) }
            .map { (hours, list) -> 
                ChartPoint("${hours}h", list.size.toFloat(), "N/A")
            }.sortedBy { it.label }

        val weekendSleep = entries.filter { 
            it.getDateTime()?.let { d ->
                calendar.time = d
                val day = calendar.get(Calendar.DAY_OF_WEEK)
                day == Calendar.SATURDAY || day == Calendar.SUNDAY
            } ?: false
        }.map { it.getTotalMinutes() }.average()

        val weekdaySleep = entries.filter { 
            it.getDateTime()?.let { d ->
                calendar.time = d
                val day = calendar.get(Calendar.DAY_OF_WEEK)
                day != Calendar.SATURDAY && day != Calendar.SUNDAY
            } ?: false
        }.map { it.getTotalMinutes() }.average()

        val contextLabel = when {
            entries.isEmpty() -> ""
            weekendSleep > weekdaySleep + 30 -> "You sleep best on weekends"
            weekdaySleep > weekendSleep + 30 -> "You sleep better on weekdays"
            else -> "Your sleep is consistent across the week"
        }

        return SleepStats(
            avgDuration7Days = SleepEntry.formatMinutesToDuration(avg7),
            avgDuration30Days = SleepEntry.formatMinutesToDuration(avg30),
            avgDurationAllTime = SleepEntry.formatMinutesToDuration(avgAll),
            avgBedtime = SleepEntry.formatMinutesToTime(avgBedtimeMinutes),
            avgWakeTime = SleepEntry.formatMinutesToTime(avgWakeMinutes),
            consistencyScore = consistency,
            bestNight = bestNight,
            worstNight = worstNight,
            currentViewData = currentViewData,
            distributionData = distribution,
            contextLabel = contextLabel,
            totalAvailableDays = totalDays
        )
    }

    private fun getShortDayName(date: Date?): String {
        if (date == null) return ""
        return SimpleDateFormat("EEE", Locale.US).format(date)
    }

    fun setMetric(metric: SleepMetric) {
        _selectedMetric.value = metric
        _inspectedDateIndex.value = 0
    }

    fun setGraphMode(mode: GraphViewMode) {
        _graphMode.value = mode
    }

    fun setInspectedIndex(index: Int) {
        _inspectedDateIndex.value = index
    }

    fun navigatePage(delta: Int) {
        val totalDays = _entries.value.size
        if (totalDays == 0) return
        val maxOffset = (totalDays - 1) / 7
        val newOffset = (_viewPageOffset.value + delta).coerceIn(0, maxOffset)
        _viewPageOffset.value = newOffset
        _inspectedDateIndex.value = 0
    }

    fun updateFilters(newFilters: SleepFilters) {
        _filters.value = newFilters
    }

    fun setSearchQuery(query: String) {
        _filters.value = _filters.value.copy(query = query)
    }

    fun clearFilters() {
        _filters.value = SleepFilters()
    }

    fun addEntry(date: String, sleepTime: String, fallAsleepMin: String, wakeTime: String) {
        viewModelScope.launch {
            try {
                val totalSleep = SleepEntry.calculateTotalSleep(sleepTime, fallAsleepMin, wakeTime)
                val newEntry = SleepEntry(
                    date = date, 
                    sleepTime = sleepTime, 
                    fallAsleepMin = fallAsleepMin, 
                    wakeTime = wakeTime,
                    totalSleep = totalSleep
                )
                repository.insertEntry(newEntry)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save entry: ${e.localizedMessage}"
            }
        }
    }

    fun addBulkEntries(entries: List<SleepEntry>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.insertEntries(entries)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to import entries: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportToJson(): String {
        return try {
            json.encodeToString(_entries.value.map { it.copy(id = 0) })
        } catch (e: Exception) {
            _errorMessage.value = "Export failed: ${e.localizedMessage}"
            ""
        }
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val entries = json.decodeFromString<List<SleepEntry>>(jsonString)
            if (entries.isNotEmpty()) {
                val validatedEntries = entries.map { entry ->
                    val calculatedTotal = SleepEntry.calculateTotalSleep(entry.sleepTime, entry.fallAsleepMin, entry.wakeTime)
                    entry.copy(id = 0, totalSleep = calculatedTotal)
                }
                addBulkEntries(validatedEntries)
                true
            } else false
        } catch (e: Exception) {
            _errorMessage.value = "Invalid JSON format: ${e.localizedMessage}"
            false
        }
    }

    fun updateEntry(entry: SleepEntry) {
        viewModelScope.launch {
            try {
                val updatedEntry = entry.copy(
                    totalSleep = SleepEntry.calculateTotalSleep(entry.sleepTime, entry.fallAsleepMin, entry.wakeTime)
                )
                repository.updateEntry(updatedEntry)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update entry: ${e.localizedMessage}"
            }
        }
    }

    fun deleteEntry(entry: SleepEntry) {
        viewModelScope.launch {
            try {
                repository.deleteEntry(entry)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete entry: ${e.localizedMessage}"
            }
        }
    }

    fun deleteAllEntries() {
        viewModelScope.launch {
            try {
                repository.deleteAllEntries()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete all entries: ${e.localizedMessage}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class SleepViewModelFactory(private val repository: SleepRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SleepViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SleepViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
