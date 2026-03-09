package com.appcontrolx.ui.screens.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.domain.AppManager
import com.appcontrolx.domain.AppScanner
import com.appcontrolx.model.AppAction
import com.appcontrolx.model.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appScanner: AppScanner,
    private val appManager: AppManager
) : ViewModel() {

    data class FilterCounts(
        val all: Int = 0,
        val user: Int = 0,
        val system: Int = 0,
        val frozen: Int = 0
    )

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

    val filteredApps: StateFlow<List<AppInfo>> = combine(_apps, _searchQuery, _selectedFilter) { apps, query, filter ->
        val normalizedQuery = query.trim().lowercase()

        apps.asSequence()
            .filter { app ->
                val matchesSearch = if (normalizedQuery.isEmpty()) {
                    true
                } else {
                    app.appName.lowercase().contains(normalizedQuery) ||
                        app.packageName.lowercase().contains(normalizedQuery)
                }

                val matchesFilter = when (filter) {
                    "user" -> !app.isSystemApp
                    "system" -> app.isSystemApp
                    "frozen" -> !app.isEnabled
                    else -> true
                }

                matchesSearch && matchesFilter
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filterCounts: StateFlow<FilterCounts> = _apps
        .map { apps ->
            FilterCounts(
                all = apps.size,
                user = apps.count { !it.isSystemApp },
                system = apps.count { it.isSystemApp },
                frozen = apps.count { !it.isEnabled }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterCounts())

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
