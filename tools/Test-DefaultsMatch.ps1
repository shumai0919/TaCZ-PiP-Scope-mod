param([Parameter(Mandatory=$true)][string]$ConfigPath)
$ErrorActionPreference='Stop'
$source=Get-Content (Join-Path $PSScriptRoot '../src/main/java/com/shumai/taczpip/PrototypeConfig.java') -Raw
$saved=@{}
foreach($line in Get-Content -LiteralPath $ConfigPath) {
    if($line -match '^\s*(\w+)\s*=\s*(true|false|[0-9.]+)\s*$'){$saved[$Matches[1]]=$Matches[2]}
}
$count=0
foreach($match in [regex]::Matches($source,'\.define(?:InRange)?\("(\w+)",\s*(true|false|[0-9.]+)')) {
    $key=$match.Groups[1].Value; $value=$match.Groups[2].Value
    if(!$saved.ContainsKey($key)){throw "Missing saved key: $key"}
    if($value -in @('true','false')) {$equal=$value -eq $saved[$key]}
    else {$equal=[double]::Parse($value,[cultureinfo]::InvariantCulture) -eq [double]::Parse($saved[$key],[cultureinfo]::InvariantCulture)}
    if(!$equal){throw "Default mismatch: $key code=$value saved=$($saved[$key])"}
    $count++
}
if($count -ne $saved.Count){throw "Key count mismatch: defaults=$count saved=$($saved.Count)"}
Write-Output "All $count registered defaults match the saved configuration (static check)."
