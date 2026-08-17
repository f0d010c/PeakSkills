param(
    [switch]$IncludeClient
)

$ErrorActionPreference = "Stop"
$ProjectDir = Split-Path -Parent $PSScriptRoot
Push-Location $ProjectDir

try {
    & .\gradlew.bat build stageProductionServer
    if ($LASTEXITCODE -ne 0) {
        throw "PeakSkills build or automated tests failed with exit code $LASTEXITCODE"
    }

    $forbiddenPatterns = @(
        "hasPermissionLevel",
        "DefaultPermissions",
        "LongArgumentType\.longArg\(1\)",
        "IntegerArgumentType\.integer\(1\)"
    )

    foreach ($pattern in $forbiddenPatterns) {
        $matches = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" |
            Select-String -Pattern $pattern
        if ($matches) {
            $matches | ForEach-Object { Write-Host $_ }
            throw "Security check matched forbidden pattern '$pattern'"
        }
    }

    $requires = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" |
        Select-String -Pattern "\.requires\("
    $unexpectedRequires = $requires | Where-Object { $_.Line -notmatch "::isOp" }
    if ($unexpectedRequires) {
        $unexpectedRequires | ForEach-Object { Write-Host $_ }
        throw "A command permission gate does not use the OperatorList-backed isOp helper"
    }

    if ($IncludeClient) {
        & .\gradlew.bat runClientGameTest
        if ($LASTEXITCODE -ne 0) {
            throw "Client GameTests failed with exit code $LASTEXITCODE"
        }
    }

    Write-Host "PeakSkills verification passed." -ForegroundColor Green
} finally {
    Pop-Location
}
