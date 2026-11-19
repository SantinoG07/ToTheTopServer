# Breakable rocks

Repos: `servidor` and `cliente`, on `main`. Protocol 5 -> 6.

## Design
- Tiled: rocks with type `Breakable` (or class) in the Surface layer.
- Server `BreakableRock`: wear grows while any character body touches it or a hand
  grips it, recovers at half rate otherwise; at 5 s it breaks: every grip on it is
  released, then the body is deactivated. After 5 s it respawns only if no character
  body overlaps its bounds.
- STATE: after the players, `breakableCount` + one byte each (wear 0..200, 255 broken).
- Client `BreakableRockView`: ochre with a short crack; turns red, cracks grow and it
  shakes past 60% wear; only a faint outline while broken.
- Map: R47, R10 (medium), R25, R29, R35, R23 (hard). Chosen among non-essential rocks;
  verified that the summit stays reachable with all six broken at once (validator with
  hanging moves plus standing on a rock top: up to 2.8 m up, 1.2 m sideways).

## Tasks
- [x] B1 map: mark 6 rocks `Breakable` (route: inline)
- [x] B2 server: `BreakableRock`, Rocks/contact listener/Arm/Character/GameScreen, STATE (route: inline)
- [x] B3 client: STATE decode, `BreakableRockView`, TerrainRenderer (route: inline)
- [x] B4 manual test by user OK
- [x] B5 commit authorized by user

## Checks
- `core:compileJava` OK in both repos (2026-11-19). No automated tests exist.
