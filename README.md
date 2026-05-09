# BlockGames

<p align="center">
  <img src="./branding/generated/stackshift-board.svg" alt="StackShift logo" width="160" />
  <img src="./branding/generated/blockwise-board.svg" alt="BlockWise logo" width="160" />
</p>

`BlockGames` is a shared puzzle-game workspace built with **Kotlin** and **Compose Multiplatform**. The repository currently contains **four distinct game apps** built on the same core architecture:

- `StackShift`
- `BlockWise`
- `MergeShift`
- `BoomBlocks`

In addition to these standalone experiences, the Android project also includes a `BlockGames` flavor that acts as a multi-game launcher and lets players switch between the four styles from one app shell.

## Quick links

- [BlockGames](#blockgames)
- [Apps in this repository](#apps-in-this-repository)
- [Shared player experience](#shared-player-experience)
- [Technology stack](#technology-stack)
- [Repository layout](#repository-layout)

## BlockGames

This repository is not a single-game codebase anymore; it is a **multi-app puzzle platform**.

At the product level, the project is organized around four gameplay identities. At the code level, those identities are backed by a common shared module, a shared design system, shared persistence/session logic, shared localization, and flavor-specific application entry points.

### App overview

| App | Core idea | Android application id |
| --- | --- | --- |
| `StackShift` | Reverse-gravity block launcher with specials and lane pressure | `com.ugurbuga.stackshift` |
| `BlockWise` | Free-placement spatial puzzle with tray management and row/column clears | `com.ugurbuga.blockwise` |
| `MergeShift` | Number-merging launcher focused on chaining equal-value blocks | `com.ugurbuga.mergeshift` |
| `BoomBlocks` | Group-clearing color puzzle with directional gravity shifts | `com.ugurbuga.boomblocks` |

### Multi-game flavor

The Android app module also defines a `BlockGames` flavor (`com.ugurbuga.blockgames`). This flavor reuses the same shared game code, but exposes the collection as a single launcher-style app where the active gameplay style can be selected by the player.

## Apps in this repository

### `StackShift`

<p align="center">
  <img src="./branding/generated/stackshift-feature-graphic-1024x500.svg" alt="StackShift feature graphic" width="600" />
</p>

`StackShift` is the most action-oriented title in the collection. It takes the familiar block-puzzle language of lanes, queue management, and line clears, then flips the flow: pieces begin in the dock and are launched upward into the board instead of falling from the top.

What defines `StackShift`:

- **Reverse-gravity placement**: the current piece starts below the board, and the player chooses a landing lane by dragging and releasing.
- **Real-time placement preview**: the UI shows where the current piece will settle before the move is committed.
- **Queue and hold management**: players manage the active piece, the next queue, and hold swaps to survive longer runs.
- **Horizontal line-clearing loop**: success comes from maintaining clean columns and setting up efficient row clears.
- **Special block types**: row clears, column clears, ghost-like movement, heavy behavior, and other special interactions help recover from tight boards.
- **Pressure-driven pacing**: the board becomes riskier as space disappears, so the game rewards fast tactical decisions rather than purely slow planning.

`StackShift` is best thought of as the project’s signature arcade puzzle: readable, fast, combo-friendly, and built around lane control.

### `BlockWise`

<p align="center">
  <img src="./branding/generated/blockwise-feature-graphic-1024x500.svg" alt="BlockWise feature graphic" width="600" />
</p>

`BlockWise` shifts the focus from launch timing to board reading. Instead of handling a single falling or launching piece, the player works with a **tray of three shapes** and can place them freely anywhere they fit on the board.

What defines `BlockWise`:

- **Free placement gameplay**: pieces are dragged directly from the tray to any valid origin on the grid.
- **Three-piece tray strategy**: after placing one piece, the remaining tray stays visible; only when all three pieces are used does a new set appear.
- **Row and column clearing**: unlike classic single-axis line systems, `BlockWise` clears both full rows and full columns.
- **Simultaneous clears**: if a move completes both a row and a column, both resolve together for larger scoring swings.
- **No special-piece dependency**: the core challenge is spatial planning, not waiting for power-ups.
- **Low-pressure, high-depth puzzle flow**: the pace is calmer than `StackShift`, but the difficulty comes from preserving space for future shapes.

`BlockWise` is the collection’s most meditative strategy game: tactile, readable, and highly dependent on foresight across the whole tray.

### `MergeShift`

`MergeShift` combines launch-based placement with number-merging progression. It starts on a compact grid and asks the player to create larger values by dropping a block onto another block with the same value.

What defines `MergeShift`:

- **Launch-to-merge core loop**: pieces are launched from the dock into a selected column.
- **Equal-value fusion**: when a block lands adjacent to or onto matching values in a mergeable configuration, they combine into a higher-value block.
- **Chain reactions**: a single placement can trigger follow-up merges, creating combo-heavy turns and large score jumps.
- **Column overflow tension**: each move grows a column upward, so space management matters as much as merge planning.
- **Board growth with progression**: as higher-value tiles appear, the playable board expands from its initial compact size to support longer runs.
- **2048-like satisfaction inside a launch puzzle shell**: the game rewards both numeric planning and positional accuracy.

`MergeShift` sits between arcade and logic design: it has the immediacy of a launcher game, but its mastery comes from engineering merge chains and protecting future board space.

### `BoomBlocks`

`BoomBlocks` moves away from piece placement entirely. Instead of dragging or launching shapes, the player interacts directly with the board by clearing connected color groups.

What defines `BoomBlocks`:

- **Tap-to-explode interaction**: players look for groups of **three or more touching blocks** of the same color and clear them with a tap.
- **Group-size scoring**: larger connected explosions deliver better score returns and more dramatic board changes.
- **Directional gravity shifts**: after an explosion, surrounding blocks are pulled based on where the cleared group was located, so the board can collapse from different directions.
- **Automatic refill system**: newly opened spaces are repopulated, keeping the board dense and the decision space constantly changing.
- **Board-reading over shape-fitting**: success depends on spotting future clusters and understanding how each explosion will reshape the playfield.
- **Fast session structure**: the rules are immediately readable, but the evolving gravity behavior creates strong depth.

`BoomBlocks` is the most direct and instantly readable app in the set, yet it still fits the broader `BlockGames` family through combo planning, score chasing, and daily puzzle hooks.

## Shared player experience

Although the four games are mechanically different, they share a consistent product layer across the repository:

- **Daily challenge infrastructure** with style-aware challenge generation and progress tracking
- **Interactive onboarding flows** tailored to each gameplay style
- **Session persistence** so active runs and challenge progress can be restored safely
- **Localization support** across multiple languages
- **Theme and visual customization** including palette and block-style options
- **Shared HUD, settings, and progression patterns** so the collection feels cohesive even when the mechanics change

This shared layer is what makes the repository valuable: each game has its own rules, but the surrounding app experience is intentionally unified.

## Technology stack

- **Kotlin Multiplatform** for shared logic and shared UI layers
- **Compose Multiplatform** for the majority of interface code
- **Android app flavors** for per-game packaging and branding
- **Shared state-driven game logic** in `composeApp`
- **Firebase / Google integrations** in mobile app shells where configured

The shared codebase targets Android, iOS, Web/Wasm, macOS, Windows, and Desktop JVM, while flavor-specific packaging and branding live in the platform app layers.

## Repository layout

Core locations in the workspace:

- `composeApp/`
  - Shared multiplatform game logic and UI
  - Common gameplay models, reducers, onboarding flows, challenge systems, settings, and theming
- `androidApp/`
  - Android application shell
  - Flavor-specific source sets for `stackshift`, `blockwise`, `mergeshift`, `boomblocks`, and `blockgames`
- `iosApp/`
  - iOS-specific host application and Xcode project
- `branding/`
  - Generated graphics, background assets, board art, and icon-generation scripts
- `artifacts/`
  - Collected build outputs and packaged binaries produced outside the README workflow

Within shared code, the most important directories are:

- `composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/game/model`
- `composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/game/logic`
- `composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/presentation`
- `composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/settings`
- `composeApp/src/commonMain/kotlin/com/ugurbuga/blockgames/ui`

## Notes

- The repository has evolved from a smaller game set into a broader shared-game platform, so the README now describes the project as a **four-app collection** rather than a two-mode demo.
- The sound, haptic, telemetry, and packaging layers are intentionally abstracted so each platform shell or app flavor can stay thin while reusing the same gameplay core.
