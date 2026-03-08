package com.appcontrolx.core

import android.app.Application
import android.content.Context
import android.os.Binder
import com.appcontrolx.IShellService
import com.appcontrolx.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class ShellService : IShellService.Stub() {

    private data class Session(
        val uid: Int,
        @Volatile var lastUsedAtMs: Long
    )

    private data class StreamResult(val text: String, val truncated: Boolean)

    private data class ProcessResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
        val timeout: Boolean,
        val failure: String?
    )

    private val sessions = ConcurrentHashMap<String, Session>()

    override fun openSession(packageName: String): String {
        cleanupExpiredSessions()
        val callerUid = Binder.getCallingUid()
        if (packageName != BuildConfig.APPLICATION_ID) {
            throw SecurityException("Unauthorized package")
        }
        if (!isPackageOwnedByUid(callerUid, packageName)) {
            throw SecurityException("Caller UID does not own package")
        }

        val now = System.currentTimeMillis()
        val token = UUID.randomUUID().toString()
        sessions[token] = Session(
            uid = callerUid,
            lastUsedAtMs = now
        )
        return token
    }

    override fun closeSession(sessionToken: String) {
        val callerUid = Binder.getCallingUid()
        val session = sessions[sessionToken] ?: return
        if (session.uid == callerUid) {
            sessions.remove(sessionToken)
        }
    }

    override fun exec(sessionToken: String, command: String): String {
        val authError = authorizeCall(sessionToken)
        if (authError != null) {
            return "ERROR:$authError"
        }

        when (val validation = ShellCommandPolicy.validate(command)) {
            is ShellCommandPolicy.ValidationResult.Denied -> {
                return "ERROR:Command rejected: ${validation.reason}"
            }

            ShellCommandPolicy.ValidationResult.Allowed -> Unit
        }

        val result = executeCommand(command)
        result.failure?.let { return "ERROR:$it" }
        if (result.timeout) {
            return "ERROR:Execution timed out after ${EXEC_TIMEOUT_SECONDS}s"
        }
        if (result.exitCode != 0) {
            val reason = result.stderr.ifEmpty { "Exit code: ${result.exitCode}" }
            return "ERROR:$reason"
        }
        return result.stdout
    }

    override fun execReturnCode(sessionToken: String, command: String): Int {
        val authError = authorizeCall(sessionToken)
        if (authError != null) {
            return -1
        }

        when (ShellCommandPolicy.validate(command)) {
            is ShellCommandPolicy.ValidationResult.Denied -> return -1
            ShellCommandPolicy.ValidationResult.Allowed -> Unit
        }

        val result = executeCommand(command)
        if (result.failure != null || result.timeout) {
            return -1
        }
        return result.exitCode
    }

    private fun authorizeCall(sessionToken: String): String? {
        cleanupExpiredSessions()
        val callerUid = Binder.getCallingUid()
        val session = sessions[sessionToken] ?: return "Unauthorized session"
        if (session.uid != callerUid) {
            return "Caller UID mismatch"
        }
        session.lastUsedAtMs = System.currentTimeMillis()
        return null
    }

    private fun executeCommand(command: String): ProcessResult {
        return try {
            val process = ProcessBuilder("sh", "-c", command).start()
            val stdoutRef = AtomicReference(StreamResult("", false))
            val stderrRef = AtomicReference(StreamResult("", false))

            val stdoutThread = thread(start = true, isDaemon = true, name = "shell-stdout") {
                stdoutRef.set(readStreamLimited(process.inputStream))
            }
            val stderrThread = thread(start = true, isDaemon = true, name = "shell-stderr") {
                stderrRef.set(readStreamLimited(process.errorStream))
            }

            val completed = process.waitFor(EXEC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!completed) {
                process.destroy()
                if (!process.waitFor(PROCESS_DESTROY_WAIT_MS, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly()
                }
                stdoutThread.join(STREAM_JOIN_WAIT_MS)
                stderrThread.join(STREAM_JOIN_WAIT_MS)
                return ProcessResult(-1, "", "", timeout = true, failure = null)
            }

            val exitCode = process.exitValue()
            stdoutThread.join(STREAM_JOIN_WAIT_MS)
            stderrThread.join(STREAM_JOIN_WAIT_MS)

            val outResult = stdoutRef.get()
            val errResult = stderrRef.get()

            val stdout = if (outResult.truncated) {
                "${outResult.text}\n...[stdout truncated]"
            } else {
                outResult.text
            }
            val stderr = if (errResult.truncated) {
                "${errResult.text}\n...[stderr truncated]"
            } else {
                errResult.text
            }

            ProcessResult(
                exitCode = exitCode,
                stdout = stdout.trim(),
                stderr = stderr.trim(),
                timeout = false,
                failure = null
            )
        } catch (e: Exception) {
            ProcessResult(
                exitCode = -1,
                stdout = "",
                stderr = "",
                timeout = false,
                failure = e.message ?: "Execution error"
            )
        }
    }

    private fun readStreamLimited(stream: java.io.InputStream): StreamResult {
        var truncated = false
        val output = StringBuilder()

        BufferedReader(InputStreamReader(stream)).use { reader ->
            val buffer = CharArray(1024)
            while (true) {
                val readCount = reader.read(buffer)
                if (readCount < 0) break

                val remaining = MAX_OUTPUT_CHARS - output.length
                if (remaining > 0) {
                    val appendCount = minOf(remaining, readCount)
                    output.append(buffer, 0, appendCount)
                    if (appendCount < readCount) {
                        truncated = true
                    }
                } else {
                    truncated = true
                }
            }
        }

        return StreamResult(output.toString(), truncated)
    }

    private fun isPackageOwnedByUid(uid: Int, packageName: String): Boolean {
        val context = resolveContext() ?: return false
        val packages = context.packageManager.getPackagesForUid(uid) ?: return false
        return packages.any { it == packageName }
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

    private fun cleanupExpiredSessions() {
        val now = System.currentTimeMillis()
        sessions.entries.removeIf { (_, session) ->
            (now - session.lastUsedAtMs) > SESSION_TTL_MS
        }
    }

    override fun onTransact(code: Int, data: android.os.Parcel, reply: android.os.Parcel?, flags: Int): Boolean {
        try {
            return super.onTransact(code, data, reply, flags)
        } catch (e: Exception) {
            throw e
        }
    }

    companion object {
        private const val EXEC_TIMEOUT_SECONDS = 15L
        private const val PROCESS_DESTROY_WAIT_MS = 250L
        private const val STREAM_JOIN_WAIT_MS = 1000L
        private const val MAX_OUTPUT_CHARS = 64 * 1024
        private const val SESSION_TTL_MS = 10 * 60 * 1000L
    }
}
