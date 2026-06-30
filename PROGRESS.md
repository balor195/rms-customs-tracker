# Customs Tracker — Build Progress

**Client:** مديرية الصيدلة والتجهيزات الطبية — الخدمات الطبية الملكية (Royal Medical Services, Jordan)
**Project path:** `D:\Claude Workspace\Customs\CustomsTracker\`
**Plan reference:** `D:\Claude Workspace\Customs\CUSTOMS_TRACKER_PLAN.md`

---

## Phase Status

| Phase | Description | Status |
|-------|-------------|--------|
| 0 | Foundation & Architecture | ✅ Done |
| 1 | Data Layer — Room + State Machine | ✅ Done |
| 2 | Auth & RBAC | ✅ Done |
| 3 | Core Transaction CRUD | ✅ Done |
| 4 | Seven-Phase Workflow Engine | ✅ Done |
| 5 | Document Management | ✅ Done |
| 6 | Alerts & SLA Engine | — |
| 7 | Dashboard & KPIs | — |
| 8 | Reporting & Export | — |
| 9 | Backend API + Offline Sync | — |
| 10 | Admin, Polish & Packaging | — |

---

## Phase 0 — Foundation & Architecture

**APK:** `app/build/outputs/apk/debug/app-debug.apk` (20.9 MB debug)

### Tech Stack
- Kotlin 2.0.21, AGP 8.5.2, Gradle 8.7
- Compose BOM 2024.12.01, Material3, Navigation Compose 2.8.5
- Hilt 2.52 (KSP 2.0.21-1.0.27)
- Room 2.6.1 — 7 entities, all reactive via `Flow<>`
- Retrofit 2.11.0 + OkHttp 4.12.0 (wired for Phase 9)
- WorkManager 2.10.0 (Phase 6 SLA + Phase 9 sync)
- CameraX 1.4.1 (Phase 5 document capture)
- security-crypto 1.1.0-alpha06 (`MasterKey.AES256_GCM`)
- compileSdk 36, minSdk 26

### Architecture Decisions
1. **Single-exit state machine** — `TransactionStateMachine.advance()` only; all 4 hard gates enforced
2. **RBAC from day zero** — `RequireRole()` composable + `LocalUserSession` CompositionLocal
3. **Additive-only schema** — Room v1, no `fallbackToDestructiveMigration`
4. **Full audit log** — every status change writes an immutable `ActivityLogEntity` atomically via `@Transaction`
5. **RTL from day zero** — `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`
6. **Bilingual AR/EN** — full `strings.xml` + `values-ar/strings.xml`
7. **SLA as data** — `SlaConfigEntity` table seeded on first install, not hardcoded
8. **Exception overlay** — BLOCKED/ON_HOLD/DISPUTED stored in `exceptionState: TransactionStatus?`; `currentStatus` is never replaced by an exception state

### Source Structure
```
app/src/main/java/com/rms/customs/
  CustomsApp.kt                         ← @HiltAndroidApp
  MainActivity.kt                       ← RTL + nav host entry
  data/
    local/
      db/CustomsDatabase.kt             ← Room @Database (7 entities, v1)
      entity/                           ← 7 @Entity classes + toDomain()/toEntity() mappers
      dao/                              ← 7 @Dao interfaces
    remote/api/CustomsApi.kt            ← Phase 9 placeholder
    repository/                         ← 5 Repository implementations
  domain/
    model/enums/                        ← 9 enums
    model/                              ← 7 domain models
    repository/                         ← 5 Repository interfaces
    statemachine/TransactionStateMachine.kt
    usecase/
  di/
    DatabaseModule.kt
    RepositoryModule.kt
    NetworkModule.kt
  presentation/
    ui/
      AppSession.kt                     ← UserSession + LocalUserSession
      RequireRole.kt                    ← RBAC guard composable
      theme/Theme.kt                    ← RMS colors: Green, Gold, CustomsColors object
```

---

## Phase 1 — Data Layer

### Added
- `domain/usecase/SlaConfigDefaults.kt` — 15 default SLA entries; Phase 4: Military=15d, Customs=10d, JFDA=12d
- `domain/usecase/Phase4Tracks.kt` — creates 3 `PhaseRecord` stubs (Military/Customs/JFDA) when status enters `GOV_PROCESSING`
- `TransactionRepositoryImpl` — auto-seeds Phase 4 tracks; injects `SlaRepository` for live SLA lookups
- `DatabaseModule` — `RoomDatabase.Callback.onCreate()` seeds all SLA configs on first install

### Tests
- **Unit (JUnit4, no device):** `TransactionStateMachineTest` — 27 tests, 0 failures — all valid transitions, skip/reverse rejections, terminal-state rejection, all 4 hard gates
- **Instrumented (device/emulator):** `TransactionDaoTest` (8), `UserDaoTest` (8), `PhaseRecordDaoTest` (5) — in-memory Room DB

---

## Phase 2 — Auth & RBAC

### Added
- `PasswordHasher` — PBKDF2WithHmacSHA256, 100k iterations, 16-byte salt; stored as `saltB64:hashB64`
- `LoginUseCase` / `SetupAdminUseCase` — domain-layer auth business logic
- `SessionStore` — `EncryptedSharedPreferences` with 8-hour TTL; auto-clears on expiry
- `AuthViewModel` — `AuthState` enum: Loading | NeedsSetup | LoggedOut | LoggedIn(session)
- `LoginScreen.kt` — Arabic card form, username/password, error animation
- `AdminSetupScreen.kt` — first-run only; live password-match validation
- `AppNavGraph.kt` — `NavHost`; `LaunchedEffect(authState)` drives navigation

### Security
- Permissions in manifest: `CAMERA`, `READ_MEDIA_IMAGES`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`
- `FileProvider` registered for `filesDir/documents/` and `cacheDir/customs_documents/`

### Roles

| Role | canWrite | canApprove | canManageUsers | canExport |
|------|----------|------------|----------------|-----------|
| ADMIN | ✓ | ✓ | ✓ | ✓ |
| COORDINATOR | ✓ | ✗ | ✗ | ✓ |
| SUPERVISOR | ✗ | ✓ | ✗ | ✓ |
| VIEWER | ✗ | ✗ | ✗ | ✗ |

---

## Phase 3 — Core Transaction CRUD

### Domain
- `StatusPhaseMapper.kt` — `TransactionStatus.toPhase(): TransactionPhase` extension; all 19 statuses mapped
- `TransactionStateMachine.nextForwardStatus(current)` helper
- `TransactionDao` — added `countWithPrefix(prefix)` (auto-ref) + `search(q)` queries
- `TransactionRepository` — added `generateRef()`, `observePhaseRecords()`, `updatePhaseRecord()`, `completePhaseRecord()`
- `TransactionRepositoryImpl.advanceStatus()` — now keeps `currentPhase` in sync with `currentStatus` on every advance

### UI
- `StatusUiUtils.kt` — `TransactionStatus.labelAr()`, `.statusColor()`, `LogAction.labelAr()`, `PhaseStatus.labelAr()`
- `TransactionListViewModel` — `TxFilter` (ALL/ACTIVE/BLOCKED/CLOSED); reactive `combine()` + in-memory filter; sorted by `updatedAt` desc
- `TransactionDetailViewModel` — `SavedStateHandle["id"]`; exposes transaction, phaseRecords, activityLog, transitionState
- `CreateTransactionViewModel` — auto-generates `RMS-YYYY-NNNN` ref on init
- `TransactionCard.kt` — colored left bar (green/amber/red), ref, supplier, title, status chip, days since update
- `TransactionListScreen.kt` — search bar + `FilterChip` row + `LazyColumn` + FAB (COORDINATOR/ADMIN only)
- `CreateTransactionScreen.kt` — read-only ref, title, supplier, tender ref, value, priority, notes
- `PhaseTransitionDialog.kt` — advance dialog (from→to with confirm) + `BlockerDialog` (requires non-blank reason)
- `TransactionDetailScreen.kt` — header card, phase timeline, 4 tabs: التفاصيل / المستندات / سجل النشاط / ملاحظات
- `MainScreen.kt` — `Scaffold` + RMS green `TopAppBar` + bottom nav (معاملات / لوحة التحكم)

### State Machine — Hard Gates

| Gate | Enforced at |
|------|-------------|
| No customs declaration before `CONTRACT_SIGNED` | `TransactionStateMachine` |
| No release order without all Phase 4 tracks complete | `TransactionStateMachine` |
| No shipment movement before `FINAL_RELEASE_ISSUED` | `TransactionStateMachine` |
| No financial close without `INSPECTION_COMPLETE` | `TransactionStateMachine` |

---

## Phase 4 — Seven-Phase Workflow Engine

### Added
- `PhaseTimelineComponent.kt` — 7-phase dot-and-line timeline; Phase 4 tracks shown inline when active
  - `TransactionPhase.resolveStatus(tx)` handles GOV_APPROVED → Phase 4 DONE special case
  - Dot colors: green=DONE, navy=IN_PROGRESS, grey=PENDING
- `Phase4TrackSheet.kt` — `ModalBottomSheet` per Phase 4 track; notes field, mark-complete, report-blocker with reason input
- `TransactionDetailViewModel` — added `updatePhaseRecord()`, `completePhaseRecord()`, `blockPhaseRecord()`

### Phase 4 Parallel Tracks

| Track | Entity | SLA |
|-------|--------|-----|
| 4.1 | Military Command | 15 days |
| 4.2 | Customs | 10 days |
| 4.3 | JFDA | 12 days |

---

## Phase 5 — Document Management

### Added
- `DocumentRepositoryImpl` — now injects `ActivityLogDao`; `save()` atomically writes a `DOC_UPLOADED` log entry
- `DocumentViewModel.kt` — Hilt VM scoped to nav back stack entry
  - `uploadFromContentUri(uri, docType, userId)` — copies content URI to temp, then to `filesDir/documents/{txId}/`
  - `uploadFromFile(file, docType, userId)` — used for camera captures
  - `delete(docId)` — deletes from DB and filesystem
  - `getFileProviderUri(doc)` — returns FileProvider URI for opening with external apps
  - Image compression: JPEG quality loop (85→30%), stops when ≤ 2 MB
  - `UploadState` sealed class: Idle / Loading / Success / Error(message)
- `DocumentCard.kt` — shows type label (AR), filename (truncated), file size from filesystem, upload date; View + Delete icon buttons
- `DocumentUploadSheet.kt` — `ModalBottomSheet` with Camera / Gallery / Files options + loading spinner while uploading
- `DocumentsTab.kt` — full Documents tab
  - Required-docs checklist: shows ✅ or ⚠️ per `DocumentType.requiredPhase` matching current phase
  - All uploaded documents list
  - Upload flow: `TakePicture` contract (with runtime `CAMERA` permission request), `GetContent`, `OpenDocument`
  - Opens files via FileProvider + `ACTION_VIEW` intent (external app)

### Document Types by Phase

| Phase | Required Documents |
|-------|--------------------|
| 3 — Clearance Prep | Purchase Order, Contract, Commercial Invoice, Packing List, Bill of Lading, Airway Bill, Certificate of Origin, Health Certificate, Product Registration Certificate |
| 4 — Gov Agencies | Customs Declaration, Military Exemption Request, Military Exemption Approval, JFDA Import Permit, Customs Inspection Report |
| 5 — Release | Customs Release Order, Military Release Order |
| 6 — Transit/Receipt | Delivery Note, Receiving Minutes |
| 7 — Financial | Payment Voucher |

### Storage Policy
Files are stored in `Context.getFilesDir()/documents/{transactionId}/` — **not** MediaStore. This is a security requirement: documents contain sensitive medical procurement data.

Camera temp files use `Context.getCacheDir()/customs_documents/` (served by FileProvider, cleaned automatically by Android).

---

## Build Environment

| Setting | Value |
|---------|-------|
| JAVA_HOME | `C:\Program Files\Android\Android Studio\jbr` |
| Android SDK | `C:\Users\mohba\AppData\Local\Android\Sdk` |
| Build command | `.\gradlew assembleDebug` (from project root) |
| Last known build | ✅ BUILD SUCCESSFUL — Phase 5 complete |

---

## Remaining Phases

### Phase 6 — Alerts & SLA Engine
- WorkManager periodic task (every 6h): scan open transactions, compute SLA breaches, write `AppNotification` rows
- Notification center screen (bell icon in top bar)
- `RECEIVE_BOOT_COMPLETED` already in manifest to re-schedule after reboot

### Phase 7 — Dashboard & KPIs
- KPI cards: open transactions, blocked count, avg clearance days, phase distribution
- Charts via a Compose-compatible charting library

### Phase 8 — Reporting & Export
- PDF export using `PdfDocument` API
- CSV export via `DocumentsContract` to Downloads
- Filters: by phase, date range, status

### Phase 9 — Backend API + Offline Sync
- FastAPI backend (separate repo)
- Retrofit endpoints already stubbed in `CustomsApi.kt`
- Outbox pattern via WorkManager for sync queue
- Conflict resolution: server wins on field-level merge

### Phase 10 — Admin, Polish & Packaging
- User management screen (ADMIN only)
- SLA config editor
- In-app PDF/image viewer (replace external-app intent)
- CameraX full capture UI (replace basic `TakePicture` contract)
- Release signing + ProGuard rules
- Accessibility audit (TalkBack + minimum touch targets)
