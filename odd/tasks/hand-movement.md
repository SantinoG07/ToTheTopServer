# Hand movement fix

Repos: `servidor` and `cliente`, branch `fix/hand-movement` in both.

## Problem
Hands oscillated violently while dragging: the arm velocity servo corrected ~3x
the error per step (gain 50 N/(m/s), hand 0.27 kg, 1/60 s), arms got the arm
density instead of the character weight, the client pointer depended on window
size/zoom (`viewport.unproject`), and per-tick pointer velocity spiked with UDP jitter.

## Tasks
- [x] T1 client: pointer = raw mouse counts x fixed meters per count (route: inline)
- [x] T2 server: `Arm.drive` position target (shoulder + clamped offset), mass-normalized
      velocity loop, gravity compensation (route: inline)
- [x] T3 server: pass `weight` to `Arm`; `Character.update(pointerDelta, step, ...)` (route: inline)
- [x] T4 server: validated `pointerDelta` (finite, max 2 m/tick) (route: inline)
- [ ] T5 manual test: server + two clients, drag both hands, different window sizes
- [x] T6 commits: servidor 6b7b59f, cliente d5ac670 (not pushed)

## Checks
- `./gradlew core:compileJava` OK in both repos (2026-11-17).
- No automated test suite exists in either repo: no runnable RED; manual test pending.
- Protocol unchanged (`InputPacket` still carries the pointer in meters).

## Next
Climbing / grip mechanics (`Character.applyClimbPull`, `Arm.grab`).
