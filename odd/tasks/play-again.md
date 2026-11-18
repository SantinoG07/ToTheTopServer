# Play again (R6) and host migration

Repos: `servidor` and `cliente`, on `main`. Protocol 4 -> 5.

## Problem
`ThreadServer.started` never went back to false: after a match every CONNECT got
`REJECT_STARTED` and the server had to be restarted (R6 failed). The server
`GameScreen` had no `dispose()`, so its Box2D `World` was never freed. The host was
hard-coded as slot 0.

## Design (user decision 2026-11-18)
- Result screen: "Play again" (back to the waiting room, same connection) or
  "Back to menu" (leave).
- Server phases: LOBBY -> RUNNING -> FINISHED -> LOBBY. In FINISHED a client that sends
  `RETURN_TO_LOBBY` waits in the room; the room goes back to LOBBY once every connected
  client returned or left. New clients may join in LOBBY and FINISHED, not in RUNNING.
- Host = `hostSlot`; when the host leaves, the remaining client that joined first
  becomes host. A client that rejoins gets a new join order, so it never gets the host back.
- ROSTER carries `hostSlot`, the phase and a per-player "in lobby" flag; START is only
  re-sent while RUNNING.
- Server `GameScreen` tracks its match id, finishes through the server, and disposes the
  world when the room returns to LOBBY.

## Tasks
- [x] P1 server: `ThreadServer.Phase`, `hostSlot` + join order, `RETURN_TO_LOBBY` (0x09),
      ROSTER v5 (+hostSlot, +phase, entry flags), message handling synchronized with
      `finishMatch`; `GameScreen` closes and disposes its world when the room is a lobby (route: inline)
- [x] P2 client: `Roster` v5, `isHost` from roster, `returnToLobby` resent until confirmed,
      START only accepted in the room, `WaitingMenu` reusable with an existing connection and
      ready label from the server, "Play again" on `MatchEndScreen` (route: inline)
- [x] P3 compile both (OK 2026-11-18); user tested OK: manual test: win/lose -> play again x2, host leaves (2 and 3
      players), old host rejoins without host, a player idles on the result screen
- [x] P4 commit authorized by user

## ROSTER v5
| Offset | Size | Field |
| --- | --- | --- |
| 0 | 1 | header `0x13` |
| 1 | 1 | protocol version |
| 2 | 1 | player count |
| 3 | 1 | ready count |
| 4 | 1 | host slot, `0xFF` = none |
| 5 | 1 | phase: 0 lobby, 1 running, 2 finished |
| 6.. | 6 + name | per player: slot, flags (bit 0 ready, bit 1 back in the room), r, g, b, name length, name |
