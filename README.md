# OZ Service NPC

Persistent Restorer and Augmenter NPC endpoints for Rising World. The plugin is a standalone adaptation of `rw-plugin-maven-template` and depends at runtime on OZ Tools, OZ Wallet and OZ Mail.

## Administration

`/osn create restorer male`, `/osn create restorer female`, `/osn create augmenter male`, and `/osn create augmenter female` create endpoints at the administrator position. `/osn move <npc-id>`, `/osn rename <npc-id> <name>`, and `/osn dissolve <npc-id>` manage them. Dissolve refuses open jobs and settles positive Wallet balances to the world account before account archival.

## World-local service catalogs

On first startup the packaged defaults are copied once next to the plugin JAR.
They are safe for administrators to edit and are retained during updates:

- `names.<world>.json` contains the male and female NPC name pools.
- `outfits.<world>.json` contains named clothing-definition arrays; the supplied sets include medieval, modern and hat variants.
- `augmentation.<world>.json` defines ordered modifier pools. Level `0` is the source/reference pool; an item always advances to the next higher enabled level and receives a random modifier from that level's array. Additional intermediate levels can therefore be added without code changes.

Use `reloadplugins` after changing one of these catalogs. Do not edit the packaged `*.default.json` files: they are only templates for a world file that does not exist yet.

## Files included

- [.github/workflows/ci.yml](.github/workflows/ci.yml)
  - for GitHub Action, you have to change `Devidian/rw-plugin-maven-template` here (2x)
- [src/assembly/rw-plugin-maven-template.xml](src/assembly/rw-plugin-maven-template.xml)
  - this is needed to pack you plugin as zip, change atleast directory/outputDirectory here
  - change name to your `pom.project.artifactId`
  - i use `pom.project.name` for directory/outputDirectory
- [src/resources/plugin.yml](src/resources/plugin.yml)
  - your plugin definition file, change as you need
- [src/de/omegazirkel/risingworld/MavenTemplate.java](src/de/omegazirkel/risingworld/MavenTemplate.java)
  - sample main file for your plugin, change name and path as you need (dont forget to change it in plugin.yml too)
- [pom.xml](pom.xml)
  - maven file, change as you need it
- [HISTORY.md](HISTORY.md)
  - for your changelog
- [README.md](README.md)
  - this file, override it as you like
- [DESIGN.md](DESIGN.md)
  - synchronized portfolio UI/design baseline; keep it aligned with the root copy

## Baseline behavior

- Requires `rw-plugin-oz-tools`.
- Uses the shared file watcher path by implementing `FileChangeListener`; changes
  to the active world-scoped `settings.<world>.json` reload plugin settings.
- Ships `settings.default.json`. On the first JSON-aware startup, an existing
  `settings.properties` is migrated atomically and retained as a timestamped
  backup; new plugins must not add `logLevel` or `reloadOnChange`, which are
  owned by OZ Tools.
- Registers a shared inventory overlay button through `InventoryOverlayButtons`
  so players get a compact entrypoint below the inventory.
- Registers a default-visible shortcut visibility provider through
  `PluginShortcutVisibility`; real plugins should connect this to a persisted
  player setting when they expose player preferences.
- Registers a `SharedIndicators` provider stub. The template returns `false` by
  default; real plugins should only show indicators when they have meaningful
  player-specific state.
- Registers a `PluginInfoStatusProvider` with generic RichText info/status
  content for the shared Tools Info/Status panel.
- Adds an `Info / Status` action to the plugin-owned radial menu. It uses the
  Tools-registered `info-status` asset key; generated plugins should not
  register a duplicate copy of that shared icon.
- Registers player settings, player data, and admin-only `PluginSettings`
  metadata with `PlayerPluginSettingsOverlay`.
- Includes an optional reflection-based `WalletBridge` scaffold for economy
  integrations. It covers Wallet availability, default currency, currency
  listing, currency registration, deposit, withdraw, balance, and default
  currency convenience calls without a compile-time Wallet dependency.
- Includes grouped sample admin settings metadata for booleans and strings, plus
  a hidden sensitive value example that should be replaced or removed in real
  plugins.
- Uses one main plugin logger name. Helper classes should call the main plugin
  logger instead of creating subsystem logger names.
- Uses the `plugin.yml` entry class as the sole Rising World event listener.
  That class only wires lifecycle and dispatches events; runtime and feature
  logic live in focused classes under `template/`.

## Shared Tools conventions

Future plugins generated from this template should route shared infrastructure
through `rw-plugin-oz-tools`:

- Template adaptation: start from this repository and rename the template
  packages, classes, assets, descriptor metadata, documentation, and
  player-facing labels. Keep its classes and responsibilities unless a concrete
  documented technical reason requires a deviation. In particular, retain the
  supplied `PluginSettings` plus settings-file model instead of introducing a
  settings `record` or a separate configuration implementation.

- Event architecture: the entry class is the only
  `net.risingworld.api.events.Listener` and the only target of
  `registerEventListener(...)`. Keep event methods as thin dispatchers and put
  their logic in thematic handler/service classes; do not register secondary
  Rising World listeners.

- UI entrypoints: use `InventoryOverlayButtons` for compact inventory actions and
  `PluginMenuManager` for the `/ozt` main plugin menu. Register
  `PluginShortcutVisibility` with a default-visible per-player predicate when a
  plugin lets players hide shared shortcuts.
- Indicators: use `SharedIndicators` for reusable HUD indicator slots. Return an
  `AssetManager` icon key from the provider, not a file path.
- Info/status: expose player-facing RichText through `PluginInfoStatusProvider`
  and open it with `PluginInfoStatusProviders.show(player, pluginName)` from
  plugin-owned buttons, menu items, or commands when appropriate. Use the shared
  `info-status` icon key for radial Info/Status buttons.
- Wallet: use the template `WalletBridge` pattern for optional economy
  integrations. Keep feature-specific spending and fulfillment rules inside the
  generated plugin, and disable economy features when Wallet is unavailable.
- Settings: register admin metadata through `PlayerPluginSettingsOverlay`; use
  `AdminSettingsEntry.group(...)` for sections and `AdminSettingsType.INTEGER`
  for numeric settings so Tools can apply shared numeric input filtering.
- i18n: load the plugin i18n instance once during `onEnable()` with
  `I18n.getInstance(this)`. Other classes may use `I18n.getInstance(pluginName)`
  after enable.
- Persistence: use `SQLiteConnectionFactory.open(this)` and repository-local
  stores for runtime data. Do not use the deprecated Tools `SQLite` class in new
  plugins.
- Escape behavior: rely on explicit close controls until the Rising World API
  provides its planned custom-overlay Escape layer.
- Common UI helpers and runtime watchers should use Tools contracts instead of
  duplicating helper code in feature plugins.

## Contributor Workflow

- Review `AGENTS.md`, `PLANS.md`, `.codex/agents.toml`, and `.codex/skills/` before making structural changes.
- Verify Rising World API usage with `scripts/verify-plugin-api.sh` when adding or changing API calls.
- Run `mvn -B -DskipTests package` and `mvn -B test` before release-facing changes are merged.
- Use `RUNTIME_TESTING.md` and `scripts/docker-runtime-smoke.sh <PluginFolderName>` for runtime smoke tests when behavior changes need server validation.
- Keep `README.md` and `HISTORY.md` current and use Conventional Commit titles for commits and PRs.

## JSON-only distribution

Settings defaults (`settings.default.json`) and translations (`i18n/*.json`)
are shipped only as JSON. Legacy default and translation `.properties` files
are no longer included. Runtime settings remain world-scoped as
`settings.<world>.json`; migration of an existing `settings.properties` and
its backup remains supported. Updating the package does not delete old files
already present on the server. Use `mvn clean package` for a fresh local
package; ZIP assembly also excludes stale legacy settings and translations.
