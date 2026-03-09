package com.appcontrolx.ui.screens.tools

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrolx.core.ShellManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ToolsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shellManager: ShellManager
) : ViewModel() {

    private val _loadingId = MutableStateFlow<String?>(null)
    val loadingId: StateFlow<String?> = _loadingId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun openHiddenSetting(setting: com.appcontrolx.ui.screens.tools.HiddenSetting) {
        viewModelScope.launch {
            _loadingId.value = setting.id
            try {
                var success = false
                
                // Try each intent until one works
                for (intentPath in setting.intents) {
                    val parts = intentPath.split("/")
                    if (parts.size == 2) {
                        val packageName = parts[0]
                        val activityName = parts[1]
                        
                        try {
                            val intent = Intent()
                            intent.setClassName(packageName, activityName)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            success = true
                            break
                        } catch (_: Exception) {
                            // Try next intent
                        }
                    }
                }
                
                // If no intent worked, try shell command for root users
                if (!success && shellManager.getMode() != com.appcontrolx.model.ExecutionMode.NONE) {
                    for (intentPath in setting.intents) {
                        val parts = intentPath.split("/")
                        if (parts.size == 2) {
                            val result = shellManager.execute("am start -n ${intentPath}")
                            if (result.isSuccess) {
                                success = true
                                break
                            }
                        }
                    }
                }
                
                if (!success) {
                    _errorMessage.value = "Setting not available on this device"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to open setting: ${e.message}"
            } finally {
                _loadingId.value = null
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
