package com.appcontrolx.ui.screens.about

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class AboutAppInfo(
    val version: String = "3.1.0",
    val versionCode: Int = 2,
    val packageName: String = "com.appcontrolx"
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _appInfo = MutableStateFlow(AboutAppInfo())
    val appInfo: StateFlow<AboutAppInfo> = _appInfo.asStateFlow()

    init {
        loadAppInfo()
    }

    private fun loadAppInfo() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            _appInfo.value = AboutAppInfo(
                version = packageInfo.versionName ?: "Unknown",
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                },
                packageName = context.packageName
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Keep default values
        }
    }
}
