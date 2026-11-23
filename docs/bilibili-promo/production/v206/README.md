# Area Hints · 走进提瓦特

## Remotion 主工程

[新版主片](Area-Hints.mp4) · [新版封面](Area-Hints-cover.jpg) · [投稿标题与简介](投稿文案.md) · [润色后的完整旁白稿](audio/revision/旁白稿.md) · [中文字幕](audio/revision/narration.srt)

[Film.jsx](graphics/Film.jsx) 按 [edit.json](edit.json) 编排 20 镜。实机使用可修剪的源片，动画直接引用 React 组件。字幕从新版 SRT 读入，旁白、配乐和游戏声各占一条时间线轨道。用 Remotion Studio 选择 `Area-Hints` 可以修改后预览整片，选择 `Cover` 可以预览封面。[打开与导出说明](graphics/README.md)

本地交付包含 `media/`、`graphics/public/` 与 [新版独立音轨](audio/revision/)。移机时一并复制这些文件。第 29 秒的风起地插画与片头的璃月港插画以实机为参考生成，[图片与生成脚本](graphics/art/)随工程保存。

[第 29 秒转场样片](review/Area-Hints-29s.mp4) 含 26–40 秒的画面、字幕和终混。新版 54 句台词使用本地流萤音色模型合成；详见[声音制作记录](audio/revision/README.md)。投稿文件完整解码得到 12,360 帧，音画时长均为 206.000 秒，9 个章节，AAC 终混 −16.01 LUFS-I、−2.80 dBTP。[成片验收](review/revision/decode.json)

## 首版 Premiere 工程存档

投稿主片为 `Areas-Hint.mp4`。片长 03:26，20 镜，1920 × 1080，逐行 60 fps，Rec.709。片中含中文字幕，播放器支持按 9 个章节跳转。

| 用途 | 本地交付物 |
| --- | --- |
| 投稿 | [主片](Areas-Hint.mp4)、[封面](封面.jpg)、[标题、简介和章节](投稿文案.md) |
| 继续剪辑 | [Premiere 工程](premiere/Areas-Hint-206.prproj)、[XML 交换时间线](premiere/Areas-Hint.xml)、[帧级剪辑表](edit.json) |
| 工程素材 | [8 条实机中间片、定格与 17 条动画](media/)，原始录屏在 `media/source/` |
| 修改动画 | [Remotion 工程与重渲说明](graphics/README.md)，对应素材在 `graphics/public/` |
| 修改声音 | [旁白](audio/Voice.wav)、[配乐](audio/Score.wav)、[游戏声与音效](audio/Game.wav)、[终混](audio/Mix.wav)、[音轨重制说明](audio/README.md) |
| 修改字幕 | [UTF-8 中文字幕](audio/narration.srt)，台词与配音时间窗在 [cues.tsv](audio/cues.tsv) |

搬移到另一台机器时，将 `premiere/`、`media/`、`audio/` 与 `graphics/` 按上述相对位置复制。原始录屏、中间片、动画、WAV、动画输入素材和投稿 MP4 在本机交付，Git 保存制作源码、小型物料及工程文件。

## 投稿文件验收

- 在 Premiere Pro 2026 中重开最终工程并播放。工程含 8 条实机源序列与 1 条总剪序列，总剪含 26 段画面、9 个章节标记、3 条从零点起的独立立体声音轨和 54 条可编辑字幕。播放时左右声道均有输出，35 个文件引用均位于交付目录且在线。
- PR 字幕轨使用默认位置。投稿版由同一剪辑表通过 FFmpeg 导出，字幕位置和颜色按镜头调整，给书页按钮、聊天栏和片尾物品栏留出空间。
- 完整解码 12,360 帧。视频与音频从零点起均为 206 秒，所有视频帧的间隔均为 1/60 秒。最终文件也在独立播放器中以 1 倍速静音播至片尾，播放器报告丢帧 0。文件为 609,064,839 字节，视频平均 23.37 Mbps，字幕已烧录。
- 终混测得 −16.01 LUFS-I、−1.72 dBTP。三条分轨逐采样相加等于 WAV 终混。旁白、终混均逐镜做过语音识别，54 条字幕的时间窗内均检测到人声。专名读音和主观音色仍待人工试听。
- 01:49–01:56 对应创建源 82–89 秒。逐帧对照源片后，420 帧连续，保存前后的聊天记录和变色“村庄”均可见。02:46 从 Xaero 截图的“璃月港”切回同名实机跳字。
- 在 (3100, 80, −3150) 处，复算 82 个域名和 9,041 个一级顶点的真实数据文件。实际精查蒙德 217 条边、风啸山坡 102 条边、风起地 37 条边，合计 356 条边。图中注明“示例坐标 · 数据文件复算”。
- [分镜校片图 1](review/check-1.jpg) · [分镜校片图 2](review/check-2.jpg) · [分镜校片图 3](review/check-3.jpg) · [完整解码结果](review/decode.json)

投稿文件 SHA-256 为 `722c2b24c9cd02456f67ebe43bb6333318c6ecadbe93020963c25d61c44cc481`。地图团队署名待地图方确认，已在投稿文案中留出说明。

## 重建投稿版

在本目录使用 Python 3、Pillow、FFmpeg 和 FFprobe。默认使用本机 ChatCut 的 FFmpeg，或使用 `FFMPEG`、`FFPROBE` 指向自有可执行文件。需要重提实机时，`prepare` 可读取原 Premiere 素材工程。重渲动画或声音的步骤见对应子目录。

```powershell
python -X utf8 master.py prepare
python -X utf8 master.py export
python -X utf8 verify.py
```

导出器按剪辑表及最终 SRT 分镜摆放字幕，最后对画面作全片两遍 H.264 VBR 编码，并封装 48 kHz AAC 终混。中间片编码默认使用 NVENC，也可设置 `AREAS_SOFTWARE_ENCODE=1` 使用 x264。验收脚本完整解码视频与音频，逐帧核对显示时间，并在本地 `review/` 生成响度结果与带时码的校片图。
