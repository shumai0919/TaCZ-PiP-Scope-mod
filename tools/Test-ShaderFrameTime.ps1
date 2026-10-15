$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$mixin = Get-Content "$root/src/main/java/com/shumai/taczpip/mixin/ShaderWorldStartMixin.java" -Raw
$renderer = Get-Content "$root/src/main/java/com/shumai/taczpip/client/ScopeFramebufferPrototype.java" -Raw
foreach ($check in @(
    ($mixin.Contains('beforeShaderWorld(pose, partialTick, time, camera, projection)')),
    ($renderer.Contains('renderWorld(copy, partialTick, frameStartTime, camera, projection)')),
    ($renderer.Contains('renderLevel(view, partialTick, frameStartTime, false')),
    ($renderer.Contains('renderWorld(view, event.getPartialTick(), Util.getNanos()'))
)) {
    if (!$check) { throw 'Frame timestamp forwarding regression' }
}

# Model Oculus 1.8.0 Timer.beginFrame, including its hourly reset.
# The scope context restores the previous main timer after its nested render.
function Advance-Timer([float]$counter, [long]$previous, [long]$current) {
    $milliseconds = [Math]::Truncate(($current - $previous) / 1000000.0)
    [float]$next = $counter + [float]($milliseconds / 1000.0)
    if ($next -ge 3600.0) { return [float]0 }
    return $next
}
[float]$counter = 1
[long]$previous = 1000000000
for ($frame = 0; $frame -lt 120; $frame++) {
    [long]$current = $previous + 16000000
    $scope = Advance-Timer $counter $previous $current
    $main = Advance-Timer $counter $previous $current
    $oldScope = Advance-Timer $counter $previous ($current + 7200000000000)
    if ($scope -ne $main -or $scope -le $counter -or $oldScope -ne 0) {
        throw "Timer regression at frame $frame"
    }
    $counter = $main
    $previous = $current
}
Write-Output 'PASS: timestamp forwarding; 120 simulated frames advance equally; old mixed-clock path resets.'
