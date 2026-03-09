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
class SetupViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val shellManager: ShellManager
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _executionMode = MutableStateFlow(ExecutionMode.NONE)
    val executionMode: StateFlow<ExecutionMode> = _executionMode.asStateFlow()

    private val _selectedTheme = MutableStateFlow(ThemeMode.SYSTEM)
    val selectedTheme: StateFlow<ThemeMode> = _selectedTheme.asStateFlow()

    init {
        detectExecutionMode()
    }

    private fun detectExecutionMode() {
        viewModelScope.launch {
            val mode = shellManager.initialize()
            _executionMode.value = mode
        }
    }

    fun nextStep() {
        if (_currentStep.value < 3) {
            _currentStep.value += 1
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value -= 1
        }
    }

    fun setTheme(theme: ThemeMode) {
        _selectedTheme.value = theme
        viewModelScope.launch {
            userPreferences.setThemeMode(theme)
        }
    }
}
