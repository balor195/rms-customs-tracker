# Engineering Specification
## RMS Customs Clearance Transaction Tracker

**Document version:** 1.0  
**Date:** 2026-06-29 (last updated 2026-07-05 — workflow/roles/schema overhaul, see inline `2026-07-05` annotations and `PROGRESS.md`)  
**Stack:** Android (Kotlin) + Python (FastAPI)

---

## Table of Contents

1. [Repository Layout](#1-repository-layout)
2. [Android App Architecture](#2-android-app-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Domain Model](#4-domain-model)
5. [State Machine](#5-state-machine)
6. [Local Database Schema](#6-local-database-schema)
7. [Dependency Injection](#7-dependency-injection)
8. [Background Workers](#8-background-workers)
9. [Sync Architecture](#9-sync-architecture)
10. [Networking](#10-networking)
11. [Authentication & Security](#11-authentication--security)
12. [Reporting & Export](#12-reporting--export)
13. [Backend (FastAPI)](#13-backend-fastapi)
14. [Navigation](#14-navigation)
15. [Build System](#15-build-system)
16. [ProGuard Rules](#16-proguard-rules)
17. [Running Locally](#17-running-locally)

---

## 1. Repository Layout

```
D:\Claude Workspace\Customs\
├── PRD.md                          ← Product requirements
├── ENGINEERING_SPEC.md             ← This document
│
├── backend\                        ← FastAPI server
│   ├── main.py                     ← App factory + CORS + router mount
│   ├── database.py                 ← SQLAlchemy engine + session factory
│   ├── models.py                   ← ORM models (Transaction, PhaseRecord)
│   ├── schemas.py                  ← Pydantic request/response models
│   ├── requirements.txt            ← Python dependencies (>=-pinned)
│   ├── start_server.bat            ← Windows one-click server launcher
│   └── routers\
│       └── sync.py                 ← POST /push, GET /pull endpoints
│
└── CustomsTracker\                 ← Android project root
    ├── build_apk.bat               ← Windows one-click APK builder
    ├── gradle\libs.versions.toml   ← Version catalog (all dependency versions)
    ├── app\build.gradle.kts        ← App-level build config
    └── app\src\main\java\com\rms\customs\
        ├── CustomsApp.kt           ← @HiltAndroidApp, WorkManager bootstrap
        ├── MainActivity.kt         ← Single-activity host
        ├── domain\
        │   ├── model\              ← Pure Kotlin data classes (no Android deps)
        │   │   ├── Transaction.kt          ← includes weightKg, isRefrigerated, defaultShelfLife
        │   │   ├── User.kt
        │   │   ├── SlaConfig.kt
        │   │   ├── AppNotification.kt
        │   │   ├── TransactionDocument.kt
        │   │   ├── ActivityLog.kt
        │   │   └── enums\          ← TransactionPhase (5), TransactionStatus,
        │   │                         UserRole (incl. CLEARANCE/WAREHOUSE),
        │   │                         Department, Priority, PhaseStatus, etc.
        │   ├── repository\         ← Repository interfaces (contracts)
        │   ├── statemachine\
        │   │   └── TransactionStateMachine.kt
        │   └── usecase\
        │       ├── LoginUseCase.kt
        │       ├── SetupAdminUseCase.kt
        │       ├── PasswordHasher.kt
        │       ├── SlaConfigDefaults.kt
        │       └── TransactionAccessScope.kt  ← Transaction.isVisibleTo(user) — division/role scoping
        ├── data\
        │   ├── local\
        │   │   ├── db\CustomsDatabase.kt   ← Room @Database (v4, 6 entities)
        │   │   ├── entity\                 ← Room @Entity classes
        │   │   ├── dao\                    ← Room @Dao interfaces
        │   │   └── SessionStore.kt         ← EncryptedSharedPreferences session
        │   ├── remote\
        │   │   ├── api\CustomsApi.kt       ← Retrofit interface
        │   │   └── dto\SyncDtos.kt         ← @Serializable DTOs + mappers
        │   ├── network\
        │   │   └── ServerUrlInterceptor.kt ← Dynamic host OkHttp interceptor
        │   ├── export\
        │   │   ├── PdfExporter.kt
        │   │   └── CsvExporter.kt
        │   └── repository\                 ← Repository implementations
        ├── di\
        │   ├── DatabaseModule.kt
        │   ├── NetworkModule.kt
        │   └── RepositoryModule.kt
        ├── work\
        │   └── SyncWorker.kt              ← Periodic sync (15 min)
        ├── notifications\
        │   └── CustomsNotificationManager.kt
        └── presentation\
            ├── viewmodel\                  ← @HiltViewModel classes
            └── ui\
                ├── AppNavGraph.kt          ← Compose NavHost + all routes
                ├── MainScreen.kt           ← BottomNavigation shell
                ├── auth\                   ← LoginScreen, AdminSetupScreen
                ├── transaction\            ← List, Detail, Create, Cards
                ├── document\              ← Document tab, upload sheet
                ├── dashboard\             ← DashboardScreen
                ├── report\                ← ReportScreen
                ├── notification\          ← NotificationCenterScreen
                └── admin\                 ← SettingsScreen, SlaAdminScreen,
                                             UserManagementScreen
```

---

## 2. Android App Architecture

### Pattern: MVVM + Clean Architecture + UDF

```
UI (Composable)
    │  observes StateFlow
    ▼
ViewModel (@HiltViewModel)
    │  calls suspend funs / collects Flows
    ▼
Repository (interface in domain, impl in data)
    │
    ├── Room DAO (local source of truth)
    └── Retrofit API (remote, via SyncRepository)
```

**Principles:**
- ViewModels expose a single `uiState: StateFlow<ScreenState>` for screen state and separate `StateFlow`s for one-shot side effects (errors, export readiness)
- UI is stateless — it only reads from StateFlow and dispatches events to the ViewModel
- Domain layer has zero Android imports; it is plain Kotlin
- Repository implementations are `@Singleton`; DAOs return `Flow<>` for live queries and `suspend fun` for writes
- Navigation is fully type-safe via `Destinations` string constants and `AppNavGraph` composable

---

## 3. Technology Stack

### Android

| Component | Library | Version |
|---|---|---|
| Language | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material3 | BOM 2024.12.01 |
| DI | Hilt (Dagger) | 2.52 |
| DI code gen | KSP | 2.0.21-1.0.27 |
| Local DB | Room | 2.6.1 |
| Navigation | Navigation Compose | 2.8.5 |
| HTTP client | Retrofit 2 + OkHttp | 2.11.0 / 4.12.0 |
| JSON | kotlinx.serialization | 1.7.3 |
| Background | WorkManager | 2.10.0 |
| Camera | CameraX | 1.4.1 |
| Async | Kotlin Coroutines | 1.9.0 |
| Preferences | DataStore + EncryptedSharedPreferences | 1.1.1 / 1.1.0-alpha06 |
| Min SDK | Android 8.0 | API 26 |
| Target SDK | Android 16 | API 36 |
| Build tools | AGP | 8.5.2 |
| JDK | JBR 17 (bundled with Android Studio) | 17 |

### Backend

| Component | Library | Version |
|---|---|---|
| Language | Python | 3.10+ (tested 3.14.4) |
| Framework | FastAPI | ≥ 0.115.5 |
| Server | Uvicorn | ≥ 0.32.1 |
| ORM | SQLAlchemy | ≥ 2.0.36 |
| Validation | Pydantic v2 | ≥ 2.11.0 |
| Database | SQLite (file: `customs_sync.db`) | — |
| Config | python-dotenv | ≥ 1.0.1 |

---

## 4. Domain Model

### Transaction

*(Updated 2026-07-05 — added `weightKg`, `isRefrigerated`, `defaultShelfLife`; `division` and `beneficiary` were already present from an earlier RMS-specific pass.)*

```kotlin
data class Transaction(
    val id: UUID,
    val transactionRef: String,       // "RMS-2026-0042"
    val title: String,
    val division: Department?,        // شعبة الدواء / المستهلكات / الأجهزة
    val accreditationNumber: String?,
    val billOfLadingNumber: String?,
    val responsibleOfficer: String,
    val beneficiary: Beneficiary?,    // RMS or Bank
    val tenderRef: String?,
    val contractRef: String?,
    val supplierName: String,
    val totalValue: Double?,
    val currency: String,             // default "JOD"
    val expectedArrivalDate: Long?,
    val actualArrivalDate: Long?,
    val shipmentStatus: ShipmentStatus,
    val weightKg: Double?,            // وزن الشحنة (كغم)
    val isRefrigerated: Boolean,      // هل الشحنة مبرّدة
    val defaultShelfLife: String?,    // العمر الافتراضي — MEDICAL_CONSUMABLES only
    val currentPhase: TransactionPhase,
    val currentStatus: TransactionStatus,
    val exceptionState: TransactionStatus?,  // BLOCKED | ON_HOLD | DISPUTED
    val priority: Priority,           // URGENT requires beneficiary=RMS && isRefrigerated
    val createdAt: Long,              // epoch ms
    val createdByUserId: UUID,
    val updatedAt: Long,              // epoch ms — sync cursor
    val closedAt: Long?,
    val notes: String?,
)
```

Computed properties: `isActive`, `isBlocked`, `daysSinceUpdate`.

Division/role visibility is computed by `Transaction.isVisibleTo(user: User)` (`domain/usecase/TransactionAccessScope.kt`), shared by the list, dashboard, and detail-screen guard.

### User

*(Updated 2026-07-05 — `department` is now nullable; `ADMIN`/`CLEARANCE`/`WAREHOUSE` accounts have `department = null`. `passwordHash`/`createdAt` live on `UserEntity`, not the domain model.)*

```kotlin
data class User(
    val id: UUID,
    val username: String,
    val displayName: String,
    val displayNameAr: String,
    val role: UserRole,
    val department: Department?,      // null for ADMIN/CLEARANCE/WAREHOUSE
    val isActive: Boolean = true,
    val lastLoginAt: Long? = null,
)
```

### SlaConfig

One row per `(phaseNumber, subPhase)` pair. Fields: `targetDays`, `escalationAfterDays`, `isActive`. Seeded with defaults via `SlaConfigDefaults.kt` on first run.

### AppNotification

Stores SLA alert notifications in-app. Fields: `type` (SLA_BREACH / SLA_ESCALATED), bilingual title/message, `isRead`, `transactionId`, `createdAt`.

### Key Enums

*(Updated 2026-07-05, twice — phase/status/role model overhauled 7→5 phases, then Phase 3 "Transit & Receipt" and Phase 5 "Transferred to Warehouses" were merged (they described the same event) leaving 4 phases. See `PROGRESS.md` → "Workflow, Roles & Field Overhaul" for the full rationale.)*

**TransactionPhase** — 4 values with `number`, `labelAr`, `labelEn` (reduced from 7, then 5):
`PHASE_1_TENDER`, `PHASE_2_CLEARANCE`, `PHASE_3_FINANCIAL`, `PHASE_4_WAREHOUSE_CONFIRMATION`.

**TransactionStatus** — 10 values (reduced from 19, then 13; in addition to the earlier removals, `IN_TRANSIT`, `RECEIVED_AT_WAREHOUSE`, `INSPECTION_COMPLETE` were removed — the separate `shipmentStatus` field already tracks physical arrival independently):

```
DRAFT → TENDER_PREPARATION → TENDER_PUBLISHED →
CLEARANCE_ISSUED →
FINANCIAL_SETTLEMENT_PENDING → CLOSED →
TRANSFERRED_TO_WAREHOUSE
+ BLOCKED | ON_HOLD | DISPUTED (exception overlays)
```

**UserRole** — `ADMIN` / `CLEARANCE` (renamed from `COORDINATOR`) / `WAREHOUSE` (new) / `SUPERVISOR` / `TENDER_OFFICER` ("ضابط العطاء", added 2026-07-05) / `VIEWER`, with computed permission properties: `canWrite`, `canCreateTransaction` (ADMIN/SUPERVISOR/TENDER_OFFICER), `canApprove`, `canManageUsers`, `canExport`, `canMarkClearanceDone` (ADMIN/CLEARANCE only), `canMarkWarehouseTransferred` (ADMIN/WAREHOUSE only), `seesAllDivisions` (ADMIN/CLEARANCE/WAREHOUSE — these three also carry `department = null`).

**Department** — `PHARMACY` (شعبة الدواء), `MEDICAL_CONSUMABLES` (شعبة المستهلكات الطبية), `MEDICAL_DEVICES` (شعبة الأجهزة الطبية). `SUPERVISOR`/`VIEWER` accounts are confined to their own division.

**Priority** — `NORMAL`, `HIGH`, `URGENT`. `URGENT` is only selectable when `beneficiary == RMS && isRefrigerated`.

---

## 5. State Machine

*(Rewritten 2026-07-05, twice — first for the 5-phase model, then simplified again to 4 phases when `IN_TRANSIT`/`RECEIVED_AT_WAREHOUSE`/`INSPECTION_COMPLETE` were removed — see `PROGRESS.md` for the full before/after.)*

`TransactionStateMachine` (`domain/statemachine/`) enforces all lifecycle rules.

**Transition table (forward):**
```
DRAFT → TENDER_PREPARATION → TENDER_PUBLISHED → CLEARANCE_ISSUED
→ FINANCIAL_SETTLEMENT_PENDING → CLOSED → TRANSFERRED_TO_WAREHOUSE
```

**Exception state recovery:** BLOCKED and ON_HOLD can transition to `TENDER_PREPARATION` or `FINANCIAL_SETTLEMENT_PENDING`. DISPUTED can only transition to `FINANCIAL_SETTLEMENT_PENDING`.

**Hard gate (enforced in `checkHardGates()`):**

| Gate | Rule |
|---|---|
| Gate | Cannot reach `TRANSFERRED_TO_WAREHOUSE` unless current status is `CLOSED` |

Reaching `TRANSFERRED_TO_WAREHOUSE` is the transaction's closing action (`isTerminal`), and its status badge renders in red (`CustomsColors.Overdue`) rather than the neutral grey used for the intermediate `CLOSED` status.

The `advance()` method returns `TransitionResult.Success(newStatus)` or `TransitionResult.Failure(reason)` — never throws. Permission checks for who *may* trigger `CLEARANCE_ISSUED` (`CLEARANCE`/`ADMIN` only) and `TRANSFERRED_TO_WAREHOUSE` (`WAREHOUSE`/`ADMIN` only) are enforced at the UI layer (`TransactionDetailScreen`), not inside the state machine itself, consistent with how `canWrite` is already gated elsewhere.

---

## 6. Local Database Schema

Room database name: `customs_tracker.db`, currently **version 6**. Migrations (mostly additive `ALTER TABLE`, per the "additive-only schema" architecture decision — the one exception is noted below):

| Migration | Change |
|---|---|
| 1 → 2 | Added `division`, `accreditationNumber`, `billOfLadingNumber`, `responsibleOfficer`, `beneficiary`, `expectedArrivalDate`, `actualArrivalDate`, `shipmentStatus` to `transactions` |
| 2 → 3 *(2026-07-05)* | **Dropped** `phase_records` (Phase-4 subsystem removed); cleared `sla_configs` (renumbered, reseeded on next open); added `weightKg`, `isRefrigerated` to `transactions` |
| 3 → 4 *(2026-07-05)* | Added `defaultShelfLife` to `transactions` |
| 4 → 5 *(2026-07-05)* | **Recreated** `users` (SQLite can't drop a `NOT NULL` constraint via `ALTER TABLE`) to make `department` nullable; nulled `department` for existing `ADMIN`/`CLEARANCE`/`WAREHOUSE` rows |
| 5 → 6 *(2026-07-05)* | Phases 3/5 merged — remaps any `transactions` rows in the removed `IN_TRANSIT`/`RECEIVED_AT_WAREHOUSE`/`INSPECTION_COMPLETE` statuses forward to `FINANCIAL_SETTLEMENT_PENDING`, and old `currentPhase` enum names to their renumbered equivalents; cleared `sla_configs` again (sub-phase numbering shifted) |

### Tables

#### `transactions`
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | UUID string |
| transactionRef | TEXT UNIQUE | e.g., "RMS-2026-0042" |
| title | TEXT | |
| division | TEXT? | Department enum — شعبة الدواء / المستهلكات / الأجهزة |
| accreditationNumber | TEXT? | رقم الاعتماد |
| billOfLadingNumber | TEXT? | رقم بوليصة الشحن |
| responsibleOfficer | TEXT | اسم الضابط المسؤول |
| beneficiary | TEXT? | Beneficiary enum — RMS or Bank |
| tenderRef | TEXT? | |
| contractRef | TEXT? | |
| supplierName | TEXT | |
| totalValue | REAL? | |
| currency | TEXT | default "JOD" |
| expectedArrivalDate | INTEGER? | epoch ms |
| actualArrivalDate | INTEGER? | epoch ms |
| shipmentStatus | TEXT | ShipmentStatus enum, default EXPECTED |
| weightKg | REAL? | *(added 2026-07-05)* وزن الشحنة |
| isRefrigerated | INTEGER | *(added 2026-07-05)* 0/1 — هل الشحنة مبرّدة |
| defaultShelfLife | TEXT? | *(added 2026-07-05)* العمر الافتراضي — MEDICAL_CONSUMABLES only |
| currentPhase | TEXT | enum name |
| currentStatus | TEXT | enum name |
| exceptionState | TEXT? | enum name or null |
| priority | TEXT | enum name |
| createdAt | INTEGER | epoch ms |
| createdByUserId | TEXT | UUID string |
| updatedAt | INTEGER | epoch ms — sync cursor |
| closedAt | INTEGER? | epoch ms |
| notes | TEXT? | |

#### `phase_records` *(removed 2026-07-05, migration 2→3)*

> Existed only to track the three Phase-4 gov-agency parallel-approval tracks (Military Command / Customs / JFDA). Since Phase 4 was removed from the workflow, this table — and the entities/DAO/domain model behind it (`PhaseRecordEntity`, `PhaseRecordDao`, `PhaseRecord`, `AssignedEntity`) — was dropped entirely rather than kept unused.

#### `users`
*(department made nullable 2026-07-05, migration 4→5)*
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | UUID string |
| username | TEXT UNIQUE | |
| displayName | TEXT | |
| displayNameAr | TEXT | |
| role | TEXT | UserRole enum |
| department | TEXT? | Department enum, or null for ADMIN/CLEARANCE/WAREHOUSE |
| passwordHash | TEXT | PBKDF2-SHA256 |
| isActive | INTEGER | 0/1 |
| lastLoginAt | INTEGER? | epoch ms |

#### `sla_configs`
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | UUID string |
| phaseNumber | INTEGER | |
| subPhase | TEXT | |
| targetDays | INTEGER | |
| escalationAfterDays | INTEGER | |
| isActive | INTEGER | 0/1 |

#### `transaction_documents`
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | UUID string |
| transactionId | TEXT FK | → transactions.id |
| type | TEXT | DocumentType enum |
| fileName | TEXT | |
| filePath | TEXT | absolute path on device |
| uploadedByUserId | TEXT | |
| uploadedAt | INTEGER | epoch ms |
| notes | TEXT? | |

#### `activity_logs`
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | UUID string |
| transactionId | TEXT | |
| userId | TEXT | |
| action | TEXT | LogAction enum |
| fromStatus | TEXT? | |
| toStatus | TEXT? | |
| notes | TEXT? | |
| timestamp | INTEGER | epoch ms |

#### `notifications`
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | UUID string |
| transactionId | TEXT | |
| type | TEXT | NotificationType enum |
| titleAr | TEXT | |
| titleEn | TEXT | |
| messageAr | TEXT | |
| messageEn | TEXT | |
| isRead | INTEGER | 0/1 |
| createdAt | INTEGER | epoch ms |

### DAO Methods (key)

**TransactionDao:**
- `observeAll(): Flow<List<TransactionEntity>>` — live stream for list screen
- `getById(id): TransactionEntity?`
- `insert(entity)` — conflict strategy REPLACE (used by sync)
- `getModifiedSince(since: Long): List<TransactionEntity>` — sync push cursor
- `bumpUpdatedAt(id, updatedAt)`

> `PhaseRecordDao` (and `countOverdueSla()`, which queried `phase_records` directly) were removed 2026-07-05 along with the table above.

**UserDao:**
- `findByUsername(username): UserEntity?` — used by login
- `observeAll(): Flow<List<UserEntity>>`
- `updateRole(id, role)`

---

## 7. Dependency Injection

Three Hilt modules:

### DatabaseModule
Provides: `CustomsDatabase` (singleton, Room), and all DAO instances extracted from it.

### NetworkModule
Provides:
- `ServerUrlInterceptor` (singleton) — reads server URL from `rms_app_config` SharedPreferences
- `OkHttpClient` (singleton) — 30s timeouts, `ServerUrlInterceptor`, `HttpLoggingInterceptor` (DEBUG level)
- `Json` (singleton) — kotlinx.serialization with `ignoreUnknownKeys = true`
- `Retrofit` (singleton) — base URL `http://rms.internal/` (placeholder; interceptor rewrites per request)
- `CustomsApi` (singleton) — Retrofit service interface

### RepositoryModule
Binds all repository interfaces to their implementations via `@Binds @Singleton`.

| Interface | Implementation |
|---|---|
| TransactionRepository | TransactionRepositoryImpl |
| UserRepository | UserRepositoryImpl |
| SlaRepository | SlaRepositoryImpl |
| NotificationRepository | NotificationRepositoryImpl |
| DocumentRepository | DocumentRepositoryImpl |
| SyncRepository | SyncRepositoryImpl |

---

## 8. Background Workers

*(`SlaCheckerWorker` was removed 2026-07-05 — it scanned `phase_records`, which only existed for the now-deleted Phase-4 tracks. `SyncWorker` is the only remaining background worker.)*

`SyncWorker` is a `@HiltWorker` (dependency injection via `@AssistedInject`), registered in `CustomsApp.kt` on first app launch using `WorkManager.enqueueUniquePeriodicWork`.

### SyncWorker

- **Schedule:** Every 15 minutes, requires `NetworkType.CONNECTED`
- **Work name:** `rms_sync_periodic`
- **Logic:** Calls `SyncRepository.sync()` which does push → pull in sequence
- **Retry:** On failure, retries up to 3 times (`runAttemptCount < 3`), then `Result.failure()`

---

## 9. Sync Architecture

### Strategy: Incremental cursor-based sync (no outbox table)

**Cursor:** The `updatedAt` field on `TransactionEntity` (epoch ms) serves as the sync watermark, bumped via `TransactionDao.bumpUpdatedAt()`.

**Push (device → server):**
1. `TransactionDao.getModifiedSince(lastSyncMs)` — all transactions changed since last successful sync
2. `POST /api/v1/sync/push` with a `SyncPushRequest` payload
3. Server applies last-write-wins upsert (`dto.updated_at >= tx.updated_at`)
4. Server stamps `server_updated_at = now_ms` on every accepted record

**Pull (server → device):**
1. `GET /api/v1/sync/pull?since={lastSyncMs}&device_id={deviceId}`
2. Server returns all transactions where `server_updated_at > since`
3. Device upserts via `TransactionDao.insert()` (REPLACE strategy)
4. Updates `last_sync_ms` in SharedPreferences to `serverTimeMs` from response

**SharedPreferences:**
- File: `rms_sync_prefs`
- Keys: `last_sync_ms` (Long, default 0), `device_id` (UUID generated once)

**Conflict resolution:** Last-write-wins. If two devices edit the same transaction offline, the one with the higher `updatedAt` wins when both sync. This is acceptable for this use case since transactions are generally owned by one coordinator.

---

## 10. Networking

### ServerUrlInterceptor

`data/network/ServerUrlInterceptor.kt` — `@Singleton` OkHttp `Interceptor`.

- Reads server URL from `rms_app_config` SharedPreferences key `server_url` on **every request**
- Rewrites `scheme`, `host`, and `port` of the outgoing request URL
- If the stored URL is malformed, falls back to the original request unchanged
- Default URL: `http://10.0.2.2:8000/` (Android emulator localhost)
- `saveUrl(url)` — trims trailing slash and appends `/` before saving

This approach allows runtime URL changes in Settings without rebuilding Retrofit.

### Retrofit API

```kotlin
interface CustomsApi {
    @POST("api/v1/sync/push")
    suspend fun push(@Body request: SyncPushRequest): SyncPushResponse

    @GET("api/v1/sync/pull")
    suspend fun pull(
        @Query("since") since: Long,
        @Query("device_id") deviceId: String,
    ): SyncPullResponse
}
```

Converter: `kotlinx.serialization` via `asConverterFactory("application/json; charset=UTF8".toMediaType())`.

### DTO ↔ Entity Mapping

`SyncDtos.kt` contains extension functions:
- `TransactionEntity.toSyncDto(): TransactionSyncDto`
- `TransactionSyncDto.toEntity(): TransactionEntity`

*(`PhaseRecordSyncDto` and its mappers were removed 2026-07-05 along with `phase_records`.)*

Field names use `@SerialName("snake_case")` to match the Python backend convention. `TransactionSyncDto` includes `weight_kg`, `is_refrigerated`, and `default_shelf_life` (added 2026-07-05) — note the Python backend's `models.py`/`schemas.py` have **not** been updated to accept these fields yet, since the FastAPI sync backend is still an unwired Phase-9 placeholder (see §13).

---

## 11. Authentication & Security

### Login Flow

1. `LoginUseCase` looks up user by username in `UserDao`
2. Hashes submitted password with `PasswordHasher` (PBKDF2-SHA256, 10,000 iterations, random salt)
3. Compares hash — on match, creates a `UserSession` and persists it via `SessionStore`
4. `AuthViewModel` drives a `AuthState` sealed class: `Unauthenticated`, `Authenticated(session)`, `NeedsAdminSetup`

### Admin Setup

On first launch, if the users table is empty, `SetupAdminUseCase` creates the default admin account. The user is taken to `AdminSetupScreen` to set admin credentials before any other screen is accessible.

### Session Persistence

`SessionStore` uses `EncryptedSharedPreferences` (AES-256-GCM for values, AES-256-SIV for keys) to store the active session across app restarts.

### RBAC in UI

`RequireRole(vararg roles: UserRole)` composable — renders its content only if the current session user has one of the specified roles; otherwise renders nothing (not even an error message). `LocalUserSession` is a `CompositionLocal` providing the session to the full composable tree.

---

## 12. Reporting & Export

### PDF Export (`PdfExporter.kt`)

Uses `android.graphics.pdf.PdfDocument` — no external PDF library.

- Page size: A4 portrait (595 × 842 pt)
- Margins: 36 pt on all sides; usable width: 523 pt
- Layout: Organization header → divider → report title + subtitle + date → green column header row → alternating-color data rows → thick bottom border → page footer
- Three report types: Weekly (7-day window), Monthly (full list), Executive (summary KPIs)
- Saved to `context.filesDir/reports/rms_<type>_<yyyyMMdd_HHmmss>.pdf`
- Shared via `Intent.ACTION_SEND` with `FileProvider`

### CSV Export (`CsvExporter.kt`)

- UTF-8 BOM prefix (`﻿`) for Excel compatibility
- Comma-delimited; fields with commas are quoted
- Same three report types as PDF
- Saved to `context.filesDir/reports/rms_<type>_<yyyyMMdd_HHmmss>.csv`

### FileProvider

Declared in `AndroidManifest.xml` with authority `com.rms.customs.fileprovider`. Paths configured in `res/xml/file_paths.xml`:

```xml
<files-path name="reports" path="reports/" />
```

---

## 13. Backend (FastAPI)

### File Structure

```
backend/
├── main.py        ← FastAPI app, CORS, table creation on startup
├── database.py    ← SQLAlchemy engine (SQLite), SessionLocal, get_db()
├── models.py      ← ORM: Transaction + PhaseRecord
├── schemas.py     ← Pydantic: request/response models
└── routers/
    └── sync.py    ← /api/v1/sync/push + /api/v1/sync/pull
```

### Endpoints

#### `GET /health`
Returns `{"status": "ok", "service": "rms-customs-sync"}`.

#### `POST /api/v1/sync/push`

**Request body:**
```json
{
  "device_id": "uuid-string",
  "pushed_at": 1751234567000,
  "transactions": [
    {
      "id": "uuid",
      "transaction_ref": "RMS-2026-0042",
      "title": "...",
      "supplier_name": "...",
      "current_phase": "PHASE_2_CLEARANCE",
      "current_status": "CLEARANCE_ISSUED",
      "priority": "NORMAL",
      "weight_kg": 120.5,
      "is_refrigerated": false,
      "created_at": 1750000000000,
      "created_by_user_id": "uuid",
      "updated_at": 1751234567000,
      "currency": "JOD"
    }
  ]
}
```

**Response:** `{"accepted": 3}` — count of records updated on server.

**Logic:** For each transaction in the request, if `dto.updated_at >= server.updated_at` (or record is new), upsert the transaction. Set `server_updated_at = now_ms`.

> ⚠️ The `weight_kg`/`is_refrigerated`/`default_shelf_life` fields (added 2026-07-05) and the `phase_records` removal are reflected on the Android side only — `backend/models.py` and `backend/schemas.py` have not been updated, since this backend is an unwired Phase-9 placeholder (see §17 for how to run it, but it is not part of the shipped app flow).

#### `GET /api/v1/sync/pull?since=<ms>&device_id=<uuid>`

**Response:**
```json
{
  "transactions": [...],
  "server_time_ms": 1751234567000
}
```

**Logic:** Returns all transactions where `server_updated_at > since`, with all their phase records. The device stores `server_time_ms` as the next pull cursor.

### Database

SQLite file: `customs_sync.db` in the backend directory. Created automatically on first startup via `Base.metadata.create_all(bind=engine)`. The `server_updated_at` column on `transactions` is indexed for efficient pull queries.

### CORS

All origins, methods, and headers are allowed (`allow_origins=["*"]`). This is intentional for a LAN-only deployment within RMS.

---

## 14. Navigation

### Destinations

```kotlin
object Dest {
    const val LOGIN            = "login"
    const val ADMIN_SETUP      = "admin_setup"
    const val MAIN             = "main"
    const val TRANSACTION_LIST = "transaction_list"
    const val TRANSACTION_DETAIL = "transaction_detail/{transactionId}"
    const val CREATE_TRANSACTION = "create_transaction"
    const val SLA_ADMIN        = "sla_admin"
    const val NOTIFICATION_CENTER = "notification_center"
    const val SETTINGS         = "settings"
    const val USER_MANAGEMENT  = "user_management"
}
```

### Route Tree

```
NavHost (startDestination = LOGIN)
├── login          → LoginScreen
├── admin_setup    → AdminSetupScreen
└── main           → MainScreen (BottomNav shell)
    ├── Bottom tab 1: transaction_list → TransactionListScreen
    │   ├── transaction_detail/{id}   → TransactionDetailScreen
    │   └── create_transaction        → CreateTransactionScreen
    ├── Bottom tab 2: dashboard       → DashboardScreen
    ├── Bottom tab 3: reports         → ReportScreen
    ├── TopBar icon: notification_center → NotificationCenterScreen
    └── TopBar icon: settings         → SettingsScreen
        ├── sla_admin                 → SlaAdminScreen
        └── user_management           → UserManagementScreen
```

`MainScreen` also owns the sync state indicator in the TopBar (cloud icon that becomes a spinner when syncing).

---

## 15. Build System

### Version Catalog (`gradle/libs.versions.toml`)

All dependency versions are declared centrally. The app-level `build.gradle.kts` references them via `libs.*` accessors.

### Build Variants

| Variant | Minify | ProGuard | Signing |
|---|---|---|---|
| Debug | No | No | Debug key (auto) |
| Release | Yes (`isMinifyEnabled = true`, `isShrinkResources = true`) | `proguard-android-optimize.txt` + `proguard-rules.pro` | Requires keystore (not included) |

### Key Gradle Settings

- `compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`
- `compileOptions = JavaVersion.VERSION_17`, `jvmTarget = "17"`
- Compose compiler plugin enabled via `kotlin.compose` plugin

### Build Scripts

**`CustomsTracker/build_apk.bat`** (Windows, double-click):
1. Auto-detects Android Studio JBR from common install paths
2. Prompts for Debug (default) or Release build
3. Runs `gradlew.bat assembleDebug` or `assembleRelease`
4. On success: opens output folder; optionally installs via ADB if device connected
5. APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## 16. ProGuard Rules

`app/proguard-rules.pro` preserves:

```
# Retrofit interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# kotlinx.serialization DTOs
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep @kotlinx.serialization.Serializable class ** { *; }

# Room DAOs
-keep @androidx.room.Dao class ** { *; }

# WorkManager CoroutineWorkers
-keep class * extends androidx.work.CoroutineWorker { *; }

# Domain enums (used in string comparisons at runtime)
-keepclassmembers enum com.rms.customs.domain.model.enums.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
```

---

## 17. Running Locally

### Backend

**Prerequisites:** Python 3.10+ installed and on PATH.

```bat
REM Double-click or run from terminal:
D:\Claude Workspace\Customs\backend\start_server.bat
```

The script:
1. Creates `.venv` if it doesn't exist
2. Installs all dependencies from `requirements.txt`
3. Starts uvicorn on `0.0.0.0:8000` with `--reload`

**URLs:**
- API base: `http://localhost:8000`
- Swagger docs: `http://localhost:8000/docs`
- Health check: `http://localhost:8000/health`
- From Android emulator: `http://10.0.2.2:8000`
- From physical device: `http://<host-machine-LAN-IP>:8000`

### Android App

**Prerequisites:** Android Studio installed (provides JBR).

```bat
REM Double-click or run from terminal:
D:\Claude Workspace\Customs\CustomsTracker\build_apk.bat
```

Select 1 (Debug) and press Enter. The APK will be at:
```
app\build\outputs\apk\debug\app-debug.apk
```

If an emulator or device is connected via ADB, the script will offer to install it directly.

### ADB Port Forwarding (physical device)

To make a physical device reach the backend running on the development machine:

```bash
adb reverse tcp:8000 tcp:8000
```

Then set the server URL in the app's Settings screen to `http://localhost:8000/`.

### First Run

1. Install APK on device/emulator
2. Open the app — Admin Setup screen appears (users table is empty)
3. Set admin username and password
4. Log in as admin
5. Go to Settings → set server URL → tap sync icon to verify connection
6. Go to SLA Admin to review/adjust SLA targets (defaults are pre-seeded)
7. Create first transaction from the + FAB on the Transaction List screen
