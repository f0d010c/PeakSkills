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

Minecraft 26.1 and newer require Java 25 and Mojang official mappings. When the mod is
ported, update the Java version in both Gradle and the workflow together; the testing
layout itself can remain the same.
