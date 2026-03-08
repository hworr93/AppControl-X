package com.appcontrolx.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appcontrolx.model.*
import com.appcontrolx.ui.viewmodels.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToApps: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val systemStats by viewModel.systemStats.collectAsStateWithLifecycle()
    val deviceInfo by viewModel.deviceInfo.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var selectedModal by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToTools) {
                        Icon(Icons.Default.Build, contentDescription = "Tools")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CPU Status Card
            item {
                CpuCard(
                    cpuStats = systemStats?.cpu,
                    onClick = { selectedModal = "cpu" }
                )
            }

            // Temperature Bar
            item {
                TemperatureBar(
                    cpuTemp = systemStats?.cpu?.temperature,
                    gpuTemp = systemStats?.gpu?.temperature
                )
            }

            // 2x2 Grid Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Battery
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        title = "Battery",
                        icon = Icons.Default.BatteryStd,
                        onClick = { selectedModal = "battery" }
                    ) {
                        BatteryContent(systemStats?.battery)
                    }

                    // Network
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        title = "Network",
                        icon = Icons.Default.Wifi,
                        onClick = { selectedModal = "network" }
                    ) {
                        NetworkContent(systemStats?.network)
                    }
                }
            }

            // Apps & Display Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Apps
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        title = "Apps",
                        icon = Icons.Default.Apps,
                        onClick = onNavigateToApps
                    ) {
                        AppsContent(apps)
                    }

                    // Display
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        title = "Display",
                        icon = Icons.Default.Monitor,
                        onClick = { selectedModal = "display" }
                    ) {
                        DisplayContent(systemStats?.display)
                    }
                }
            }

            // RAM & Storage Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // RAM
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        title = "RAM",
                        icon = Icons.Default.Memory,
                        onClick = { selectedModal = "memory" }
                    ) {
                        RamContent(systemStats?.ram)
                    }

                    // Storage
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        title = "Storage",
                        icon = Icons.Default.Storage,
                        onClick = { selectedModal = "storage" }
                    ) {
                        StorageContent(systemStats?.storage)
                    }
                }
            }

            // Device Info Card
            item {
                deviceInfo?.let { info ->
                    DeviceInfoCard(info)
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modals
    if (selectedModal != null) {
        val modalTitle = when (selectedModal) {
            "cpu" -> "CPU Details"
            "battery" -> "Battery Details"
            "network" -> "Network Details"
            "display" -> "Display Details"
            "memory" -> "RAM Details"
            "storage" -> "Storage Details"
            else -> "Details"
        }

        AlertDialog(
            onDismissRequest = { selectedModal = null },
            title = { Text(modalTitle) },
            text = {
                when (selectedModal) {
                    "cpu" -> CpuDetailContent(systemStats?.cpu)
                    "battery" -> BatteryDetailContent(systemStats?.battery)
                    "network" -> NetworkDetailContent(systemStats?.network)
                    "display" -> DisplayDetailContent(systemStats?.display)
                    "memory" -> RamDetailContent(systemStats?.ram)
                    "storage" -> StorageDetailContent(systemStats?.storage)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedModal = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun CpuCard(
    cpuStats: CpuStats?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CPU Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onClick) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Core Frequencies
            val frequencies = cpuStats?.coreFrequencies ?: emptyList()
            if (frequencies.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    frequencies.take(4).forEach { freq ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${freq} MHz",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Text(
                    "No CPU data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TemperatureBar(
    cpuTemp: Float?,
    gpuTemp: Float?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "CPU: ${cpuTemp?.let { "${it.toInt()}°C" } ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Monitor,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "GPU: ${gpuTemp?.let { "${it.toInt()}°C" } ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun BatteryContent(battery: BatteryStats?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.BatteryStd,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "${battery?.percent ?: "--"}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    battery?.temperature?.let { "${it.toInt()}°C" } ?: "--",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (battery?.isCharging == true) "Charging" else "Not charging",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NetworkContent(network: NetworkStats?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Wifi,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                when {
                    network?.wifi?.connected == true -> "Wi-Fi"
                    network?.mobile?.connected == true -> "Mobile"
                    else -> "Offline"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                when {
                    network?.wifi?.connected == true -> "${network.wifi.speed} Mbps"
                    network?.mobile?.connected == true -> network.mobile.type
                    else -> "No connection"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppsContent(apps: List<AppInfo>) {
    val userApps = apps.count { !it.isSystemApp }
    val systemApps = apps.count { it.isSystemApp }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "${apps.size}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                "Total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$userApps User",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$systemApps System",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DisplayContent(display: DisplayStats?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                display?.gpu ?: "--",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                display?.resolution ?: "--",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                display?.frameRate?.let { "${it}Hz" } ?: "--",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RamContent(ram: RamStats?) {
    val usedPercent = ram?.usedPercent ?: 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            progress = { usedPercent / 100f },
            modifier = Modifier.size(56.dp),
            strokeWidth = 5.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Column {
            Text(
                "${formatBytes(ram?.usedBytes)} used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${formatBytes(ram?.totalBytes)} total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageContent(storage: StorageStats?) {
    val usedPercent = storage?.usedPercent ?: 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            progress = { usedPercent / 100f },
            modifier = Modifier.size(56.dp),
            strokeWidth = 5.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Column {
            Text(
                "${formatBytes(storage?.usedBytes)} used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${formatBytes(storage?.totalBytes)} total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceInfoCard(deviceInfo: DeviceInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFBBF24)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        deviceInfo.brand.take(2),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    deviceInfo.model,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    deviceInfo.processor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    deviceInfo.androidVersion,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Uptime: ${deviceInfo.uptime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Deep sleep: ${deviceInfo.deepSleep} (${deviceInfo.deepSleepPercent}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Detail Content Composables
@Composable
private fun CpuDetailContent(cpu: CpuStats?) {
    Column {
        cpu?.let { c ->
            DetailRow("Usage", "${c.usagePercent.toInt()}%")
            DetailRow("Cores", c.cores.toString())
            DetailRow("Temperature", c.temperature?.let { "${it.toInt()}°C" } ?: "--")
            if (c.coreFrequencies.isNotEmpty()) {
                val topFrequencies = c.coreFrequencies.take(8).joinToString(", ") { "${it} MHz" }
                DetailRow("Frequencies", topFrequencies)
            }
        }
    }
}

@Composable
private fun BatteryDetailContent(battery: BatteryStats?) {
    Column {
        battery?.let { b ->
            DetailRow("Level", "${b.percent}%")
            DetailRow("Temperature", "${b.temperature}°C")
            DetailRow("Status", if (b.isCharging) "Charging" else "Not charging")
            DetailRow("Health", b.health)
            DetailRow("Technology", b.technology)
            DetailRow("Voltage", "${b.voltage}mV")
            DetailRow("Capacity", "${b.capacity}mAh")
            DetailRow("Remaining", b.remainingTime)
        }
    }
}

@Composable
private fun NetworkDetailContent(network: NetworkStats?) {
    Column {
        network?.let { n ->
            DetailRow("Wi-Fi Connected", if (n.wifi.connected) "Yes" else "No")
            if (n.wifi.connected) {
                DetailRow("SSID", n.wifi.ssid)
                DetailRow("IP", n.wifi.ip)
                DetailRow("Speed", "${n.wifi.speed} Mbps")
                DetailRow("Signal", "${n.wifi.signal}% (${n.wifi.signalDbm} dBm)")
            }
            DetailRow("Mobile Connected", if (n.mobile.connected) "Yes" else "No")
            if (n.mobile.connected) {
                DetailRow("Type", n.mobile.type)
            }
            DetailRow("SIM Present", if (n.sim.present) "Yes" else "No")
        }
    }
}

@Composable
private fun DisplayDetailContent(display: DisplayStats?) {
    Column {
        display?.let { d ->
            DetailRow("GPU", d.gpu)
            DetailRow("Resolution", d.resolution)
            DetailRow("Density", "${d.density} DPI")
            DetailRow("Screen Size", d.screenSize)
            DetailRow("Frame Rate", "${d.frameRate}Hz")
        }
    }
}

@Composable
private fun RamDetailContent(ram: RamStats?) {
    Column {
        ram?.let { r ->
            DetailRow("Total", formatBytes(r.totalBytes))
            DetailRow("Used", formatBytes(r.usedBytes))
            DetailRow("Available", formatBytes(r.availableBytes))
            DetailRow("Usage", "${r.usedPercent.toInt()}%")
            DetailRow("ZRAM Total", formatBytes(r.zramTotal))
            DetailRow("ZRAM Used", formatBytes(r.zramUsed))
        }
    }
}

@Composable
private fun StorageDetailContent(storage: StorageStats?) {
    Column {
        storage?.let { s ->
            DetailRow("Total", formatBytes(s.totalBytes))
            DetailRow("Used", formatBytes(s.usedBytes))
            DetailRow("Available", formatBytes(s.availableBytes))
            DetailRow("Usage", "${s.usedPercent.toInt()}%")
            DetailRow("Apps", formatBytes(s.appsBytes))
            DetailRow("System", formatBytes(s.systemBytes))
            DetailRow("Filesystem", s.filesystem)
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

private fun formatBytes(bytes: Long?): String {
    if (bytes == null) return "--"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}
