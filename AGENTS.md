# AGENTS.md

## What this repo is
- This is a Kotlin Multiplatform + Compose Multiplatform puzzle platform, not a single game. The shared app lives in `composeApp/`; thin hosts live in `androidApp/` and `iosApp/`.
- Naming is historical: Gradle root is `BlockGames`, package names use `com.ugurbuga.blockgames`, and the flagship Android flavor `StackShift` is displayed as **Puzzle Shift** (`README.md`, `androidApp/build.gradle.kts`).
- Optimize for low token usage: prefer targeted reads/searches, reuse existing patterns, and avoid rewriting large files or restating obvious repo structure in long form.

## Big picture architecture
- Treat `composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/App.kt` as the real application shell. It owns route state, startup bootstrap, onboarding gating, settings persistence, notification scheduling, session restore/save, and `GameViewModel` lifetime.
- Gameplay is layered:
  - models in `game/model/`
  - per-style rules in `game/logic/*GameLogic.kt`
  - reducer/store/view-model in `presentation/game/`
  - Compose screens in `ui/game/`
- `GameLogic.create()` in `game/logic/GameLogic.kt` is the style switchboard. It delegates to `StackShiftGameLogic`, `BlockWiseGameLogic`, `BlockSortGameLogic`, `MergeShiftGameLogic`, or `BoomBlocksGameLogic` based on `GameplayStyle`.
- `ui/game/game/GameScreen.kt` is the UI switchboard for style-specific screens. If you add or change game behavior, trace both the logic dispatch and the screen dispatch.

## Core runtime patterns
- Current game identity is controlled globally through `GlobalPlatformConfig.gameplayStyle` in `platform/PlatformConfig.kt`. Many startup, persistence, challenge, and decoding paths depend on it.
- Android flavor selection feeds that global style through `BuildConfig.GAMEPLAY_STYLE` in `androidApp/src/main/kotlin/com/ugurbuga/blockgames/AndroidApp.kt`.
- Sessions are persisted aggressively from `App.kt`: active state is buffered via `pendingSessionState`, saved after a short debounce, and also saved immediately when leaving `AppRoute.Game`.
- Saved sessions are style-aware via `GameSessionSlot` and `sessionSlotFor(...)` in `settings/GameSessionStorage.kt`; daily challenges use date-scoped slots.
- Interactive onboarding is not a generic tutorial overlay. Each style has a dedicated factory/state machine such as `StackShiftGameOnboardingStateFactory` or `BlockSortOnboardingStateFactory`, and `App.kt` decides whether tutorial/onboarding must gate play.
- Changes in shared code should continue to work across Android, iOS, desktop JVM, and web/wasm unless the change is explicitly platform-scoped; check `commonMain` first and keep `expect/actual` boundaries intact.

## Persistence and cross-platform boundaries
- Storage is implemented with `expect/actual` objects. Check `settings/AppSettingsStorage.kt`, `settings/GameSessionStorage.kt`, and `settings/HighScoreStorage.kt` in `commonMain`, then the platform implementations under `androidMain/`, `iosMain/`, `jvmMain/`, and `wasmJsMain/`.
- `settings/GameSessionStorage.kt` contains a custom versioned codec (`GameSessionCodec`, current `Version = 7`). If `GameState` changes, preserve backward compatibility instead of casually rewriting the format.
- Localization and telemetry are also abstracted across platforms (`localization/AppEnvironment.kt`, `telemetry/Telemetry.kt`). Prefer extending the shared interface + platform actuals rather than inlining platform checks in common UI.

## Android-specific conventions
- `androidApp/` is mostly packaging/config: flavors, signing, ads, Firebase, and the host activity. Game logic should usually not be added here.
- Product flavors are the app identities (`StackShift`, `BlockWise`, `BlockSort`, `MergeShift`, `BoomBlocks`) in `androidApp/build.gradle.kts`. Adding a new identity means touching flavor config, resources, and often startup selection.
- Firebase config is generated from `google.properties` by `generateLocalFirebaseConfig`; `process*GoogleServices` tasks depend on it. Prefer updating `google.properties(.example)` over hand-editing generated `src/<Flavor>/google-services.json`.
- Ads are configured from `ads.properties`; signing can be flavor-scoped through `keystore.properties`.

## When adding a new app / gameplay identity
- Wire it through both switchboards: add the gameplay style to `game/logic/GameLogic.kt` and the matching screen path to `ui/game/game/GameScreen.kt`.
- Add all player-facing flows expected by this repo, not just the core mechanic: tutorial screen support, interactive onboarding state/factory, and daily challenge support/screens.
- Update startup and persistence paths in `App.kt` so the new style can be selected, bootstrapped, saved/restored, and gated through tutorial/onboarding like existing styles.
- Add flavor/app packaging work where relevant: `androidApp/build.gradle.kts`, flavor resources, app naming/branding, and any platform startup mapping.
- Review telemetry, localization, and session compatibility for the new style; this repo assumes new game identities participate in those shared systems.

## Developer workflows
- Shared tests: `./gradlew :composeApp:allTests`
- JVM-only tests: `./gradlew :composeApp:jvmTest`
- Desktop run: `./gradlew :composeApp:jvmRun`
- Web dev server: `./gradlew :composeApp:runWeb`
- Android debug build: `./gradlew :androidApp:assembleDebug`
- Install a specific Android flavor: `./gradlew :androidApp:installStackshiftDebug` (similarly `installBlockwiseDebug`, `installBlocksortDebug`, etc.)
- Cross-platform packaging helper: `./gradlew buildAllArtifacts`
- Desktop packaging (`packageDesktopApp`, `packageDmg`, `packageMsi`) needs a full JDK with `jpackage`; `composeApp/build.gradle.kts` auto-searches common macOS JDK locations.

## Repo-specific change guidance
- When changing startup/navigation/session behavior, inspect `App.kt` first; many “simple” fixes actually require matching updates to route handling, onboarding gating, and persistence.
- When changing `GameState` or session semantics, update tests in `composeApp/src/commonTest/`—especially `settings/GameSessionCodecTest.kt`, `BlockSortSessionCompatibilityTest.kt`, and reducer/game-logic tests.
- When changing strings or language behavior, check both `settings/AppSettingsBootstrap.kt` and localization tests like `src/jvmTest/.../LocalizedStringsParityTest.kt`.
- Preserve the existing action/event vocabulary (`TelemetryActionNames`, `GameEvent`) when possible; the UI, haptics/sound mapping, and analytics are wired around those enums/constants.
- Prefer extending existing shared abstractions over branching by platform in UI code; if behavior differs per platform, add/adjust `expect/actual` implementations instead of scattering checks through `commonMain`.
- For non-trivial changes, validate the narrowest useful matrix: shared tests first, then the most relevant platform run/build (`jvmRun`, `assembleDebug`, `runWeb`, packaging tasks) for the area you changed.

