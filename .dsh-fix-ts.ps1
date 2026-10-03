
$block = @(
  '',
  '    /**',
  '     * 兼容占位：历史基线的产物里有 areahint/xaero/minimap/XaeroMinimapBridge$TransformState.class，',
  '     * 为保证 1.20.4 产物与基线逐条一致（D1）而保留；实际逻辑已迁到 src 的 MinimapTransformState。',
  '     */',
  '    public record TransformState(double ps, double pc, double zoom, int halfViewW, int halfViewH, boolean circle) {',
  '    }'
) -join "`r`n"

$fixed = 0
Get-ChildItem versions -Recurse -File -Filter 'XaeroMinimapBridge.java' | Where-Object { $_.FullName -notmatch 'build' } | ForEach-Object {
  $raw = [System.IO.File]::ReadAllText($_.FullName)
  if ($raw -match 'record TransformState') { 'SKIP ' + $_.FullName; return }
  $idx = $raw.LastIndexOf('}')
  if ($idx -lt 0) { 'NO-BRACE ' + $_.FullName; return }
  $out = $raw.Substring(0, $idx) + $block + "`r`n}" + $raw.Substring($idx + 1)
  [System.IO.File]::WriteAllText($_.FullName, $out)
  $fixed++
  'FIXED ' + $_.FullName.Replace((Get-Location).Path + '\','')
}
'共修复 ' + $fixed + ' 个'
