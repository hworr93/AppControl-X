package com.appcontrolx.ui.viewmodels

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.core.ShellManager
import com.appcontrolx.data.ThemeMode
import com.appcontrolx.data.UserPreferences
import com.appcontrolx.domain.AppManager
import com.appcontrolx.model.ActionHistoryItem
import com.appcontrolx.model.AppAction
import com.appcontrolx.model.ExecutionMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppInfoData(
    val version: String = "3.1.0",
    val versionCode: Int = 2,
    val packageName: String = "com.appcontrolx",
    val installLocation: String = "Internal"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val shellManager: ShellManager,
    private val appManager: AppManager
) : ViewModel() {

    private val _executionMode = MutableStateFlow(ExecutionMode.NONE)
    val executionMode: StateFlow<ExecutionMode> = _executionMode.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    private val _actionHistory = MutableStateFlow<List<ActionHistoryItem>>(emptyList())
    val actionHistory: StateFlow<List<ActionHistoryItem>> = _actionHistory.asStateFlow()

    private val _appInfo = MutableStateFlow(AppInfoData())
    val appInfo: StateFlow<AppInfoData> = _appInfo.asStateFlow()

    init {
        loadExecutionMode()
        loadActionHistory()
        loadAppInfo()
    }

    private fun loadExecutionMode() {
        _executionMode.value = shellManager.getMode()
    }

    private fun loadActionHistory() {
        // TODO: Implement action history storage
        _actionHistory.value = emptyList()
    }

    private fun loadAppInfo() {
        viewModelScope.launch {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                _appInfo.value = AppInfoData(
                    version = packageInfo.versionName ?: "Unknown",
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode
                    },
                    packageName = context.packageName,
                    installLocation = "Internal"
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // Keep default values
            }
        }
    }

    fun setExecutionMode(mode: ExecutionMode) {
        viewModelScope.launch {
            shellManager.setMode(mode.name)
            _executionMode.value = shellManager.getMode()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun rollbackAction(item: ActionHistoryItem) {
        viewModelScope.launch {
            val rollbackAction = when (item.action) {
                AppAction.FREEZE -> AppAction.UNFREEZE
                AppAction.UNFREEZE -> AppAction.FREEZE
                else -> return@launch
            }
            appManager.executeAction(item.packageName, rollbackAction)
            loadActionHistory()
        }
    }

    fun clearHistory() {
        // TODO: Implement action history clearing
        _actionHistory.value = emptyList()
    }
}
