package com.yannickpulver.synctool

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
@Composable
fun App() {
    var sourceFolder by remember { mutableStateOf<File?>(null) }
    var targetFolder by remember { mutableStateOf<File?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableStateOf(0f) }
    var syncProgressText by remember { mutableStateOf("") }
    var syncResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Folder selection boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Source folder box
                FolderSelectionBox(
                    modifier = Modifier.weight(1f),
                    title = "Source Folder",
                    selectedFolder = sourceFolder,
                    onFolderSelected = { 
                        sourceFolder = it
                        errorMessage = null
                        syncResult = null
                    }
                )
                
                // Arrow indicator
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync direction",
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterVertically),
                    tint = Color.Black
                )
                
                // Target folder box
                FolderSelectionBox(
                    modifier = Modifier.weight(1f),
                    title = "Target Folder",
                    selectedFolder = targetFolder,
                    onFolderSelected = { 
                        targetFolder = it
                        errorMessage = null
                        syncResult = null
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sync button
            Button(
                onClick = {
                    scope.launch {
                        if (sourceFolder != null && targetFolder != null) {
                            performSync(
                                source = sourceFolder!!,
                                target = targetFolder!!,
                                onSyncStart = { 
                                    isSyncing = true
                                    syncProgress = 0f
                                    syncProgressText = ""
                                    syncResult = null
                                    errorMessage = null
                                },
                                onSyncProgress = { progress, progressText ->
                                    syncProgress = progress
                                    syncProgressText = progressText
                                },
                                onSyncComplete = { result ->
                                    isSyncing = false
                                    syncProgress = 1f
                                    syncResult = result
                                },
                                onSyncError = { error ->
                                    isSyncing = false
                                    syncProgress = 0f
                                    errorMessage = error
                                }
                            )
                        } else {
                            errorMessage = "Please select both source and target folders"
                        }
                    }
                },
                enabled = !isSyncing && sourceFolder != null && targetFolder != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(56.dp)
            ) {
                if (isSyncing) {
                    if (syncProgress > 0f) {
                        CircularProgressIndicator(
                            progress = syncProgress,
                            modifier = Modifier.size(24.dp),
                            color = LocalContentColor.current
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = LocalContentColor.current
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (syncProgressText.isNotEmpty()) syncProgressText else "Syncing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Folders", fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status messages
            syncResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Green.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ Sync completed successfully!\n$result",
                            color = Color(0xFF0D7A0D), // Dark green for better contrast
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "✗ Error: $error",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Red.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun FolderSelectionBox(
    modifier: Modifier = Modifier,
    title: String,
    selectedFolder: File?,
    onFolderSelected: (File) -> Unit
) {
    val directoryPickerLauncher = rememberDirectoryPickerLauncher(
        title = "Select $title"
    ) { directory ->
        directory?.let { platformFile ->
            val file = File(platformFile.path)
            onFolderSelected(file)
        }
    }

    Card(
        onClick = {
            directoryPickerLauncher.launch()
        },
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (selectedFolder != null) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (selectedFolder != null) {
                Text(
                    text = selectedFolder.name,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = selectedFolder.absolutePath,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                Text(
                    text = "Click to select folder",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
        }
    }
}

suspend fun performSync(
    source: File,
    target: File,
    onSyncStart: () -> Unit,
    onSyncProgress: (Float, String) -> Unit,
    onSyncComplete: (String) -> Unit,
    onSyncError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            onSyncStart()
            
            // Use rsync if available, otherwise fall back to manual copy
            val result = try {
                executeRsync(source, target, onSyncProgress)
            } catch (e: Exception) {
                // Fallback to manual copy if rsync is not available
                executeManualSync(source, target, onSyncProgress)
            }
            
            onSyncComplete(result)
        } catch (e: Exception) {
            onSyncError(e.message ?: "Unknown error occurred")
        }
    }
}

private suspend fun executeRsync(source: File, target: File, onProgress: (Float, String) -> Unit): String = withContext(Dispatchers.IO) {
    // First, count total files to sync
    val totalFiles = source.walkTopDown().count { it.isFile }
    onProgress(0f, "0 / $totalFiles files")
    
    val command = listOf(
        "rsync",
        "-av",
        "--info=progress2",  // Better progress output with overall transfer progress
        "--update",  // Only update files that are newer in source
        "${source.absolutePath}/",
        target.absolutePath
    )
    
    val processBuilder = ProcessBuilder(command)
    val process = processBuilder.start()
    
    val outputBuilder = StringBuilder()
    val errorBuilder = StringBuilder()
    
    // Read output line by line to parse progress
    val outputReader = process.inputStream.bufferedReader()
    val errorReader = process.errorStream.bufferedReader()
    
    var line: String?
    var processedFiles = 0
    
    while (outputReader.readLine().also { line = it } != null) {
        line?.let { currentLine ->
            outputBuilder.appendLine(currentLine)
            
            // Parse --info=progress2 output which looks like:
            // "1,234,567  89%  123.45kB/s    0:00:42 (xfr#123, to-chk=456/789)"
            if (currentLine.matches(Regex("\\s*[0-9,]+\\s+\\d+%.*\\(xfr#\\d+.*"))) {
                // Extract transfer count and to-check info
                val xfrMatch = Regex("xfr#(\\d+)").find(currentLine)
                val toChkMatch = Regex("to-chk=(\\d+)/(\\d+)").find(currentLine)
                
                if (xfrMatch != null && toChkMatch != null) {
                    val transferred = xfrMatch.groupValues[1].toIntOrNull() ?: 0
                    val remaining = toChkMatch.groupValues[1].toIntOrNull() ?: 0
                    val total = toChkMatch.groupValues[2].toIntOrNull() ?: 0
                    
                    if (total > 0) {
                        val completed = total - remaining
                        val progress = completed.toFloat() / total
                        val progressText = "$completed / $total files"
                        onProgress(progress, progressText)
                    }
                }
            }
            // Also handle regular file transfers (files being copied)
            else if (currentLine.trim().isNotEmpty() && 
                !currentLine.contains("sending incremental file list") &&
                !currentLine.contains("sent ") &&
                !currentLine.contains("total size") &&
                !currentLine.startsWith("rsync:") &&
                !currentLine.contains("deleting ") &&
                currentLine.length > 5 &&
                !currentLine.matches(Regex("\\s*[0-9,]+\\s+\\d+%.*"))) {
                
                processedFiles++
                val progress = if (totalFiles > 0) processedFiles.toFloat() / totalFiles else 0f
                val progressText = "$processedFiles / $totalFiles files"
                onProgress(progress, progressText)
            }
        }
    }
    
    // Read any errors
    while (errorReader.readLine().also { line = it } != null) {
        line?.let { errorBuilder.appendLine(it) }
    }
    
    val exitCode = process.waitFor()
    
    if (exitCode != 0) {
        throw Exception("rsync failed with exit code $exitCode: ${errorBuilder.toString()}")
    }
    
    onProgress(1f, "$totalFiles / $totalFiles files") // Ensure we show 100% at the end
    "Files synchronized using rsync (additive mode - no files deleted)\n${outputBuilder.toString()}"
}

private suspend fun executeManualSync(source: File, target: File, onProgress: (Float, String) -> Unit): String = withContext(Dispatchers.IO) {
    if (!target.exists()) {
        target.mkdirs()
    }
    
    var newFiles = 0
    var updatedFiles = 0
    var skippedFiles = 0
    
    // Count total files first for progress tracking
    val totalFiles = source.walkTopDown().count { it.isFile }
    var processedFiles = 0
    
    onProgress(0f, "0 / $totalFiles files")
    
    source.walkTopDown().forEach { sourceFile ->
        val relativePath = sourceFile.relativeTo(source)
        val targetFile = File(target, relativePath.path)
        
        if (sourceFile.isDirectory) {
            targetFile.mkdirs()
        } else {
            targetFile.parentFile?.mkdirs()
            
            if (!targetFile.exists()) {
                // New file - copy it
                sourceFile.copyTo(targetFile, overwrite = false)
                newFiles++
            } else if (sourceFile.lastModified() > targetFile.lastModified()) {
                // Source file is newer - update it
                sourceFile.copyTo(targetFile, overwrite = true)
                updatedFiles++
            } else {
                // Target file is same or newer - skip
                skippedFiles++
            }
            
            processedFiles++
            val progress = processedFiles.toFloat() / totalFiles
            val progressText = "$processedFiles / $totalFiles files"
            onProgress(progress, progressText)
        }
    }
    
    "Files synchronized manually (additive mode)\n" +
           "New files: $newFiles, Updated files: $updatedFiles, Skipped files: $skippedFiles"
}