$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..'
$shader = Get-Content "$root/src/main/resources/assets/tacz_pip_scope/shaders/core/scope_image.fsh" -Raw
$description = Get-Content "$root/src/main/resources/assets/tacz_pip_scope/shaders/core/scope_image.json" -Raw | ConvertFrom-Json
$composite = Get-Content "$root/src/main/java/com/shumai/taczpip/client/ScopeLensComposite.java" -Raw
foreach ($name in @('Coating', 'Haze')) {
    if ($shader -notmatch "uniform float $name;" -or $description.uniforms.name -notcontains $name -or
        !$composite.Contains('safeGetUniform("' + $name + '")')) { throw "Missing uniform wiring: $name" }
}
foreach ($lang in @('ja_jp', 'en_us')) {
    $labels = Get-Content "$root/src/main/resources/assets/tacz_pip_scope/lang/$lang.json" -Raw | ConvertFrom-Json
    foreach ($key in @('lensCoating', 'coatingStrength', 'lensHaze', 'hazeStrength')) {
        if (!$labels."tacz_pip_scope.config.$key" -or !$labels."tacz_pip_scope.config.$key.tooltip") {
            throw "Missing translation: $lang/$key"
        }
    }
}
# Reference checks of the bounded blend, not a GPU render test.
foreach ($source in @(0.0, 0.1, 0.5, 1.0)) {
    foreach ($reflection in @(0.0, 0.12, 1.0)) {
        foreach ($veil in @(0.0, 0.08, 1.0)) {
            $result = $source + (1-$source)*(1-(1-$reflection)*(1-$veil))
            if ($result -lt $source -or $result -gt 1) { throw 'Blend out of range' }
            if ($reflection -eq 0 -and $veil -eq 0 -and $result -ne $source) { throw 'Zero changed source' }
        }
    }
}
$surface = $shader.Substring($shader.IndexOf('if (Coating > 0.0'))
if ($surface -match '\bdiscard\b' -or $surface -match 'gl_FragDepth') { throw 'Surface effect changes aperture/depth' }
if ($shader -notmatch 'Vignette == 0.0 && Coating == 0.0 && Haze == 0.0') { throw 'Missing exact bypass' }
Write-Output 'Lens surface: uniform/translation/static aperture and reference blend checks passed (not GPU-tested).'
