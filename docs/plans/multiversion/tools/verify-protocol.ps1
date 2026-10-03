<#
协议字段序列核对：逐文件比较「迁移前(HEAD)」与「迁移后」的 writeX/readX 调用序列。
方法名本身编码了字段类型（writeString/writeInt/readBoolean…），序列一致即字段顺序与类型未变。
#>
$files = git diff --name-only HEAD -- src | Where-Object { $_ -like '*.java' }
$bad = 0
foreach ($f in $files) {
    $beforeLines = git show "HEAD:$f"
    if (-not $beforeLines) { continue }
    $before = ($beforeLines -join "`n")
    $after = (Get-Content $f -Raw)
    $b = ([regex]::Matches($before, '\.(write\w+|read\w+)\s*\(') | ForEach-Object { $_.Groups[1].Value }) -join ','
    $a = ([regex]::Matches($after, '\.(write\w+|read\w+)\s*\(') | ForEach-Object { $_.Groups[1].Value }) -join ','
    if ($b -ne $a) {
        $bad++
        'MISMATCH ' + $f
        '   HEAD : ' + $b
        '   现在 : ' + $a
    }
}
if ($bad -eq 0) { 'RESULT: PASS —— ' + $files.Count + ' 个改动文件的 read/write 调用序列完全一致（字段顺序与类型未变）' } else { 'RESULT: FAIL —— ' + $bad + ' 个文件序列不一致'; exit 1 }
