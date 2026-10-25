param(
    [Parameter(Mandatory=$true)][string[]]$JarPaths,
    [string]$JavaHome = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
if (!$JavaHome) { throw 'Set JAVA_HOME to Java 17.' }
$root = Split-Path $PSScriptRoot -Parent
$cache = Join-Path $root 'work/gradle-user/caches/modules-2/files-2.1'
$jars = @()
foreach ($module in @('asm', 'asm-tree')) {
    $file = Get-ChildItem (Join-Path $cache "org.ow2.asm/$module") -Recurse -Filter '*.jar' |
        Sort-Object FullName | Select-Object -Last 1
    if (!$file) { throw "Missing $module; build the project first." }
    $jars += $file.FullName
}
$jars += Get-ChildItem (Join-Path $cache 'org.lwjgl') -Recurse -Filter '*.jar' |
    Where-Object { $_.Name -match '^lwjgl(-opengl)?-3\.[\d.]+\.jar$' } |
    Select-Object -ExpandProperty FullName
$cp = $jars -join ';'
$testDir = Join-Path $root 'work/dh-compat/tests'
New-Item -ItemType Directory -Force $testDir | Out-Null
$javac = Join-Path $JavaHome 'bin/javac.exe'
$java = Join-Path $JavaHome 'bin/java.exe'
$sources = @(
    'src/main/java/com/shumai/taczpip/compat/DhAbi.java',
    'src/main/java/com/shumai/taczpip/compat/DhVersionRules.java',
    'src/main/java/com/shumai/taczpip/client/DhModernState.java',
    'src/main/java/com/shumai/taczpip/client/DhTemporalState.java',
    'tools/DhAbiTest.java', 'tools/DhModernStateTest.java'
) | ForEach-Object { Join-Path $root $_ }
& $javac -encoding UTF-8 -cp $cp -d $testDir @sources
if ($LASTEXITCODE) { throw 'DH test compilation failed' }
& $java -cp "$testDir;$cp" DhAbiTest @JarPaths
if ($LASTEXITCODE) { throw 'DH JAR contract test failed' }
& $java -cp "$testDir;$cp" DhModernStateTest
if ($LASTEXITCODE) { throw 'DH modern state test failed' }
# The legacy test uses tiny stubs, never the real Minecraft runtime.
$legacyDir = Join-Path $root 'work/dh-compat/legacy-tests'
$legacySources = @('src/main/java/com/shumai/taczpip/client/DhScopeBuffers.java',
    'tools/dh-buffer-test/DhScopeBuffersTest.java',
    'tools/dh-buffer-test/com/seibel/distanthorizons/core/render/glObject/texture/DhFramebuffer.java',
    'tools/dh-buffer-test/net/minecraft/client/Minecraft.java') | ForEach-Object { Join-Path $root $_ }
& $javac -encoding UTF-8 -d $legacyDir @legacySources
if ($LASTEXITCODE) { throw 'Legacy test compilation failed' }
& $java -cp $legacyDir DhScopeBuffersTest
if ($LASTEXITCODE) { throw 'Legacy bank test failed' }
