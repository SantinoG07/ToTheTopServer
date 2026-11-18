# Impulse and swing

Repos: `servidor` (steps 1-2), `cliente` + `servidor` (step 3, protocol 2 -> 3). On `main`.

## Tasks
- [x] S1 soft swing: A/D push = 0.15x body weight, ramped over 0.5 s (restarts on
      direction change), stops above 4 m/s sideways; only while gripping (route: inline)
- [x] S2 impulse cooldown 2 s on top of resting + not gripping (route: inline)
- [x] S3 manual test by user OK; committed separately
- [x] S4 directed impulse: `KEY_W` bit (S is not sent, it does nothing), ProtocolVersion
      2 -> 3 in both repos; none/W = up, A/D = 45 deg, W+A/D = 65 deg (route: inline)
- [x] S4b stronger impulse: whole-body take-off speed 5 m/s (was ~1.1 m/s effective),
      pushes only the torso for 0.12 s (user prefers limbs to follow; an instant torso kick
      would yank the joints) (route: inline)
- [x] S5 manual test by user OK; committed and pushed in both repos

## Checks
- `./gradlew core:compileJava` OK (2026-11-17). No automated tests exist.
