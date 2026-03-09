package com.appcontrolx.core

import android.app.Application
import android.content.Context
import android.os.Binder
import com.appcontrolx.IShellService
import com.appcontrolx.BuildConfig
import java.util.concurrent.TimeUnit

class ShellService : IShellService.Stub() {

    override fun exec(command: String): String {
        validateCaller()
        return try {
            executeCommand(command)
        } catch (e: Exception) {
            throw SecurityException(e.message ?: "Execution failed")
        }
    }

    override fun execReturnCode(command: String): Int {
        validateCaller()
        return try {
            executeCommandReturnCode(command)
        } catch (e: Exception) {
            -1
        }
    }

    private fun validateCaller() {
        val callerUid = Binder.getCallingUid()
        val context = resolveContext() ?: throw SecurityException("Context unavailable")
        val packages = context.packageManager.getPackagesForUid(callerUid)
        if (packages?.contains(BuildConfig.APPLICATION_ID) != true) {
            throw SecurityException("Unauthorized caller")
        }
    }

    private fun executeCommand(command: String): String {
        val process = ProcessBuilder("sh", "-c", command)
            .redirectErrorStream(true)
            .start()

        val completed = process.waitFor(EXEC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            throw Exception("Command timeout after ${EXEC_TIMEOUT_SECONDS}s")
        }

        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.exitValue() != 0) {
            throw Exception(output.ifEmpty { "Exit code: ${process.exitValue()}" })
        }
        return output.trim()
    }

    private fun executeCommandReturnCode(command: String): Int {
        val process = ProcessBuilder("sh", "-c", command)
            .redirectErrorStream(true)
            .start()

        val completed = process.waitFor(EXEC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            return -1
        }
        return process.exitValue()
    }

    private fun resolveContext(): Context? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val application = activityThreadClass
                .getMethod("currentApplication")
                .invoke(null) as? Application
            application?.applicationContext
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val EXEC_TIMEOUT_SECONDS = 15L
    }
}
