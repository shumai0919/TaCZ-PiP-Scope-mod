param([Parameter(Mandatory=$true)][string]$PackRoot)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$rows = [System.Collections.Generic.List[object]]::new()
function Inspect-Json($pack, $path, $text) {
    if ($path -notmatch '/(display|data)/' -or $path -notmatch '\.json$') { return }
    try { $j = $text | ConvertFrom-Json } catch { return }
    if ($j.scope -or $j.sight -or $j.builtin_attachments) {
        $rows.Add([pscustomobject]@{Pack=$pack; Path=$path; Scope=$j.scope; Sight=$j.sight; Model=$j.model; Zoom=($j.zoom -join ','); Views=($j.views -join ','); Builtin=($j.builtin_attachments | ConvertTo-Json -Compress)})
    }
}
foreach($pack in Get-ChildItem -LiteralPath $PackRoot) {
    if ($pack.PSIsContainer) {
        foreach($file in Get-ChildItem -LiteralPath $pack.FullName -Recurse -Filter '*.json') {
            $relative=$file.FullName.Substring($pack.FullName.Length).Replace('\','/')
            if ($relative -match '/(display|data)/') { Inspect-Json $pack.Name $relative ([IO.File]::ReadAllText($file.FullName)) }
        }
    } elseif ($pack.Extension -eq '.zip') {
        $zip=[IO.Compression.ZipFile]::OpenRead($pack.FullName)
        try {
            foreach($entry in $zip.Entries) {
                if ($entry.FullName -match '/(display|data)/' -and $entry.FullName -match '\.json$') {
                    $reader=[IO.StreamReader]::new($entry.Open())
                    try { Inspect-Json $pack.Name $entry.FullName $reader.ReadToEnd() } finally { $reader.Dispose() }
                }
            }
        } finally { $zip.Dispose() }
    }
}
$rows | ConvertTo-Json -Depth 6
