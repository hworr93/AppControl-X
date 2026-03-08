package com.appcontrolx.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.domain.AppScanner
import com.appcontrolx.domain.SystemMonitor
import com.appcontrolx.model.AppInfo
import com.appcontrolx.model.DeviceInfo
import com.appcontrolx.model.SystemStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val systemMonitor: SystemMonitor,
    private val appScanner: AppScanner
) : ViewModel() {

    private val _systemStats = MutableStateFlow<SystemStats?>(null)
    val systemStats: StateFlow<SystemStats?> = _systemStats.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadInitialData()
        startMonitoring()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Load device info
            _deviceInfo.value = systemMonitor.getDeviceInfo()
            
            // Load apps (without icons for faster load)
            _apps.value = appScanner.scanAllApps(forceRefresh = false, includeIcons = false)
            
            // Load initial stats
            _systemStats.value = systemMonitor.getSystemStats()
            
            _isLoading.value = false
        }
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            while (isActive) {
                _systemStats.value = systemMonitor.getSystemStats()
                delay(2000) // Update every 2 seconds
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        systemMonitor.stopSystemMonitor()
    }
}
