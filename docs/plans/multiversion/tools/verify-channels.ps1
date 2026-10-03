<#
通道一致性核对：逐文件比较「迁移前(HEAD)」与「迁移后(工作区)」用到的通道标识多重集。
HEAD 侧的通道可能来自三种写法，全部要解析：
  1) Packets.<常量>            -> 查 Packets.java 的常量表
  2) 文件内自定义的 String 常量 -> 查该文件自己的常量声明
  3) new Identifier("ns:path") -> 字面量
工作区侧统一为 Channel.<常量> -> 查 Channel.java。
#>
$files = git diff --name-only HEAD -- src | Where-Object { $_ -like '*.java' }

$headPackets = (git show HEAD:src/main/java/areahint/network/Packets.java) -join "`n"
$packetsMap = @{}
foreach ($m in [regex]::Matches($headPackets, 'public static final Identifier\s+(\w+)\s*=\s*new Identifier\(\s*Areashint\.MOD_ID\s*,\s*"([^"]+)"\s*\)')) { $packetsMap[$m.Groups[1].Value] = 'areas-hint:' + $m.Groups[2].Value }
foreach ($m in [regex]::Matches($headPackets, 'public static final String\s+(\w+)\s*=\s*Areashint\.MOD_ID \+ ":([^"]+)"')) { $packetsMap[$m.Groups[1].Value] = 'areas-hint:' + $m.Groups[2].Value }

$chanSrc = (Get-Content src/main/java/areahint/network/Channel.java -Raw)
$chanMap = @{}
foreach ($m in [regex]::Matches($chanSrc, 'Channel\s+(\w+)\s*=\s*of\(\s*"([^"]+)"\s*\)')) { $chanMap[$m.Groups[1].Value] = 'areas-hint:' + $m.Groups[2].Value }
foreach ($m in [regex]::Matches($chanSrc, 'Channel\s+(\w+)\s*=\s*of\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)')) { $chanMap[$m.Groups[1].Value] = $m.Groups[2].Value + ':' + $m.Groups[3].Value }

$bad = 0
foreach ($f in $files) {
    $beforeLines = git show "HEAD:$f"
    if (-not $beforeLines) { continue }
    $before = ($beforeLines -join "`n")

    # 文件内自定义的通道字符串常量
    $localMap = @{}
    foreach ($m in [regex]::Matches($before, '(?:static final String|String)\s+(\w+)\s*=\s*"((?:areahint|areashint|areas-hint):[A-Za-z0-9_]+)"')) { $localMap[$m.Groups[1].Value] = $m.Groups[2].Value }

    $b = New-Object System.Collections.ArrayList
    foreach ($m in [regex]::Matches($before, 'Packets\.(\w+)')) { $n = $m.Groups[1].Value; if ($packetsMap.ContainsKey($n)) { [void]$b.Add($packetsMap[$n]) } }
    foreach ($m in [regex]::Matches($before, 'new Identifier\(\s*(\w+)\s*\)')) {
        $n = $m.Groups[1].Value
        if ($localMap.ContainsKey($n)) { [void]$b.Add($localMap[$n]) }
        elseif ($packetsMap.ContainsKey($n)) { [void]$b.Add($packetsMap[$n]) }
    }
    foreach ($m in [regex]::Matches($before, 'new Identifier\(\s*"([^"]+)"\s*\)')) { [void]$b.Add($m.Groups[1].Value) }

    $after = (Get-Content $f -Raw)
    $a = New-Object System.Collections.ArrayList
    foreach ($m in [regex]::Matches($after, 'Channel\.(\w+)')) { $n = $m.Groups[1].Value; if ($chanMap.ContainsKey($n)) { [void]$a.Add($chanMap[$n]) } }

    $bs = (($b | Sort-Object) -join ',')
    $as = (($a | Sort-Object) -join ',')
    if ($bs -ne $as) {
        $bad++
        'MISMATCH ' + $f
        '   HEAD : [' + $bs + ']'
        '   现在 : [' + $as + ']'
    }
}
if ($bad -eq 0) { 'RESULT: PASS —— 所有改动文件的通道多重集与迁移前完全一致（' + $files.Count + ' 个文件）' } else { 'RESULT: FAIL —— ' + $bad + ' 个文件通道不一致'; exit 1 }
