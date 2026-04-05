# StackShift

StackShift is a reverse-Tetris inspired puzzle game built with **Kotlin** and **Compose Multiplatform** for **Android, iOS, and Desktop**.

## Overview

Instead of falling from the top, pieces spawn near the bottom of the board. You drag them into place, the game snaps them to the nearest valid grid position, and completed rows are cleared for points, combos, and increasing pressure.

## Features

- **Drag-first gameplay** with touch and mouse support.
- **Placement preview** that shows the landing footprint before release.
- **Line clearing** with score, combo, and clear animation feedback.
- **Special block types** with dedicated icons and behavior hints.
- **Theme and visual customization** for colors, block styles, and board styling.
- **Multiplatform UI** shared across Android, iOS, and Desktop.

## Controls

- **Drag** the active piece onto the board.
- **Release** to place it, or return to the spawn area if the position is invalid.
- **Pause** / **Resume** and **Restart** are available from the top HUD.
- **Settings** lets you adjust theme and gameplay visuals.

## Project structure

Shared code lives in [`composeApp/src/commonMain/kotlin`](./composeApp/src/commonMain/kotlin).

- `game/model`
  - Immutable game data such as `GameState`, `Board`, `Piece`, and `PlacementPreview`.
- `game/logic`
  - Pure gameplay rules: spawning, collision, snapping, clearing, scoring, and difficulty.
- `ui/game`
  - Compose UI for the board, HUD, tray, settings, and in-game overlays.
- `settings`
  - App settings model and persistence helpers.
- `localization`
  - Shared localization access for UI text.
- `ui/theme`
  - Theme palette helpers and Compose Material theme wiring.

Platform-specific entry points live in:

- [`composeApp`](./composeApp/src)
  - Shared Compose Multiplatform app code and platform hosts.
- [`iosApp`](./iosApp/iosApp)
  - iOS entry point used by Xcode.

## Requirements

- A recent **JDK** compatible with the Gradle setup.
- **Android Studio** for Android development.
- **Xcode** for iOS builds and simulator testing.
- **Desktop JVM** support if you want to run the desktop target.

## Setup

Clone the repository and open it in your IDE, then let Gradle sync the project.

```sh
./gradlew build
```

## Run commands

### Android

```sh
./gradlew :composeApp:assembleDebug
```

### Desktop (JVM)

```sh
./gradlew :composeApp:run
```

### Desktop distributable app image

```sh
./gradlew :composeApp:packageDesktopApp
```

This creates a portable desktop app image under `composeApp/build/compose/binaries/main/app/` for the current platform. You can copy that output folder anywhere and run the app from there.

### iOS simulator compile check

```sh
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

To run the full iOS app, open [`iosApp`](./iosApp) in Xcode and launch it from there.

## Validation

These are the commands used to verify the shared code and platform targets:

```sh
./gradlew build
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:compileKotlinJvm :composeApp:jvmTest
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

## Notes

- The sound and haptic layer is intentionally abstract so platform-specific implementations can be added later without changing shared game logic.
- The board uses a compact array-backed representation to keep collision and row-clear checks efficient.
- Gradle may still show Compose Multiplatform / KMP deprecation warnings depending on the toolchain, but the project builds successfully.
