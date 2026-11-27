# Service jobs and custody
<!-- codemap-id: rw-plugin-oz-service-npc/jobs -->

## Responsibility

Own service jobs and NPC state. Wallet owns money and Mail owns delivered attachments.

## Source and tests

- [`ServiceJobService`](../../src/de/omegazirkel/risingworld/servicenpc/ServiceJobService.java) — job creation, payment and delivery.
- [`ServiceNpcRepository`](../../src/de/omegazirkel/risingworld/servicenpc/ServiceNpcRepository.java) — NPC and job persistence.
- [`ServiceInventoryTransfer`](../../src/de/omegazirkel/risingworld/servicenpc/ServiceInventoryTransfer.java) — inventory custody.
- [Augmenter intake](../../src/de/omegazirkel/risingworld/servicenpc/AugmenterIntake.java) records payment, inventory and refund steps. [Intake tests](../../src-test/de/omegazirkel/risingworld/servicenpc/AugmenterIntakeTest.java) cover partial failure and review state.
- [Repository index](../README.md) and [settings defaults](../../src/settings.default.json).

## Dependencies

- `rw-plugin-oz-wallet/ledger` — service payment and system accounts. [Integration source](../../src/de/omegazirkel/risingworld/servicenpc/ServiceJobService.java)
- `rw-plugin-oz-mail/delivery` — job attachment delivery through Tools MailBridge. [Integration source](../../src/de/omegazirkel/risingworld/servicenpc/ServiceJobService.java)
