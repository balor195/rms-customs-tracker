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
| 2 | DI & networking (Hilt→Koin, Retrofit→Ktor) | 🔶 In progress — 2a done, 2026-07-06 |
| 3 | Persistence (Room→Room-KMP, DataStore) | ⬜ Not started |
| 4 | Platform abstractions (expect/actual) | ⬜ Not started |
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

## Phase 2 — DI & networking (in progress)

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

### Phase 2b — Ktor migration (not started)

- Replace **Retrofit + OkHttp** with **Ktor client** (OkHttp engine on Android, Darwin engine on iOS) against the existing FastAPI REST contract in `backend/` (plain JSON/REST, no Android-specific transport assumptions — confirmed in Phase 1 research).
- Reimplement `ServerUrlInterceptor`'s runtime-editable-URL behavior as a Ktor plugin.

### Phase 2c — UUID→String migration (not started)

- Do the UUID→String migration across `domain/model/*.kt`, `domain/repository/*.kt`, and the ~34 dependent files (Room entities/converters, DTOs, ViewModels, UI).

### Phase 2d — Move domain/ to commonMain (not started)

- Once 2b/2c are done, move the rest of `domain/` (models, repositories, usecases minus `PasswordHasher`, statemachine) to `commonMain`. This is the only sub-step needing iOS CI to verify.

## Phase 3 — Persistence (not started)

- Migrate **Room** to Room 2.7+ KMP (bundled SQLite driver) so `data/local` (db, dao, entity) can move to `commonMain`.
- Migrate `datastore-preferences` to its multiplatform artifact.

## Phase 4 — Platform abstractions via expect/actual (not started)

Each of these needs an Android impl (already exists, just needs wrapping) + a new iOS impl:
- **`SecureStorage`** — Android Keystore/`EncryptedSharedPreferences` (`SessionStore.kt`) vs. iOS Keychain.
- **Background sync scheduling** — WorkManager (`SyncWorker.kt`) vs. iOS `BGTaskScheduler`.
- **Notifications** — `NotificationManager` (`CustomsNotificationManager.kt`) vs. `UNUserNotificationCenter`.
- **Document capture** — CameraX vs. `UIImagePickerController`/AVFoundation.
- **PDF export** — `android.graphics.pdf.PdfDocument` (`PdfExporter.kt`) vs. an iOS-compatible renderer.
- Also needs a **password hashing** abstraction: `PasswordHasher.kt` currently uses `javax.crypto`/`java.security` (JVM-only) — not covered by the Phase 1 scope note, but blocks login working on iOS.

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
