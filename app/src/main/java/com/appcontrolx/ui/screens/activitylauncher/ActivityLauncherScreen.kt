package com.appcontrolx.ui.screens.activitylauncher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appcontrolx.model.AppActivities
import com.appcontrolx.model.ActivityInfo
import com.appcontrolx.ui.viewmodels.ActivityLauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLauncherScreen(
    onNavigateBack: () -> Unit,
    viewModel: ActivityLauncherViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val expandedApps by viewModel.expandedApps.collectAsStateWithLifecycle()
    val launchingActivity by viewModel.launchingActivity.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Activity Launcher",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search apps or activities...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { viewModel.setFilter("all") },
                    label = { Text("All") },
                    leadingIcon = { Icon(Icons.Default.Package, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = selectedFilter == "user",
                    onClick = { viewModel.setFilter("user") },
                    label = { Text("User") },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = selectedFilter == "system",
                    onClick = { viewModel.setFilter("system") },
                    label = { Text("System") },
                    leadingIcon = { Icon(Icons.Default.Monitor, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Stats
            val totalActivities = apps.sumOf { it.activities.size }
            Text(
                text = "${apps.size} apps • $totalActivities activities",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Loading State
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No activities found",
                            style = MaterialTheme.typography.titleMedium,
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
                    items(apps, key = { it.packageName }) { app ->
                        AppActivityCard(
                            app = app,
                            isExpanded = expandedApps.contains(app.packageName),
                            launchingActivity = launchingActivity,
                            onToggle = { viewModel.toggleAppExpansion(app.packageName) },
                            onLaunchActivity = { packageName, activityName ->
                                viewModel.launchActivity(packageName, activityName)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppActivityCard(
    app: AppActivities,
    isExpanded: Boolean,
    launchingActivity: String?,
    onToggle: () -> Unit,
    onLaunchActivity: (String, String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column {
            // App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
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
                            Icon(Icons.Default.Package, contentDescription = null)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        "${app.activities.size}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    if (isExpanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Activities List
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    app.activities.forEachIndexed { index, activity ->
                        ActivityItem(
                            activity = activity,
                            packageName = app.packageName,
                            isLaunching = launchingActivity == "${app.packageName}/${activity.activityName}",
                            onLaunch = { onLaunchActivity(app.packageName, activity.activityName) },
                            showBorder = index < app.activities.size - 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(
    activity: ActivityInfo,
    packageName: String,
    isLaunching: Boolean,
    onLaunch: () -> Unit,
    showBorder: Boolean
) {
    val shortName = remember(activity.activityName, packageName) {
        if (activity.activityName.startsWith(packageName)) {
            activity.activityName.substring(packageName.length)
        } else {
            activity.activityName
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLaunch)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .then(
                if (showBorder) {
                    Modifier.padding(bottom = 1.dp)
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (isLaunching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    shortName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (activity.isExported) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF22C55E).copy(alpha = 0.1f)
                    ) {
                        Text(
                            "EXPORTED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF22C55E),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!activity.canLaunchWithoutRoot) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEAB308).copy(alpha = 0.1f)
                    ) {
                        Text(
                            "Requires Root/Shizuku",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEAB308),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                activity.activityName,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showBorder) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 60.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}
