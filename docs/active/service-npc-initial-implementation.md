# OZ Service NPC – initial implementation

## Objective

Provide the standalone `OZ - Service NPC` plugin, derived from the Maven template, with persistent Restorer and Augmenter NPC endpoints.

## Ownership and dependencies

- Owner: `rw-plugin-oz-service-npc`; no feature logic is added to OZ Tools.
- Uses Tools for settings, i18n, SQLite, radial menu, overlays and reflection bridges.
- Requires Wallet system accounts and Mail plugin delivery at job acceptance/delivery. Discord is optional per service type.

## Delivery scope

- [x] Template-derived standalone project, descriptor, entry point, baseline settings/info/menu integration.
- [x] Persistent NPC records, default JSON name catalog, rehydration, gender/type radial creation, per-NPC Wallet accounts, safe account settlement on dissolve.
- [x] Player interaction overlay based on `BasePluginOverlayWithTabs`, including service, admin-settings and job-log tabs.
- [x] Default Restorer duration/cost policy, names, clothing sets and Augmenter modifier/material/time policy are packaged as editable world-scoped JSON inputs. Defaults are copied once as `names.<world>.json`, `outfits.<world>.json` and `augmentation.<world>.json`; later deployments retain those files.
- [ ] Restorer: persistent custody record, payment to the NPC system account, exact item-state snapshot, minute-precise duration display, player/admin order log, delayed idempotent Mail delivery, and refund handling when removal fails are implemented. Runtime acceptance must verify payment, removal, restart persistence, delivery and recovery paths.
- [x] Augmenter: modifier pools and their ordered tier levels are configured in JSON, including level 0 as the non-serviceable source band. Material custody, payment, delivery and runtime acceptance are implemented.

## Follow-up architecture review

- [x] Compared the Boss Informant, Marketplace Crier, and Service NPC initialization paths after Dev acceptance. All use a delayed dummy-NPC initialization pattern, but persisted appearance, missing-NPC policy, account ownership and rehydration replacement differ. No Tools helper is extracted yet: a generic scheduler alone would not remove the domain-specific failure risks. Revisit only when a third consumer needs the identical callback contract.

## Invariants and risks

- A job cannot be accepted unless Wallet and Mail are available; payment must be reversible if custody/delivery preparation fails.
- Dissolving an NPC is denied while jobs are open. Once no jobs remain, every positive account balance is transferred idempotently to the world account before archival.
- Rehydration replaces only the NPC id while retaining endpoint/account identity.
- Names, config and player-visible messages are packaged defaults; world files are never overwritten by deployment.

## Validation and rollback

- [x] `mvn -B -DskipTests package` and entrypoint verifier.
- [ ] Build a clean package and upload it beside Wallet/Mail/Tools on Development. The server console stdin is `/dev/null`, so an administrator must issue `reloadplugins` in-game before startup and interaction evidence can be collected.
- [ ] Verify both radial creation variants, persistence across reload, localized PluginSettings defaults, player/admin order logs, and a Restorer acceptance/delivery cycle.
- Rollback: remove only `OZServiceNPC` from Development; retain the data directory until no open job needs recovery.
