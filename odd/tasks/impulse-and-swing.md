# Impulse and swing

Repos: `servidor` (steps 1-2), `cliente` + `servidor` (step 3, protocol 2 -> 3). On `main`.

## Tasks
- [x] S1 soft swing: A/D push = 0.15x body weight, ramped over 0.5 s (restarts on
      direction change), stops above 4 m/s sideways; only while gripping (route: inline)
- [x] S2 impulse cooldown 2 s on top of resting + not gripping (route: inline)
- [x] S3 manual test by user OK; committed separately
- [ ] S4 directed impulse: send W/S key bits, bump ProtocolVersion 2 -> 3 in both repos;
      none/W = up, A/D = ~45 deg diagonal, W+A/D = steeper diagonal, S = nothing

## Checks
- `./gradlew core:compileJava` OK (2026-11-17). No automated tests exist.
