$ErrorActionPreference = 'Stop'
$source = Get-Content (Join-Path $PSScriptRoot '../src/main/java/com/shumai/taczpip/PrototypeConfig.java') -Raw
# Configured 2.2.3 ForgeValue.getComment uses substring(max(indexOf(...))-1).
# Reproduce its failing input without starting Minecraft.
$reproduced = $false
try { $null = 'Range: 0.0 ~ 0.6'.Substring(-1) } catch { $reproduced = $true }
if (!$reproduced) { throw 'Expected original failure was not reproduced' }
$count = 0
foreach ($entry in [regex]::Matches($source, '(?s)\b(\w+)\s*=\s*builder\.((?:"[^"]*"|[^";])*);')) {
    if ($entry.Groups[2].Value -notmatch '\.defineInRange\(') { continue }
    $comment = [regex]::Match($entry.Groups[2].Value, 'comment\("([^"]+)"\)')
    if (!$comment.Success) { throw "Missing numeric config comment: $($entry.Groups[1].Value)" }
    $generated = $comment.Groups[1].Value + "`nRange: 0.0 ~ 0.6"
    $offset = [Math]::Max($generated.IndexOf('Range:'), $generated.IndexOf('Allowed Values:')) - 1
    $suffix = $generated.Substring($offset)
    if ($suffix -ne "`nRange: 0.0 ~ 0.6") { throw 'Incorrect range suffix' }
    $count++
}
if ($count -ne 9) { throw "Expected nine numeric settings, found $count" }
Write-Output "Configured comment regression: old failure reproduced; all $count numeric definitions passed. (Static check, not GUI test.)"
