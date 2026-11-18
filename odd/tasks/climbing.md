# Climbing (pull-up)

Repo: `servidor` only, on `main` (user decision 2026-11-17). Protocol unchanged.

## Problem
With one hand gripping, dragging the other pushed the torso with up to 3x the body
weight (~2350 N) in the pointer direction. The force had to travel through a light
arm chain (hand 0.8 kg -> torso 40 kg), so joints stretched and the ragdoll exploded.

## Tasks
- [x] T1 `JointFactory.createRopeJoint`; on grab, `Arm` ties torso shoulder to the grip
      point with max length = arm length; destroyed on release (route: inline)
- [x] T2 `Arm.drive` keeps the pointer movement past the reach as `reachOverflow` (route: inline)
- [x] T3 `Character.applyLift`: mass-scaled body servo, overflow sets body speed, holds
      height otherwise, horizontal free unless pulled, cap 1.5x weight; old climb pull
      and unused `weight` field removed (route: inline)
- [x] T4 manual test by user: normal climbing works; edge cases still explode (follow-up)
- [x] T5 commit authorized by user

## Checks
- `./gradlew core:compileJava` OK (2026-11-17). No automated tests exist: manual test pending.

## Tuning knobs
`MAX_LIFT_SPEED`, `LIFT_RESPONSE`, `LIFT_SMOOTHING`, `LIFT_MAX_WEIGHT_FACTOR` in `Character`.
