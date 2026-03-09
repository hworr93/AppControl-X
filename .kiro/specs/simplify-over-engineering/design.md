# Design Document

## Overview

Refactoring akan menghapus layer abstraksi yang tidak perlu sambil mempertahankan fungsionalitas dan keamanan. Pendekatan utama adalah konsolidasi validasi ke satu layer, simplifikasi Shizuku service dengan mengandalkan built-in security, dan menghapus fitur yang tidak terpakai.

## Architecture Changes

### Before
```
ShellManager → ShellCommandPolicy (validation)
            → ShellService → ShellCommandPolicy (validation again)
            → SafetyValidator (package + injection check)

ShellService: Session management (UUID, ConcurrentHashMap, TTL)
            : Manual thread management (AtomicReference, thread spawning)

AppManager → SafetyValidator (package validation)
          → ActionHistoryStore (with rollback metadata)
```

### After
```
ShellManager → CommandValidator (single validation layer)
            → ShellService (simple UID check only)

ShellService: Direct UID validation via Shizuku
            : Simple ProcessBuilder.exec()

AppManager → SafetyValidator (package safety only)
          → ActionHistoryStore (simple logging)
```

## Components and Interfaces

### 1. ShellService Simplification

**Remove:**
- `openSession()` / `closeSession()` methods
- Session token parameter from `exec()` and `execReturnCode()`
- ConcurrentHashMap, UUID, TTL cleanup logic
- Manual thread spawning for stdout/stderr
- AtomicReference wrappers
- Complex stream truncation logic

**Keep:**
- Direct UID validation via `Binder.getCallingUid()`
- Timeout handling (15s)
- Error handling

**New AIDL Interface:**
```java
interface IShellService {
    String exec(String command);
    int execReturnCode(String command);
}
```

**Simplified Execution:**
```kotlin
private fun executeCommand(command: String): String {
    val process = ProcessBuilder("sh", "-c", command)
        .redirectErrorStream(true)
        .start()

    val completed = process.waitFor(15, TimeUnit.SECONDS)
    if (!completed) {
        process.destroyForcibly()
        throw TimeoutException("Command timeout")
    }

    val output = process.inputStream.bufferedReader().readText()
    if (process.exitValue() != 0) {
        throw Exception(output.ifEmpty { "Exit code: ${process.exitValue()}" })
    }
    return output.trim()
}
```

### 2. Command Validation Consolidation

**Create New:** `CommandValidator` in `core/` package

**Responsibilities:**
- Validate command syntax (forbidden chars, length, shell patterns)
- Validate command whitelist (pm, am, appops, cmd, dumpsys)
- Single source of truth for command validation

**Remove:**
- `ShellCommandPolicy` class entirely
- Command validation from `ShellService`
- Injection detection from `SafetyValidator`

**Integration Point:**
- Called only in `ShellManager.execute()` before sending to service

### 3. SafetyValidator Simplification

**Keep Only:**
- Package name format validation
- Safety level determination (CRITICAL, FORCE_STOP_ONLY, WARNING, SAFE)
- Allowed actions per safety level

**Remove:**
- Injection character detection (handled by CommandValidator)
- Excessive hardcoded package lists

**Reduced Critical Packages (25 items):**
```kotlin
private val CRITICAL_PACKAGES = setOf(
    // Self
    "com.appcontrolx",

    // Core Android
    "android",
    "com.android.systemui",
    "com.android.settings",
    "com.android.phone",
    "com.android.shell",
    "com.android.bluetooth",
    "com.android.wifi",
    "com.android.networkstack",
    "com.android.permissioncontroller",
    "com.android.packageinstaller",
    "com.android.webview",

    // Google Core
    "com.google.android.gms",
    "com.google.android.gsf",
    "com.android.vending",

    // Vendor Launchers (critical only)
    "com.android.launcher3",
    "com.miui.home",
    "com.sec.android.app.launcher",
    "com.oppo.launcher",
    "com.huawei.android.launcher",

    // Root/Shizuku
    "com.topjohnwu.magisk",
    "rikka.shizuku",
    "moe.shizuku.privileged.api",
    "me.weishu.kernelsu"
)
```

**Force-Stop Only (5 items):**
```kotlin
private val FORCE_STOP_ONLY_PACKAGES = setOf(
    "com.miui.powerkeeper",
    "com.samsung.android.lool",
    "com.coloros.safecenter",
    "com.huawei.systemmanager",
    "com.google.android.apps.adm"
)
```

### 4. ShellManager Updates

**Changes:**
- Remove session token management
- Add CommandValidator call before execution
- Simplify Shizuku service binding (no session open/close)
- Update `executeViaShizuku()` to call new AIDL interface

**Flow:**
```kotlin
suspend fun execute(command: String): Result<String> {
    // Single validation point
    CommandValidator.validate(command).getOrElse {
        return Result.failure(it)
    }

    when (currentMode) {
        ExecutionMode.ROOT -> executeViaRoot(command)
        ExecutionMode.SHIZUKU -> executeViaShizuku(command)
        ExecutionMode.NONE -> Result.failure(...)
    }
}

private fun executeViaShizuku(command: String): Result<String> {
    val service = getShizukuService() ?: return Result.failure(...)
    return try {
        val output = service.exec(command) // No token needed
        Result.success(output)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 5. ActionHistory Simplification

**Remove from ActionHistoryItem:**
- `canRollback: Boolean` field

**Remove from AppManager:**
- `isRollbackSupported()` method
- Rollback logic in `executeAction()`

**Simplified History Storage:**
```kotlin
actionHistoryStore.addAction(
    ActionHistoryItem(
        packageName = packageName,
        appName = resolveAppName(packageName),
        action = action,
        timestamp = System.currentTimeMillis()
    )
)
```

## Data Models

### Modified Models

**ActionHistoryItem:**
```kotlin
@Serializable
data class ActionHistoryItem(
    val packageName: String,
    val appName: String,
    val action: AppAction,
    val timestamp: Long
)
```

**IShellService.aidl:**
```java
interface IShellService {
    String exec(String command);
    int execReturnCode(String command);
}
```

## Error Handling

**Maintained:**
- Timeout handling (15s for commands)
- Exit code validation
- Exception propagation
- Security exceptions for unauthorized operations

**Simplified:**
- No session expiry errors
- No token mismatch errors
- Direct error messages from command execution

## Testing Strategy

**Validation:**
- Build project without errors
- Verify lint passes
- Test all app actions (freeze, unfreeze, force-stop, etc.)
- Verify security validation still blocks dangerous operations

**Manual Testing:**
- Test with ROOT mode
- Test with SHIZUKU mode
- Verify action history still logs correctly
- Confirm UI remains unchanged

## Migration Notes

**Breaking Changes:**
- AIDL interface changed (Shizuku service needs rebuild)
- ActionHistoryItem serialization format changed (old history incompatible)

**Non-Breaking:**
- All app functionality remains identical
- UI unchanged
- User experience unchanged
