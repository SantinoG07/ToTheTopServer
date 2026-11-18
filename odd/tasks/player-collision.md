# Player collision and climbing other players

Repo: `servidor` only, on `main`. Protocol and client unchanged (grip flags already sent).

## Problem
Every character used `groupIndex = -1`, so characters never collided with each other.
Hands could only grab `Rocks`.

## Tasks
- [x] T1 one negative collision group per slot (`GameScreen.collisionGroup`, -10 - slot) (route: inline)
- [x] T2 `BodyPart(owner, arm, leg)` user data on all ten bodies (route: inline)
- [x] T3 `GameContactListener`: surface = rock or body of another character, checked in both
      directions; legs on another player count as support (route: inline)
- [x] T4 before disposing a leaving player, other players release grips on it
      (`Character.releaseGripsOn`) to avoid dangling native joints (route: inline)
- [x] T4b lift and A/D swing push the gripped body back with the opposite force
      (`Character.applyClimbingForce`, `Arm.pushGrip`): holding a player and pulling
      dragged both through the air (route: inline)
- [x] T5 manual test by user with two clients, including disconnect while being held
- [x] T6 commit authorized by user

## Checks
- `./gradlew core:compileJava` OK (2026-11-17). No automated tests exist.
