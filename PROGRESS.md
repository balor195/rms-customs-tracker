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
| 4 | Seven-Phase Workflow Engine | ⚠️ Superseded 2026-07-05 — see [Workflow, Roles & Field Overhaul](#workflow-roles--field-overhaul-2026-07-05) |
| 5 | Document Management | ✅ Done |
| 6 | Alerts & SLA Engine | ➖ Removed 2026-07-05 — `SlaCheckerWorker` depended entirely on the removed Phase-4 subsystem |
| 7 | Dashboard & KPIs | ⚠️ Simplified 2026-07-05 — SLA/overdue KPIs removed (no data source), rest intact |
| UI | UI/UX Overhaul (cross-cutting) | ✅ Done |
| 8 | Reporting & Export | — |
| 9 | Backend API + Offline Sync | — |
| 10 | Admin, Polish & Packaging | — |
| 11 | Workflow, Roles & Field Overhaul | ✅ Done — see [dated entry](#workflow-roles--field-overhaul-2026-07-05) below |

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

> ⚠️ **Superseded 2026-07-05** — `COORDINATOR` was renamed to `CLEARANCE`, and `WAREHOUSE` was added. See [Workflow, Roles & Field Overhaul](#workflow-roles--field-overhaul-2026-07-05) for the current role table.

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

> ⚠️ **Superseded 2026-07-05** — Phases 2–4 (Evaluation, Clearance Docs, Gov-Agency Processing) were removed and the Phase-4 parallel-track subsystem described below (`PhaseRecord`, `AssignedEntity`, `Phase4Tracks`, `Phase4TrackSheet`) was deleted entirely. This section is kept for historical reference. See [Workflow, Roles & Field Overhaul](#workflow-roles--field-overhaul-2026-07-05).

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
| Last known build | ✅ BUILD SUCCESSFUL — Phase 7 + UI/UX Overhaul complete |

---

---

## Phase 6 — Alerts & SLA Engine (Partial)

> ➖ **Removed 2026-07-05** — `SlaCheckerWorker` scanned `PhaseRecord`s, which only ever existed for the now-deleted Phase 4 tracks. With no remaining data source, the worker (and its WorkManager schedule in `CustomsApp.kt`) was deleted rather than left silently broken. `SlaConfig` data/admin screen remain as configuration, but there is no automated breach/escalation engine anymore. See [Workflow, Roles & Field Overhaul](#workflow-roles--field-overhaul-2026-07-05).

### Done
- `SyncViewModel` + `SyncState` sealed class (`Idle` | `Syncing` | `Error`) — used in `MainScreen.kt` top bar
- `SyncState` indicator in top bar: rotating Sync icon while syncing, `CloudOff` on error, `CloudDone` on idle
- Manual sync trigger on top-bar icon tap
- `overdueSlaCount` exposed from `DashboardViewModel` — drives red alert banner on dashboard

### Remaining
- `SlaCheckerWorker` — WorkManager periodic job (every 6h) that scans active phases for SLA breaches
- `NotificationRepository` — write `AppNotification` rows on breach, escalation after threshold
- Notification center screen (bell icon already present with unread badge via `NotificationViewModel`)
- Android `NotificationManager` channel + deep link into transaction detail

---

## Phase 7 — Dashboard & KPIs

> ⚠️ **Simplified 2026-07-05** — `overdueSlaCount`, `overdueItems`, `avgMilitaryDays`, `avgCustomsDays`, and `delayedRatioPct` were removed along with the Phase-4 subsystem they depended on (see below). Summary cards, shipment breakdown, phase distribution, and value-by-division are unaffected.

### Added
- `DashboardViewModel.kt` — aggregates `DashboardStats` from `TransactionRepository`:
  - `totalActive`, `overdueSlaCount`, `closedThisMonth`
  - `shipmentExpected`, `shipmentArrived`, `shipmentCleared`, `upcomingArrivalsCount`
  - `phaseDistribution: Map<TransactionPhase, Int>`
  - `valueByDivision: List<DivisionValueEntry>`
  - `overdueItems: List<OverdueItem>` — ranked by days overdue
  - KPI fields: `avgClearanceDays`, `avgMilitaryDays`, `avgCustomsDays`, `overdueRatePct`, `exemptRatePct`
- `DashboardScreen.kt` — `LazyColumn` screen:
  - `OverdueAlertBanner` — red-bordered banner when `overdueSlaCount > 0`
  - Summary row: 3 `SummaryCard` composables (active / overdue / closed this month)
  - Shipment status breakdown row
  - `PhaseDistributionChart` — horizontal bar chart per phase, 22dp bars, 6dp radius
  - `ValueByDivisionChart` — bar chart by medical division
  - `KpiGrid` — 2-column grid of KPI tiles with `KpiRow` highlight (3dp navy left bar)
  - `OverdueRow` list — priority-ranked overdue transactions with left accent bar

---

## UI/UX Overhaul — 6 Ideas (2026-06-30)

A complete visual overhaul of the presentation layer. All changes are in `CustomsTracker/app/src/main/java/com/rms/customs/presentation/`.

### Idea 1 — Color System (`theme/Theme.kt`)

Replaced placeholder green/gold palette with a navy-first, military-context color scheme.

| Token | Before | After |
|-------|--------|-------|
| `primary` | Gold (`#F9A825`) | Navy deep (`#1A237E`) |
| `secondary` | Teal | Navy mid (`#1565C0`) |
| `primaryContainer` | — | Navy light (`#E8EAF6`) |

**`CustomsColors` object** — semantic tokens accessible from any composable:

| Name | Color | Usage |
|------|-------|-------|
| `Overdue` | `#B71C1C` (deep red) | Blocked transactions, SLA breach |
| `Warning` | `#F57F17` (amber) | Transactions over 7 days without update |
| `OnTime` | `#1B5E20` (forest green) | Completed phases, on-schedule items |
| `Military` | `#1A237E` (navy) | Active phase nodes, primary actions |
| `GoldAccent` | `#F9A825` | Semantic-only accent (not in color scheme) |

### Idea 2 — Arabic Typography (`theme/Typography.kt` — new file)

Added `androidx.compose.ui:ui-text-google-fonts` (downloadable fonts, no bundled .ttf required).

- **Noto Naskh Arabic** — `display` / `headline` / `title` styles (formal script, official documents)
- **Noto Sans Arabic** — `body` / `label` styles (clean, legible at small sizes)
- `bodyLarge` line height: `26.sp` (1.625× for Arabic diacritics)
- `labelLarge` weight: `SemiBold` (buttons / chips)
- Graceful fallback to system Arabic font if Google Fonts unavailable

### Idea 3 — Dashboard Redesign (`dashboard/DashboardScreen.kt`)

| Before | After |
|--------|-------|
| Centered spinner on load | Shimmer skeleton (summary cards + chart + KPI rows) |
| — | `OverdueAlertBanner` when overdue count > 0 |
| Plain section labels | `SectionHeader` with 3dp navy accent bar |
| Basic cards | `SummaryCard` with tonal colored background (8% alpha) + icon |
| Text-based chart | `PhaseDistributionChart` — 22dp bars, 6dp radius, 10% tinted track |
| — | `KpiRow` — `primary.copy(0.05f)` background + 3dp left bar |
| Emoji indicators | `CheckCircle` / `DateRange` icons from Material Icons Extended |
| — | `OverdueRow` — 3dp left accent bar (red/amber) filling full row height |
| Mixed radii | 16dp everywhere; 1dp elevation |

### Idea 4 — Phase Timeline Redesign (`transaction/PhaseTimelineComponent.kt`)

| Element | Before | After |
|---------|--------|-------|
| DONE node | Plain grey dot | 22dp green circle + white `Check` icon |
| IN_PROGRESS node | Filled dot | 26dp navy + outer 6dp glow ring (15% alpha) |
| PENDING node | Plain dot | 16dp outlined circle (1.5dp grey border) |
| Status labels | Text-only | `StatusBadge` — colored rounded chip (مكتملة / جارية / معلقة) |
| Track rows | Plain text | `CheckCircle` / `Cancel` / `Warning` / `Schedule` icons + 2dp left accent bar |
| Active row | No highlight | `primary.copy(0.04f)` background |
| Connector | Single colour | Green (DONE), 35% primary (IN_PROGRESS), `outlineVariant` (PENDING) |
| Left column | 24dp | 36dp |

### Idea 5 — Transaction Card Redesign (`transaction/TransactionCard.kt`)

| Element | Before | After |
|---------|--------|-------|
| Accent bar height | Fixed 4dp box | `IntrinsicSize.Min` + `fillMaxHeight()` — stretches full card height |
| Priority indicator | Text only | `PriorityBadge` — tonal colored box (red URGENT, amber HIGH) |
| Status chip | `AssistChip` / `SuggestionChip` | `MiniChip` — custom non-interactive Box (no ripple, smaller padding) |
| Phase progress | — | `PhaseProgressBar` — 7 segments: green (done), navy (active), `surfaceVariant` (pending) |
| Card shape | Default | `RoundedCornerShape(16.dp)`, elevation 1dp |
| Blocked / overdue | No background | Tinted background (`Overdue.copy(0.04f)` / `Warning.copy(0.03f)`) |
| Transaction ref color | `onSurfaceVariant` | `onSurface` (higher contrast) |
| Days display | Plain text | Amber + SemiBold when > 7 days without update |

### Idea 6 — Micro-interactions & Loading States

#### New: `Shimmer.kt`
Shared `ShimmerBox` composable — animated `Brush.linearGradient` sweeping left to right (900ms, `LinearEasing`, `RepeatMode.Restart`). Used by all skeleton composables.

#### `TransactionCard.kt` — press scale
Spring-physics tap feedback via `MutableInteractionSource` + `collectIsPressedAsState`:
- Press: scales to 0.97× (`animateFloatAsState`)
- Release: springs back to 1.0× (`DampingRatioMediumBouncy` + `StiffnessHigh`)

#### `MainScreen.kt` — sync icon rotation
Replaced `CircularProgressIndicator` during `SyncState.Syncing` with a rotating `Icons.Default.Sync`:
- `infiniteRepeatable(tween(700, LinearEasing))` — full 360° rotation
- `Modifier.rotate(syncRotation)` applied to icon

#### `PhaseTransitionDialog.kt` — slide-up bottom sheets
Both `PhaseTransitionDialog` and `BlockerDialog` converted from `AlertDialog` to `ModalBottomSheet`:
- `skipPartiallyExpanded = true` — always fully expanded
- Built-in Material 3 spring animation on open/dismiss
- Rounded top corners (20dp); `Button` / `OutlinedButton` row at bottom
- Loading state: `CircularProgressIndicator` inside button replaces label
- Function signatures unchanged — callers (`TransactionDetailScreen.kt`) need no modification

#### `TransactionListScreen.kt` — skeleton list
Loading state replaced `Text("جارٍ التحميل…")` with 5× `TransactionCardSkeleton`:
- Each skeleton mirrors `TransactionCard` structure: 4dp shimmer accent bar + 3 shimmer text rows + 4dp progress bar placeholder
- Laid out in the same `LazyColumn` as real cards (identical spacing/padding)

#### `DashboardScreen.kt` — skeleton dashboard
`CircularProgressIndicator` replaced with `DashboardSkeleton` `LazyColumn`:
- Section header placeholder (shimmer pill)
- 3-column shimmer row (summary cards, 80dp tall, 16dp radius)
- Chart area placeholder (72dp tall, 12dp radius)
- 4× KPI row placeholders (48dp tall, 8dp radius)

---

## Remaining Phases

### Phase 6 — Alerts & SLA Engine (remaining work)
> ⚠️ As of 2026-07-05, `SlaCheckerWorker` (which partially implemented this) was removed along with the Phase-4 subsystem it depended on — see [Workflow, Roles & Field Overhaul](#workflow-roles--field-overhaul-2026-07-05). Any future SLA-alert engine needs a new per-transaction-phase tracking mechanism (e.g., timestamped phase entries on `Transaction`/`ActivityLog`), since `PhaseRecord` no longer exists.
- Notification center screen (bell icon + badge already wired in `MainScreen.kt`)
- Android `NotificationManager` channel + deep link into transaction detail on tap
- `RECEIVE_BOOT_COMPLETED` already in manifest to re-schedule after reboot

### Phase 8 — Reporting & Export
- PDF export using `PdfDocument` API
- CSV export via `DocumentsContract` to Downloads
- Filters: by phase, date range, status

### Phase 9 — Backend API + Offline Sync
- FastAPI backend (separate repo under `backend/`)
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

---

## Workflow, Roles & Field Overhaul (2026-07-05)

Requested changes to the customs-clearance workflow, permission model, division scoping, and tender-intake fields. Full plan: `polymorphic-strolling-lake.md` (session plan file).

### 1. Phase/status model simplified (7 phases → 5)

Phases 2 (Evaluation & Contract), 3 (Clearance Documentation), and 4 (Gov-Agency Processing) were removed. The old "Release" phase was renamed and the workflow gained a new final confirmation phase.

| # | Before | After |
|---|--------|-------|
| 1 | تحضير المناقصة وإصدارها (Tender Prep) | *unchanged* |
| 2 | التقييم والعقد (Evaluation & Contract) | **removed** |
| 3 | إعداد وثائق التخليص (Clearance Docs) | **removed** |
| 4 | الجهات الحكومية (Gov-Agency, parallel tracks) | **removed** |
| 5 | أمر الإفراج (Release Order) | → **"طلب تخليص" (Clearance Request)** |
| 6 | النقل والاستلام (Transit & Receipt) | → **Phase 3** (content unchanged) |
| 7 | التسوية المالية (Financial Settlement) | → **Phase 4** (content unchanged) |
| — | — | → **Phase 5 (new): "تم النقل الى المستودعات"** — a checkbox-style final confirmation, only reachable after Phase 4 (financial closing) |

`TransactionStatus` was trimmed to match: `EVALUATION_IN_PROGRESS`, `CONTRACT_PENDING_SIGNATURE`, `CONTRACT_SIGNED`, `CLEARANCE_DOCS_PREPARATION`, `DECLARATION_SUBMITTED`, `GOV_PROCESSING`, `GOV_APPROVED` were removed; `FINAL_RELEASE_ISSUED` was renamed to `CLEARANCE_ISSUED` ("تم التخليص"); a new terminal status `TRANSFERRED_TO_WAREHOUSE` was added (the terminal flag moved off `CLOSED`, which is now a pass-through step). `TransactionStateMachine` transitions and hard gates were rewritten accordingly, and `TransactionStateMachineTest` was rewritten (still 100% passing).

### 2. Phase-4 gov-agency subsystem removed entirely (dead code)

Since Phase 4 no longer exists, its entire supporting subsystem — which had no other use — was deleted:
`PhaseRecord`, `AssignedEntity`, `Phase4Tracks`, `Phase4TrackSheet`, `PhaseRecordEntity`, `PhaseRecordDao`, and all repository/ViewModel/UI wiring (`observePhaseRecords`, `completePhaseRecord`, etc.).

**Ripple effect discovered during this removal:** `SlaCheckerWorker` (the entire background SLA-alert engine) and `TransactionDao.countOverdueSla()` both queried `phase_records` exclusively — since `PhaseRecord`s were only ever created for Phase-4 tracks, both were dead-on-arrival once Phase 4 was gone and were removed rather than left silently non-functional. Dashboard KPIs (`overdueSlaCount`, `overdueItems`, `avgMilitaryDays`, `avgCustomsDays`, `delayedRatioPct`) and equivalent PDF/CSV report columns were removed for the same reason.

Room DB bumped **v2 → v3**: `MIGRATION_2_3` drops the `phase_records` table and clears `sla_configs` (renumbered to match the new phase count; reseeded on next app open via a `count()`-gated `onOpen` callback instead of the old `onCreate`-only seed).

### 3. New final phase: "تم النقل الى المستودعات" (checkbox)

Modeled as a normal `advanceStatus(TRANSFERRED_TO_WAREHOUSE)` call (reusing the state machine + activity log), but rendered in `TransactionDetailScreen` as a `Checkbox` instead of the generic "advance" button — enabled only for the role permitted to confirm it (see roles below), and only once the transaction has reached `CLOSED`.

### 4. Roles: `CLEARANCE` + `WAREHOUSE`

`COORDINATOR` ("منسق التخليص") was renamed to **`CLEARANCE`** ("التخليص"). A new **`WAREHOUSE`** ("المستودعات") role was added.

| Role | canWrite / canCreateTransaction | canMarkClearanceDone | canMarkWarehouseTransferred | seesAllDivisions |
|------|:---:|:---:|:---:|:---:|
| ADMIN | ✓ | ✓ | ✓ | ✓ |
| CLEARANCE | ✗ | ✓ | ✗ | ✓ |
| WAREHOUSE | ✗ | ✗ | ✓ | ✓ (cleared-only, see below) |
| SUPERVISOR | ✓ | ✗ | ✗ | ✗ (own division only) |
| TENDER_OFFICER *(added 2026-07-05)* | ✓ | ✗ | ✗ | ✗ (own division only) |
| VIEWER | ✗ | ✗ | ✗ | ✗ (own division only) |

- `CLEARANCE` is the only role (besides ADMIN) that can advance a transaction to `CLEARANCE_ISSUED` ("تم التخليص").
- `WAREHOUSE` is the only role (besides ADMIN) that can check the new "تم النقل الى المستودعات" checkbox.
- `TENDER_OFFICER` ("ضابط العطاء") can create and edit transactions/documents/notes, but only within their own division, and cannot approve or export.

### 5. Division isolation (شعبة)

`SUPERVISOR`, `VIEWER`, and `TENDER_OFFICER` are confined to their own `Department`:
- **Transaction list & dashboard:** filtered to `tx.division == user.department` (shared helper: `domain/usecase/TransactionAccessScope.kt` → `Transaction.isVisibleTo(user)`).
- **Create screen:** division dropdown is locked/pre-filled to the user's own department instead of a free picker.
- **Detail screen:** direct navigation to a transaction outside the user's scope (e.g., via notification) now shows a blocked state instead of the transaction.
- `WAREHOUSE` sees every division, but only transactions that have passed `CLEARANCE_ISSUED` (phase ≥ 2). `ADMIN`/`CLEARANCE` see everything, unrestricted.

### 6. New tender-intake fields

- **الوزن (Weight, kg)** — `Transaction.weightKg: Double?`
- **نوع الشحنة (Refrigerated y/n)** — `Transaction.isRefrigerated: Boolean`
- **العمر الافتراضي (Shelf life, free text)** — `Transaction.defaultShelfLife: String?`, shown only when division = `MEDICAL_CONSUMABLES` (شعبة المستهلكات). Room bumped **v3 → v4** (`MIGRATION_3_4`).
- **Business rule:** `Priority.URGENT` ("عاجل") can only be selected when `beneficiary == RMS` **and** `isRefrigerated == true`; enforced both in the Create screen (chip disabled + auto-reset) and in `CreateTransactionViewModel` (hard validation).

All three new fields round-trip through `TransactionEntity`, `SyncDtos`, the detail view, and the CSV full-export report.

### 7. `TENDER_OFFICER` role + division-less accounts (follow-up, same day)

- Added **`TENDER_OFFICER`** ("ضابط العطاء"): can create and edit transactions, upload documents, and add notes, but strictly within their own division — cannot see or create outside it, and cannot approve or export. Same division-scoping mechanism as `SUPERVISOR`/`VIEWER` (§5).
- **`User.department` is now nullable** (`Department?`). `ADMIN`, `CLEARANCE`, and `WAREHOUSE` no longer belong to any division — they were already functionally unrestricted (`seesAllDivisions`), but previously still had an arbitrary department value on their account; that value is now `null` for these three roles, both for newly-created accounts and existing ones (migration backfill).
- `UserManagementScreen`'s create-account and change-role dialogs now hide the division picker entirely for `ADMIN`/`CLEARANCE`/`WAREHOUSE`, and require it for `SUPERVISOR`/`VIEWER`/`TENDER_OFFICER`.
- Room bumped **v4 → v5** (`MIGRATION_4_5`): recreates the `users` table (SQLite can't drop a `NOT NULL` constraint via `ALTER TABLE`) with `department` nullable, then nulls it out for existing `ADMIN`/`CLEARANCE`/`WAREHOUSE` rows.
- `UserRepository.updateRole()` now also takes a `department: Department?` so changing a user's role and re-scoping their division happens atomically.

### 8. Phases 3 & 5 merged — workflow now 4 phases (follow-up, same day)

Phase 3 ("النقل والاستلام في المستودعات الطبية" — Transit & Warehouse Receipt) and the old Phase 5 ("تم النقل الى المستودعات") described the same real-world event, so Phase 3 was removed and the final phase renumbered forward:

| # | Phase |
|---|-------|
| 1 | تحضير المناقصة وإصدارها (Tender Prep) |
| 2 | طلب تخليص (Clearance Request) |
| 3 | إغلاق المعاملة والتسوية المالية (Financial Settlement) — was phase 4 |
| 4 | تم النقل الى المستودعات (Transferred to Warehouses) — was phase 5, still the final/terminal phase |

- `TransactionStatus.IN_TRANSIT`, `RECEIVED_AT_WAREHOUSE`, `INSPECTION_COMPLETE` removed — the chain now goes `CLEARANCE_ISSUED → FINANCIAL_SETTLEMENT_PENDING → CLOSED → TRANSFERRED_TO_WAREHOUSE` directly. (The separate `shipmentStatus` field — EXPECTED/ARRIVED/CLEARED on the Shipment Status card — already tracks physical arrival independently of the phase workflow, so no tracking was lost.)
- **Closing color:** `TRANSFERRED_TO_WAREHOUSE` now renders in **red** (`CustomsColors.Overdue`) instead of grey — performing this action visibly marks the transaction as closed. `CLOSED` (the intermediate financial-closing step) keeps its previous grey.
- Confirmed unchanged: `WAREHOUSE` (or `ADMIN`) remains the only role that can perform this final confirmation (`canMarkWarehouseTransferred`), gated in `TransactionDetailScreen`.
- **Bug fix found in passing:** `DocumentType.requiredPhase` was still using the *original* 7-phase numbering (3/4/5/6/7) from before the very first phase-count reduction earlier this session — meaning the "required documents" checklist had been silently broken (comparing against phase numbers that no longer existed, 1–4 at that point) since that change. Remapped all document types to the current 1–4 phase numbers.
- Room bumped **v5 → v6** (`MIGRATION_5_6`): remaps any existing rows sitting in the removed transit statuses/phase forward to `FINANCIAL_SETTLEMENT_PENDING`/`PHASE_3_FINANCIAL`, and reseeds `sla_configs` (sub-phase numbering shifted).

### Verification
- `.\gradlew assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**, all unit tests passing (state machine suite rewritten again for the 4-phase model).
- ⚠️ Room migrations v2→v6 were **not** exercised against a live pre-v2 install in this session (no device/emulator attached) — verified via `exportSchema=false` compile-time check only. Recommend either a fresh install or a manual upgrade test before shipping to devices with existing data.

### 9. Bug fix — `TENDER_OFFICER` could perform clearance via the Shipment Status card (user-reported)

Found by manual testing: `ShipmentStatusCard` (`TransactionDetailScreen.kt`) has its own independent "تأكيد اكتمال التخليص" (Confirm Clearance Complete) button — separate from the actual workflow's `CLEARANCE_ISSUED` transition — that sets `Transaction.shipmentStatus` from `ARRIVED` to `CLEARED`. When `CLEARANCE`/`WAREHOUSE`/`TENDER_OFFICER` roles were introduced earlier this session, only the real workflow transition was re-gated to `canMarkClearanceDone`; this second button was left gated by the generic `canWrite` flag, which `TENDER_OFFICER` (and `SUPERVISOR`) also carry — so a Tender Officer could still mark a shipment "cleared" through this card even though they can't touch `CLEARANCE_ISSUED` itself.

**Fix:** `ShipmentStatusCard` now takes a separate `canMarkCleared` parameter (`role.canMarkClearanceDone`, i.e. `ADMIN`/`CLEARANCE` only) gating just the `ARRIVED → CLEARED` button; the `EXPECTED → ARRIVED` "تسجيل وصول الشحنة" button remains gated by general `canWrite` since registering physical arrival isn't a clearance action.

### 10. ADMIN "view-as" testing mode (in-memory, not persisted)

ADMIN accounts can now temporarily impersonate any other role/division combination for manual testing (e.g. "ضابط عطاء" + Pharmacy, "ضابط عطاء" + Medical Devices, Clearance, Warehouse), without creating separate test accounts or logging in/out.

- **`UserSession`** (`AppSession.kt`) gained `realUser: User?` (and `isViewingAs` helper). `AuthViewModel.viewAs(role, department)` swaps `session.user`'s `role`/`department` for the picked persona while keeping the same `id`/`username` — so anything the simulated persona creates (`createdByUserId`, activity log entries) is still correctly attributed to the real admin account. `exitViewAs()` restores the original. Neither touches `SessionStore` or the database — the override is purely in-memory and resets on logout or app restart.
- **Starting it:** `SettingsScreen` → "وضع التجربة" card (visible only while genuinely acting as `ADMIN` — it naturally hides itself once a persona is active, same as any other admin-only section). Role dropdown (all roles except `ADMIN`) plus a division dropdown shown only for division-scoped roles.
- **Exiting it:** a persistent gold banner appears at the top of `MainScreen` ("وضع تجربة: {role} — {division}") with a "الرجوع كمسؤول" button — placed outside the admin-gated section specifically so it's still reachable no matter how restrictive the simulated role's permissions are.
- **Bug caught while wiring this up:** `TransactionListScreen`/`DashboardScreen` pushed the current user into their ViewModels via `LaunchedEffect(session?.user?.id)`. Since view-as intentionally keeps the same `id`, switching personas wouldn't have re-triggered that effect — division-scoping would've stayed stuck on whichever persona loaded first. Changed the key to `session?.user` (whole object) so it reacts to role/department changes too.

### Verification
- `.\gradlew assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**.

### 11. Clearance/warehouse permission enforced at the repository layer too (user-reported, hardened)

User reported a real `TENDER_OFFICER` account (role confirmed correct via User Management) was able to walk a transaction straight through `تم التخليص` and reach `مغلقة` via "تقديم للمرحلة التالية" — something that line-by-line review said `canAdvanceNext`/`canMarkClearanceDone` should prevent. No second copy of `UserRole.kt` or alternate call path to `advanceStatus` was found, so the exact UI-level trigger wasn't conclusively identified — but the underlying architectural gap was real and worth closing regardless: **`TransactionRepositoryImpl.advanceStatus` never checked the actor's role at all** — the `CLEARANCE_ISSUED`/`TRANSFERRED_TO_WAREHOUSE` restriction existed *only* as UI button-visibility logic in `TransactionDetailScreen`, with zero enforcement underneath. Any UI edge case that showed the button/checkbox would have gone straight through.

**Fix:** `TransactionRepositoryImpl` now injects `UserDao`, looks up the actor's role, and rejects (`error(...)`, surfaced through the existing `TransitionUiState.Error` → snackbar path) any `advanceStatus` call targeting `CLEARANCE_ISSUED` without `canMarkClearanceDone`, or `TRANSFERRED_TO_WAREHOUSE` without `canMarkWarehouseTransferred` — regardless of what UI path triggered the call.

### 12. `closedAt` / "مغلقة" label mismatch (related, found while investigating the above)

User also flagged that closing a transaction reached "مغلقة" *before* the final "تم النقل الى المستودعات" step, when closing should be the very last thing. Two real issues:
- `Transaction.closedAt` was stamped when reaching `CLOSED` (the intermediate financial-settlement step), not the true terminal `TRANSFERRED_TO_WAREHOUSE` — so dashboard "closed this month" and CSV export duration figures counted transactions as closed a step early. Moved the `closedAt` stamp to `TRANSFERRED_TO_WAREHOUSE`.
- The `CLOSED` status label "مغلقة" (literally "closed") misleadingly implied the transaction was fully done one step early. Renamed to "التسوية المالية مكتملة" (Financial settlement complete); "مغلقة" is now only ever associated with the true final step.

### 13. Workflow re-simplified to the exact 4-step operational process (user-specified)

User specified the real operational process precisely, which turned out simpler than the milestone-11/12 model: create → tender officer marks shipment arrived at airport → clearance marks cleared → warehouse marks transferred (closes the transaction, notifies everyone). No "نشر المناقصة" step, no separate "التسوية المالية" phase, and no separate "حالة الشحنة" card — that card was in fact the root cause of the earlier dual-clearance-button confusion (milestone 9), so removing it was the right call independent of this simplification.

**New phase/status model (4 phases, replacing the milestone-8 5→4 model):**

| # | Phase | Status | Actor |
|---|-------|--------|-------|
| 1 | تحضير المعاملة | `DRAFT`/`TENDER_PREPARATION` | ضابط العطاء creates/edits |
| 2 | وصلت الشحنة للمطار | `ARRIVED_AT_AIRPORT` *(new)* | ضابط العطاء — exclusive |
| 3 | تم التخليص | `CLEARANCE_ISSUED` | التخليص — exclusive |
| 4 | تم النقل الى المستودعات | `TRANSFERRED_TO_WAREHOUSE` | المستودعات — exclusive, terminal, closes the transaction |

- Removed `TENDER_PUBLISHED`, `FINANCIAL_SETTLEMENT_PENDING`, `CLOSED` from `TransactionStatus`; state machine is now a strict single-path chain (no branching, so the old `checkHardGates()` function was removed as redundant — the transition map alone fully enforces order).
- **Removed `ShipmentStatus` enum, `Transaction.shipmentStatus`, and the `ShipmentStatusCard`/`ShipmentStepIndicator` composables entirely.** `actualArrivalDate` is retained but now auto-stamped by `TransactionRepositoryImpl` when `ARRIVED_AT_AIRPORT` is reached, instead of via a separate manual button.
- The generic "advance to next phase" button in `TransactionDetailScreen` now shows the specific action label (`nextStatus.labelAr()` — e.g. "وصلت الشحنة للمطار") instead of the generic "تقديم للمرحلة التالية", matching the named-button process description.
- **New: closure notifications.** Reaching `TRANSFERRED_TO_WAREHOUSE` now creates an `AppNotification` (`NotificationType.TRANSACTION_CLOSED`) and posts an Android system notification via a new `CustomsNotificationManager.postTransactionClosedNotification()` / `CHANNEL_TRANSACTION_CLOSED` channel. Note: `AppNotification` has no per-user targeting (it's a single shared feed), so in the current local-only, unsynced architecture this reaches "all accounts logged into this device" — genuine cross-device/all-org delivery would require the still-unwired Phase-9 backend sync.
- `DocumentType.requiredPhase` remapped again (clearance's paperwork shifted from phase 2 to phase 3; a new phase-2 bucket added for shipping/transport documents relevant on arrival; the removed financial phase's `PAYMENT_VOUCHER` moved to phase 4 alongside the other closing documents).
- Dashboard: removed the shipment expected/arrived/cleared 3-card breakdown (redundant with phase distribution now); kept the "upcoming arrivals" banner, re-derived from `currentPhase.number < 2` instead of `shipmentStatus`.
- `SlaConfigDefaults`/`SlaAdminScreen` sub-phase numbering and labels updated to match.
- Room bumped **v6 → v7** (`MIGRATION_6_7`): remaps removed statuses/phases forward, recreates `transactions` without the `shipmentStatus` column, reseeds `sla_configs`.

### 14. Warehouse visibility threshold left stale after the phase renumbering in #13 (user-reported)

User reported the same symptom pattern again at "المرحلة 3: تم التخليص" — tender officer able to trigger the advance action before clearance had acted, leaving the transaction unreachable for both Clearance and Warehouse. Re-verified `TransactionDetailScreen`'s `canAdvanceNext` gating and `TransactionRepositoryImpl.advanceStatus`'s role check line-by-line (both from #11) — both are still intact and correct; no way found for `TENDER_OFFICER` to actually complete `CLEARANCE_ISSUED`/`TRANSFERRED_TO_WAREHOUSE` through the app's code paths.

**Bug actually found while re-checking:** `TransactionAccessScope.kt` (`Transaction.isVisibleTo`) still gated `WAREHOUSE` visibility with the *pre-#13* phase number — `currentPhase.number >= 2` — which was correct when Clearance was phase 2, but #13 renumbered Clearance to phase 3 (inserting the new "وصلت الشحنة للمطار" as phase 2) without updating this threshold. Net effect: `WAREHOUSE` could see transactions from `ARRIVED_AT_AIRPORT` onward — i.e. **before** Clearance had acted — rather than only `CLEARANCE_ISSUED` onward as required ("رؤية المعاملات التي تم تخليصها فقط"). This is a visibility leak, not a permission bypass (Warehouse still couldn't act early, since `canAdvanceNext`/the repository check remained correctly gated on the real status) — so on its own it doesn't explain "stuck, nobody can act," but it was a confirmed regression and is fixed now regardless.

**Fix:** Changed the threshold to `TransactionPhase.PHASE_3_CLEARANCE.number` (named reference instead of a magic number, so it can't silently drift again on a future renumbering).

**Status of the core reported symptom:** Not reproduced via code review after two independent passes — `TENDER_OFFICER`'s `canMarkClearanceDone`/`canMarkWarehouseTransferred` are `false`, both the button-visibility check and the repository-layer check (added in #11) correctly block the transition, and no other call path to `advanceStatus` exists. Asked the user to confirm they're testing the freshly rebuilt APK and, if it still reproduces, to report the exact button pressed and whether an error/snackbar appeared — that would point to a genuinely different bug (e.g. the UI somehow bypassing `TransactionRepository.advanceStatus` entirely) than anything found so far.

### Verification
- `.\gradlew assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**, all unit tests passing.

### 15. "View as" impersonation silently re-authorized as the real admin at the repository layer (user-reported)

User confirmed the core workflow (#13/#14) now works correctly, but reported that testing via Admin's "وضع التجربة" (view-as) feature — simulating `CLEARANCE` or `WAREHOUSE` — didn't behave the same as a real dedicated account of that role.

**Root cause:** `AuthViewModel.viewAs()` builds the simulated user via `realUser.copy(role = role, department = department)` — deliberately keeping the same `id` as the real admin so activity is still attributed correctly. But `TransactionRepositoryImpl.advanceStatus`'s permission check (added in #11 as defense-in-depth) re-derived the actor's role by looking the `actorUserId` back up in the `users` table: `userDao.getById(actorUserId)?.role`. Since that ID is always the real admin's ID during a "view as" session — never the simulated role — this lookup always resolved back to `ADMIN`, which passes every permission check unconditionally. In effect, the repository-layer gate was silently a no-op for the entire duration of any "view as" session, regardless of which role was being simulated: it always evaluated as if the real admin (not the simulated role) were the actor. The UI-layer gate (`canAdvanceNext`, correctly keyed off `session.user.role`) still matched the simulated role, so the two layers disagreed — which is exactly the "doesn't work the same way" symptom, since a *real* `CLEARANCE`/`WAREHOUSE`/`TENDER_OFFICER` account has no such split (its DB row's role and its session role are always identical).

**Fix:** `TransactionRepository.advanceStatus`/`TransactionRepositoryImpl.advanceStatus` now take an explicit `actorRole: UserRole` parameter supplied by the caller (`TransactionDetailViewModel.advanceStatus`, in turn from `session.user.role` in `TransactionDetailScreen`) instead of re-deriving it from the DB via `actorUserId`. `session.user.role` is exactly the simulated role during "view as" and exactly the real role otherwise, so both cases are now handled by the same code path with no special-casing. The now-unused `UserDao` injection was removed from `TransactionRepositoryImpl`.

### Verification
- `.\gradlew assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**, all unit tests passing.
