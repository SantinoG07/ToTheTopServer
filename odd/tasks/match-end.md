# Match end: summit, time limit, defeat

Repos: `servidor` and `cliente`, on `main`.

## Findings (2026-11-18)
- Victory exists: any head above the highest rock vertex (`GameMap.getSummitY`, `MatchEnd`).
- No time limit, no `RESULT_LOSE`, no defeat screen.
- After victory the client only offers "Back to menu" (disconnects); R6 not verified.
- The map was duplicated and out of sync: client had `Rock6` and another spawn.

## Tasks
- [x] M0 single map source: `servidor/build.gradle` task `syncMap` copies
      `../cliente/assets/maps/*.tmx|*.tsx` before `processResources` (skipped when the client
      repo is absent; the committed copy is the fallback). Verified: a stale copy is restored
      by a build. (route: inline)
- [ ] M1 summit as a Tiled rectangle `Summit` in the `Gameplay` group (server)
- [ ] M2 time limit (map property) + `RESULT_LOSE` + remaining time in state (protocol 4)
      + defeat screen (both repos)
- [ ] M3 red edge tint in the last part of the time (client)
- [ ] M4 R6: two consecutive matches (win and loss) without restarting programs

## Checks
- `./gradlew syncMap` and `lwjgl3:processResources` OK in servidor.
