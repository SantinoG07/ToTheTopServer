# Server state sync

Objective: after `START` the server simulates every player at a fixed tick and
broadcasts the resulting body state, so the client can render the match it
receives instead of computing it.

Branch: `feat/server-state-sync` (never `main`; work reaches `main` only through
a manual cherry-pick).
Delivery: one commit per work unit on this branch.
Test runner: the repository has no test source set; evidence is produced with
`gradlew compileJava --offline` plus temporary Java probes kept outside the
repository.

Source TODO: `TODO-ToTheTop-Client.md` on the desktop, sections
`Server dependencies (not tasks in this repository)` and
`Open contract decisions`.

## Specs

- **S1** State sender: "a fixed-tick loop that encodes and broadcasts the state
  packet at 60 Hz after `START`. Today the server renders and `world.step()`s but
  sends nothing."
- **S2** Fixed-step simulation: "`world.step(delta, 10, 4)` runs on the render
  delta (frame-rate dependent). Move it to an accumulator so the state cadence
  matches the contract."
- **S3** Input wiring: "the stored `InputState` per slot is read by nobody; the
  `Character` must consume it (cursor target, swings, impulse)."
- **S4** "Ignore `INPUT` before `started`: `handleInput` stores packets sent
  before the match begins."
- **S5** "Remove the empty `Server.java`."
- **S6** Freeze open contract decisions 1, 2 and 3: one identical `STATE`
  datagram broadcast to every client with the slot multiplexed inside it, ten
  body parts (the simulation has no calf bodies), and the layout below.
- **S7** Item 3: "Every new constant (`STATE`, TCP messages, port) must land
  byte-identical in both repos."
- **S8** Not in this scope (still open in the TODO): TCP event channel (items
  4, 11, 12), room codes (13), keepalive negotiation (14), disconnection policy
  (open decision 5) and names/colours (8).

## Contract: `STATE` packet

Header `NetworkHeaders.STATE = 0x15`, little endian (`ByteBuffer` default),
coordinates in world meters, angles in degrees (the client's
`Character.applyPartState(int partIndex, Vector2 pos, float angleDeg)`).

Common header, 4 bytes:

| Offset | Size | Field |
| --- | --- | --- |
| 0 | 1 | header, `0x15` |
| 1 | 1 | `ProtocolVersion.CURRENT` |
| 2 | 1 | `matchState`: `0` = running |
| 3 | 1 | `playerCount` |

Then `playerCount` player blocks, in slot order:

| Offset | Size | Field |
| --- | --- | --- |
| 0 | 1 | `slot` |
| 1 | 1 | `partCount`, always `10` today |
| 2 | 12 × `partCount` | per part: `x` f32, `y` f32, `angleDeg` f32 |
| … | 1 | `gripMask`: bit 0 left hand gripped, bit 1 right hand gripped |
| … | 1 | `flags`: bit 0 unconscious |

Part order, top to bottom:

| Index | Part | Index | Part |
| --- | --- | --- | --- |
| 0 | head | 5 | right upper arm |
| 1 | torso | 6 | right forearm |
| 2 | left upper arm | 7 | right hand |
| 3 | left forearm | 8 | left leg |
| 4 | left hand | 9 | right leg |

Worst case, four players: `4 + 4 × (2 + 120 + 2) = 500` bytes, well under any
path MTU.

`matchState` and `flags` are reserved: the simulation has no match end and no
unconsciousness yet, so the server always writes `0`. They are in the layout now
so the client can parse one fixed shape instead of reshaping the packet later.

## Tasks

| ID | Task | Specs | Route | Status |
| --- | --- | --- | --- | --- |
| T1 | Freeze the contract in this document | S6, S7 | inline | done |
| T2 | `StatePacket`, `PlayerState`, `PartState` + `NetworkHeaders.STATE` | S1, S6, S7 | inline | pending |
| T3 | `Character` exposes its 10 bodies and the grip state of both hands | S3 | inline | pending |
| T4 | Fixed 60 Hz accumulator in `GameScreen` | S2 | inline | pending |
| T5 | Broadcast the encoded state every fixed step | S1, S6 | inline | pending |
| T6 | Each character consumes its slot input: pointer velocity, held buttons, grab on release | S3 | inline | pending |
| T7 | Ignore `INPUT` before `started` | S4 | inline | pending |
| T8 | Remove the empty `Server` class | S5 | inline | pending |

## Log

- **L1** (2026-11-04, user): "bien, tenes un todo en el escritorio, neceisot que
  lo sigas, y me vayas haciendo los cambios. Todo en la branch que estas ahoira,
  que nos es la main, si queres podes ir creando branches con cada imple, qu
  elos commits sean claros y no commitees despues de muchisimos cambios. No
  toques el main, quiero que vaya todo a branches externas, asi cuando pueda,
  reviso el codigo y con cherrypick me traigo lo que quiero. Como pasos, tenes
  el todo del escritorio a seguir, maneja eso"
- **L2**: the desktop TODO declares its own scope as client-only and lists the
  server work under `Server dependencies`. Since this session works in
  `ToTheTop-Server`, the tasks above are exactly those dependencies.
- **L3**: no test source set exists, so each task is verified with a probe that
  exercises the real classes or the real UDP server.
