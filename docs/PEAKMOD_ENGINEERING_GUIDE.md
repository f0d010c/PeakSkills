# PeakMod Engineering Guide

This is the practical engineering standard for the PeakMod ecosystem. It adapts the
useful parts of `FUTURE_APP_ENGINEERING_GUIDEBOOK.md` to Minecraft/Fabric development.
Web-only mechanisms such as OIDC/AAL2, HTML and CSV safety, tenant APIs, payments,
tax, and relational-database migrations are intentionally not requirements here.

## Product and module boundaries

- PeakSkills owns progression: XP, skills, stats, pets, collections, and player
  progression data.
- PeakGear owns vanilla-compatible gear definitions, stats, and abilities.
- Economy, guilds/social systems, dungeons, world generation, and other major systems
  belong in separate optional mods. They may depend on PeakSkills through a small,
  documented compatibility API.
- Do not place a feature in a core mod merely because that is convenient. Name its
  owner, stored state, commands/events, and dependencies before implementation.
- Cross-mod integration must fail safely when an optional mod is absent. Avoid static
  references that make an optional dependency mandatory at class-load time.

## Invariants

Every feature starts with statements that must always remain true. Tests should prove
both the permitted path and the forbidden or boundary path.

PeakMod-wide invariants include:

- A non-operator cannot execute an administrative mutation.
- A player action cannot mutate another player's private progression unless an
  explicitly authorized operator command does so.
- XP, collection counts, pet XP, item stats, quantities, durations, and command
  arguments remain within documented bounds; additions saturate instead of wrapping.
- A player-placed resource cannot be broken repeatedly for progression rewards.
- A reward-consuming action checks the live inventory state and cannot execute twice
  because of rapid clicks, retries, or duplicate events.
- Saving is atomic: interrupted writes must leave either the old valid data or the new
  valid data, never a half-written primary file.
- A backup is not considered trustworthy until an automated restore test proves it.
- Tick callbacks are bounded and do not spawn, kill, block on I/O, or perform an
  unbounded scan inside an entity/world iteration.
- Server-only mods use vanilla registries and data components where client registry
  synchronization would otherwise require client installation.

## Feature worksheet

For a meaningful feature, record these points in its issue, design note, or pull
request:

1. Player outcome and operator outcome.
2. Owning mod and optional dependencies.
3. Persistent state, data version, bounds, and upgrade/default behavior.
4. Trust boundaries: commands, packets, NBT/components, inventory clicks, world and
   entity events, config files, and other mods.
5. Authorization and ownership rules.
6. State transitions, including retries, duplicate events, disconnects, restarts, and
   partial failure.
7. Fast unit tests, real-server GameTests, and any client-only test that is genuinely
   required.
8. Operational evidence: useful logs, failure artifacts, backup/restore behavior, and
   how an operator can recover.
9. Documentation, compatibility notes, and changelog entry.

## Testing strategy

Use the lowest-cost layer that proves the behavior:

- Unit tests: XP tables, stat totals, thresholds, clamps, saturating arithmetic,
  serialization, recipes, loot eligibility, and pure state transitions.
- Server GameTests: command registration/permissions, world interaction, entity
  behavior, inventory effects, lifecycle integration, persistence, and restore flows.
- Client GameTests: rendering, screens, key/input behavior, or integrated-client
  mixins only. Do not run a client merely to prove server logic.
- Packaged-server smoke tests: build the actual distributable JAR, boot it with its
  declared runtime dependencies, issue a real command, restart the same world, and
  verify readiness again.

Important test rules:

- A bug fix includes the smallest regression test that fails before the fix.
- Test negative paths: non-OP users, missing optional dependencies, malformed or old
  data, negative and maximum values, duplicate clicks/events, full inventory, absent
  backup, and restart behavior.
- Prefer deterministic seeds and fixed thresholds. Avoid timing-sensitive sleeps when
  an observable state can be polled.
- Keep expensive client and packaging checks manual/nightly; keep unit and server
  feature tests on every pull request.
- Upload logs, reports, crash reports, and screenshots on failure only, with short
  retention. Successful runs should produce little noise.
- The artifact tested in the packaging job must be the artifact intended for release.

## Data safety and recovery

- Treat disk data, config, NBT/components, commands, inventory clicks, and events from
  other mods as untrusted inputs. Validate type, range, presence, and ownership.
- Persist explicit data versions. Missing fields receive safe defaults; unsupported or
  corrupt values fail closed or are clamped with a useful warning.
- Write player data through a temporary sibling file followed by atomic replacement.
- Keep backups separate from primary data, use unique names, and define retention so
  storage cannot grow forever.
- Run recurring restore verification in CI or a disposable test world. A successful
  backup write alone is not evidence of recoverability.
- Never log full player-data JSON, raw custom NBT, secrets, session tokens, IPs, or
  other private values. Prefer UUID, feature, transition, and bounded reason codes.

## Security and abuse resistance

- Operator gates query the server OperatorList and fail closed. Never leave a constant
  permission bypass or depend on a permission API known not to enforce correctly.
- Every numeric command argument has an explicit minimum and maximum.
- World/entity rewards verify plausible player distance and event ownership.
- Reward and crafting paths use per-player cooldowns or idempotency guards, re-check
  the live slot/item before consumption, and grant only after validation succeeds.
- Use safe arithmetic for all attacker-influenced accumulation.
- Keep administrative mutations auditable: actor, target, action, bounded before/after
  summary, and outcome. Do not include private payloads.

## Runtime design and observability

- Server tick work has a known upper bound. Queue entity spawn/removal and other
  structural world changes outside active iteration.
- Isolate optional or background work so one failure does not disable unrelated
  progression. Log a concise failure with enough context to reproduce it.
- Use stable log messages and low-cardinality categories. Avoid one metric or label per
  player, item UUID, block coordinate, or arbitrary input.
- Log state transitions and recovery events, not every tick or successful XP award.
- Define operator-facing health signals for persistent events and integrations: active
  state, bounded progress, last success/failure, and recovery command.

## Repository-owned verification

Each mod owns one documented verification entry point that works locally and in CI.
For PeakSkills this is `scripts/verify-peakskills.ps1`. It must compile, run relevant
tests, stage the production artifact, and enforce static security checks. CI should use
the same Gradle tasks and toolchain rather than maintaining a separate test reality.

Required CI policy:

- Pull requests and pushes run compilation, unit tests, server GameTests, artifact
  staging, and security checks.
- Manual/nightly verification runs client GameTests and a clean packaged-server boot,
  real command probe, stop, and restart.
- A failed scheduled/manual run creates or updates one visible failure issue; a later
  successful run closes it.
- The protected default branch requires the primary build/server-test check, blocks
  force pushes and deletion, and is changed through a tested pull request.

## Definition of done

A change is complete only when:

- the behavior and non-behavior are explicit;
- the correct mod owns it and optional dependencies remain optional;
- trust-boundary, permission, bounds, overflow, cooldown, distance, ownership, and race
  checks have been reviewed;
- the affected mod compiles and all relevant automated tests pass;
- the production artifact is staged or smoke-tested when packaging/runtime changed;
- failure evidence and recovery behavior are adequate;
- README/development documentation and `CHANGELOG.md` reflect notable changes;
- the exact committed revision is pushed and its required GitHub check succeeds.

## Maintainability ratchet

- New code follows these rules even where legacy code does not yet comply.
- When touching a risky legacy path, improve the nearest missing invariant or test in
  the same change when scope permits.
- Track hotspots by repeated defects, oversized classes, duplicated stat calculations,
  and frequent mapping-port breakage. Refactor from evidence, not aesthetics alone.
- Keep active docs short and authoritative; archive superseded plans instead of leaving
  conflicting instructions beside current ones.

