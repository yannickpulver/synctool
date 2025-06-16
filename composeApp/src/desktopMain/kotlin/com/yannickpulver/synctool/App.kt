package com.yannickpulver.synctool

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun App(toolbarHeight: Int) {
    var sourceFolder by remember { mutableStateOf<File?>(null) }
    var targetFolder by remember { mutableStateOf<File?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableStateOf(0f) }
    var syncProgressText by remember { mutableStateOf("") }
    var syncResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cancelMessage by remember { mutableStateOf<String?>(null) }
    var syncJob by remember { mutableStateOf<Job?>(null) }

    val scope = rememberCoroutineScope()

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = toolbarHeight.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp),
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

            // Sync/Stop buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sync button
                Button(
                    onClick = {
                        val job = scope.launch {
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
                                        cancelMessage = null
                                    },
                                    onSyncProgress = { progress, progressText ->
                                        syncProgress = progress
                                        syncProgressText = progressText
                                    },
                                    onSyncComplete = { result ->
                                        isSyncing = false
                                        syncProgress = 1f
                                        syncResult = result
                                        syncJob = null
                                    },
                                    onSyncError = { error ->
                                        isSyncing = false
                                        syncProgress = 0f
                                        errorMessage = error
                                        syncJob = null
                                    }
                                )
                            } else {
                                errorMessage = "Please select both source and target folders"
                            }
                        }
                        syncJob = job
                    },
                    enabled = !isSyncing && sourceFolder != null && targetFolder != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f)
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

                // Stop button (only visible during sync)
                if (isSyncing) {
                    Button(
                        onClick = {
                            syncJob?.cancel()
                            syncJob = null
                            isSyncing = false
                            syncProgress = 0f
                            syncProgressText = ""
                            cancelMessage = "Sync cancelled by user"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier
                            .height(56.dp)
                            .width(120.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop", fontSize = 16.sp)
                    }
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

            cancelMessage?.let { cancel ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⏹ $cancel",
                            color = Color.Gray.copy(alpha = 0.8f),
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FolderSelectionBox(
    modifier: Modifier = Modifier,
    title: String,
    selectedFolder: File?,
    onFolderSelected: (File) -> Unit
) {
    var isDragOver by remember { mutableStateOf(false) }

    val directoryPickerLauncher = rememberDirectoryPickerLauncher(
        title = "Select $title"
    ) { directory ->
        directory?.let { platformFile ->
            val file = File(platformFile.path)
            onFolderSelected(file)
        }
    }

    val dragAndDropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                isDragOver = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragOver = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragOver = false

                when (val dragData = event.dragData()) {
                    is DragData.FilesList -> {
                        val files = dragData.readFiles()
                        if (files.isNotEmpty()) {
                            val it = files.first()
                            val path = if (it.startsWith("file")) {
                                it.drop(5).replace("%20", " ")
                            } else {
                                it
                            }

                            onFolderSelected(File(path))
                        }
                        return false
                    }

                    else -> {
                        return false
                    }
                }
            }
        }
    }

    Card(
        onClick = {
            directoryPickerLauncher.launch()
        },
        modifier = modifier
            .height(200.dp)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragOver) Color(0xFFEBF3FF) else Color.White
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
                        text = "Click to select folder\nor drag & drop here",
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
                println("SyncTool: Attempting to use rsync...")
                executeRsync(source, target, onSyncProgress)
            } catch (e: CancellationException) {
                throw e // Re-throw cancellation to be handled by outer catch
            } catch (e: Exception) {
                // Fallback to manual copy if rsync is not available
                println("SyncTool: rsync failed (${e.message}), falling back to manual copy...")
                executeManualSync(source, target, onSyncProgress)
            }

            onSyncComplete(result)
        } catch (e: CancellationException) {
            // Handle cancellation gracefully - don't call onSyncError
            // The UI will be reset by the stop button click handler
        } catch (e: Exception) {
            onSyncError(e.message ?: "Unknown error occurred")
        }
    }
}

private suspend fun executeRsync(
    source: File,
    target: File,
    onProgress: (Float, String) -> Unit
): String = withContext(Dispatchers.IO) {
    // First, count total files to sync
    val totalFiles = source.walkTopDown().count { it.isFile }
    onProgress(0f, "0 / $totalFiles files")

    val command = listOf(
        "rsync",
        "-av",
        "--times",  // Explicitly preserve modification times
        "--progress",  // Progress output (compatible with older rsync versions)
        "${source.absolutePath}/",
        target.absolutePath
    )

    val processBuilder = ProcessBuilder(command)
    val process = processBuilder.start()

    try {

        val outputBuilder = StringBuilder()
        val errorBuilder = StringBuilder()

        // Read output line by line to parse progress
        val outputReader = process.inputStream.bufferedReader()
        val errorReader = process.errorStream.bufferedReader()

        var line: String?
        var processedFiles = 0

        while (outputReader.readLine().also { line = it } != null) {
            // Check for cancellation
            ensureActive()

            line?.let { currentLine ->
                outputBuilder.appendLine(currentLine)

                // Parse --progress output which shows progress per file like:
                // "     32,768   8%   31.25MB/s    0:00:00"
                // "filename.txt"
                // Or overall progress like: "to-chk=123/456)"
                if (currentLine.contains("to-chk=")) {
                    // Extract to-check info from lines like "to-chk=123/456)"
                    val toChkMatch = Regex("to-chk=(\\d+)/(\\d+)").find(currentLine)

                    if (toChkMatch != null) {
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
                // Handle file names being transferred (simple progress tracking)
                else if (currentLine.trim().isNotEmpty() &&
                    !currentLine.contains("sending incremental file list") &&
                    !currentLine.contains("sent ") &&
                    !currentLine.contains("total size") &&
                    !currentLine.startsWith("rsync:") &&
                    !currentLine.contains("deleting ") &&
                    !currentLine.matches(Regex("\\s*[0-9,]+\\s+\\d+%.*")) &&
                    !currentLine.matches(Regex(".*\\s+\\d+%\\s+.*")) &&
                    currentLine.length > 5
                ) {

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
        println("SyncTool: rsync completed successfully")
        "Files synchronized using rsync (additive mode - no files deleted)\n${outputBuilder.toString()}"
    } catch (e: Exception) {
        // Kill the process if still running (in case of cancellation)
        if (process.isAlive) {
            process.destroyForcibly()
        }
        throw e
    }
}

private suspend fun executeManualSync(
    source: File,
    target: File,
    onProgress: (Float, String) -> Unit
): String = withContext(Dispatchers.IO) {
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

    println("SyncTool: Starting manual file copy (rsync not available)")

    source.walkTopDown().forEach { sourceFile ->
        // Check for cancellation
        ensureActive()

        val relativePath = sourceFile.relativeTo(source)
        val targetFile = File(target, relativePath.path)

        if (sourceFile.isDirectory) {
            targetFile.mkdirs()
        } else {
            targetFile.parentFile?.mkdirs()

            if (!targetFile.exists()) {
                // New file - copy it
                sourceFile.copyTo(targetFile, overwrite = false)
                // Preserve original timestamp
                targetFile.setLastModified(sourceFile.lastModified())
                newFiles++
            } else if (sourceFile.lastModified() > targetFile.lastModified()) {
                // Source file is newer - update it
                sourceFile.copyTo(targetFile, overwrite = true)
                // Preserve original timestamp
                targetFile.setLastModified(sourceFile.lastModified())
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

    println("SyncTool: Manual sync completed - New: $newFiles, Updated: $updatedFiles, Skipped: $skippedFiles")
    "Files synchronized manually (additive mode)\n" +
            "New files: $newFiles, Updated files: $updatedFiles, Skipped files: $skippedFiles"
}