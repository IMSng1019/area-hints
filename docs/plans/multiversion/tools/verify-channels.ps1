<#
通道一致性核对：逐文件比较「改动前(HEAD)」与「工作区」用到的通道标识多重集。
两侧都解析：Packets.<常量>（HEAD 版常量表）、Channel.<常量>（工作区常量表）、new Identifier("ns:path")、文件内自定义字符串常量。
判据：集合必须完全相同，否则说明协议通道被改动。
#>
$files = git diff --name-only HEAD -- src | Where-Object { $_ -like '*.java' }

$headPackets = (git show HEAD:src/main/java/areahint/network/Packets.java 2>$null) -join "`n"
$packetsMap = @{}
foreach ($m in [regex]::Matches($headPackets, 'public static final Identifier\s+(\w+)\s*=\s*new Identifier\(\s*Areashint\.MOD_ID\s*,\s*"([^"]+)"\s*\)')) { $packetsMap[$m.Groups[1].Value] = 'areas-hint:' + $m.Groups[2].Value }
foreach ($m in [regex]::Matches($headPackets, 'public static final String\s+(\w+)\s*=\s*Areashint\.MOD_ID \+ ":([^"]+)"')) { $packetsMap[$m.Groups[1].Value] = 'areas-hint:' + $m.Groups[2].Value }

$chanSrc = (Get-Content src/main/java/areahint/network/Channel.java -Raw)
$chanMap = @{}
foreach ($m in [regex]::Matches($chanSrc, 'Channel\s+(\w+)\s*=\s*of\(\s*"([^"]+)"\s*\)')) { $chanMap[$m.Groups[1].Value] = 'areas-hint:' + $m.Groups[2].Value }
foreach ($m in [regex]::Matches($chanSrc, 'Channel\s+(\w+)\s*=\s*of\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)')) { $chanMap[$m.Groups[1].Value] = $m.Groups[2].Value + ':' + $m.Groups[3].Value }

function Collect([string]$text, [hashtable]$packets, [hashtable]$channels) {
    $list = New-Object System.Collections.ArrayList
    $local = @{}
    foreach ($m in [regex]::Matches($text, '(?:static final String|String)\s+(\w+)\s*=\s*"((?:areahint|areashint|areas-hint):[A-Za-z0-9_]+)"')) { $local[$m.Groups[1].Value] = $m.Groups[2].Value }
    foreach ($m in [regex]::Matches($text, 'Packets\.(\w+)')) { $n = $m.Groups[1].Value; if ($packets.ContainsKey($n)) { [void]$list.Add($packets[$n]) } }
    foreach ($m in [regex]::Matches($text, 'Channel\.(\w+)')) { $n = $m.Groups[1].Value; if ($channels.ContainsKey($n)) { [void]$list.Add($channels[$n]) } }
    foreach ($m in [regex]::Matches($text, 'new Identifier\(\s*(\w+)\s*\)')) {
        $n = $m.Groups[1].Value
        if ($local.ContainsKey($n)) { [void]$list.Add($local[$n]) }
        elseif ($packets.ContainsKey($n)) { [void]$list.Add($packets[$n]) }
    }
    foreach ($m in [regex]::Matches($text, 'new Identifier\(\s*"([^"]+)"\s*\)')) { [void]$list.Add($m.Groups[1].Value) }
    return (($list | Sort-Object) -join ',')
}

$bad = 0
foreach ($f in $files) {
    $beforeLines = git show "HEAD:$f" 2>$null
    if (-not $beforeLines) { continue }
    $bs = Collect (($beforeLines -join "`n")) $packetsMap $chanMap
    $as = Collect ((Get-Content $f -Raw)) $packetsMap $chanMap
    if ($bs -ne $as) {
        $bad++
        'MISMATCH ' + $f
        '   HEAD : [' + $bs + ']'
        '   现在 : [' + $as + ']'
    }
}
if ($bad -eq 0) { 'RESULT: PASS —— ' + $files.Count + ' 个改动文件的通道多重集与改动前完全一致' } else { 'RESULT: FAIL —— ' + $bad + ' 个文件通道不一致'; exit 1 }
