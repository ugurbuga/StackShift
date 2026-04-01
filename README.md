# StackShift

StackShift is a reverse-Tetris inspired puzzle game built with **Kotlin + Compose Multiplatform** for **Android, iOS, and Desktop**.

## Core gameplay

- Pieces **spawn at the bottom** of the screen instead of falling from the top.
- The player **drags** the active piece onto the board.
- When released, the piece **snaps to the nearest valid grid position**.
- Filled horizontal rows are **cleared**, awarding score and combos.
- Difficulty ramps up over time by unlocking **more complex piece shapes**.

## Controls

- **Drag** the active piece with touch or mouse.
- Valid target cells are highlighted while dragging.
- Releasing on an invalid area sends the piece back to its spawn zone.
- Use **Pause** / **Resume** and **Restart** from the top HUD.

## Architecture

Shared game code lives in [`composeApp/src/commonMain/kotlin`](./composeApp/src/commonMain/kotlin).

### Main layers

- `game/model`
  - Immutable state objects such as `GameState`, `BoardMatrix`, `Piece`, `PlacementPreview`
- `game/logic`
  - Pure gameplay rules: spawn, collision, nearest valid snap, line clearing, combo, scoring, difficulty
- `presentation/game`
  - `GameViewModel` with `StateFlow` and coroutine-based game loop
- `ui/game`
  - Compose UI: board rendering, draggable piece overlay, HUD, tray, pause overlay
- `platform/feedback`
  - Sound and haptic abstractions with no-op defaults

## Project structure

- [`composeApp`](./composeApp/src) contains the shared Compose Multiplatform app and platform-specific hosts.
- [`iosApp`](./iosApp/iosApp) contains the iOS entry point used by Xcode.

## Quick run commands

### Android

```sh
./gradlew :composeApp:assembleDebug
```

### Desktop (JVM)

```sh
./gradlew :composeApp:run
```

### iOS simulator shared code compile check

```sh
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

To run the full iOS app, open [`iosApp`](./iosApp) in Xcode and launch it from there.

## Validation commands used

```sh
./gradlew :composeApp:compileKotlinJvm :composeApp:jvmTest
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

## Notes

- The current sound/haptic layer is intentionally abstract so platform-specific implementations can be added without changing shared game logic.
- The board uses a compact array-backed representation to keep collision and row-clear checks efficient.
- The current setup still uses the standard Compose Multiplatform app template, so Gradle may show a **KMP + Android application deprecation warning**. The game builds successfully, but a future migration to the newer Android/KMP project structure is recommended.
