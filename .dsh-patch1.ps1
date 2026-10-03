
$files = @(
 'src/client/java/areahint/commandui/CommandListScreen.java',
 'src/client/java/areahint/commandui/HelpCommandScreen.java',
 'src/client/java/areahint/commandui/DeleteHintVertexScreen.java',
 'src/client/java/areahint/commandui/WizardOptionScreen.java',
 'src/client/java/areahint/commandui/WizardSelectionListScreen.java',
 'src/client/java/areahint/gui/AreasHintConfigScreen.java',
 'src/client/java/areahint/management/client/AreaManagementScreen.java',
 'src/client/java/areahint/soundevent/SoundEventListScreen.java',
 'src/client/java/areahint/soundevent/SoundEventCategoryScreen.java'
)
$changed = 0
foreach ($f in $files) {
    $raw = [System.IO.File]::ReadAllText((Resolve-Path $f))
    $script:nl = if ($raw.Contains("`r`n")) { "`r`n" } else { "`n" }
    $orig = $raw

    $raw = [regex]::Replace($raw, '(?m)^([ \t]*)this\.setRenderBackground\(false\);[ \t]*\r?$', {
        param($m)
        $ind = $m.Groups[1].Value
        $ind + '// 1.20.5 起列表控件不再自绘背景、setRenderBackground 被移除，故只在 1.20.4 及以前调用' + $script:nl +
        $ind + '//? if <=1.20.4 {' + $script:nl +
        $ind + 'this.setRenderBackground(false);' + $script:nl +
        $ind + '//?}'
    })

    $raw = [regex]::Replace($raw, '(?m)^([ \t]*)protected int getScrollbarPositionX\(\) \{[ \t]*\r?$', {
        param($m)
        $ind = $m.Groups[1].Value
        $ind + '// 1.20.5 起 getScrollbarPositionX 改名为 getScrollbarX，方法体共用' + $script:nl +
        $ind + '//? if <=1.20.4 {' + $script:nl +
        $ind + 'protected int getScrollbarPositionX() {' + $script:nl +
        $ind + '//?} else {' + $script:nl +
        $ind + '/*protected int getScrollbarX() {*/' + $script:nl +
        $ind + '//?}'
    })

    if ($raw -ne $orig) {
        [System.IO.File]::WriteAllText((Resolve-Path $f), $raw)
        $changed++
        'patched ' + ($f -replace '.*/','')
    } else {
        'NO-CHANGE ' + ($f -replace '.*/','')
    }
}
'共修改 ' + $changed + ' 个文件'
