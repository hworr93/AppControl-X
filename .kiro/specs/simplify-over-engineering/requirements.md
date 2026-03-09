# Requirements Document

## Introduction

AppControlX memiliki beberapa layer yang over-engineered yang menambah kompleksitas tanpa memberikan value signifikan. Refactoring ini bertujuan untuk menyederhanakan codebase dengan menghapus abstraksi berlebihan, menggabungkan validasi ganda, dan menghilangkan fitur yang tidak terpakai, sambil tetap mempertahankan fungsionalitas inti dan keamanan aplikasi.

## Requirements

### Requirement 1: Simplify Shizuku Session Management

**User Story:** As a developer, I want to remove the complex session token system in ShellService, so that the code is simpler and easier to maintain while still secure.

#### Acceptance Criteria

1. WHEN ShellService receives a command request THEN it SHALL validate caller UID directly without session tokens
2. WHEN a command is executed via Shizuku THEN it SHALL rely on Shizuku's built-in security instead of custom session management
3. WHEN ShellService is refactored THEN it SHALL remove ConcurrentHashMap, UUID tokens, and TTL cleanup logic
4. WHEN the simplified service is used THEN it SHALL maintain the same security guarantees as before

### Requirement 2: Merge Duplicate Command Validation

**User Story:** As a developer, I want to consolidate command validation into a single layer, so that there's no redundant validation logic across multiple classes.

#### Acceptance Criteria

1. WHEN a shell command is prepared for execution THEN it SHALL be validated only once
2. WHEN validation logic is consolidated THEN it SHALL be placed in ShellManager as the single source of truth
3. WHEN ShellCommandPolicy is removed THEN its validation rules SHALL be integrated into the remaining validator
4. WHEN SafetyValidator is simplified THEN it SHALL focus only on package-level safety checks, not command syntax

### Requirement 3: Simplify ShellService Stream Handling

**User Story:** As a developer, I want to remove manual thread management for process streams, so that the code is more maintainable and less error-prone.

#### Acceptance Criteria

1. WHEN ShellService executes a command THEN it SHALL use simple ProcessBuilder.exec() without manual thread spawning
2. WHEN process output is read THEN it SHALL use standard InputStream reading without AtomicReference wrappers
3. WHEN output truncation is needed THEN it SHALL use simple string length checks instead of complex stream limiting
4. WHEN the simplified execution completes THEN it SHALL maintain the same timeout and error handling behavior

### Requirement 4: Reduce SafetyValidator Hardcoded Lists

**User Story:** As a developer, I want to minimize the hardcoded package lists in SafetyValidator, so that the code is more maintainable and focused on truly critical packages.

#### Acceptance Criteria

1. WHEN SafetyValidator is refactored THEN it SHALL keep only 20-30 truly critical system packages
2. WHEN a package is in the critical list THEN it SHALL be essential for system stability (SystemUI, Settings, Phone, Bluetooth, etc.)
3. WHEN vendor-specific packages are evaluated THEN only the most critical ones SHALL be retained
4. WHEN injection detection is reviewed THEN redundant checks already covered by command validation SHALL be removed

### Requirement 5: Remove Unused Rollback Feature

**User Story:** As a developer, I want to remove the rollback feature from ActionHistoryStore, so that the codebase doesn't carry unused complexity.

#### Acceptance Criteria

1. WHEN ActionHistoryItem is simplified THEN it SHALL remove the `canRollback` field
2. WHEN action history is stored THEN it SHALL only log the action without rollback metadata
3. WHEN AppManager executes actions THEN it SHALL not check or set rollback support
4. WHEN the refactoring is complete THEN action history SHALL still be viewable but without rollback UI/logic

### Requirement 6: Maintain Existing Functionality

**User Story:** As a user, I want the app to work exactly the same after refactoring, so that I don't experience any regressions or breaking changes.

#### Acceptance Criteria

1. WHEN the refactoring is complete THEN all app actions (freeze, unfreeze, force-stop, etc.) SHALL work identically
2. WHEN commands are executed THEN security validation SHALL still prevent dangerous operations
3. WHEN the app is built THEN it SHALL compile without errors and pass existing lint checks
4. WHEN the app runs THEN UI and navigation SHALL remain unchanged
