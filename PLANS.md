# PLANS.md

- [ ] Validate the next-300926 change on Development with a controlled player check.
- [ ] Wire the existing Service NPC Discord channel and thread settings to a defined notification event when that event is specified.

Planning is stored in repository-local docs.

- Active implementation tasks: [docs/active/](docs/active/)
- Roadmaps and larger plans: [docs/roadmaps/](docs/roadmaps/)
- Completed phase summaries: [docs/phase-archive.md](docs/phase-archive.md)
- Planning and documentation standards: [docs/policies/repository-policy.md](docs/policies/repository-policy.md)

Every implementation plan must include objective, ownership, dependencies, risks, validation strategy, affected repositories/plugins, rollback considerations, and a markdown checkbox checklist.

## Backlog validation (2026-10-15)

- [x] Implement: Service NPC settings now offer random catalog clothing and a copy of the player's worn clothing for authorized global and faction managers. Serialized clothing persists in the existing outfit field. The augmenter also accepts the explicitly listed axes, knives, hammers, scythes, sickles, chainsaw, and mining drill.
- [ ] Verify behavior on Development after upload, including persistence and permissions where relevant.
