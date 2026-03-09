package com.appcontrolx.ui.screens.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appcontrolx.model.AppAction
import com.appcontrolx.model.AppInfo
import com.appcontrolx.model.SafetyLevel
import com.appcontrolx.ui.screens.apps.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppListViewModel = hiltViewModel()
) {
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val filterCounts by viewModel.filterCounts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedApps by viewModel.selectedApps.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<AppAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Apps",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                        Icon(
                            if (isSelectionMode) Icons.Default.Close else Icons.Default.CheckBox,
                            contentDescription = if (isSelectionMode) "Exit selection" else "Select multiple"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter Tabs
            FilterTabs(
                selectedFilter = selectedFilter,
                onFilterChange = { viewModel.setFilter(it) },
                counts = filterCounts,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Selection Actions
            AnimatedVisibility(
                visible = isSelectionMode && selectedApps.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                SelectionActionsCard(
                    selectedCount = selectedApps.size,
                    onFreeze = { viewModel.executeBatchAction(AppAction.FREEZE) },
                    onUnfreeze = { viewModel.executeBatchAction(AppAction.UNFREEZE) },
                    onForceStop = { viewModel.executeBatchAction(AppAction.FORCE_STOP) },
                    onUninstall = { viewModel.executeBatchAction(AppAction.UNINSTALL) },
                    modifier = Modifier.padding(16.dp)
                )
            }

            // App Count
            Text(
                text = "${filteredApps.size} apps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // App List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No apps found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (searchQuery.isNotEmpty()) "Try a different search" else "No apps in this category",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppItem(
                            app = app,
                            isSelected = selectedApps.contains(app.packageName),
                            isSelectionMode = isSelectionMode,
                            onSelect = { viewModel.toggleAppSelection(app.packageName) },
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleAppSelection(app.packageName)
                                } else {
                                    selectedApp = app
                                    showDetailSheet = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // App Detail Bottom Sheet
    if (showDetailSheet && selectedApp != null) {
        AppDetailSheet(
            app = selectedApp!!,
            onDismiss = { showDetailSheet = false },
            onAction = { action ->
                pendingAction = action
                showActionDialog = true
            }
        )
    }

    // Action Confirmation Dialog
    if (showActionDialog && selectedApp != null && pendingAction != null) {
        ActionConfirmDialog(
            appName = selectedApp!!.appName,
            action = pendingAction!!,
            onConfirm = {
                viewModel.executeAction(selectedApp!!.packageName, pendingAction!!)
                showActionDialog = false
                showDetailSheet = false
            },
            onDismiss = {
                showActionDialog = false
                pendingAction = null
            }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search apps...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun FilterTabs(
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    counts: AppListViewModel.FilterCounts,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == "all",
            onClick = { onFilterChange("all") },
            label = { Text("All") },
            leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = { Text(counts.all.toString(), style = MaterialTheme.typography.labelSmall) }
        )
        FilterChip(
            selected = selectedFilter == "user",
            onClick = { onFilterChange("user") },
            label = { Text("User") },
            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = { Text(counts.user.toString(), style = MaterialTheme.typography.labelSmall) }
        )
        FilterChip(
            selected = selectedFilter == "system",
            onClick = { onFilterChange("system") },
            label = { Text("System") },
            leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = { Text(counts.system.toString(), style = MaterialTheme.typography.labelSmall) }
        )
        FilterChip(
            selected = selectedFilter == "frozen",
            onClick = { onFilterChange("frozen") },
            label = { Text("Frozen") },
            leadingIcon = { Icon(Icons.Default.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = { Text(counts.frozen.toString(), style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
    }
}

@Composable
private fun SelectionActionsCard(
    selectedCount: Int,
    onFreeze: () -> Unit,
    onUnfreeze: () -> Unit,
    onForceStop: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckBox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column {
                    Text(
                        "$selectedCount apps selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Choose an action below",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AcUnit,
                    label = "Freeze",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onFreeze
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.WbSunny,
                    label = "Unfreeze",
                    color = Color(0xFF22C55E),
                    onClick = onUnfreeze
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.StopCircle,
                    label = "Force Stop",
                    color = Color(0xFFEAB308),
                    onClick = onForceStop
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Delete,
                    label = "Uninstall",
                    color = MaterialTheme.colorScheme.error,
                    onClick = onUninstall
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val contentColor = if (enabled) color else color.copy(alpha = 0.5f)
    val containerColor = if (enabled) color.copy(alpha = 0.1f) else color.copy(alpha = 0.05f)

    if (enabled) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            onClick = onClick
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            color = containerColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun AppItem(
    app: AppInfo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onSelect: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox
            if (isSelectionMode) {
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckBox,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // App Icon
            val iconBitmap = remember(app.iconBase64) {
                app.iconBase64?.let { base64 ->
                    try {
                        android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                            .let { bytes ->
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                    } catch (e: Exception) { null }
                }
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (iconBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = iconBitmap.asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                        color = if (!app.isEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (!app.isEnabled) {
                        Icon(
                            Icons.Default.AcUnit,
                            contentDescription = "Frozen",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "v${app.versionName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatBytes(app.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (app.isSystemApp) {
                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "System",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEAB308)
                        )
                    }
                }
            }

            // More button
            if (!isSelectionMode) {
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDetailSheet(
    app: AppInfo,
    onDismiss: () -> Unit,
    onAction: (AppAction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // App Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val iconBitmap = remember(app.iconBase64) {
                    app.iconBase64?.let { base64 ->
                        try {
                            android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                .let { bytes ->
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }
                        } catch (e: Exception) { null }
                    }
                }

                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (iconBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = iconBitmap.asImageBitmap(),
                            contentDescription = app.appName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                Column {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Details
            DetailRow("Version", app.versionName)
            DetailRow("Size", formatBytes(app.size))
            DetailRow("Type", if (app.isSystemApp) "System" else "User")
            DetailRow("Status", if (app.isEnabled) "Enabled" else "Frozen")
            DetailRow("Running", if (app.isRunning) "Yes" else "No")
            DetailRow("Safety", app.safetyLevel.name)

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Text(
                "Actions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            val canExecute = app.safetyLevel != SafetyLevel.CRITICAL
            val canFreeze = app.safetyLevel == SafetyLevel.SAFE || app.safetyLevel == SafetyLevel.WARNING

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (app.isEnabled) {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AcUnit,
                        label = "Freeze",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onAction(AppAction.FREEZE) },
                        enabled = canFreeze && canExecute
                    )
                } else {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WbSunny,
                        label = "Unfreeze",
                        color = Color(0xFF22C55E),
                        onClick = { onAction(AppAction.UNFREEZE) },
                        enabled = canExecute
                    )
                }
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.StopCircle,
                    label = "Force Stop",
                    color = Color(0xFFEAB308),
                    onClick = { onAction(AppAction.FORCE_STOP) },
                    enabled = canExecute
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CleaningServices,
                    label = "Clear Cache",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { onAction(AppAction.CLEAR_CACHE) },
                    enabled = canExecute
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Delete,
                    label = "Uninstall",
                    color = MaterialTheme.colorScheme.error,
                    onClick = { onAction(AppAction.UNINSTALL) },
                    enabled = canExecute && !app.isSystemApp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActionConfirmDialog(
    appName: String,
    action: AppAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val actionName = action.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    val isDestructive = action == AppAction.UNINSTALL || action == AppAction.CLEAR_DATA

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$actionName $appName?") },
        text = {
            Text(
                when (action) {
                    AppAction.FREEZE -> "This will disable the app. You can unfreeze it later."
                    AppAction.UNFREEZE -> "This will re-enable the app."
                    AppAction.FORCE_STOP -> "This will stop the app from running. It may restart automatically."
                    AppAction.UNINSTALL -> "This will uninstall the app from your device. This action cannot be undone."
                    AppAction.CLEAR_CACHE -> "This will clear the app's cache data."
                    AppAction.CLEAR_DATA -> "This will clear all app data. You will need to log in again."
                    AppAction.RESTRICT_BACKGROUND -> "This will restrict the app from running in background."
                    AppAction.ALLOW_BACKGROUND -> "This will allow the app to run in background."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(actionName)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}
