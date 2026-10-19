$ErrorActionPreference='Stop'
$root=Join-Path $PSScriptRoot '..'
$composite=Get-Content "$root/src/main/java/com/shumai/taczpip/client/ScopeLensComposite.java" -Raw
$config=Get-Content "$root/src/main/java/com/shumai/taczpip/PrototypeConfig.java" -Raw
if (!$config.Contains('defineInRange("surfaceTextureType", 2, 0, 3)')) { throw 'Variant range/default changed' }
if (!$composite.Contains('PrototypeConfig.SURFACE_TEXTURE_TYPE.get()')) { throw 'Selector not bound' }
Add-Type -AssemblyName System.Drawing
$hashes=@()
foreach($name in @('lens_surface','lens_surface_fine','lens_surface_wiped','lens_surface_smudged')) {
    $path=Join-Path $root "src/main/resources/assets/tacz_pip_scope/textures/effect/$name.png"
    if (!$composite.Contains("textures/effect/$name.png")) { throw "Missing resource $name" }
    $bitmap=[System.Drawing.Bitmap]::new($path)
    try { if($bitmap.Width -ne $bitmap.Height -or $bitmap.Width -lt 512){throw "Invalid image dimensions $name"} } finally {$bitmap.Dispose()}
    $metadata=Get-Content "$path.mcmeta" -Raw | ConvertFrom-Json
    if(!$metadata.texture.blur -or !$metadata.texture.clamp){throw "Invalid texture filtering $name"}
    $hashes+=(Get-FileHash -LiteralPath $path).Hash
}
if(($hashes | Select-Object -Unique).Count -ne 4){throw 'Duplicate variant assets'}
foreach($lang in @('ja_jp','en_us')) {
    $labels=Get-Content "$root/src/main/resources/assets/tacz_pip_scope/lang/$lang.json" -Raw -Encoding UTF8 | ConvertFrom-Json
    if(!$labels.'tacz_pip_scope.config.surfaceTextureType' -or !$labels.'tacz_pip_scope.config.surfaceTextureType.tooltip'){throw "Missing label $lang"}
}
Write-Output 'Surface variants: four distinct readable square PNGs, filtering, binding, defaults and translations passed.'
