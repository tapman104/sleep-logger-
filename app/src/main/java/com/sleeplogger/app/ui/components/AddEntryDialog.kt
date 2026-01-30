package com.sleeplogger.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleeplogger.app.SleepEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryDialog(
    onDismiss: () -> Unit,
    onSave: (SleepEntry) -> Unit,
    existingEntry: SleepEntry? = null
) {
    var date by remember { mutableStateOf(existingEntry?.date ?: "") }
    var sleepTime by remember { mutableStateOf(existingEntry?.sleepTime ?: "") }
    var fallAsleepMin by remember { mutableStateOf(existingEntry?.fallAsleepMin ?: "") }
    var wakeTime by remember { mutableStateOf(existingEntry?.wakeTime ?: "") }
    
    val isEditing = existingEntry != null
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Sleep Entry" else "Add Sleep Entry",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (e.g., 8-1-26)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = sleepTime,
                    onValueChange = { sleepTime = it },
                    label = { Text("Sleep Time (e.g., 11:39pm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = fallAsleepMin,
                    onValueChange = { fallAsleepMin = it },
                    label = { Text("Time to Fall Asleep (e.g., 30min)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = wakeTime,
                    onValueChange = { wakeTime = it },
                    label = { Text("Wake Time (e.g., 6:45am)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (date.isNotBlank() && sleepTime.isNotBlank() && 
                        fallAsleepMin.isNotBlank() && wakeTime.isNotBlank()) {
                        val entry = SleepEntry(
                            id = existingEntry?.id ?: 0,
                            date = date.trim(),
                            sleepTime = sleepTime.trim(),
                            fallAsleepMin = fallAsleepMin.trim(),
                            wakeTime = wakeTime.trim()
                        )
                        onSave(entry)
                    }
                },
                enabled = date.isNotBlank() && sleepTime.isNotBlank() && 
                         fallAsleepMin.isNotBlank() && wakeTime.isNotBlank()
            ) {
                Text(if (isEditing) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
