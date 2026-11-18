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
- [x] M1 summit = Tiled rectangle `Summit` (Player layer) added above Rock5, sticking out
      above the map top; `GameMap.getSummitArea`, `MatchEnd` checks head inside; fallback to
      everything above the highest rock if the object is missing (route: inline)
- [x] M2 time limit = map property `timeLimit` (300 s, default 300) read by `GameMap`;
      `RESULT_LOSE`; STATE header +2 floats (remaining, limit) -> 14 bytes, protocol 3 -> 4;
      client `VictoryScreen` renamed `MatchEndScreen.show(won)` ("Victory!" / "Time's up!")
      (route: inline)
- [x] M3 client `TimeWarningOverlay`: red vignette generated with a Pixmap, linear from 0 at
      30% time left to 1 at 0; center alpha 0.12, edges 0.6; hidden once finished (route: inline)
- [x] M4 R6: done in `play-again.md` (play again, host migration); user tested OK

## Checks
- `./gradlew syncMap` and `lwjgl3:processResources` OK in servidor.
