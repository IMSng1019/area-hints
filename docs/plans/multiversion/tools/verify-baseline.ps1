<#
D1 基线核对脚本：确认 1.20.4 产物仍然包含基线的全部 456 个条目，且未预期变化的条目内容逐字节一致。
用法： pwsh -NoProfile -File docs/plans/multiversion/tools/verify-baseline.ps1 [-Jar <新产物路径>] [-AllowChanged <正则>]
判定： 缺失条目 = 0 且「内容变化的条目」全部匹配 -AllowChanged 才算 PASS；新增条目只做报告。
#>
param(
    [string]$Jar = 'versions/1.20.4/build/libs/areas-hint-fabric-1.20.4-4.6.0.jar',
    [string]$Baseline = 'docs/plans/multiversion/probe-logs/baseline-1.20.4.jar',
    [string]$BaselineListing = 'docs/plans/multiversion/probe-logs/baseline-1.20.4-listing.txt',
    [string]$AllowChanged = '^$'
)

Add-Type -AssemblyName System.IO.Compression.FileSystem

function Read-Entries($path) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $path))
    $map = @{}
    foreach ($e in $zip.Entries) {
        $ms = New-Object System.IO.MemoryStream
        $s = $e.Open(); $s.CopyTo($ms); $s.Close()
        $bytes = $ms.ToArray(); $ms.Close()
        $sha = [System.Security.Cryptography.SHA256]::Create().ComputeHash($bytes)
        $map[$e.FullName] = [BitConverter]::ToString($sha).Replace('-', '')
    }
    $zip.Dispose()
    return $map
}

$new = Read-Entries $Jar
$old = Read-Entries $Baseline
$expected = Get-Content $BaselineListing

$missing = @(); $changed = @(); $unexpected = @()
foreach ($name in $expected) {
    if (-not $new.ContainsKey($name)) { $missing += $name; continue }
    if ($name -eq 'META-INF/MANIFEST.MF') { continue }   # manifest 含构建元数据，单独放行
    if ($old.ContainsKey($name) -and $old[$name] -ne $new[$name]) {
        $changed += $name
        if ($name -notmatch $AllowChanged) { $unexpected += $name }
    }
}
$added = @($new.Keys | Where-Object { -not $old.ContainsKey($_) })

'基线条目数 = ' + $expected.Count + '，当前产物条目数 = ' + $new.Count + '，新增条目数 = ' + $added.Count
if ($added.Count -gt 0) { '新增条目：'; $added | Sort-Object | ForEach-Object { '  + ' + $_ } }
if ($missing.Count -gt 0) { '缺失条目：'; $missing | ForEach-Object { '  - ' + $_ } }
if ($changed.Count -gt 0) { '内容变化的基线条目（允许清单 = ' + $AllowChanged + '）：'; $changed | ForEach-Object { '  * ' + $_ } }

if ($missing.Count -eq 0 -and $unexpected.Count -eq 0) {
    'RESULT: PASS —— 基线 ' + $expected.Count + ' 个条目全部存在；内容变化的条目均在允许清单内'
} else {
    'RESULT: FAIL'
    exit 1
}
