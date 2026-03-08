package com.appcontrolx.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.core.ShellManager
import com.appcontrolx.data.ThemeMode
import com.appcontrolx.data.UserPreferences
import com.appcontrolx.model.ExecutionMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val shellManager: ShellManager
) : ViewModel() {

    val isSetupCompleted: StateFlow<Boolean> = userPreferences.isSetupCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<ThemeMode> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    private val _executionMode = MutableStateFlow(ExecutionMode.NONE)
    val executionMode: StateFlow<ExecutionMode> = _executionMode.asStateFlow()

    init {
        detectExecutionMode()
    }

    fun completeSetup() {
        viewModelScope.launch {
            userPreferences.setSetupCompleted(true)
            userPreferences.setFirstLaunch(false)
        }
    }

    fun resetSetup() {
        viewModelScope.launch {
            userPreferences.resetSetup()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun detectExecutionMode() {
        viewModelScope.launch {
            val mode = shellManager.initialize()
            _executionMode.value = mode
        }
    }
}
