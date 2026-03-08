package com.appcontrolx.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.core.ShellManager
import com.appcontrolx.domain.AppScanner
import com.appcontrolx.model.AppActivities
import com.appcontrolx.model.AppActivityFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityLauncherViewModel @Inject constructor(
    private val appScanner: AppScanner,
    private val shellManager: ShellManager
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppActivities>>(emptyList())
    val apps: StateFlow<List<AppActivities>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("user")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _expandedApps = MutableStateFlow<Set<String>>(emptySet())
    val expandedApps: StateFlow<Set<String>> = _expandedApps.asStateFlow()

    private val _launchingActivity = MutableStateFlow<String?>(null)
    val launchingActivity: StateFlow<String?> = _launchingActivity.asStateFlow()

    init {
        loadActivities()
    }

    private fun loadActivities() {
        viewModelScope.launch {
            _isLoading.value = true
            val filter = AppActivityFilter(type = _selectedFilter.value, search = _searchQuery.value)
            _apps.value = appScanner.scanAppActivities(filter)
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadActivities()
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
        loadActivities()
    }

    fun toggleAppExpansion(packageName: String) {
        val current = _expandedApps.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _expandedApps.value = current
    }

    fun launchActivity(packageName: String, activityName: String) {
        val key = "$packageName/$activityName"
        _launchingActivity.value = key
        viewModelScope.launch {
            try {
                // Launch using shell command
                shellManager.execute("am start -n $packageName/$activityName")
            } finally {
                kotlinx.coroutines.delay(500)
                _launchingActivity.value = null
            }
        }
    }
}
