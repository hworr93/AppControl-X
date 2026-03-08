package com.appcontrolx.core

import org.junit.Assert.assertTrue
import org.junit.Test

class ShellCommandPolicyTest {

    @Test
    fun allowsSupportedAppCommands() {
        val commands = listOf(
            "pm disable-user --user 0 com.example.app",
            "pm clear --cache-only com.example.app",
            "am start -n com.example.app/com.example.app.MainActivity\$Nested",
            "cmd appops query-op RUN_IN_BACKGROUND ignore"
        )

        commands.forEach { command ->
            val result = ShellCommandPolicy.validate(command)
            assertTrue("Expected command to be allowed: $command", result is ShellCommandPolicy.ValidationResult.Allowed)
        }
    }

    @Test
    fun deniesMaliciousShellPatterns() {
        val commands = listOf(
            "pm clear com.example.app && pm enable com.example.app",
            "pm clear com.example.app\$(id)",
            "pm clear com.example.app > /sdcard/out.txt"
        )

        commands.forEach { command ->
            val result = ShellCommandPolicy.validate(command)
            assertTrue("Expected command to be denied: $command", result is ShellCommandPolicy.ValidationResult.Denied)
        }
    }
}
