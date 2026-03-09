package com.appcontrolx.domain

import android.Manifest
import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.os.storage.StorageManager
import android.telephony.TelephonyManager
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.appcontrolx.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Enhanced SystemMonitor with comprehensive device information.
 * Adapted from ahmmedrejowan/DeviceInfo - https://github.com/ahmmedrejowan/DeviceInfo
 */
@Singleton
class SystemMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val cpuStateLock = Any()
    private val deviceStateLock = Any()
    private val storageCacheLock = Any()
    private var lastCpuTotal: Long = 0
    private var lastCpuIdle: Long = 0
    private var lastAwakeTime: Long = SystemClock.uptimeMillis()
    private var accumulatedSleepTime: Long = 0
    private var cachedAppsStorageBytes: Long = -1L
    private var appsStorageCacheTimestamp: Long = 0L
    private val appsStorageCacheTtlMs: Long = 60_000L

    fun getSystemStats(): SystemStats {
        return try {
            SystemStats(
                cpu = getCpuStats(),
                gpu = getGpuStats(),
                ram = getRamStats(),
                storage = getStorageStats(),
                battery = getBatteryStats(),
                network = getNetworkStats(),
                display = getDisplayStats()
            )
        } catch (e: Exception) {
            // Ultimate fallback to prevent crash
            SystemStats(
                cpu = CpuStats(0f, 0f, 0, emptyList()),
                gpu = GpuStats("Unknown", 0f),
                ram = RamStats(0, 0, 0, 0f, 0, 0),
                storage = StorageStats(0, 0, 0, 0f, 0, 0, "Unknown"),
                battery = BatteryStats(0, 0f, false, "Unknown", "Unknown", 0, 0, "Unknown"),
                network = NetworkStats(WifiStats(false, "", "", 0, 0, 0), MobileStats(false, ""), SimStats(false)),
                display = DisplayStats("Unknown", "Unknown", 0, "Unknown", 0)
            )
        }
    }

    fun getDeviceInfo(): DeviceInfo {
        try {
            val uptimeMs = SystemClock.elapsedRealtime()
            val awakeTimeMs = SystemClock.uptimeMillis()
            val deepSleepMs = uptimeMs - awakeTimeMs

            // Track accumulated sleep time for more accurate percentage
            synchronized(deviceStateLock) {
                val currentAwake = SystemClock.uptimeMillis()
                val awakeDiff = currentAwake - lastAwakeTime
                lastAwakeTime = currentAwake
                accumulatedSleepTime += (uptimeMs - awakeDiff - accumulatedSleepTime).coerceAtLeast(0)
            }

            val deepSleepPercent = if (uptimeMs > 0) {
                ((deepSleepMs.toFloat() / uptimeMs.toFloat()) * 100).toInt()
            } else 0

            return DeviceInfo(
                model = getDeviceModelName(),
                brand = Build.BRAND.uppercase(),
                processor = getProcessorName(),
                androidVersion = getAndroidVersionString(),
                uptime = formatDuration(uptimeMs),
                deepSleep = formatDuration(deepSleepMs),
                deepSleepPercent = deepSleepPercent
            )
        } catch (e: Exception) {
            return DeviceInfo("Unknown", "Unknown", "Unknown", "Unknown", "0s", "0s", 0)
        }
    }

    fun getRealtimeStatus(): RealtimeStatus {
        return RealtimeStatus(
            cpuFrequencies = getCoreFrequencies(),
            cpuTemp = getCpuTemperature(),
            gpuTemp = getGpuTemperature()
        )
    }

    fun stopSystemMonitor() {
        synchronized(cpuStateLock) {
            lastCpuTotal = 0
            lastCpuIdle = 0
        }
        synchronized(deviceStateLock) {
            lastAwakeTime = SystemClock.uptimeMillis()
            accumulatedSleepTime = 0
        }
        synchronized(storageCacheLock) {
            cachedAppsStorageBytes = -1L
            appsStorageCacheTimestamp = 0L
        }
    }

    fun getCoreFrequencies(): List<Long> {
        return try {
            val cores = Runtime.getRuntime().availableProcessors()
            getCoreFrequenciesInternal(cores)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getCpuStats(): CpuStats {
        return try {
            val usage = calculateCpuUsage()
            val temperature = getCpuTemperature()
            val cores = Runtime.getRuntime().availableProcessors()
            val frequencies = getCoreFrequenciesInternal(cores)

            CpuStats(
                usagePercent = usage,
                temperature = temperature,
                cores = cores,
                coreFrequencies = frequencies
            )
        } catch (e: Exception) {
            CpuStats(0f, 0f, 0, emptyList())
        }
    }

    private fun getGpuStats(): GpuStats {
        return try {
            val gpuName = getGpuName()
            val gpuTemp = getGpuTemperature()

            GpuStats(
                name = gpuName,
                temperature = gpuTemp
            )
        } catch (e: Exception) {
            GpuStats("Unknown", 0f)
        }
    }

    private fun calculateCpuUsage(): Float {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val line = reader.readLine()
            reader.close()

            val parts = line.split("\\s+".toRegex())
            val user = parts[1].toLong()
            val nice = parts[2].toLong()
            val system = parts[3].toLong()
            val idle = parts[4].toLong()
            val iowait = parts[5].toLong()
            val irq = parts[6].toLong()
            val softirq = parts[7].toLong()

            val total = user + nice + system + idle + iowait + irq + softirq
            val (totalDiff, idleDiff) = synchronized(cpuStateLock) {
                val totalDiffLocal = total - lastCpuTotal
                val idleDiffLocal = idle - lastCpuIdle
                lastCpuTotal = total
                lastCpuIdle = idle
                Pair(totalDiffLocal, idleDiffLocal)
            }

            if (totalDiff == 0L) 0f
            else ((totalDiff - idleDiff).toFloat() / totalDiff.toFloat()) * 100f
        } catch (e: Exception) {
            0f
        }
    }

    private fun getCpuTemperature(): Float? {
        val thermalPaths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/class/hwmon/hwmon0/temp1_input",
            "/sys/devices/platform/soc/soc:qcom,bcl/power_supply/msm-bcl-qm/temp"
        )

        for (path in thermalPaths) {
            try {
                val file = File(path)
                if (file.canRead()) {
                    val temp = file.readText().trim().toLong()
                    return if (temp > 1000) temp / 1000f else temp.toFloat()
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun getGpuName(): String {
        // Try to get from SOC model on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socModel = Build.SOC_MODEL
            if (socModel.isNotEmpty() && socModel != "UNKNOWN") {
                return getGpuFromSoc(socModel)
            }
        }

        // Try to read from /proc/cpuinfo
        try {
            val file = File("/proc/cpuinfo")
            if (file.canRead()) {
                val cpuInfo = file.readText()
                val hardwareLine = cpuInfo.lines().find { it.trim().startsWith("Hardware") }
                val hardware = hardwareLine?.substringAfter(":")?.trim() ?: ""

                if (hardware.contains("Qualcomm") || hardware.contains("SDM") || hardware.contains("SM")) {
                    return getGpuFromSoc(hardware)
                }
            }
        } catch (_: Exception) {}

        return "Unknown GPU"
    }

    private fun getGpuFromSoc(socName: String): String {
        // Map common SoC names to GPU names
        return when {
            socName.contains("SM8750") || socName.contains("8 Gen 4") -> "Adreno 830"
            socName.contains("SM8650") || socName.contains("8 Gen 3") -> "Adreno 750"
            socName.contains("SM8550") || socName.contains("8 Gen 2") -> "Adreno 740"
            socName.contains("SM8475") || socName.contains("8+ Gen 1") -> "Adreno 730"
            socName.contains("SM8450") || socName.contains("8 Gen 1") -> "Adreno 730"
            socName.contains("SM8350") || socName.contains("888") -> "Adreno 660"
            socName.contains("SM8250") || socName.contains("865") -> "Adreno 650"
            socName.contains("SM8150") || socName.contains("855") -> "Adreno 640"
            socName.contains("SDM845") || socName.contains("845") -> "Adreno 630"
            socName.contains("SDM835") || socName.contains("835") -> "Adreno 540"
            socName.contains("SDM765") || socName.contains("765") -> "Adreno 620"
            socName.contains("SDM750") || socName.contains("750") -> "Adreno 619"
            socName.contains("SDM730") || socName.contains("730") -> "Adreno 618"
            socName.contains("SDM720") || socName.contains("720") -> "Adreno 618"
            socName.contains("Dimensity") && socName.contains("9000") -> "Mali-G710"
            socName.contains("Dimensity") && socName.contains("8000") -> "Mali-G610"
            socName.contains("Exynos") && socName.contains("2400") -> "Xclipse 940"
            socName.contains("Exynos") && socName.contains("2200") -> "Xclipse 920"
            socName.contains("Tensor") && socName.contains("G4") -> "Mali-G715"
            socName.contains("Tensor") && socName.contains("G3") -> "Mali-G715"
            else -> socName
        }
    }

    private fun getGpuTemperature(): Float? {
        val gpuThermalPaths = listOf(
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone2/temp",
            "/sys/class/kgsl/kgsl-3d0/temp",
            "/sys/devices/virtual/thermal/thermal_zone1/temp"
        )

        for (path in gpuThermalPaths) {
            try {
                val file = File(path)
                if (file.canRead()) {
                    val temp = file.readText().trim().toLong()
                    return if (temp > 1000) temp / 1000f else temp.toFloat()
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun getCoreFrequenciesInternal(cores: Int): List<Long> {
        val frequencies = mutableListOf<Long>()
        for (i in 0 until cores) {
            try {
                val file = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
                if (file.canRead()) {
                    val freq = file.readText().trim().toLong() / 1000
                    frequencies.add(freq)
                } else {
                    frequencies.add(0L)
                }
            } catch (_: Exception) {
                frequencies.add(0L)
            }
        }
        return frequencies
    }

    private fun getRamStats(): RamStats {
        return try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)

            val totalBytes = memInfo.totalMem
            val availableBytes = memInfo.availMem
            val usedBytes = totalBytes - availableBytes
            val usedPercent = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f

            // Get ZRAM info
            val (zramTotal, zramUsed) = getZramInfo()

            RamStats(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                availableBytes = availableBytes,
                usedPercent = usedPercent,
                zramTotal = zramTotal,
                zramUsed = zramUsed
            )
        } catch (e: Exception) {
            RamStats(0, 0, 0, 0f, 0, 0)
        }
    }

    private fun getZramInfo(): Pair<Long, Long> {
        try {
            val zramSizeFile = File("/sys/block/zram0/disksize")
            val zramUsedFile = File("/sys/block/zram0/mem_used_total")

            if (zramSizeFile.exists() && zramUsedFile.exists() && zramSizeFile.canRead() && zramUsedFile.canRead()) {
                val total = zramSizeFile.readText().trim().toLongOrNull() ?: 0L
                val used = zramUsedFile.readText().trim().toLongOrNull() ?: 0L
                return Pair(total, used)
            }
        } catch (_: Exception) {}
        return Pair(0L, 0L)
    }

    private fun getStorageStats(): StorageStats {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes
            val availableBytes = stat.availableBytes
            val usedBytes = totalBytes - availableBytes
            val usedPercent = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f

            // Get apps and system storage size
            val appsBytes = getAppsStorageSize()
            val systemBytes = getSystemStorageSize()
            val filesystem = getFilesystem()

            StorageStats(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                availableBytes = availableBytes,
                usedPercent = usedPercent,
                appsBytes = appsBytes,
                systemBytes = systemBytes,
                filesystem = filesystem
            )
        } catch (e: Exception) {
            StorageStats(0, 0, 0, 0f, 0, 0, "Unknown")
        }
    }

    private fun getAppsStorageSize(): Long {
        val now = System.currentTimeMillis()
        val cached = synchronized(storageCacheLock) {
            if (cachedAppsStorageBytes >= 0 && (now - appsStorageCacheTimestamp) < appsStorageCacheTtlMs) {
                cachedAppsStorageBytes
            } else {
                null
            }
        }
        if (cached != null) return cached

        return try {
            val computed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val uuid = StorageManager.UUID_DEFAULT

                val pm = context.packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                var totalSize = 0L
                for (app in packages) {
                    try {
                        val stats = storageStatsManager.queryStatsForPackage(uuid, app.packageName, android.os.Process.myUserHandle())
                        totalSize += stats.appBytes + stats.dataBytes + stats.cacheBytes
                    } catch (_: Exception) {}
                }
                totalSize
            } else {
                0L
            }

            synchronized(storageCacheLock) {
                cachedAppsStorageBytes = computed
                appsStorageCacheTimestamp = now
            }
            computed
        } catch (_: Exception) {
            0L
        }
    }

    private fun getSystemStorageSize(): Long {
        return try {
            val systemStat = StatFs("/system")
            systemStat.totalBytes - systemStat.availableBytes
        } catch (_: Exception) {
            0L
        }
    }

    private fun getFilesystem(): String {
        return try {
            val file = File("/proc/mounts")
            if (file.canRead()) {
                val mounts = file.readText()
                val dataMount = mounts.lines().find { it.contains("/data") }
                when {
                    dataMount?.contains("f2fs") == true -> "f2fs"
                    dataMount?.contains("ext4") == true -> "ext4"
                    dataMount?.contains("erofs") == true -> "erofs"
                    else -> "unknown"
                }
            } else "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    private fun getBatteryStats(): BatteryStats {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val percent = if (scale > 0) (level * 100 / scale) else 0

            val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val temperature = temp / 10f

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val healthInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
            val health = when (healthInt) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                else -> "Unknown"
            }

            val technology = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
            val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

            // Get battery capacity (mAh) - requires BatteryManager service
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000

            // Calculate remaining time estimate
            val remainingTime = calculateRemainingTime(percent, isCharging)

            BatteryStats(
                percent = percent,
                temperature = temperature,
                isCharging = isCharging,
                health = health,
                technology = technology,
                voltage = voltage,
                capacity = if (capacity > 0) capacity else 0,
                remainingTime = remainingTime
            )
        } catch (e: Exception) {
            BatteryStats(0, 0f, false, "Unknown", "Unknown", 0, 0, "Unknown")
        }
    }

    private fun calculateRemainingTime(percent: Int, isCharging: Boolean): String {
        return try {
            // Simple estimation based on average usage patterns
            if (isCharging) {
                val remainingPercent = 100 - percent
                val minutesToFull = (remainingPercent * 1.2).toInt() // Assume ~1.2 min per percent
                formatDurationShort((minutesToFull * 60 * 1000).toLong())
            } else {
                // Estimate based on current percent and average screen-on time of 6 hours at 100%
                val minutesRemaining = (percent * 3.6).toInt() // 6 hours = 360 min for 100%
                formatDurationShort((minutesRemaining * 60 * 1000).toLong())
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getNetworkStats(): NetworkStats {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val hasNetworkStatePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) == PackageManager.PERMISSION_GRANTED
            val hasWifiStatePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_WIFI_STATE
            ) == PackageManager.PERMISSION_GRANTED
            val hasReadPhoneStatePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            // Check permissions for WifiManager and TelephonyManager access
            // If we don't handle this, it crashes on restricted devices
            val wifiStats = try {
                val wifiConnected = if (hasNetworkStatePermission) {
                    val network = connectivityManager.activeNetwork
                    val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                } else {
                    false
                }

                if (wifiConnected && hasWifiStatePermission) {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    @Suppress("DEPRECATION")
                    val wifiInfo = wifiManager.connectionInfo
                    val ssid = wifiInfo.ssid?.replace("\"", "") ?: "Unknown"
                    val speed = wifiInfo.linkSpeed
                    val rssi = wifiInfo.rssi
                    val signalLevel = WifiManager.calculateSignalLevel(rssi, 100)

                    val ip = try {
                        val ipInt = wifiInfo.ipAddress
                        String.format(
                            "%d.%d.%d.%d",
                            ipInt and 0xff,
                            ipInt shr 8 and 0xff,
                            ipInt shr 16 and 0xff,
                            ipInt shr 24 and 0xff
                        )
                    } catch (_: Exception) { "" }

                    WifiStats(true, ssid, ip, speed, signalLevel, rssi)
                } else {
                    WifiStats(false, "", "", 0, 0, 0)
                }
            } catch (e: Exception) {
                WifiStats(false, "", "", 0, 0, 0)
            }

            val (mobileConnected, mobileType, simPresent) = try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val mobConnected = if (hasNetworkStatePermission) {
                    val network = connectivityManager.activeNetwork
                    val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                } else {
                    false
                }

                val type = if (mobConnected) {
                    if (hasReadPhoneStatePermission) {
                        @Suppress("DEPRECATION")
                        when (telephonyManager.networkType) {
                            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                            TelephonyManager.NETWORK_TYPE_NR -> "5G"
                            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
                            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
                            else -> "Mobile"
                        }
                    } else {
                        "Mobile"
                    }
                } else ""

                val sim = if (hasReadPhoneStatePermission) {
                    telephonyManager.simState == TelephonyManager.SIM_STATE_READY
                } else {
                    false
                }
                Triple(mobConnected, type, sim)
            } catch (e: Exception) {
                Triple(false, "", false)
            }

            NetworkStats(
                wifi = wifiStats,
                mobile = MobileStats(connected = mobileConnected, type = mobileType),
                sim = SimStats(present = simPresent)
            )
        } catch (e: Exception) {
            NetworkStats(WifiStats(false, "", "", 0, 0, 0), MobileStats(false, ""), SimStats(false))
        }
    }

    private fun getDisplayStats(): DisplayStats {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val mode = display.mode

            val width = mode.physicalWidth
            val height = mode.physicalHeight
            val refreshRate = mode.refreshRate.toInt()

            val metrics = context.resources.displayMetrics
            val density = metrics.densityDpi

            // Calculate screen size in inches
            val widthInches = width / metrics.xdpi.toDouble()
            val heightInches = height / metrics.ydpi.toDouble()
            val screenSize = sqrt(widthInches * widthInches + heightInches * heightInches)
            val screenSizeStr = String.format("%.2f\"", screenSize)

            DisplayStats(
                gpu = getGpuName(),
                resolution = "$width x $height",
                density = density,
                screenSize = screenSizeStr,
                frameRate = refreshRate
            )
        } catch (e: Exception) {
            DisplayStats("Unknown", "Unknown", 0, "Unknown", 0)
        }
    }

    private fun getDeviceModelName(): String {
        return try {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL

            if (model.startsWith(manufacturer, ignoreCase = true)) {
                model.replaceFirstChar { it.uppercase() }
            } else {
                "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
            }
        } catch (e: Exception) {
            "Unknown Device"
        }
    }

    private fun getProcessorName(): String {
        return try {
            // Try to get SOC model on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val socModel = Build.SOC_MODEL
                if (socModel.isNotEmpty() && socModel != "UNKNOWN") {
                    return formatProcessorName(socModel)
                }
            }

            // Fallback to reading from /proc/cpuinfo
            try {
                val file = File("/proc/cpuinfo")
                if (file.canRead()) {
                    val cpuInfo = file.readText()
                    val hardwareLine = cpuInfo.lines().find { it.trim().startsWith("Hardware") }
                    val hardware = hardwareLine?.substringAfter(":")?.trim()

                    if (hardware != null) {
                        return formatProcessorName(hardware)
                    }
                }
            } catch (_: Exception) {}

            Build.HARDWARE
        } catch (e: Exception) {
            "Unknown Processor"
        }
    }

    private fun formatProcessorName(name: String): String {
        // Clean up common prefixes
        var cleaned = name
            .replace("Qualcomm Technologies, Inc", "")
            .replace("Qualcomm", "")
            .trim()

        // Map to marketing names
        cleaned = when {
            cleaned.contains("SM8750") -> "Snapdragon 8 Gen 4"
            cleaned.contains("SM8650") -> "Snapdragon 8 Gen 3"
            cleaned.contains("SM8550") -> "Snapdragon 8 Gen 2"
            cleaned.contains("SM8475") -> "Snapdragon 8+ Gen 1"
            cleaned.contains("SM8450") -> "Snapdragon 8 Gen 1"
            cleaned.contains("SM8350") -> "Snapdragon 888"
            cleaned.contains("SM8250") -> "Snapdragon 865"
            cleaned.contains("SM7675") -> "Snapdragon 8s Gen 4"
            cleaned.contains("SM7550") -> "Snapdragon 7+ Gen 3"
            cleaned.contains("SM7450") -> "Snapdragon 7 Gen 3"
            cleaned.contains("lahaina") -> "Snapdragon 888"
            cleaned.contains("kona") -> "Snapdragon 865"
            cleaned.contains("taro") -> "Snapdragon 8 Gen 1"
            cleaned.contains("kalama") -> "Snapdragon 8 Gen 2"
            cleaned.contains("pineapple") -> "Snapdragon 8 Gen 3"
            else -> cleaned.ifEmpty { name }
        }

        return cleaned
    }

    private fun getAndroidVersionString(): String {
        return try {
            val version = Build.VERSION.RELEASE
            val sdkInt = Build.VERSION.SDK_INT

            val codename = when (sdkInt) {
                36 -> "Baklava"
                35 -> "Vanilla Ice Cream"
                34 -> "Upside Down Cake"
                33 -> "Tiramisu"
                32 -> "Sv2"
                31 -> "Snow Cone"
                30 -> "Red Velvet Cake"
                29 -> "Quince Tart"
                28 -> "Pie"
                else -> ""
            }

            if (codename.isNotEmpty()) {
                "Android $version ($codename)"
            } else {
                "Android $version"
            }
        } catch (e: Exception) {
            "Unknown Android"
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60)) % 24
        val days = millis / (1000 * 60 * 60 * 24)

        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    private fun formatDurationShort(millis: Long): String {
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60)) % 24
        val days = millis / (1000 * 60 * 60 * 24)

        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
