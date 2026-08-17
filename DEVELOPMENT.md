# PeakSkills development and testing

PeakSkills uses three automated test layers. The normal Gradle `build` runs unit
tests and server GameTests. A client-startup GameTest and the packaged production-server smoke
test run nightly in GitHub Actions or can be started manually from the Actions tab.

## Local verification

Run the same required verification used by pull requests:

```powershell
.\scripts\verify-peakskills.ps1
```

This performs the full build, unit tests, server GameTests, production-JAR staging,
and static command-permission checks. To additionally launch the client GameTest:

```powershell
.\scripts\verify-peakskills.ps1 -IncludeClient
```

Individual Gradle tasks are also available:

```powershell
.\gradlew.bat test
.\gradlew.bat runGameTest
.\gradlew.bat runClientGameTest
.\gradlew.bat stageProductionServer
```

## Where tests live

- `src/test/java`: fast progression, bounds, overflow, and data-model tests.
- `src/gametest/java`: tests that require a real Minecraft server or client.
- `.github/workflows/ci.yml`: public GitHub-hosted CI and nightly production checks.

Every bug fix should include the smallest regression test capable of reproducing the
problem. Prefer unit tests for calculations and data invariants, server GameTests for
commands/world/entity behavior, and client GameTests only for actual client input,
screens, rendering, or integrated-server behavior.

## CI policy

- Pushes and pull requests run `build`, which includes unit and server GameTests.
- Manual and nightly runs launch the client-startup GameTest in a virtual display.
- Manual and nightly runs boot the packaged JAR in a clean Fabric server container
  and execute a PeakSkills command over RCON.
- Logs, reports, crash reports, and screenshots are uploaded only when a job fails
  and expire after seven days.

Minecraft 26.1 and newer require Java 25 and Mojang official names. PeakSkills targets
Minecraft 26.1.2, so local verification and CI both run on Java 25. Manual and nightly
failures open or update one `ci-failure` GitHub issue; a later successful run closes it.

The wider ecosystem's invariants, ownership rules, definition of done, test strategy,
backup expectations, and observability rules are recorded in
[`docs/PEAKMOD_ENGINEERING_GUIDE.md`](docs/PEAKMOD_ENGINEERING_GUIDE.md).

## Server-driven GUI foundation

- Inventory grids use bundled SGui `2.0.0+26.1`; clients remain completely vanilla.
- `LegacyContainerGui` adapts existing 54-slot renderers to locked virtual slots and
  accepts only explicitly supported click types.
- Native Minecraft dialogs use one-use, player-bound action tokens with a two-minute
  expiry. Never place a privileged operation directly behind an unvalidated command.
- Use `PeakDialogs.confirm` before irreversible GUI actions. Rewarding or consuming
  actions still require their own cooldown, inventory recheck, and server-side bounds.
