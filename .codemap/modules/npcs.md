# NPC management and UI
<!-- codemap-id: rw-plugin-oz-service-npc/npcs -->

## Responsibility

Manage service NPCs, faction access, configuration and interaction.

## Source and tests

- [`ServiceNpcManager`](../../src/de/omegazirkel/risingworld/servicenpc/ServiceNpcManager.java) — NPC management and faction permissions.
- [`ServiceNpcRuntime`](../../src/de/omegazirkel/risingworld/servicenpc/ServiceNpcRuntime.java) — lifecycle wiring.
- [`ServiceNpcEventHandler`](../../src/de/omegazirkel/risingworld/servicenpc/ServiceNpcEventHandler.java) — command and NPC event handling.
- [`ServiceNpcOverlay`](../../src/de/omegazirkel/risingworld/servicenpc/ServiceNpcOverlay.java) — service UI.
- No test directory was identified. Consult the repository validation instructions.
- [Repository index](../README.md) and [settings defaults](../../src/settings.default.json).

## Dependencies

- `rw-plugin-oz-player-factions/factions` — optional faction membership and permissions via Tools FactionBridge. [Integration source](../../src/de/omegazirkel/risingworld/servicenpc/ServiceNpcManager.java)
