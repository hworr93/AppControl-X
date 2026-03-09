package com.appcontrolx.core

import org.junit.Assert.assertTrue
import org.junit.Test

class CommandValidatorTest {

    @Test
    fun allowsSupportedCommandFamilies() {
        val commands = listOf(
            "pm disable-user --user 0 com.example.app",
            "am force-stop com.example.app",
            "appops set com.example.app RUN_IN_BACKGROUND ignore",
            "cmd appops query-op RUN_IN_BACKGROUND ignore",
            "dumpsys activity processes"
        )

        commands.forEach { command ->
            val result = CommandValidator.validate(command)
            assertTrue("Expected command to be allowed: $command", result.isSuccess)
        }
    }

    @Test
    fun rejectsDisallowedCommandPrefixes() {
        val commands = listOf(
            "sh -c pm clear com.example.app",
            "rm -rf /",
            "ls /data"
        )

        commands.forEach { command ->
            val result = CommandValidator.validate(command)
            assertTrue("Expected command to be rejected: $command", result.isFailure)
        }
    }

    @Test
    fun rejectsDangerousShellPatterns() {
        val commands = listOf(
            "pm clear com.example.app && pm enable com.example.app",
            "pm clear com.example.app$(id)",
            "pm clear com.example.app > /sdcard/out.txt"
        )

        commands.forEach { command ->
            val result = CommandValidator.validate(command)
            assertTrue("Expected dangerous command to be rejected: $command", result.isFailure)
        }
    }

    @Test
    fun rejectsBlankAndInvalidCommands() {
        val blank = CommandValidator.validate("   ")
        val invalidStructure = CommandValidator.validate("pm clear")

        assertTrue("Expected blank command to be rejected", blank.isFailure)
        assertTrue("Expected invalid command structure to be rejected", invalidStructure.isFailure)
    }
}
