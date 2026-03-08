package com.appcontrolx.core

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.appcontrolx.BuildConfig
import com.appcontrolx.IShellService
import com.appcontrolx.model.ExecutionMode
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShellManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var currentMode: ExecutionMode = ExecutionMode.NONE
    private val serviceLock = Any()

    @Volatile
    private var shellService: IShellService? = null

    @Volatile
    private var serviceLatch = CountDownLatch(1)

    @Volatile
    private var isBound = false

    @Volatile
    private var isBinding = false

    @Volatile
    private var sessionToken: String? = null

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, ShellService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            synchronized(serviceLock) {
                shellService = IShellService.Stub.asInterface(service)
                isBound = true
                isBinding = false
                sessionToken = null
                serviceLatch.countDown()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(serviceLock) {
                shellService = null
                sessionToken = null
                isBound = false
                isBinding = false
                serviceLatch = CountDownLatch(1)
            }
        }
    }

    fun initialize(): ExecutionMode {
        currentMode = detectMode()
        if (currentMode == ExecutionMode.SHIZUKU) {
            bindShizukuService()
        }
        return currentMode
    }

    fun getMode(): ExecutionMode = currentMode

    fun setMode(modeName: String) {
        currentMode = when (modeName.uppercase()) {
            "ROOT" -> if (isRootAvailable()) ExecutionMode.ROOT else throw IllegalStateException("Root not available")
            "SHIZUKU" -> if (isShizukuReady()) {
                bindShizukuService()
                ExecutionMode.SHIZUKU
            } else throw IllegalStateException("Shizuku not available")
            else -> ExecutionMode.NONE
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuGranted(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    private fun isShizukuReady(): Boolean {
        return isShizukuAvailable() && isShizukuGranted()
    }

    private fun detectMode(): ExecutionMode {
        return when {
            isRootAvailable() -> ExecutionMode.ROOT
            isShizukuReady() -> ExecutionMode.SHIZUKU
            else -> ExecutionMode.NONE
        }
    }

    private fun isRootAvailable(): Boolean {
        return try {
            Shell.isAppGrantedRoot() == true
        } catch (e: Exception) {
            false
        }
    }

    private fun bindShizukuService() {
        synchronized(serviceLock) {
            if (!Shizuku.pingBinder() || isBound || isBinding) return
            try {
                serviceLatch = CountDownLatch(1)
                isBinding = true
                Shizuku.bindUserService(userServiceArgs, serviceConnection)
            } catch (e: Exception) {
                shellService = null
                isBound = false
                isBinding = false
                currentMode = ExecutionMode.NONE
            }
        }
    }

    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        when (val validation = ShellCommandPolicy.validate(command)) {
            is ShellCommandPolicy.ValidationResult.Denied -> {
                return@withContext Result.failure(SecurityException("Command not allowed: ${validation.reason}"))
            }

            ShellCommandPolicy.ValidationResult.Allowed -> Unit
        }

        when (currentMode) {
            ExecutionMode.ROOT -> executeViaRoot(command)
            ExecutionMode.SHIZUKU -> executeViaShizuku(command)
            ExecutionMode.NONE -> Result.failure(IllegalStateException("No execution mode available"))
        }
    }

    private fun executeViaRoot(command: String): Result<String> {
        return try {
            val shell = Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(30)
                .build("su")

            if (!shell.isRoot) {
                return Result.failure(IllegalStateException("Root access denied"))
            }

            val result = shell.newJob().add(command).exec()
            if (result.isSuccess) {
                Result.success(result.out.joinToString("\n"))
            } else {
                Result.failure(Exception(result.err.joinToString("\n").ifEmpty { "Command failed" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeViaShizuku(command: String): Result<String> {
        val service = getShizukuService()
            ?: return Result.failure(IllegalStateException("Shizuku service not available"))

        return try {
            val token = getOrCreateSessionToken(service)
                ?: return Result.failure(SecurityException("Failed to open authorized session"))
            val output = service.exec(token, command)
            if (output.startsWith("ERROR:")) {
                Result.failure(Exception(output.removePrefix("ERROR:")))
            } else {
                Result.success(output)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getShizukuService(timeoutMs: Long = 3000): IShellService? {
        shellService?.let { return it }

        val latch = synchronized(serviceLock) {
            shellService?.let { return it }
            if (!isBound && !isBinding) {
                bindShizukuService()
            }
            serviceLatch
        }

        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        return synchronized(serviceLock) { shellService }
    }

    private fun getOrCreateSessionToken(service: IShellService): String? {
        synchronized(serviceLock) {
            sessionToken?.let { return it }
        }

        val newToken = service.openSession(context.packageName)
        synchronized(serviceLock) {
            sessionToken = newToken
            return sessionToken
        }
    }

    fun cleanup() {
        val serviceToClose = synchronized(serviceLock) { shellService }
        val tokenToClose = synchronized(serviceLock) { sessionToken }

        if (serviceToClose != null && !tokenToClose.isNullOrBlank()) {
            try {
                serviceToClose.closeSession(tokenToClose)
            } catch (_: Exception) {
            }
        }

        synchronized(serviceLock) {
            if (isBound || isBinding) {
                try {
                    Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
                } catch (_: Exception) {
                }
            }
            shellService = null
            sessionToken = null
            isBound = false
            isBinding = false
            serviceLatch = CountDownLatch(1)
        }
    }
}
