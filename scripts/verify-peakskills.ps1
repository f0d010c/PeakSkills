param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$ProjectDir = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectDir

function Run-Step {
    param(
        [string]$Name,
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "==> $Name" -ForegroundColor Cyan
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE"
    }
}

Run-Step "Unit tests" { .\gradlew.bat test }
Run-Step "Compile" { .\gradlew.bat compileJava }

Write-Host ""
Write-Host "==> Permission/security grep" -ForegroundColor Cyan
$patterns = @(
    "hasPermissionLevel",
    "DefaultPermissions",
    "LongArgumentType\.longArg\(1\)",
    "IntegerArgumentType\.integer\(1\)"
)
foreach ($pattern in $patterns) {
    $matches = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" |
        Select-String -Pattern $pattern
    if ($matches) {
        $matches | ForEach-Object { Write-Host $_ }
        throw "Security grep matched '$pattern'"
    }
}

$requires = Get-ChildItem -Path "src\main\java\com\peakskills\command" -Recurse -Filter "*.java" |
    Select-String -Pattern "\.requires"
if ($requires) {
    $badRequires = $requires | Where-Object { $_.Line -notmatch "::isOp" }
    if ($badRequires) {
        $badRequires | ForEach-Object { Write-Host $_ }
        throw "Found command requires() not using ::isOp"
    }
}

if (-not $SkipBuild) {
    Run-Step "Full build" { .\gradlew.bat build }
}

Write-Host ""
Write-Host "PeakSkills verification passed." -ForegroundColor Green
