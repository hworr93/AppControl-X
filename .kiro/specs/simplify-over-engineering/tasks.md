# Implementation Plan

- [x] 1. Create CommandValidator and consolidate validation logic
  - Extract validation rules from ShellCommandPolicy
  - Move to new CommandValidator class in core package
  - Keep all whitelist rules (pm, am, appops, cmd, dumpsys)
  - _Requirements: 2.1, 2.3_

- [x] 2. Simplify SafetyValidator
  - [x] 2.1 Remove injection detection logic
    - Delete hasInjectionAttempt() method
    - Remove INJECTION_CHARS constant
    - Remove injection check from validatePackageName()
    - _Requirements: 2.4, 4.4_

  - [x] 2.2 Reduce hardcoded package lists
    - Trim CRITICAL_PACKAGES to 25 essential items
    - Trim FORCE_STOP_ONLY_PACKAGES to 5 items
    - Remove WARNING_PACKAGES entirely
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 3. Update AIDL interface and simplify ShellService
  - [x] 3.1 Modify IShellService.aidl interface
    - Remove openSession() and closeSession() methods
    - Remove sessionToken parameter from exec() and execReturnCode()
    - _Requirements: 1.1, 1.3_

  - [x] 3.2 Simplify ShellService implementation
    - Remove Session data class
    - Remove sessions ConcurrentHashMap
    - Remove cleanupExpiredSessions() method
    - Remove authorizeCall() method
    - Remove getOrCreateSessionToken() method
    - Add direct UID validation in exec() methods
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [x] 3.3 Simplify command execution
    - Remove manual thread spawning for stdout/stderr
    - Remove AtomicReference wrappers
    - Remove StreamResult data class
    - Remove readStreamLimited() method
    - Use simple ProcessBuilder with redirectErrorStream(true)
    - Use bufferedReader().readText() for output
    - Maintain timeout handling
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 3.4 Remove ShellCommandPolicy validation from ShellService
    - Delete validation call in exec() method
    - Delete validation call in execReturnCode() method
    - _Requirements: 2.1, 2.2_

- [x] 4. Update ShellManager
  - [x] 4.1 Integrate CommandValidator
    - Add CommandValidator.validate() call in execute() method
    - Remove ShellCommandPolicy import and usage
    - _Requirements: 2.1, 2.2_

  - [x] 4.2 Remove session token management
    - Remove sessionToken volatile field
    - Remove getOrCreateSessionToken() method
    - Update executeViaShizuku() to call service.exec(command) directly
    - Remove session cleanup from cleanup() method
    - _Requirements: 1.1, 1.3_

- [x] 5. Remove rollback feature
  - [x] 5.1 Update ActionHistoryItem model
    - Remove canRollback field from data class
    - _Requirements: 5.1_

  - [x] 5.2 Update AppManager
    - Remove isRollbackSupported() method
    - Remove canRollback parameter from actionHistoryStore.addAction() call
    - _Requirements: 5.2, 5.3_

- [x] 6. Delete obsolete files
  - Delete ShellCommandPolicy.kt file
  - _Requirements: 2.3_

- [x] 7. Verify build and functionality
  - Build project and resolve any compilation errors
  - Run lint checks
  - Verify all app actions work identically
  - _Requirements: 6.1, 6.2, 6.3, 6.4_
