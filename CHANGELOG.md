# Changelog

All notable changes to this project are documented in this file.
The format is based on Keep a Changelog, and this project adheres
to Semantic Versioning.

---

## [0.6.0] - 2026-09-04

### Added

* Added a new **How to Play** menu with the game's story and gameplay controls.
* Added instructions for left and right hand movement.
* Added instructions for the current keyboard controls.
* Added an **Exit** button to the main menu.

---

## [0.5.0] - 2026-08-31

### Added

* Added visual sprites for the player's body parts.
* Added sprites for the hands, forearms, and arms.
* Added a loading screen and a functional main menu.
* Added a pause menu accessible with `ESC`.
* Added Continue, Main Menu, and Settings options to the pause menu.
* Added a Settings menu accessible from the main menu and pause menu.
* Added music and sound-effect volume controls.
* Added a shared audio manager for global audio settings.
* Added reusable slider and label styles to the UI skin.
* Added support for loading textures, sounds, and music through the asset management system.
* Added test assets for the `AssetManager`.

### Changed

* Reorganized player sprites and asset files.
* Centralized UI style creation in `AssetService`.
* Renamed the `screen` package to `menus`.
* Updated the application window title to `ToTheTop`.

### Fixed

* Fixed sprite-related issues in `GameScreen`.
* Fixed music loading and asset integration.
* Fixed references left over after renaming the `screen` package to `menus`.
* Fixed launcher references after the project package rename.

### Removed

* Removed obsolete classes from the previous project structure.

---

## [0.4.0] - 2026-08-27

### Added

* Added an initial test map created with **Tiled**.
* Added support for loading the map and its gameplay objects.
* Added physical rock and surface objects to the test map.
* Added a new hands-based player movement system.
* Added independent left and right arm control.
* Added force-based hand movement following mouse input.
* Added counter-force from the controlled arm to the character's torso.
* Added physics-based arm and hand control using Box2D.
* Added a new `AssetService` based on LibGDX's `AssetManager`.
* Added automatic asset loading and management.
* Added support for loading game textures, sounds, and music through the asset system.

### Changed

* Reworked the player movement system around physical hand control.
* Reorganized the project classes into functional packages.
* Reworked the project architecture to separate entities, input, map, physics, assets, and menus.
* Adjusted the character's shoulder configuration.
* Removed rotation limits from the arm joints to allow more natural arm movement.
* Reworked the project after recovering the complete project state.
* Updated the launcher to use the new project package.

### Fixed

* Fixed incorrect attributes and obsolete fields in the `Character` class.
* Removed obsolete mouse-position handling from `GameScreen`.
* Fixed hand dragging behavior between the character and input systems.
* Fixed launcher package references.

### Removed

* Removed the previous movement system and all classes and methods associated with it.
* Removed the previous grip-point implementation.
* Removed obsolete grab-point naming and references.
* Removed obsolete classes from the previous project structure.

---

## [0.3.0] - 2026-07-18

### Added

* Added the initial project `CHANGELOG.md`.
* Updated the project `README.md` with project information.
* Added the project proposal documentation to the Wiki.

### Changed

* Renamed the project from `CucoProject` to `ToTheTop`.
* Renamed the project's Java packages to match the new project name.
* Updated project references after the rename.

---

## [0.2.0] - 2026-06-12

### Added

* Added the first playable physics prototype using **Box2D**.
* Added a physical player body with a torso and body parts.
* Added the complete character body including:

    * Head
    * Torso
    * Upper arms
    * Forearms
    * Hands
    * Legs
* Added physics joints connecting the character's body parts.
* Added angular limits to the character's joints.
* Added collision configuration between character body parts.
* Added a character weight parameter.
* Added automatic density calculation for body parts based on the character's weight.
* Added force-based player movement following the mouse cursor.
* Added two physical arms attached to the torso.
* Added a prototype grip-point system.
* Added the initial gameplay controls.
* Added fullscreen startup.
* Added cursor hiding during gameplay.

### Changed

* Replaced the original simple player prototype with a fully physical character.
* Reworked the character composition to use independent Box2D bodies for its body parts.
* Replaced hardcoded arm-part ratios with dedicated constants.
* Renamed the grip-point terminology from `grab points` to `grip points`.

---

## [0.1.0] - 2026-05-31

### Added

* Created the initial LibGDX project using **Liftoff**.
* Added the initial `core` and `lwjgl3` modules.
* Added the initial desktop launcher.
* Added the first project configuration and basic game structure.
* Added the first main menu prototype.
* Added the initial game window configuration and resolution.

---

## Development History

The project evolved from a basic LibGDX prototype into a physics-based climbing game prototype focused on physical character control.

The current prototype is centered around:

* A fully physical ragdoll-like player character.
* Box2D-based body and joint simulation.
* Independent physical arm control.
* Mouse-based hand movement.
* Physical interaction between the player's body and the environment.
* Tiled-based map construction.
* A modular project architecture.
* A screen/menu management system.
* Centralized asset and audio management.

Multiplayer networking, the final climbing system, the complete mountain, the eruption system, and other planned gameplay systems are still under development.
