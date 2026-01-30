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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkInputDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var bulkText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Bulk Input Sleep Entries",
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
                Text(
                    text = "Enter multiple entries, one per line:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = bulkText,
                    onValueChange = { bulkText = it },
                    label = { Text("Entries") },
                    placeholder = { Text("8-1-26-----11:39pm+30min-------6:45am\n8-1-27-----12:15am+15min-------7:30am") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )
                
                Text(
                    text = "Format: date-----sleep_time+minutes-------wake_time\n\nExample: 8-1-26-----11:39pm+30min-------6:45am",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bulkText.isNotBlank()) {
                        onSave(bulkText.trim())
                    }
                },
                enabled = bulkText.isNotBlank()
            ) {
                Text("Save Entries")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
