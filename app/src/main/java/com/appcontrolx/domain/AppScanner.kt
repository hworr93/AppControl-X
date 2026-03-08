package com.appcontrolx.domain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Base64
import com.appcontrolx.core.ShellManager
import com.appcontrolx.model.AppActivities
import com.appcontrolx.model.AppActivityFilter
import com.appcontrolx.model.AppInfo
import com.appcontrolx.model.ExecutionMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shellManager: ShellManager,
    private val safetyValidator: SafetyValidator
) {
    private val packageManager: PackageManager = context.packageManager
    private val cacheLock = Any()
    private val cachedAppsByIconMode = mutableMapOf<Boolean, CacheEntry<List<AppInfo>>>()
    private var runningPackagesCache: CacheEntry<Set<String>>? = null
    private var backgroundRestrictedCache: CacheEntry<Set<String>>? = null
    private val cacheValidityMs = 30_000L
    private val runningCacheValidityMs = 10_000L

    private data class CacheEntry<T>(
        val value: T,
        val timestamp: Long
    )

    suspend fun scanAllApps(forceRefresh: Boolean = false, includeIcons: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val cached = synchronized(cacheLock) {
                cachedAppsByIconMode[includeIcons]?.takeIf { (now - it.timestamp) < cacheValidityMs }?.value
            }
            if (cached != null) {
                return@withContext cached
            }
        }

        val runningPackages = getRunningPackages(forceRefresh)
        val backgroundRestrictedPackages = getBackgroundRestrictedPackages(forceRefresh)
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)

        val apps = packages.mapNotNull { pkg ->
            try {
                val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isEnabled = appInfo.enabled
                val isFrozen = isAppFrozen(pkg.packageName, appInfo)
                val isRunning = pkg.packageName in runningPackages

                AppInfo(
                    packageName = pkg.packageName,
                    appName = appInfo.loadLabel(packageManager).toString(),
                    iconBase64 = if (includeIcons) getIconBase64(appInfo) else null, // Lazy load icons
                    versionName = pkg.versionName ?: "",
                    isSystemApp = isSystemApp,
                    isEnabled = isEnabled,
                    isRunning = isRunning,
                    isFrozen = isFrozen,
                    isBackgroundRestricted = pkg.packageName in backgroundRestrictedPackages,
                    size = getAppSize(appInfo),
                    uid = appInfo.uid,
                    safetyLevel = safetyValidator.getSafetyLevel(pkg.packageName),
                    installPath = appInfo.sourceDir,
                    targetSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.targetSdkVersion else null,
                    minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else null,
                    permissions = pkg.requestedPermissions?.size
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.appName.lowercase() }

        synchronized(cacheLock) {
            cachedAppsByIconMode[includeIcons] = CacheEntry(apps, now)
        }
        apps
    }

    suspend fun getAppIcon(packageName: String): String? = withContext(Dispatchers.IO) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            getIconBase64(appInfo)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun scanAppActivities(filter: AppActivityFilter = AppActivityFilter()): List<AppActivities> = withContext(Dispatchers.IO) {
        val packages = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)
        packages.mapNotNull { pkg ->
            try {
                // Filter by app type first
                val appInfo = pkg.applicationInfo
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                when (filter.type) {
                    "user" -> if (isSystemApp) return@mapNotNull null
                    "system" -> if (!isSystemApp) return@mapNotNull null
                    else -> {} // "all" - include everything
                }

                // Filter by search query (app name or package name)
                if (filter.search.isNotEmpty()) {
                    val appName = appInfo.loadLabel(packageManager).toString()
                    val searchLower = filter.search.lowercase()
                    val matchesApp = appName.lowercase().contains(searchLower) ||
                                    pkg.packageName.lowercase().contains(searchLower)

                    if (!matchesApp) {
                        // Check if any activity matches
                        val matchesActivity = pkg.activities?.any { activityInfo ->
                            activityInfo.name.lowercase().contains(searchLower)
                        } ?: false

                        if (!matchesActivity) return@mapNotNull null
                    }
                }

                val activities = pkg.activities?.mapNotNull { activityInfo ->
                    try {
                        // Filter hidden/test activities
                        if (isHiddenActivity(activityInfo.name)) return@mapNotNull null

                        com.appcontrolx.model.ActivityInfo(
                            activityName = activityInfo.name,
                            isExported = activityInfo.exported,
                            canLaunchWithoutRoot = activityInfo.exported,
                            hasLauncherIntent = activityInfo.labelRes != 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: return@mapNotNull null

                if (activities.isEmpty()) return@mapNotNull null

                AppActivities(
                    packageName = pkg.packageName,
                    appName = appInfo.loadLabel(packageManager).toString(),
                    iconBase64 = getIconBase64(appInfo),
                    isSystem = isSystemApp,
                    activities = activities.sortedBy { it.activityName }
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.appName.lowercase() }
    }

    private fun isHiddenActivity(name: String): Boolean {
        val lowerName = name.lowercase()
        return lowerName.contains("test") ||
               lowerName.contains("debug") ||
               lowerName.contains("internal") ||
               lowerName.endsWith("receiver") ||
               lowerName.endsWith("service") ||
               lowerName.endsWith("provider")
    }

    fun invalidateCache() {
        synchronized(cacheLock) {
            cachedAppsByIconMode.clear()
            runningPackagesCache = null
            backgroundRestrictedCache = null
        }
    }

    private suspend fun getRunningPackages(forceRefresh: Boolean): Set<String> {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val cached = synchronized(cacheLock) {
                runningPackagesCache?.takeIf { (now - it.timestamp) < runningCacheValidityMs }?.value
            }
            if (cached != null) return cached
        }

        val running = mutableSetOf<String>()

        if (shellManager.getMode() != ExecutionMode.NONE) {
            try {
                val result = shellManager.execute("dumpsys activity processes")
                result.onSuccess { output ->
                    val appPattern = Regex("""ProcessRecord\{[^}]+\s+\d+:([a-zA-Z][a-zA-Z0-9_.]*)/""")
                    appPattern.findAll(output).forEach { match ->
                        match.groupValues.getOrNull(1)?.let { running.add(it) }
                    }
                }
            } catch (_: Exception) {}
        }

        synchronized(cacheLock) {
            runningPackagesCache = CacheEntry(running.toSet(), now)
        }
        return running
    }

    private suspend fun getBackgroundRestrictedPackages(forceRefresh: Boolean): Set<String> {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val cached = synchronized(cacheLock) {
                backgroundRestrictedCache?.takeIf { (now - it.timestamp) < cacheValidityMs }?.value
            }
            if (cached != null) return cached
        }

        val restricted = mutableSetOf<String>()
        if (shellManager.getMode() != ExecutionMode.NONE) {
            try {
                shellManager.execute("cmd appops query-op RUN_ANY_IN_BACKGROUND ignore").onSuccess { output ->
                    restricted += parsePackagesFromAppOps(output)
                }
                if (restricted.isEmpty()) {
                    shellManager.execute("cmd appops query-op RUN_IN_BACKGROUND ignore").onSuccess { output ->
                        restricted += parsePackagesFromAppOps(output)
                    }
                }
            } catch (_: Exception) {}
        }

        val immutable = restricted.toSet()
        synchronized(cacheLock) {
            backgroundRestrictedCache = CacheEntry(immutable, now)
        }
        return immutable
    }

    private fun parsePackagesFromAppOps(output: String): Set<String> {
        val packages = mutableSetOf<String>()

        val keyedPattern = Regex("""(?:package|pkg=)\s*([a-zA-Z][a-zA-Z0-9_.]*)""")
        keyedPattern.findAll(output).forEach { match ->
            match.groupValues.getOrNull(1)?.let { if (it.contains('.')) packages.add(it) }
        }

        if (packages.isEmpty()) {
            val genericPattern = Regex("""\b([a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z0-9_]+)+)\b""")
            genericPattern.findAll(output).forEach { match ->
                match.groupValues.getOrNull(1)?.let { packages.add(it) }
            }
        }

        return packages
    }

    private fun isAppFrozen(packageName: String, appInfo: ApplicationInfo): Boolean {
        return try {
            when (packageManager.getApplicationEnabledSetting(packageName)) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> !appInfo.enabled
                else -> !appInfo.enabled
            }
        } catch (_: Exception) {
            !appInfo.enabled
        }
    }

    private fun getAppSize(appInfo: ApplicationInfo): Long {
        return try {
            appInfo.sourceDir?.let { java.io.File(it).length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getIconBase64(appInfo: ApplicationInfo): String? {
        return try {
            val drawable = appInfo.loadIcon(packageManager)
            val bitmap = drawableToBitmap(drawable)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
