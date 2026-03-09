package com.appcontrolx.core

internal object CommandValidator {

    private val forbiddenChars = setOf(';', '|', '&', '`', '>', '<', '\\', '\n', '\r', '\u0000')
    private val packageRegex = Regex("^[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+$")
    private val componentRegex = Regex("^[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+/[a-zA-Z0-9_.$]+$")
    private val componentRelativeRegex = Regex("^[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+/\\.[a-zA-Z0-9_.$]+$")

    fun validate(command: String): Result<Unit> {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty command"))
        }
        if (trimmed.length > 512) {
            return Result.failure(IllegalArgumentException("Command too long"))
        }
        if (trimmed.any { it in forbiddenChars || it.isISOControl() && !it.isWhitespace() }) {
            return Result.failure(SecurityException("Command contains forbidden characters"))
        }
        if (containsForbiddenShellPattern(trimmed)) {
            return Result.failure(SecurityException("Command contains forbidden shell pattern"))
        }

        val tokens = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty command"))
        }

        return if (isAllowedTokens(tokens)) {
            Result.success(Unit)
        } else {
            Result.failure(SecurityException("Command is not in allowed policy"))
        }
    }

    private fun isAllowedTokens(tokens: List<String>): Boolean {
        return when (tokens.firstOrNull()) {
            "pm" -> isAllowedPm(tokens)
            "am" -> isAllowedAm(tokens)
            "appops" -> isAllowedAppOps(tokens)
            "cmd" -> isAllowedCmd(tokens)
            "dumpsys" -> tokens.size == 3 && tokens[1] == "activity" && tokens[2] == "processes"
            else -> false
        }
    }

    private fun isAllowedPm(tokens: List<String>): Boolean {
        return when {
            tokens.size == 5 &&
                tokens[1] == "disable-user" &&
                tokens[2] == "--user" &&
                tokens[3] == "0" &&
                isPackageName(tokens[4]) -> true

            tokens.size == 3 &&
                tokens[1] == "enable" &&
                isPackageName(tokens[2]) -> true

            tokens.size == 6 &&
                tokens[1] == "uninstall" &&
                tokens[2] == "-k" &&
                tokens[3] == "--user" &&
                tokens[4] == "0" &&
                isPackageName(tokens[5]) -> true

            tokens.size == 4 &&
                tokens[1] == "clear" &&
                tokens[2] == "--cache-only" &&
                isPackageName(tokens[3]) -> true

            tokens.size == 3 &&
                tokens[1] == "clear" &&
                isPackageName(tokens[2]) -> true

            else -> false
        }
    }

    private fun isAllowedAm(tokens: List<String>): Boolean {
        if (tokens.size < 2) return false
        return when (tokens[1]) {
            "force-stop" -> tokens.size == 3 && isPackageName(tokens[2])
            "start" -> tokens.size == 4 && tokens[2] == "-n" && isComponentName(tokens[3])
            else -> false
        }
    }

    private fun isAllowedAppOps(tokens: List<String>): Boolean {
        return tokens.size == 5 &&
            tokens[1] == "set" &&
            isPackageName(tokens[2]) &&
            tokens[3] == "RUN_IN_BACKGROUND" &&
            tokens[4] in setOf("ignore", "allow")
    }

    private fun isAllowedCmd(tokens: List<String>): Boolean {
        return tokens.size == 5 &&
            tokens[1] == "appops" &&
            tokens[2] == "query-op" &&
            tokens[3] in setOf("RUN_IN_BACKGROUND", "RUN_ANY_IN_BACKGROUND") &&
            tokens[4] == "ignore"
    }

    private fun containsForbiddenShellPattern(command: String): Boolean {
        return command.contains("&&") ||
            command.contains("||") ||
            command.contains("$(")
    }

    private fun isPackageName(value: String): Boolean = packageRegex.matches(value)

    private fun isComponentName(value: String): Boolean {
        return componentRegex.matches(value) || componentRelativeRegex.matches(value)
    }
}
