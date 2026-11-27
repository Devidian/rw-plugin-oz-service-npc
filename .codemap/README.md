# rw-plugin-oz-service-npc
<!-- codemap-id: rw-plugin-oz-service-npc -->

## Purpose

Persistent Restorer and Augmenter service NPCs, jobs, payments and item delivery. [Descriptor](../src/resources/plugin.yml) names [`ServiceNpcPlugin`](../src/de/omegazirkel/risingworld/ServiceNpcPlugin.java); [`ServiceNpcRuntime`](../src/de/omegazirkel/risingworld/servicenpc/ServiceNpcRuntime.java) handles lifecycle and dispatch.

## Modules

- [Service jobs and custody](modules/jobs.md).
- [NPC management and UI](modules/npcs.md).

## Build and tests

[Maven](../pom.xml) builds against the Rising World API and OZ Tools. Follow [repository instructions](../AGENTS.md) for validation and see [custody tests](../src-test/).

## Dependencies

- `rw-plugin-oz-tools/shared-api` — build and shared runtime infrastructure. [Build configuration](../pom.xml)
- `rw-plugin-oz-tools/ui-settings` — the runtime retires the scoped menu and player and admin settings registrations on disable. [Runtime](../src/de/omegazirkel/risingworld/servicenpc/ServiceNpcRuntime.java)
