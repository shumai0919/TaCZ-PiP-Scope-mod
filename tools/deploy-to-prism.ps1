[CmdletBinding()]
param(
    [string]$PrismInstanceDir = $env:PRISM_INSTANCE_DIR,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($PrismInstanceDir)) {
    throw 'Set PRISM_INSTANCE_DIR or pass -PrismInstanceDir with the Prism instance directory.'
}

$instanceDir = [System.IO.Path]::GetFullPath($PrismInstanceDir)
$minecraftDir = Join-Path $instanceDir '.minecraft'
if (-not (Test-Path -LiteralPath $minecraftDir -PathType Container)) {
    # PrismLauncher can also use "minecraft" as the instance game directory.
    $minecraftDir = Join-Path $instanceDir 'minecraft'
}
if (-not (Test-Path -LiteralPath $minecraftDir -PathType Container)) {
    throw "Prism instance does not contain a .minecraft or minecraft directory: $instanceDir"
}

if (-not $SkipBuild) {
    & (Join-Path $projectDir 'gradlew.bat') build
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }
}

$libsDir = Join-Path $projectDir 'build\libs'
$builtJars = @(Get-ChildItem -LiteralPath $libsDir -File -Filter 'tacz-pip-scope-*.jar' |
    Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' })
if ($builtJars.Count -ne 1) {
    throw "Expected exactly one deployable JAR in $libsDir, found $($builtJars.Count)."
}

$modsDir = Join-Path $minecraftDir 'mods'
New-Item -ItemType Directory -Path $modsDir -Force | Out-Null

Get-ChildItem -LiteralPath $modsDir -File -Filter 'tacz-pip-scope-*.jar' |
    Remove-Item -Force

$destination = Join-Path $modsDir $builtJars[0].Name
Copy-Item -LiteralPath $builtJars[0].FullName -Destination $destination -Force
Write-Host "Deployed: $destination"
