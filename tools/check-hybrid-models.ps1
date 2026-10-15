param([string]$PackRoot, [string]$AuditFile)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$definitions=Get-Content -LiteralPath $AuditFile -Raw | ConvertFrom-Json
foreach ($d in $definitions | Where-Object {$_.Scope -eq $true -and $_.Sight -eq $true}) {
    $parts=$d.Model.Split(':',2)
    $path='assets/'+$parts[0]+'/geo_models/'+$parts[1]+'.json'
    $pack=Get-Item -LiteralPath (Join-Path $PackRoot $d.Pack)
    $text=$null
    if ($pack.PSIsContainer) {
        $file=Join-Path $pack.FullName $path
        if(Test-Path -LiteralPath $file) { $text=[IO.File]::ReadAllText($file) }
    } else {
        $zip=[IO.Compression.ZipFile]::OpenRead($pack.FullName)
        try {
            $entry=$zip.GetEntry($path)
            if($entry) {
                $reader=[IO.StreamReader]::new($entry.Open())
                try {$text=$reader.ReadToEnd()} finally {$reader.Dispose()}
            }
        } finally {$zip.Dispose()}
    }
    $names=@()
    if($text) {
        $j=$text | ConvertFrom-Json
        foreach($property in $j.PSObject.Properties) {
            if($property.Name -eq 'minecraft:geometry') {foreach($geometry in $property.Value) {$names+=@($geometry.bones.name)}}
            elseif($property.Name -like 'geometry.*') {$names+=@($property.Value.bones.name)}
        }
    }
    [pscustomobject]@{Pack=$d.Pack;Model=$d.Model;Found=[bool]$text;OpticalNodes=(@($names | Where-Object {$_ -match '^ocular_scope(_\d+)?$'}) -join ',');SightNodes=(@($names | Where-Object {$_ -match '^ocular(_sight)?(_\d+)?$'}) -join ',');Views=$d.Views}
}
