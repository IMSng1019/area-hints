<#
协议字段序列核对：逐文件比较「改动前(HEAD)」与「工作区」的缓冲区读写调用序列。
只统计网络缓冲区真正用到的那几个方法（writeString/writeBoolean/writeInt/writeLong/writeDouble
与 readString/readBoolean/readInt/readLong/readDouble/isReadable），
避免把文件 I/O 的 readAreaData/readDescription 之类误算成协议字段。
#>
$files = git diff --name-only HEAD -- src | Where-Object { $_ -like '*.java' }
$pattern = '\.(writeString|writeBoolean|writeInt|writeLong|writeDouble|readString|readBoolean|readInt|readLong|readDouble|isReadable)\s*\('
$bad = 0
foreach ($f in $files) {
    $beforeLines = git show "HEAD:$f" 2>$null
    if (-not $beforeLines) { continue }
    $before = ($beforeLines -join "`n")
    $after = (Get-Content $f -Raw)
    $b = ([regex]::Matches($before, $pattern) | ForEach-Object { $_.Groups[1].Value }) -join ','
    $a = ([regex]::Matches($after, $pattern) | ForEach-Object { $_.Groups[1].Value }) -join ','
    if ($b -ne $a) {
        $bad++
        'MISMATCH ' + $f
        '   HEAD : ' + $b
        '   现在 : ' + $a
    }
}
if ($bad -eq 0) { 'RESULT: PASS —— ' + $files.Count + ' 个改动文件的协议字段读写序列完全一致' } else { 'RESULT: FAIL —— ' + $bad + ' 个文件序列不一致'; exit 1 }
