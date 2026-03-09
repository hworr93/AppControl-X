package com.appcontrolx.ui.screens.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appcontrolx.ui.viewmodels.ToolsViewModel

data class HiddenSetting(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val intents: List<String>
)

data class HiddenSettingSection(
    val section: String,
    val items: List<HiddenSetting>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateToApps: () -> Unit,
    onNavigateToActivityLauncher: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ToolsViewModel = hiltViewModel()
) {
    val loadingId by viewModel.loadingId.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val hiddenSettingsSections = getHiddenSettingsSections()

    LaunchedEffect(errorMessage) {
        val message = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = message,
            actionLabel = "Dismiss",
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )
        viewModel.clearError()
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tools",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Actions
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Apps,
                        title = "App Manager",
                        subtitle = "Freeze, stop, uninstall",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToApps
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PlayArrow,
                        title = "Activity Launcher",
                        subtitle = "Launch any activity",
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = onNavigateToActivityLauncher
                    )
                }
            }

            // Hidden Settings Sections
            hiddenSettingsSections.forEach { section ->
                item {
                    Text(
                        section.section,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column {
                            section.items.forEachIndexed { index, item ->
                                SettingItem(
                                    setting = item,
                                    isLoading = loadingId == item.id,
                                    onClick = { viewModel.openHiddenSetting(item) },
                                    showBorder = index < section.items.size - 1
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.2f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingItem(
    setting: HiddenSetting,
    isLoading: Boolean,
    onClick: () -> Unit,
    showBorder: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (showBorder) {
                    Modifier.padding(bottom = 1.dp)
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        setting.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    setting.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    setting.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showBorder) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

private fun getHiddenSettingsSections(): List<HiddenSettingSection> {
    return listOf(
        HiddenSettingSection(
            section = "Display",
            items = listOf(
                HiddenSetting(
                    id = "extra-dim",
                    icon = Icons.Default.LightMode,
                    title = "Extra Dim",
                    description = "Reduce brightness below minimum",
                    intents = listOf(
                        "com.android.settings/com.android.settings.display.ReduceBrightColorsPreferenceFragment",
                        "com.android.settings/com.android.settings.Settings\$ReduceBrightColorsSettingsActivity"
                    )
                ),
                HiddenSetting(
                    id = "display-color",
                    icon = Icons.Default.Palette,
                    title = "Display Color",
                    description = "QColor, MiraVision, Screen mode",
                    intents = listOf(
                        "com.qualcomm.qti.qcolor/com.qualcomm.qti.qcolor.QColorActivity",
                        "com.mediatek.miravision.ui/com.mediatek.miravision.ui.MiraVisionActivity",
                        "com.android.settings/com.android.settings.display.ColorModePreferenceFragment",
                        "com.samsung.android.app.screenmode/com.samsung.android.app.screenmode.ScreenModeSettingsActivity"
                    )
                )
            )
        ),
        HiddenSettingSection(
            section = "Notifications",
            items = listOf(
                HiddenSetting(
                    id = "notification-log",
                    icon = Icons.Default.Notifications,
                    title = "Notification Log",
                    description = "View all past notifications",
                    intents = listOf(
                        "com.android.settings/com.android.settings.notification.NotificationStation",
                        "com.miui.securitycenter/com.miui.notificationlog.ui.main.NotificationLogActivity",
                        "com.android.settings/com.android.settings.Settings\$NotificationStationActivity"
                    )
                ),
                HiddenSetting(
                    id = "notification-history",
                    icon = Icons.Default.NotificationImportant,
                    title = "Notification History",
                    description = "Recently snoozed notifications",
                    intents = listOf(
                        "com.android.settings/com.android.settings.notification.history.NotificationHistoryActivity",
                        "com.android.settings/com.android.settings.Settings\$NotificationHistoryActivity"
                    )
                )
            )
        ),
        HiddenSettingSection(
            section = "Battery",
            items = listOf(
                HiddenSetting(
                    id = "battery-optimization",
                    icon = Icons.Default.BatteryStd,
                    title = "Battery Optimization",
                    description = "App battery usage settings",
                    intents = listOf(
                        "com.android.settings/com.android.settings.Settings\$AppBatteryUsageActivity",
                        "com.android.settings/com.android.settings.Settings\$HighPowerApplicationsActivity",
                        "com.android.settings/com.android.settings.fuelgauge.PowerUsageSummary"
                    )
                ),
                HiddenSetting(
                    id = "power-mode",
                    icon = Icons.Default.Bolt,
                    title = "Power Mode",
                    description = "Performance and battery saver",
                    intents = listOf(
                        "com.android.settings/com.android.settings.fuelgauge.PowerModeSettings",
                        "com.android.settings/com.android.settings.Settings\$PowerModeSettingsActivity",
                        "com.miui.powerkeeper/com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
                        "com.samsung.android.lool/com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                )
            )
        ),
        HiddenSettingSection(
            section = "System",
            items = listOf(
                HiddenSetting(
                    id = "device-info",
                    icon = Icons.Default.PhoneAndroid,
                    title = "Device Info",
                    description = "Detailed device information",
                    intents = listOf(
                        "com.android.settings/com.android.settings.Settings\$DeviceInfoSettingsActivity",
                        "com.android.settings/com.android.settings.DeviceInfoSettings"
                    )
                ),
                HiddenSetting(
                    id = "running-services",
                    icon = Icons.Default.DeveloperMode,
                    title = "Running Services",
                    description = "Device diagnostic and services",
                    intents = listOf(
                        "com.android.devicediagnostics/com.android.devicediagnostics.MainActivity",
                        "com.android.settings/com.android.settings.Settings\$DevRunningServicesActivity",
                        "com.android.settings/com.android.settings.applications.RunningServices"
                    )
                )
            )
        ),
        HiddenSettingSection(
            section = "Apps",
            items = listOf(
                HiddenSetting(
                    id = "unknown-sources",
                    icon = Icons.Default.Security,
                    title = "Unknown Sources",
                    description = "Install from unknown apps",
                    intents = listOf(
                        "com.android.settings/com.android.settings.Settings\$ManageExternalSourcesActivity",
                        "com.android.settings/com.android.settings.applications.manageapplications.ManageExternalSourcesActivity"
                    )
                ),
                HiddenSetting(
                    id = "manage-apps",
                    icon = Icons.Default.Apps,
                    title = "Manage Apps",
                    description = "System app manager",
                    intents = listOf(
                        "com.android.settings/com.android.settings.Settings\$ManageApplicationsActivity",
                        "com.android.settings/com.android.settings.applications.ManageApplications"
                    )
                ),
                HiddenSetting(
                    id = "autostart",
                    icon = Icons.Default.RocketLaunch,
                    title = "Autostart Manager",
                    description = "Control app autostart (OEM)",
                    intents = listOf(
                        "com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity",
                        "com.coloros.safecenter/com.coloros.privacypermissionsentry.PermissionTopActivity",
                        "com.oplus.safecenter/com.oplus.safecenter.permission.startup.StartupAppListActivity",
                        "com.vivo.permissionmanager/com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                        "com.huawei.systemmanager/com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                        "com.oneplus.security/com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
                        "com.samsung.android.lool/com.samsung.android.sm.ui.battery.BatteryActivity",
                        "com.asus.mobilemanager/com.asus.mobilemanager.autostart.AutoStartActivity"
                    )
                )
            )
        )
    )
}
