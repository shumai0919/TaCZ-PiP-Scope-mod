$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..'
$shader = Get-Content "$root/src/main/resources/assets/tacz_pip_scope/shaders/core/scope_image.fsh" -Raw
$description = Get-Content "$root/src/main/resources/assets/tacz_pip_scope/shaders/core/scope_image.json" -Raw | ConvertFrom-Json
$composite = Get-Content "$root/src/main/java/com/shumai/taczpip/client/ScopeLensComposite.java" -Raw
foreach ($name in @('Coating', 'Haze', 'Reflection', 'PeripheralBlur', 'Dirt', 'Scratches', 'ScratchDensity', 'ScratchSize', 'SurfacePattern', 'EdgeRefraction', 'Ghost', 'SurfaceTexture', 'IdleGlass')) {
    if ($shader -notmatch "uniform float $name;" -or $description.uniforms.name -notcontains $name -or
        !$composite.Contains('safeGetUniform("' + $name + '")')) { throw "Missing uniform wiring: $name" }
}
foreach ($lang in @('ja_jp', 'en_us')) {
    $labels = Get-Content "$root/src/main/resources/assets/tacz_pip_scope/lang/$lang.json" -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($key in @('lensCoating', 'coatingStrength', 'lensHaze', 'hazeStrength', 'lensReflection', 'reflectionStrength', 'peripheralBlur', 'peripheralBlurStrength', 'lensDirt', 'dirtStrength', 'lensScratches', 'scratchStrength', 'scratchDensity', 'scratchSize', 'surfacePattern', 'edgeRefraction', 'edgeRefractionStrength', 'lensGhost', 'ghostStrength', 'gunFocusBlur', 'gunBlurStrength', 'idleGlass', 'idleGlassStrength', 'surfaceTexture')) {
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
if ($shader -notmatch 'Reflection == 0.0 && PeripheralBlur == 0.0') { throw 'Missing new effects bypass' }
if ($shader -notmatch 'Dirt == 0.0 && Scratches == 0.0') { throw 'Missing surface detail bypass' }
if ($shader -notmatch 'uniform vec2 LensView;' -or $description.uniforms.name -notcontains 'LensView' -or
    !$composite.Contains('safeGetUniform("LensView")')) { throw 'Missing eye projection wiring' }
if ($shader -notmatch 'smoothstep\(0.45, 1.0, radius\) \* aimGuard \* PeripheralBlur') { throw 'Missing shape/aim protection' }
Write-Output 'Lens surface: uniform/translation/static aperture and reference blend checks passed (not GPU-tested).'
if ($shader -match '\bSampler2\b' -or $composite -match 'SCRATCH_TEXTURE') { throw 'Repeated scratch stamp path remains' }
if (!$shader.Contains('if(SurfaceTexture>0.5) marks=imageScratches;')) { throw 'Full image scratch path missing' }
if (!$shader.Contains('else marks=scratchPattern(surfacePosition,footprint);')) { throw 'Procedural fallback missing' }
if (!$shader.Contains('if(ScratchSize<=0.0) imageScratches=0.0;')) { throw 'Zero size bypass missing' }
Write-Output 'Continuous surface image: no scratch stamps, direct image path and zero-size bypass passed.'
if ($shader.Contains('SurfaceDiagnostic') -or $description.uniforms.name -contains 'SurfaceDiagnostic') { throw 'Diagnostic remains' }
if (!$shader.Contains('imageScratches=clamp(detail*5.0,0.0,1.0)*inside.x*inside.y;') -or $shader.Contains('float threshold=')) { throw 'Image threshold returned' }
if (!$composite.Contains('PrototypeConfig.SURFACE_TEXTURE.get() || scratchDensity>0')) { throw 'Image depends on procedural density' }
Write-Output 'Normal surface: diagnostic removed, continuous image detail and density independence passed.'
