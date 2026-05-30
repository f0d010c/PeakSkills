param(
    [Parameter(Mandatory = $true)]
    [string]$ModsDir,

    [switch]$Build
)

$ErrorActionPreference = "Stop"

$ProjectDir = Split-Path -Parent $PSScriptRoot
$ResolvedModsDir = Resolve-Path $ModsDir

if ($Build) {
    Push-Location $ProjectDir
    try {
        .\gradlew.bat build
        if ($LASTEXITCODE -ne 0) {
            throw "Build failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

$jar = Get-ChildItem -Path (Join-Path $ProjectDir "build\libs") -Filter "peakskills-*.jar" -File |
    Where-Object { $_.Name -notmatch "sources" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jar) {
    throw "No built PeakSkills jar found. Run with -Build or build first."
}

Get-ChildItem -LiteralPath $ResolvedModsDir.Path -Filter "peakskills-*.jar" -File |
    Remove-Item -Force

$destination = Join-Path $ResolvedModsDir.Path $jar.Name
Copy-Item -LiteralPath $jar.FullName -Destination $destination -Force

Write-Host "Deployed $($jar.Name) to $destination" -ForegroundColor Green
