# iOS Support via Kotlin/Compose Multiplatform — Plan & Progress

> **Started:** 2026-07-06
> **Repo:** `github.com/balor195/rms-customs-tracker` (private) — pushed here specifically so GitHub Actions could build the iOS target
> **Why this doc exists:** this is a multi-session migration. Each phase below is scoped so it can be picked up independently — read the Context, check the Status line, then resume at the first unchecked phase.

---

## Context

CustomsTracker was a single-module native Android app (Kotlin 2.0.21, AGP 8.7.0, Jetpack Compose, Hilt, Room, Retrofit, WorkManager, CameraX) talking to a plain FastAPI/REST backend (`backend/`), with no shared cross-platform layer. The goal is to add an iOS target while reusing as much of the existing `domain`/`data`/`presentation` code as possible, via **Kotlin Multiplatform + Compose Multiplatform** — not a from-scratch Swift rewrite, and not a Flutter/RN rewrite that would discard the existing Kotlin/Compose investment.

**Key constraint:** development happens on Windows with no local Mac. Xcode/iOS builds only run on macOS, so **GitHub Actions macOS runners are the only way to build or verify the iOS target.** Every phase below must be verifiable through CI, not local Xcode. `gh` CLI is installed as a portable binary (no admin rights available) at:
`C:\Users\mohba\AppData\Local\gh-portable\bin\gh.exe` — authenticated as `balor195`, with `repo` + `workflow` token scopes.

This is a large migration overall (Hilt→Koin, Retrofit→Ktor, Room→Room-KMP, plus expect/actual work for WorkManager, CameraX, notifications, secure storage, and PDF export — none of which have direct iOS equivalents). It will not land in one pass. Each phase should get its own plan/review before starting — don't attempt to implement phases 2-6 in one sitting.

---

## Status

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Skeleton, CI, domain migration | ✅ Done — 2026-07-06 |
| 2 | DI & networking (Hilt→Koin, Retrofit→Ktor) | ✅ Done — 2026-07-06 |
| 3 | Persistence (Room→Room-KMP, DataStore) | ✅ Done — verified on iOS CI, 2026-07-07 |
| 4 | Platform abstractions (expect/actual) | 🔶 In progress — see sub-phase table below |
| 5 | UI migration to commonMain | ⬜ Not started |
| 6 | iOS polish & release | ⬜ Not started |

---

## Phase 1 — Skeleton, CI, and domain migration ✅ Done

### What shipped

1. **`CustomsTracker/app` converted to a KMP module**: `com.android.application` + `org.jetbrains.kotlin.multiplatform` + `org.jetbrains.compose` (Compose Multiplatform 1.7.3) applied together. Targets: `androidTarget()`, `iosArm64()`, `iosSimulatorArm64()`, `iosX64()` (producing a static `shared` framework). `gradle/libs.versions.toml` updated with the new plugin/library entries.
2. **Source sets restructured** to KMP convention:
   - `app/src/main/java` → `app/src/androidMain/kotlin`, `app/src/main/res` → `app/src/androidMain/res`, `app/src/main/AndroidManifest.xml` → `app/src/androidMain/AndroidManifest.xml`
   - `app/src/test/java` → `app/src/androidUnitTest/kotlin`, `app/src/androidTest/java` → `app/src/androidInstrumentedTest/kotlin`
   - `domain/model/enums/*` (10 files, zero JVM-only deps) → `app/src/commonMain/kotlin/...`
   - **Everything else stayed in `androidMain`** — see "Scope correction" below for why.
3. **`iosApp/` Xcode project** — generated via **XcodeGen** (`iosApp/project.yml`), not a hand-written `.xcodeproj`, since there's no local Xcode to validate a hand-rolled `project.pbxproj` against. Contains a minimal SwiftUI `App` (`iosAppApp.swift`) wrapping a `UIViewControllerRepresentable` (`ContentView.swift`) that hosts `MainViewController()` (in `app/src/iosMain/kotlin/.../MainViewController.kt`), which renders a placeholder `App()` composable (`app/src/commonMain/kotlin/.../App.kt`) — proves Compose Multiplatform UI actually renders on iOS, not just that Kotlin compiles for it.
4. **CI**: `.github/workflows/ios-build.yml` on `macos-latest` — installs XcodeGen, generates the Xcode project, builds the shared Kotlin framework via a Gradle Run Script build phase (`:app:embedAndSignAppleFrameworkForXcode`, wired into `project.yml`'s `preBuildScripts`), builds the app for whatever iPhone simulator the runner image actually has (picked dynamically, not hardcoded), boots it, installs and launches the app, and uploads a screenshot + diagnostic logs as a workflow artifact.

### Scope correction found during implementation

The domain layer was assessed as "platform-agnostic, no Android imports" — true, but incomplete: it also uses **`java.util.UUID`** (in `domain/model/*.kt`, `domain/repository/*.kt` — 34 files touch this transitively through Room entities, DTOs, ViewModels, UI) and **`javax.inject.Inject`** (Hilt annotations on `LoginUseCase`, `SetupAdminUseCase`, `TransactionStateMachine`) — neither available on Kotlin/Native. Fixing this properly means a UUID→String migration across ~34 files plus stripping/replacing the Hilt annotations — real work, not a "just move the files" change. Decision made with the user: keep Phase 1 truly zero-behavior-change on the Android side, so only the dependency-free enums moved now; the rest of `domain/` moves in **Phase 2**, bundled with the Hilt→Koin migration (since that's exactly when the `@Inject` annotations need to go anyway) and the Room-KMP work (since Room entities are where most of the UUID usage lives).

### Bugs found only by actually running the pipeline (fixed, in order)

1. **Missing Unix `gradlew`** — the repo only ever had `gradlew.bat` (Windows-only project history). CI's `chmod +x gradlew` failed with "No such file or directory." Fixed by running `gradlew.bat wrapper --gradle-version 8.9` to regenerate it properly (don't hand-type Gradle's wrapper script — it's easy to get subtly wrong), plus a `.gitattributes` (`/gradlew text eol=lf`) so Windows checkouts don't corrupt the shebang line with CRLF.
2. **Hardcoded `iPhone 15` simulator** — the `macos-latest` runner's current Xcode image (26.5 at time of writing) doesn't ship that device. Fixed by picking whatever iPhone simulator is available at CI runtime (`xcrun simctl list devices available | grep iPhone | head -n 1`) instead of hardcoding a name.
3. **Compose Multiplatform iOS crash-on-launch** — `xcrun simctl launch` reported success (it only fails if the launch *request* fails, not if the app subsequently crashes), but the screenshot showed the home screen, not the app. Root cause only visible via captured console logs: Compose Multiplatform's UIKit integration throws `IllegalStateException` at startup unless `Info.plist` has `CADisableMinimumFrameDurationOnPhone`. Fixed via `INFOPLIST_KEY_CADisableMinimumFrameDurationOnPhone: YES` in `project.yml`. **Lesson for future CI debugging on this project:** always capture `simctl launch --console-pty` output and crash logs, not just a screenshot — a crashed app and a slow-to-render app look identical in a single screenshot taken after a fixed sleep.

### How to verify Phase 1 is still working

```
# Android (local, no Mac needed) — from CustomsTracker/
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"   # PowerShell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest

# iOS (needs a push — CI is the only way to check this)
gh run list --repo balor195/rms-customs-tracker --limit 3
gh run view <run-id> --repo balor195/rms-customs-tracker
gh run download <run-id> --repo balor195/rms-customs-tracker --name ios-launch-diagnostics
# → check screenshots/launch.png actually shows the app UI, not the home screen
```

---

## Phase 2 — DI & networking ✅ Done

Split into four sequenced sub-steps (2a–2d) — see the approved plan at the top of this phase's work for the full rationale. Each sub-step is its own commit and independently verifiable; only 2d needs iOS CI.

### Phase 2a — Koin migration ✅ Done — 2026-07-06

- Removed Hilt entirely: plugin, `hilt-*`/`ksp` processor entries from `libs.versions.toml` and both `build.gradle.kts` files; added Koin 4.0.0 (`koin-core`, `koin-android`, `koin-androidx-compose`, `koin-androidx-workmanager`) via BOM.
- `di/DatabaseModule.kt`, `di/NetworkModule.kt`, `di/RepositoryModule.kt` converted from Hilt `@Module`/`@Binds`/`@Provides` to Koin `module { single { ... } }` DSL. Added `di/CoreModule.kt` (SessionStore, CustomsNotificationManager, CsvExporter, PdfExporter, TransactionStateMachine, LoginUseCase, SetupAdminUseCase) and `di/ViewModelModule.kt` (all 11 ViewModels via `viewModel { ... }`).
- Stripped `@Inject`/`@Singleton`/`@HiltViewModel`/`@AndroidEntryPoint`/`@HiltAndroidApp` everywhere — Koin needs no annotations, just plain constructors resolved via `get()` in module definitions. This included the three domain/usecase classes (`LoginUseCase`, `SetupAdminUseCase`, `TransactionStateMachine`) even though they don't move to `commonMain` until 2d, so that move is a pure file relocation later.
- `CustomsApp.kt`: replaced Hilt bootstrap with `startKoin { androidContext(...); workManagerFactory(); modules(...) }`.
- `SyncWorker.kt`: dropped `@HiltWorker`/`@AssistedInject`; now a plain `CoroutineWorker(context, params, syncRepository)` constructed via Koin's `worker { params -> SyncWorker(params.get(), params.get(), get()) }` DSL (`di/WorkModule.kt`).
- All 12 `hiltViewModel()` call sites across 11 Composable files switched to `koinViewModel()`.

**Bug found only by actually compiling (fixed):** `KoinApplication.workManagerFactory()` (called inside `startKoin { }`) returns `Unit` — it only *registers* the WorkManager integration in the Koin graph, it does not return a usable `WorkerFactory`. The actual `WorkerFactory` to hand to `Configuration.Builder().setWorkerFactory(...)` is `org.koin.androidx.workmanager.factory.KoinWorkerFactory()`, a plain class with a no-arg constructor (it resolves the global Koin instance lazily at worker-creation time via `KoinComponent`, so it's safe to instantiate before `startKoin` runs). Found by decompiling the actual Koin 4.0.0 artifact with `javap` after the first compile attempt failed with a type mismatch — matches Phase 1's lesson that library API assumptions need checking against the real thing, not memory.

**Verified:** `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` green on Windows. No emulator/device was available in this session to manually click through login/dashboard/sync, so that manual pass is still outstanding — do it before starting 2b if possible.

### Phase 2b — Ktor migration ✅ Done — 2026-07-06

- Replaced **Retrofit + OkHttp** with **Ktor client 3.0.3** (OkHttp engine on Android, Darwin engine on iOS added in `iosMain` even though it's not wired into DI yet — that's Phase 5) against the existing FastAPI REST contract in `backend/`. `CustomsApi` is now a plain class wrapping an injected `HttpClient` with the same two suspend methods (`push`/`pull`), same endpoints, same `kotlinx.serialization` config.
- `ServerUrlInterceptor.kt` (OkHttp `Interceptor`) replaced by `data/network/ServerUrl.kt`: a plain `ServerUrlHolder` (unchanged SharedPreferences-backed read/save) plus a Ktor `createClientPlugin("ServerUrl")` that rewrites `request.url.protocol/host/port` in `onRequest`, reading the holder fresh on every request (not baked in at client-construction time) so the Settings screen's "change server URL" still takes effect without an app restart.
- `di/NetworkModule.kt` now builds the Ktor `HttpClient(OkHttp) { ... }` with `HttpTimeout`, `ContentNegotiation` (kotlinx.serialization json), `Logging` (BODY level), and installs `ServerUrlPlugin`.
- `SettingsViewModel.kt` updated to depend on `ServerUrlHolder` instead of `ServerUrlInterceptor` (same `readUrl()`/`saveUrl()` method names, so it was a pure type-rename).

**Verified:** `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` green on Windows on the first compile attempt (no new library-API surprises this time). Manual sync push/pull against the local FastAPI backend and the Settings screen's live URL-change flow were not re-tested in this session (no emulator/device attached) — worth a manual pass before 2c.

### Phase 2c — UUID→String migration ✅ Done — 2026-07-06

- All 6 domain models, 5 domain repository interfaces, 6 data repository impls, 6 Room entity mapper files, `SessionStore.kt`, 6 ViewModels, and 2 Composables (`DocumentsTab.kt`, `NotificationCenterScreen.kt`) switched from `java.util.UUID` to `String`. New IDs now generated via Kotlin 2.0's multiplatform stdlib `kotlin.uuid.Uuid.random().toString()` (`@OptIn(ExperimentalUuidApi::class)`) instead of `java.util.UUID.randomUUID()`.
- Room entity mapper `toDomain()`/`toEntity()` functions across all 6 entities lost their `UUID.fromString(...)`/`.toString()` conversions entirely — both sides were already `String` under the hood (Room columns are `TEXT`), so this was a straight pass-through simplification, not a schema change.
- `TransactionStateMachineTest.kt` (androidUnitTest) updated to build its test `Transaction` fixtures with `Uuid.random().toString()`.
- **Left untouched, deliberately:** the 3 `androidInstrumentedTest` DAO tests (`PhaseRecordDaoTest`, `UserDaoTest`, `TransactionDaoTest`) already operated purely on `Entity` types (`String` ids) and only used `java.util.UUID.randomUUID()` as a convenient random-string generator for test fixtures — not a reference to the domain `UUID` type being removed. No changes needed; still valid Android-only JVM test code.
- `PasswordHasher.kt` is unaffected (never used `UUID`, stays androidMain per the Phase 2 plan).

**Verified:** `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` green.

### Phase 2d — Move domain/ to commonMain ✅ Done — 2026-07-06

- Moved (via `git mv`, preserving history) all of `domain/model/*.kt` (6 files), `domain/repository/*.kt` (6 interfaces, including `SyncRepository`), `domain/usecase/{LoginUseCase,SetupAdminUseCase,SlaConfigDefaults,TransactionAccessScope}.kt`, and `domain/statemachine/TransactionStateMachine.kt` from `androidMain` to `commonMain`. **`PasswordHasher.kt` stays in `androidMain`** (comment added explaining why — only consumed by `UserRepositoryImpl`, which stays androidMain until Phase 3).
- Extracted `NotificationType` out of `AppNotification.kt` into `commonMain/domain/model/enums/NotificationType.kt`, matching where the other enums already live — fixed the Phase 1 inconsistency flagged during Phase 2 planning. Updated the 7 dependent files' imports.
- `Transaction.kt`'s `daysSinceUpdate` switched from `System.currentTimeMillis()` (JVM-only) to `kotlinx-datetime`'s `Clock.System.now().toEpochMilliseconds()`. Added `kotlinx-datetime:0.6.1` to `commonMain` deps.
- No import changes were needed in any `androidMain` consumer of the moved classes — packages didn't change (only source-set location did), and `androidMain` already depends on `commonMain` in KMP, so this was a pure file relocation plus the two content fixes above.

**Verified:** `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` green (confirms `androidMain`, which depends on `commonMain`, still compiles and runs correctly against the moved code). iOS CI push confirms the actual payoff — that `domain/` now compiles for Kotlin/Native (`iosArm64`/`iosSimulatorArm64`/`iosX64`), which can't be checked without a Mac.

## Phase 3 — Persistence (in progress)

Split into two sub-steps (3a, 3b), same bisectable-commit pattern as Phase 2.

### Phase 3a — Room 2.6.1 → 2.8.4 KMP migration ✅ Done — 2026-07-06

- **Version decision, verified against live docs, not memory:** Room shipped a new major version (Room 3.0, `androidx.room3` package, full rename) as recently as March 2026, but it's very new/still stabilizing. The official, currently-live "Set up Room Database for KMP" doc still recommends **Room 2.8.4** for multiplatform projects — targeted that instead of jumping to 3.0 or staying on the pre-KMP 2.6.1.
- Confirmed via research before starting: all 6 entities were already String/Long/Int/Double/Boolean-only (Phase 2c's UUID removal already did this work) and all 6 DAOs were already fully KMP-safe (`Flow`/`suspend fun`, no `LiveData`/`Cursor`/`Context`) — so **DAOs and entities moved to `commonMain` via `git mv` with zero code changes.** All the real work was in `CustomsDatabase.kt` and `di/DatabaseModule.kt`.
- `CustomsDatabase.kt`: added `@ConstructedBy(CustomsDatabaseConstructor::class)` + `expect object CustomsDatabaseConstructor : RoomDatabaseConstructor<CustomsDatabase>` (Room's KSP compiler generates the `actual` bodies per target — confirmed by checking the generated file after a local build, not assumed). Rewrote all 6 migrations: `migrate(database: SupportSQLiteDatabase)` → `migrate(connection: SQLiteConnection)`, `database.execSQL(...)` → `connection.execSQL(...)` — SQL strings themselves unchanged.
- Dropped the `RoomDatabase.Callback`-based SLA-seeding (its new-API shape was the one piece not worth chasing down blind) in favor of a behaviorally-equivalent "seed after building the database" coroutine launch in `di/DatabaseModule.kt` — same effect, no dependency on Callback's exact new signature.
- Added platform-specific (not expect/actual — different parameter shapes) `getDatabaseBuilder()` functions: Android's takes a `Context` and uses `context.getDatabasePath(...)` (same location Room's old constructor used, so no data loss for existing local dev databases); iOS's takes no params and resolves the path via `NSFileManager`/`NSDocumentDirectory`.
- Gradle: bumped `room` to `2.8.4`, added `sqlite-bundled:2.7.0`, moved `room-runtime`/`room-ktx` to `commonMain`, added per-target KSP processor deps (`kspIosX64`/`kspIosArm64`/`kspIosSimulatorArm64` alongside the existing `kspAndroid`) — Kotlin/Native doesn't share one compiled artifact across those three targets like JVM does, so KSP needs to run once per target.
- **Found and fixed while touching this area (pre-existing, unrelated to this migration):** `PhaseRecordDaoTest.kt` (`androidInstrumentedTest`) referenced a `PhaseRecordDao`/`PhaseRecordEntity` deleted back in `MIGRATION_2_3` — deleted the dead test. `TransactionDaoTest.kt`'s `makeEntity()` helper was missing 10 constructor params added by migrations 1-3 (never updated) — fixed. `UserDaoTest.kt` called a `dao.verifyCredentials(...)` method that doesn't exist on `UserDao` (that logic lives in `UserRepositoryImpl` + `PasswordHasher` since Phase 2) — rewrote the test to check what the DAO actually does (round-trip the stored hash via `getByUsername`). None of this compiled before Phase 3 touched it (`androidInstrumentedTest` was never part of any prior verification step in this project — no emulator available in this dev environment).

**De-risking note that worked as intended:** the Migration-signature rewrite (the single highest-uncertainty item going in) was fully caught by a local `:app:compileDebugKotlinAndroid` — no iOS CI cycle needed to validate it. Only the per-target KSP wiring and iOS-specific builder needed the actual CI round-trip.

**Bugs the CI round-trip caught (exactly the kind of thing it's for):**
1. First push failed at `kspKotlinIosSimulatorArm64` with "No matching variant of `kotlinx-coroutines-android`" — `room-ktx` had been carried over into `commonMain` alongside `room-runtime` on the assumption both belonged together, but `room-ktx` is Android-only (pulls in `kotlinx-coroutines-android` transitively, which has no iOS variant) and, on inspection, wasn't actually used anywhere in the codebase — Room's own Flow support in `room-runtime` already covers every DAO here. Removed `room-ktx` entirely rather than relocating it.
2. Second push got past dependency resolution but failed at the same task with a KLIB resolver error: Room 2.8.4's precompiled `room-common` klib for `iosSimulatorArm64` was built with a Kotlin compiler producing klib ABI version `2.2.0`, while this project's Kotlin 2.0.21 only understands ABI `1.8.0` — a hard binary-compatibility wall, not a config mistake (Room 2.x's later 2.8.x patch releases were evidently built with a much newer Kotlin toolchain than 2.0.21 by the time they shipped). Rather than bumping the whole Kotlin/Compose-Multiplatform/AGP/KSP toolchain (bigger blast radius, a separate task from Phase 3's scope), pinned `room` down to `2.7.2` — an earlier KMP-stable patch release, evidently built against a Kotlin compiler whose klib ABI matches 2.0.21.

Neither of these two bugs was catchable from the local Android-only compile — both are specific to how the iOS `kspKotlinIosSimulatorArm64` task resolves and links Kotlin/Native klibs, which only a real CI run (or a Mac) can exercise.

3. Third push (Room pinned to 2.7.2) got further but hit the same class of error again: Room 2.7.2's klib was "produced by 2.1.10 compiler" (klib ABI `1.201.0`), still newer than this project's Kotlin 2.0.21. This proved the problem wasn't which Room *patch* to pick — **no Room-KMP release is compatible with Kotlin 2.0.21's klib ABI at all**; Room's Kotlin/Native support appears to uniformly require Kotlin 2.1.10+. Confirmed with the user before proceeding (bigger change than Phase 3 originally scoped) and **bumped the whole toolchain**: `kotlin` 2.0.21 → **2.2.20**, `compose-multiplatform` 1.7.3 → **1.8.2**, `ksp` → **2.2.20-2.0.4**, `room` restored to **2.8.4** (its ABI `2.2.0` requirement is now satisfied by the 2.2.20 compiler). Chosen pairing verified against live docs: Compose Multiplatform 1.8.0+'s official minimum is Kotlin 2.1.0 (recommended 2.1.20), and Kotlin 2.2.20 is explicitly called out as preferred for iOS targets — both comfortably satisfied by 2.2.20.
4. The toolchain bump surfaced two more issues, both fixed and both locally verifiable (no CI needed): (a) `androidx.sqlite:sqlite-bundled` no longer publishes an `iosX64` (Intel simulator) variant — dropped the `iosX64()` target entirely (CI already runs on Apple Silicon `macos-latest`, and `iosX64` was the legacy Intel-simulator target the wider Kotlin/Native ecosystem has been retiring); (b) KSP `2.2.20-2.0.3` had a task-dependency validation bug against Compose Multiplatform 1.8.2's resource-generator tasks (Gradle's stricter implicit-dependency checker flagged it) — the next patch, `2.2.20-2.0.4`, fixed it.

**Verified:** `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` green, plus `:app:compileDebugAndroidTestKotlinAndroid` green (instrumented tests still can't run without an emulator, but now they at least compile).

### Phase 3b — DataStore-multiplatform swap ✅ Done — 2026-07-06

- Relocated `datastore-preferences` (was declared but entirely unused) from `androidMain` to `commonMain`.
- Swapped `ServerUrlHolder` and `SyncRepositoryImpl`'s plain `SharedPreferences` usage onto it — both stay in `androidMain` for now (no expect/actual yet, that's Phase 4/5 territory once wired for iOS).
- **Design note:** DataStore's API is Flow/suspend-only, but `ServerUrlHolder.readUrl()`/`SyncRepositoryImpl.getLastSyncTimeMs()` are called synchronously today (ViewModel field initializers, a computed property getter) — rather than changing `SyncRepository`'s interface or two ViewModels' shape just for a storage-technology swap, both classes now keep an in-memory `@Volatile` cache (seeded once via `runBlocking` at construction, kept in sync on every write) so the public API stays byte-for-byte the same. Writes stay fire-and-forget (`CoroutineScope(Dispatchers.IO).launch { ... }`) rather than blocking, matching the old `SharedPreferences.edit().apply()`'s non-blocking semantics.
- `SessionStore.kt` stays on `EncryptedSharedPreferences` as planned — DataStore has no encrypted-storage equivalent; it moves to Phase 4's `SecureStorage` abstraction instead.

**Verified:** `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlinAndroid` all green.

### Phase 3 iOS CI verification — three more real bugs found only by running the pipeline (all fixed, 2026-07-07)

3a/3b compiled and passed locally, but the first iOS CI push after them still failed — a reminder that "compiles on Android" says nothing about Kotlin/Native. Three separate, unrelated bugs surfaced in sequence, each only visible from an actual CI run:

1. **`androidx.sqlite` klib ABI mismatch.** `sqlite-bundled` was pinned to `2.7.0` (the latest stable release), but that artifact was compiled with a Kotlin compiler producing klib ABI `2.3.0` — newer than this project's Kotlin 2.2.20 (ceiling `2.2.0`) can consume. Checked Room 2.8.4's own published Gradle module metadata (`room-runtime-iossimulatorarm64-2.8.4.module`) rather than guessing: it declares `androidx.sqlite` **2.6.2** as its tested transitive dependency, so pinned to that instead of the newer, incompatible 2.7.0. Fixed in `gradle/libs.versions.toml`.
2. **Missing `@OptIn(ExperimentalForeignApi::class)`.** With the klib error gone, the build got past dependency resolution and into actual source compilation for the first time — immediately surfacing that `DatabaseBuilder.kt`'s use of `NSFileManager.URLForDirectory` (a cinterop Foundation API) needed the opt-in annotation. A one-line fix invisible until the build reached this file.
3. **`CADisableMinimumFrameDurationOnPhone` silently dropped from the compiled `Info.plist`, reviving the exact Phase 1 crash.** With both build errors fixed, CI reported "success" — but per Phase 1's own lesson, that doesn't mean the app didn't crash. Checking `logs/launch-console.log` (not just the screenshot) showed the identical `PlistSanityCheck` `IllegalStateException` from Phase 1, even though `iosApp/project.yml` still had `INFOPLIST_KEY_CADisableMinimumFrameDurationOnPhone: YES` set, unchanged since that original fix. Added a CI diagnostic step (`plutil -p` on the actual built `Info.plist`) rather than guessing, which proved the key was completely absent — while sibling `INFOPLIST_KEY_*` settings for Apple-standard keys (`CFBundleDisplayName`, the `UILaunchScreen`/`UIApplicationSceneManifest` `_Generation` flags) came through correctly. Root cause: Xcode's `GENERATE_INFOPLIST_FILE` → `INFOPLIST_KEY_*` synthesis only reliably picks up keys Xcode's build system recognizes as standard Info.plist keys; this key is a private one meaningful only to Compose Multiplatform's runtime check, not an Apple API, so it was silently discarded rather than erroring. **Fix:** switched to XcodeGen's `info.path`/`info.properties` block in `project.yml`, which writes a literal Info.plist file with this key baked in directly, bypassing Xcode's key-recognition filtering — `GENERATE_INFOPLIST_FILE` still merges its auto-synthesized keys into that file, so nothing else regressed (confirmed via the same plist dump).

**Verified end-to-end on iOS CI (run [28865580240](https://github.com/balor195/rms-customs-tracker/actions/runs/28865580240)):** built `Info.plist` contains `CADisableMinimumFrameDurationOnPhone => 1` alongside all previously-working keys; `launch-console.log`, `device-process.log`, and the crash-reports directory are all clean (zero exceptions, zero crash reports); launch screenshot shows the placeholder UI's text and layout correctly in place. This confirms the Room-KMP + DataStore persistence layer from 3a/3b actually compiles and runs on Kotlin/Native/iOS, not just Android — Phase 3's real goal.

**Minor unresolved observation, not a Phase 3 blocker:** this run's screenshot renders the placeholder text at unusually low contrast (very light gray/white on white) compared to earlier runs' clearly-legible dark-on-lavender rendering. `App.kt` has no animation or conditional coloring that would explain this, and the CI picked a different simulator model this run (iPhone 16 Pro vs. iPhone 17 Pro previously) — most likely a simulator-level display/appearance default (e.g. Dark Mode resolution, True Tone) rather than an app bug, but unconfirmed. Worth a look before Phase 5 (UI migration) if it recurs, since that phase will actually depend on this rendering correctly.

## Phase 4 — Platform abstractions via expect/actual (in progress)

Six genuinely independent subsystems, each with a different risk profile — split into sequenced, independently-verifiable sub-phases (4a-4f) rather than attempted in one pass, matching Phase 2's (2a-2d) and Phase 3's (3a-3b) pattern:

| Sub-phase | Subsystem | Status | Why this order |
|---|---|---|---|
| **4a** | Password hashing (`PasswordHasher.kt`: `javax.crypto`/`java.security`, JVM-only) | ⬜ | Smallest scope, no UI/DI coupling, hard blocker for login — do first |
| 4b | Secure storage (`SessionStore.kt`: Android Keystore/`EncryptedSharedPreferences` → iOS Keychain) | ⬜ | Small, but needs a new cinterop pattern (`platform.Security`); do right after 4a while crypto/cinterop context is fresh |
| 4c | Background sync scheduling (`SyncWorker.kt`: WorkManager → iOS `BGTaskScheduler`) | ⬜ | No UI dependency, but real design surface (Info.plist `BGTaskSchedulerPermittedIdentifiers`/`UIBackgroundModes`) — apply the Phase 3 `info.properties` lesson from the start instead of rediscovering it |
| 4d | Notifications (`CustomsNotificationManager.kt`: `NotificationManager` → `UNUserNotificationCenter`) | ⬜ | Note: `postSlaNotification` is currently dead code (unused, likely reserved for the not-yet-built `SlaCheckerWorker` from the original 7-phase workflow plan) — port the API shape for parity but don't over-invest in testing an unused path |
| 4e | Document capture — **not CameraX** (no CameraX usage exists despite the declared Gradle deps; the actual flow is the system camera app via `ActivityResultContracts.TakePicture()` + `FileProvider`, in `DocumentsTab.kt`) → iOS `UIImagePickerController` | ⬜ | Needs a UIViewController-presentation bridge from Kotlin — new pattern, moderate risk |
| 4f | PDF export (`PdfExporter.kt`: `android.graphics.pdf.PdfDocument`/`Canvas`, 299 lines, 6 report types) → iOS `UIGraphicsPDFRenderer`/Core Text | ⬜ | Largest, highest-uncertainty (Arabic RTL text shaping via Core Text vs. Android's Canvas) — do last, after the smaller pieces have built up iOS-specific plumbing experience |

Each sub-phase gets its own commit(s) and CI verification before starting the next.

## Phase 5 — UI migration to commonMain (not started)

- Move `presentation/ui` composables to `commonMain`.
- Fix the two Context-coupled screens (`ReportScreen.kt`, `DocumentsTab.kt`) using the Phase 4 abstractions.
- Swap `navigation-compose` and the Google Fonts integration for Compose Multiplatform equivalents (both are Android-first APIs).

## Phase 6 — iOS polish & release (not started)

- Safe-area/gesture handling, permissions/`Info.plist` entries, push notification setup.
- TestFlight pipeline via CI (extend `.github/workflows/ios-build.yml` or add a release workflow).
- App Store submission prep.

---

## Notes for whoever resumes this

- **Don't skip the "own plan per phase" step.** Phase 1 alone surfaced two scope corrections (UUID/Hilt in domain, Xcode CI quirks) that weren't visible from a first read of the codebase. Phases 2-4 touch far more surface area (DI graph, network layer, persistence) — plan and get alignment before writing code, the same way Phase 1 did.
- **CI is slow to iterate on**: a full run (framework build + Xcode build + simulator boot/launch) takes ~9-12 minutes on a cold cache, and GitHub Actions runners don't persist caches between runs by default unless you add `actions/cache` for the Gradle/Kotlin-Native caches — worth doing before Phase 2 if iteration speed becomes painful.
- **Test on the actual CI, not assumptions** — three of Phase 1's four real bugs (missing `gradlew`, wrong simulator name, Info.plist crash) were invisible from reading the code and only surfaced by watching an actual run fail. `simctl launch` succeeding does not mean the app didn't crash — always check console/crash logs, not just exit codes.
