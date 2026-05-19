# Puzzle Shift

<p align="center">
  <img src="./branding/generated/stackshift-board.svg" alt="Puzzle Shift logo" width="160" />
</p>

`Puzzle Shift` is a multi-game puzzle platform built with **Kotlin** and **Compose Multiplatform**. The repository contains **five distinct games** integrated into a single unified experience:

- `StackShift`
- `BlockWise`
- `BlockSort`
- `MergeShift`
- `BoomBlocks`

Players can access all games through the **Puzzle Shift** launcher, which serves as a seamless multi-game shell. Individual standalone versions are also supported for each gameplay style.

## Quick links

- [Puzzle Shift](#puzzle-shift)
- [Apps in this repository](#apps-in-this-repository)
- [Shared player experience](#shared-player-experience)
- [Technology stack](#technology-stack)
- [Repository layout](#repository-layout)

## Puzzle Shift

This repository is a **multi-app puzzle platform**. At the product level, it delivers five unique gameplay identities. At the code level, those identities are backed by a common shared module, a shared design system, shared persistence, and flavor-specific application entry points.

### Game overview

| Game | Core idea | Android application id |
| --- | --- | --- |
| `StackShift` | Reverse-gravity block launcher with specials and lane pressure | `com.ugurbuga.stackshift` |
| `BlockWise` | Free-placement spatial puzzle with tray management and row/column clears | `com.ugurbuga.blockwise` |
| `BlockSort` | Color-stack sorting puzzle with level-based logic and round progression | `com.ugurbuga.blocksort` |
| `MergeShift` | Number-merging launcher focused on chaining equal-value blocks | `com.ugurbuga.mergeshift` |
| `BoomBlocks` | Group-clearing color puzzle with directional gravity shifts | `com.ugurbuga.boomblocks` |

## Apps in this repository

### `StackShift`

`StackShift` is the action-oriented title in the collection. It takes the familiar block-puzzle language of lanes and line clears, then flips the flow: pieces begin in the dock and are launched upward into the board instead of falling from the top.

- **Reverse-gravity placement**: Launch pieces upward into selected lanes.
- **Real-time placement preview**: See where the piece will land before committing.
- **Queue and hold management**: Strategically swap and plan for upcoming pieces.
- **Special block types**: Clear rows/columns, use ghosts for gaps, or heavy blocks for impact.

### `BlockWise`

`BlockWise` shifts the focus from launch timing to spatial strategy. The player works with a **tray of three shapes** and can place them freely anywhere they fit on the grid.

- **Free placement gameplay**: Drag pieces directly from the tray to any valid origin.
- **Three-piece tray strategy**: Only get a new set once all three current shapes are used.
- **Simultaneous clears**: Clear rows and columns at the same time for massive scores.
- **No-pressure flow**: Focus on spatial planning rather than speed.

### `BlockSort`

`BlockSort` introduces a logic-heavy sorting challenge to the platform. Inspired by liquid-sorting puzzles but adapted for a block-based grid, it requires players to organize mixed stacks into single-color columns.

- **Contiguous stack movement**: Tap a column to pick up its top color run, then move it to a matching color or an empty slot.
- **Level-based progression**: Challenges grow in complexity with more colors and taller columns.
- **Dynamic board layout**: As columns increase, the board wraps into multiple rows to maintain visibility.
- **Bonus slots**: Use special "Add Column" power-ups to rescue a locked board state.
- **Time Attack mode**: Solve mixed layouts against the clock to earn bonus time.

### `MergeShift`

`MergeShift` combines launch-based placement with numeric progression. It asks the player to create larger values by dropping a block onto another with the same value.

- **Launch-to-merge core loop**: Direct numeric blocks into lanes to trigger fusions.
- **Chain reactions**: Single placements can trigger a cascade of merges across the board.
- **Column overflow tension**: Growth is vertical, so space management is critical for survival.
- **Board scaling**: The playable grid expands as higher-value tiles are unlocked.

### `BoomBlocks`

`BoomBlocks` is about direct board interaction. Instead of placing shapes, players clear connected color groups with a single tap.

- **Tap-to-explode**: Find and clear groups of three or more touching blocks.
- **Directional gravity shifts**: After an explosion, the board collapses toward the void from different directions.
- **Automatic refill system**: The board stays dense, requiring constant reading of the changing color clusters.
- **Fast-paced sessions**: Simple rules with deep emerging tactical complexity.

## Shared player experience

Although the five games are mechanically different, they share a consistent core:

- **Daily challenge infrastructure** with style-aware tasks and monthly calendar progress.
- **Interactive onboarding** tailored specifically to each of the five gameplay styles.
- **Session persistence** ensuring active runs and challenge progress are always saved.
- **Localization support** across English, Turkish, Spanish, French, German, Russian, and more.
- **Deep customization**: 10+ block styles (Flat, Bubble, Neon, Crystal, etc.) and multiple color palettes.

## Technology stack

- **Kotlin Multiplatform** for shared logic and UI.
- **Compose Multiplatform** for the entire interface layer.
- **Android app flavors** for per-game packaging and the unified launcher.
- **Firebase / Google integrations** for crashlytics and ads (where applicable).

## Repository layout

- `composeApp/`: Shared multiplatform game logic, UI, and common assets.
- `androidApp/`: Android host application with flavor-sets for each game identity.
- `iosApp/`: iOS host application.
- `branding/`: Source assets for icons, feature graphics, and board art.
- `artifacts/`: Packaging outputs and release binaries.

## Notes

The repository has evolved from a single-game demo into a **5-game puzzle ecosystem**. The flagship entry point is now branded as **Puzzle Shift**, serving as the primary multi-game experience for the platform.
