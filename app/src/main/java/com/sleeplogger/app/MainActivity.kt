package com.sleeplogger.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sleeplogger.app.database.SleepDatabase
import com.sleeplogger.app.repository.SleepRepository
import com.sleeplogger.app.ui.theme.*
import com.sleeplogger.app.utils.parseBulkInput
import com.sleeplogger.app.viewmodel.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val shouldShowAddDialog = intent?.action == "com.sleeplogger.app.ACTION_ADD_ENTRY"

        setContent {
            SleepLoggerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val db = SleepDatabase.getDatabase(context)
                    val repository = SleepRepository(db.sleepDao())
                    val viewModelFactory = SleepViewModelFactory(repository)
                    SleepLoggerApp(
                        viewModel = viewModel(factory = viewModelFactory),
                        initialShowAddDialog = shouldShowAddDialog
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepLoggerApp(viewModel: SleepViewModel, initialShowAddDialog: Boolean = false) {
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(initialShowAddDialog) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showStatsScreen by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<SleepEntry?>(null) }
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Crossfade(targetState = showStatsScreen, label = "ScreenTransition") { isStats ->
        if (isStats) {
            StatisticsScreen(
                stats = stats,
                viewModel = viewModel,
                onBack = { showStatsScreen = false }
            )
        } else {
            Scaffold(
                topBar = {
                    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    "Sleep Logger",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ) 
                            },
                            navigationIcon = {
                                IconButton(onClick = { showStatsScreen = true }) {
                                    Icon(Icons.Default.Analytics, contentDescription = "Statistics", tint = MutedPurple)
                                }
                            },
                            actions = {
                                IconButton(onClick = { showSettingsDialog = true }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search your logs...", color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, "Search", tint = SoftBlue)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showFilterSheet = true }) {
                                        Icon(Icons.Default.FilterList, "Filter", tint = if (filters.hasActiveFilters()) SoftBlue else Color.Gray)
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SoftBlue,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedContainerColor = DeepGray,
                                    unfocusedContainerColor = DeepGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }

                        if (filters.hasActiveFilters()) {
                            LazyRow(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (filters.startDate != null) {
                                    item {
                                        FilterChip(
                                            onClick = { viewModel.updateFilters(filters.copy(startDate = null, endDate = null)) },
                                            label = { Text("Date Range", fontSize = 12.sp) },
                                            selected = true,
                                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SoftBlue.copy(alpha = 0.2f),
                                                selectedLabelColor = SoftBlue
                                            )
                                        )
                                    }
                                }
                                if (filters.minDuration != null) {
                                    item {
                                        FilterChip(
                                            onClick = { viewModel.updateFilters(filters.copy(minDuration = null, maxDuration = null)) },
                                            label = { Text("Duration", fontSize = 12.sp) },
                                            selected = true,
                                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SoftBlue.copy(alpha = 0.2f),
                                                selectedLabelColor = SoftBlue
                                            )
                                        )
                                    }
                                }
                                item {
                                    TextButton(onClick = { viewModel.clearFilters() }) {
                                        Text("Clear All", fontSize = 12.sp, color = SoftBlue)
                                    }
                                }
                            }
                        }
                    }
                },
                floatingActionButton = {
                    LargeFloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = SoftBlue,
                        contentColor = Charcoal,
                        shape = CircleShape,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add entry", modifier = Modifier.size(32.dp))
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (entries.isEmpty()) {
                        EmptyStateView(searchQuery.isNotBlank() || filters.hasActiveFilters())
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(entries, key = { it.id }) { entry ->
                                SleepEntryCard(
                                    entry = entry,
                                    onEdit = { editingEntry = it },
                                    onDelete = { viewModel.deleteEntry(it) }
                                )
                            }
                        }
                    }
                    
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MutedPurple
                        )
                    }
                }

                if (showFilterSheet) {
                    FilterBottomSheet(
                        filters = filters,
                        onDismiss = { showFilterSheet = false },
                        onApply = { 
                            viewModel.updateFilters(it)
                            showFilterSheet = false
                        }
                    )
                }

                if (showAddDialog) {
                    AddEntryDialog(
                        onDismiss = { showAddDialog = false },
                        onSave = { date, sleep, fallAsleep, wake ->
                            viewModel.addEntry(date, sleep, fallAsleep, wake)
                            showAddDialog = false
                        }
                    )
                }

                if (showSettingsDialog) {
                    SettingsDialog(
                        onDismiss = { showSettingsDialog = false },
                        viewModel = viewModel
                    )
                }

                editingEntry?.let { entry ->
                    EditEntryDialog(
                        entry = entry,
                        onDismiss = { editingEntry = null },
                        onSave = { updated ->
                            viewModel.updateEntry(updated)
                            editingEntry = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(isSearching: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isSearching) "🔎" else "😴",
                fontSize = 64.sp
            )
            Text(
                text = if (isSearching) "No matching logs" else "No sleep data yet",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = if (isSearching) "Try a different search" else "Tap + to start tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SleepEntryCard(
    entry: SleepEntry,
    onEdit: (SleepEntry) -> Unit,
    onDelete: (SleepEntry) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = DeepGray,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = SoftBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = entry.date,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Brightness3, null, tint = MoonBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(entry.sleepTime, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ ${entry.fallAsleepMin}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LightMode, null, tint = SubtleYellow, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(entry.wakeTime, color = Color.White)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftGray,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "😴 Total: ${entry.totalSleep}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MutedPurple
                    )
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onEdit(entry) }) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = SoftBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", color = SoftBlue)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { onDelete(entry) }) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = Color(0xFFEF9A9A))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = Color(0xFFEF9A9A))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEntryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    var date by remember { mutableStateOf("") }
    var sleepTime by remember { mutableStateOf("") }
    var fallAsleepMin by remember { mutableStateOf("30min") }
    var wakeTime by remember { mutableStateOf("") }

    val datePickerDialog = DatePickerDialog(
        context, 
        { _, y, m, d -> 
            val cal = Calendar.getInstance().apply { set(y, m, d) }
            date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(cal.time)
        }, 
        calendar.get(Calendar.YEAR), 
        calendar.get(Calendar.MONTH), 
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    val sleepTimePicker = TimePickerDialog(context, { _, h, m -> sleepTime = formatTime(h, m) }, 23, 0, false)
    val wakeTimePicker = TimePickerDialog(context, { _, h, m -> wakeTime = formatTime(h, m) }, 7, 0, false)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = DeepGray,
                modifier = Modifier.wrapContentHeight().fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("Add Sleep Entry", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    PickerField(label = "Date", value = date, icon = Icons.Default.CalendarToday, tint = SoftBlue) { datePickerDialog.show() }
                    PickerField(label = "Sleep Time", value = sleepTime, icon = Icons.Default.Brightness3, tint = MoonBlue) { sleepTimePicker.show() }
                    
                    Column {
                        Text("Time to Fall Asleep", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        OutlinedTextField(
                            value = fallAsleepMin,
                            onValueChange = { fallAsleepMin = it },
                            placeholder = { Text("30min") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoftBlue,
                                focusedContainerColor = SoftGray,
                                unfocusedContainerColor = SoftGray
                            )
                        )
                    }

                    PickerField(label = "Wake Time", value = wakeTime, icon = Icons.Default.LightMode, tint = SubtleYellow) { wakeTimePicker.show() }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Button(
                            onClick = { onSave(date, sleepTime, fallAsleepMin, wakeTime) },
                            enabled = date.isNotEmpty() && sleepTime.isNotEmpty() && wakeTime.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftBlue, disabledContainerColor = Color.DarkGray)
                        ) {
                            Text("Save", color = Charcoal, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PickerField(label: String, value: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SoftGray,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(if (value.isEmpty()) "Select $label" else value, color = if (value.isEmpty()) Color.DarkGray else Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filters: SleepFilters,
    onDismiss: () -> Unit,
    onApply: (SleepFilters) -> Unit
) {
    var currentFilters by remember { mutableStateOf(filters) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepGray,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Filters", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            
            // Date Preset
            Text("Date Preset", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { 
                            val now = Calendar.getInstance()
                            when(preset) {
                                DatePreset.LAST_7_DAYS -> {
                                    val start = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }.time
                                    currentFilters = currentFilters.copy(startDate = start, endDate = now.time)
                                }
                                DatePreset.LAST_30_DAYS -> {
                                    val start = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -30) }.time
                                    currentFilters = currentFilters.copy(startDate = start, endDate = now.time)
                                }
                                else -> {}
                            }
                        },
                        label = { Text(preset.name.lowercase().replace("_", " ")) }
                    )
                }
            }

            // Duration Filter
            Text("Sleep Duration", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val durations = listOf("< 5h" to (0 to 300), "5-7h" to (300 to 420), "7h+" to (420 to 1440))
                durations.forEach { (label, range) ->
                    FilterChip(
                        selected = currentFilters.minDuration == range.first && currentFilters.maxDuration == range.second,
                        onClick = { 
                            currentFilters = currentFilters.copy(minDuration = range.first, maxDuration = range.second)
                        },
                        label = { Text(label) }
                    )
                }
            }

            Button(
                onClick = { onApply(currentFilters) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SoftBlue)
            ) {
                Text("Apply Filters", color = Charcoal, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

fun SleepFilters.hasActiveFilters(): Boolean {
    return query.isNotBlank() || startDate != null || minDuration != null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    stats: SleepStats,
    viewModel: SleepViewModel,
    onBack: () -> Unit
) {
    val selectedMetric by viewModel.selectedMetric.collectAsStateWithLifecycle()
    val graphMode by viewModel.graphMode.collectAsStateWithLifecycle()
    val inspectedIndex by viewModel.inspectedDateIndex.collectAsStateWithLifecycle()
    val viewOffset by viewModel.viewPageOffset.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    
    BackHandler(onBack = onBack)

    val currentData = stats.currentViewData
    val inspectedPoint = currentData.getOrNull(currentData.size - 1 - inspectedIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Charcoal)
    ) {
        TopAppBar(
            title = { Text("Sleep Statistics", style = MaterialTheme.typography.titleMedium, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Primary Metric Section
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                val heroValue = when (selectedMetric) {
                    SleepMetric.DURATION -> inspectedPoint?.rawEntry?.totalSleep ?: "--h --m"
                    SleepMetric.BEDTIME -> inspectedPoint?.rawEntry?.sleepTime ?: "--:--"
                    SleepMetric.WAKETIME -> inspectedPoint?.rawEntry?.wakeTime ?: "--:--"
                    SleepMetric.CONSISTENCY -> "${inspectedPoint?.value?.toInt() ?: 0}%"
                }
                
                Text(heroValue, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(if (viewOffset == 0 && inspectedIndex == 0) "Latest Entry" else inspectedPoint?.date ?: "", color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))

                // Metric Selector Dropdown
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    Surface(
                        onClick = { showMenu = true },
                        color = DeepGray,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedMetric.name.lowercase().capitalize(), color = Color.White)
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                        }
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(DeepGray)) {
                        SleepMetric.entries.forEach { metric ->
                            DropdownMenuItem(
                                text = { Text(metric.name.lowercase().capitalize(), color = Color.White) },
                                onClick = { 
                                    viewModel.setMetric(metric)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showMenu = false 
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Graph Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepGray)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GraphViewMode.entries.forEach { mode ->
                    val isSelected = graphMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SoftGray else Color.Transparent)
                            .clickable { 
                                viewModel.setGraphMode(mode)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            mode.name.lowercase().capitalize(),
                            color = if (isSelected) Color.White else Color.Gray,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(stats.contextLabel, color = SoftBlue, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Graph Section
            Box(modifier = Modifier.height(220.dp).fillMaxWidth()) {
                if (currentData.isNotEmpty()) {
                    ModernSleepChart(
                        data = currentData,
                        selectedIndex = currentData.size - 1 - inspectedIndex,
                        metric = selectedMetric,
                        viewMode = graphMode,
                        onPointSelected = { 
                            viewModel.setInspectedIndex(currentData.size - 1 - it)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
            }

            // Week Pagination Navigation
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigatePage(1) }, enabled = viewOffset * 7 < stats.totalAvailableDays - 7) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Prev Week", tint = if (viewOffset * 7 < stats.totalAvailableDays - 7) Color.White else Color.DarkGray)
                }
                Text(
                    text = if (currentData.isNotEmpty()) "${currentData.first().date} - ${currentData.last().date}" else "",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                IconButton(onClick = { viewModel.navigatePage(-1) }, enabled = viewOffset > 0) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next Week", tint = if (viewOffset > 0) Color.White else Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Expanded Detail Card
            AnimatedContent(targetState = inspectedPoint, label = "DetailExpansion") { point ->
                point?.rawEntry?.let { entry ->
                    Surface(
                        color = DeepGray,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                DetailItem("Sleep start", entry.sleepTime, MoonBlue)
                                DetailItem("Wake time", entry.wakeTime, SubtleYellow)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                DetailItem("Falling asleep", entry.fallAsleepMin, SoftBlue)
                                val avgMins = stats.avgDurationAllTime.toMinutes()
                                val diff = entry.getTotalMinutes() - avgMins
                                val diffText = if (diff >= 0) "+${diff/60}h ${diff%60}m" else "-${abs(diff)/60}h ${abs(diff)%60}m"
                                DetailItem("vs Average", diffText, MutedPurple)
                            }
                        }
                    }
                }
            }

            // Summary Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernMiniStat(label = "Avg Sleep", value = stats.avgDuration7Days, modifier = Modifier.weight(1f))
                ModernMiniStat(label = "Consistency", value = "${stats.consistencyScore}%", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernMiniStat(label = "Avg Bedtime", value = stats.avgBedtime, modifier = Modifier.weight(1f))
                ModernMiniStat(label = "Avg Wake", value = stats.avgWakeTime, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, color: Color) {
    Column {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Text(value, color = color, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ModernSleepChart(
    data: List<ChartPoint>, 
    selectedIndex: Int, 
    metric: SleepMetric, 
    viewMode: GraphViewMode,
    onPointSelected: (Int) -> Unit
) {
    val barBrush = Brush.verticalGradient(listOf(MutedPurple, MutedPurple.copy(alpha = 0.3f)))
    val accentColor = MutedPurple
    
    val maxVal = (data.maxByOrNull { it.value }?.value ?: 10f).coerceAtLeast(1f)
    val minVal = if (metric == SleepMetric.DURATION) 0f else (data.minByOrNull { it.value }?.value ?: 0f) * 0.95f
    val goalValue = 8f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val spacing = width / data.size
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val index = (offset.x / spacing).toInt().coerceIn(0, data.size - 1)
                        onPointSelected(index)
                    }
                }
                .pointerInput(data) {
                    detectHorizontalDragGestures { change, _ ->
                        val index = (change.position.x / spacing).toInt().coerceIn(0, data.size - 1)
                        onPointSelected(index)
                    }
                }
        ) {
            val goalY = height - ((goalValue - minVal) / (maxVal - minVal)) * height
            if (metric == SleepMetric.DURATION) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, goalY),
                    end = Offset(width, goalY),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }

            data.forEachIndexed { index, point ->
                val x = index * spacing + (spacing / 2)
                val normalizedValue = (point.value - minVal) / (maxVal - minVal)
                val barHeight = (normalizedValue * height).coerceAtLeast(8.dp.toPx())
                
                when (viewMode) {
                    GraphViewMode.BAR, GraphViewMode.DISTRIBUTION -> {
                        val barWidth = spacing * 0.6f
                        drawRoundRect(
                            brush = if (index == selectedIndex) barBrush else Brush.linearGradient(listOf(Color.DarkGray, Color.DarkGray)),
                            topLeft = Offset(x - barWidth / 2, height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(12.dp.toPx())
                        )
                    }
                    GraphViewMode.LINE -> {
                        if (index > 0) {
                            val prevNorm = (data[index-1].value - minVal) / (maxVal - minVal)
                            drawLine(
                                accentColor.copy(alpha = 0.5f), 
                                Offset((index-1)*spacing + spacing/2, height - prevNorm*height), 
                                Offset(x, height - normalizedValue*height), 
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        drawCircle(
                            if (index == selectedIndex) accentColor else Color.DarkGray, 
                            radius = if (index == selectedIndex) 6.dp.toPx() else 4.dp.toPx(), 
                            center = Offset(x, height - normalizedValue*height)
                        )
                    }
                    GraphViewMode.RANGE -> {
                        drawCircle(accentColor.copy(alpha = 0.2f), radius = 12.dp.toPx(), center = Offset(x, height - normalizedValue*height))
                        drawCircle(accentColor, radius = 4.dp.toPx(), center = Offset(x, height - normalizedValue*height))
                    }
                }
                
                if (index == selectedIndex) {
                    drawLine(accentColor.copy(alpha = 0.3f), Offset(x, 0f), Offset(x, height), strokeWidth = 1.dp.toPx())
                }
            }
        }
    }
}

private fun String.toMinutes(): Int {
    return try {
        val parts = this.replace("m", "").split("h ")
        val h = parts[0].trim().toInt()
        val m = if (parts.size > 1) parts[1].trim().toInt() else 0
        h * 60 + m
    } catch (e: Exception) { 480 }
}

@Composable
fun ModernMiniStat(label: String, value: String, modifier: Modifier) {
    Surface(color = DeepGray, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

fun formatTime(h: Int, m: Int): String {
    val ap = if (h >= 12) "pm" else "am"
    val dh = if (h == 0) 12 else if (h > 12) h - 12 else h
    return "$dh:${m.toString().padStart(2, '0')}$ap"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(onDismiss: () -> Unit, viewModel: SleepViewModel) {
    val context = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.readText()
                if (viewModel.importFromJson(jsonString)) {
                    Toast.makeText(context, "Logs imported successfully", Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import logs", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showAbout) {
        AboutModal(onDismiss = { showAbout = false })
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = DeepGray) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Settings", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingsOption("Export JSON", Icons.Default.Share) {
                    val data = viewModel.exportToJson()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Sleep", data))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                
                SettingsOption("Import JSON", Icons.Default.FileUpload) {
                    importLauncher.launch("application/json")
                }
                
                SettingsOption("About", Icons.Default.Info) {
                    showAbout = true
                }
                
                SettingsOption("Delete All", Icons.Default.Delete, color = Color(0xFFEF9A9A)) {
                    viewModel.deleteAllEntries()
                    onDismiss()
                }
                
                Button(
                    onClick = onDismiss, 
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGray)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutModal(onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Charcoal,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MutedPurple.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("😴", fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("About", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

            // Section: App
            Column {
                Text("Sleep Logger", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "A simple, privacy-first sleep tracking app built for calm, consistent logging.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Section: Developer
            Column {
                Text("Developer", style = MaterialTheme.typography.labelLarge, color = SoftBlue)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Built by an independent developer who values simplicity, performance, and user privacy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            // Section: Links
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutLinkItem(Icons.Default.Code, "GitHub Profile") {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tapman104"))
                    context.startActivity(intent)
                }
                AboutLinkItem(Icons.Default.Terminal, "GitHub Repository") {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tapman104"))
                    context.startActivity(intent)
                }
            }

            // Section: Support
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DeepGray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Support Development", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "If you find this app useful, you can support its development.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/tapman"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftBlue.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("☕ Buy Me a Coffee", color = SoftBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Footer
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("v1.0.0", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(4.dp))
                Text("No data is collected or shared.", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AboutLinkItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.OpenInNew, null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SettingsOption(label: String, icon: ImageVector, color: Color = Color.White, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = color)
    }
}

@Composable
fun EditEntryDialog(entry: SleepEntry, onDismiss: () -> Unit, onSave: (SleepEntry) -> Unit) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(entry.date) }
    var sleepTime by remember { mutableStateOf(entry.sleepTime) }
    var fallAsleepMin by remember { mutableStateOf(entry.fallAsleepMin) }
    var wakeTime by remember { mutableStateOf(entry.wakeTime) }

    val datePicker = DatePickerDialog(context, { _, y, m, d -> 
        val cal = Calendar.getInstance().apply { set(y, m, d) }
        date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(cal.time)
    }, 2024, 0, 1)
    val sleepPicker = TimePickerDialog(context, { _, h, m -> sleepTime = formatTime(h, m) }, 23, 0, false)
    val wakePicker = TimePickerDialog(context, { _, h, m -> wakeTime = formatTime(h, m) }, 7, 0, false)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(32.dp), color = DeepGray) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Edit Entry", style = MaterialTheme.typography.titleLarge, color = Color.White)
                PickerField("Date", date, Icons.Default.CalendarToday, SoftBlue) { datePicker.show() }
                PickerField("Sleep Time", sleepTime, Icons.Default.Brightness3, MoonBlue) { sleepPicker.show() }
                OutlinedTextField(
                    value = fallAsleepMin,
                    onValueChange = { fallAsleepMin = it },
                    label = { Text("Fall Asleep Duration") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftBlue,
                        focusedContainerColor = SoftGray,
                        unfocusedContainerColor = SoftGray
                    )
                )
                PickerField("Wake Time", wakeTime, Icons.Default.LightMode, SubtleYellow) { wakePicker.show() }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel", color = Color.Gray) }
                    Button(onClick = { onSave(entry.copy(date=date, sleepTime=sleepTime, fallAsleepMin=fallAsleepMin, wakeTime=wakeTime)) }, modifier = Modifier.weight(1f)) { Text("Save") }
                }
            }
        }
    }
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
