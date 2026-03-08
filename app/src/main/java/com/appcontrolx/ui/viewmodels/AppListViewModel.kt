package com.appcontrolx.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.domain.AppManager
import com.appcontrolx.domain.AppScanner
import com.appcontrolx.model.AppAction
import com.appcontrolx.model.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appScanner: AppScanner,
    private val appManager: AppManager
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("user")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _apps.value = appScanner.scanAllApps(forceRefresh = false, includeIcons = true)
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun getFilteredApps(): List<AppInfo> {
        val query = _searchQuery.value.lowercase()
        val filter = _selectedFilter.value

        return _apps.value.filter { app ->
            // Search filter
            val matchesSearch = if (query.isEmpty()) true else {
                app.appName.lowercase().contains(query) ||
                app.packageName.lowercase().contains(query)
            }

            // Category filter
            val matchesFilter = when (filter) {
                "user" -> !app.isSystemApp
                "system" -> app.isSystemApp
                "frozen" -> !app.isEnabled
                else -> true
            }

            matchesSearch && matchesFilter
        }.sortedBy { it.appName.lowercase() }
    }

    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedApps.value = emptySet()
        }
    }

    fun toggleAppSelection(packageName: String) {
        val current = _selectedApps.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedApps.value = current
    }

    fun executeAction(packageName: String, action: AppAction) {
        viewModelScope.launch {
            appManager.executeAction(packageName, action)
            appScanner.invalidateCache()
            loadApps()
        }
    }

    fun executeBatchAction(action: AppAction) {
        val selected = _selectedApps.value.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            appManager.executeBatchAction(selected, action) { _, _, _ -> }
            appScanner.invalidateCache()
            _selectedApps.value = emptySet()
            _isSelectionMode.value = false
            loadApps()
        }
    }
}
