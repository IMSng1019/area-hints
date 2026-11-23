# Area Hints 推广片 · Remotion 主工程

主合成 `Area-Hints` 包含 03:26 正片的画面、声音与字幕。实机、17 条独立动画、字幕和三个声音分轨都能在 Studio 时间线上预览。画幅为 1920 × 1080，60 fps，Rec.709。[主合成](Film.jsx) · [封面](Cover.jsx)

## 预览与重渲

本片使用 Node.js 22.18.0、npm 10.9.3、Remotion 4.0.545、React 19.2.0 与 Python 3.13.15。现有本地工程已配好依赖与素材，可在本目录运行以下 PowerShell 命令。转移到其他机器时一起复制 `public/`，再使用 `npm.cmd ci` 安装锁定版本。

```powershell
npm.cmd start
npm.cmd run render
npm.cmd run render -- g10 g11
```

默认导出 `../Area-Hints.mp4` 和 `../Area-Hints-cover.jpg`。指定镜头编号时可以单独导出无声 H.264 到 `../media/`。渲染器自动查找本机 Chrome 或 Edge，其他安装路径可通过 `REMOTION_BROWSER_EXECUTABLE` 指定。FFmpeg 在最后写入播放器章节并复制已编码的音画流。

只调整了声音且字幕和画面都已定稿时，可运行 `node render.mjs Soundtrack` 重新导出同一套 Remotion 混音，再用 `node publish.mjs --refresh-sound` 替换现有主片的声音。

实机剪点和章节位于 `../edit.json`，分镜组件从 `scenes.jsx` 进入。在 Studio 的主合成 Props 中可以分别调节 `voiceGain`、`scoreGain`、`gameGain` 以及 `showCaptions`。修改 `../audio/revision/narration.srt` 或替换三个同名 WAV 后重新启动预览或执行渲染，脚本会同步最新版素材。完整交付目录中已备齐本片使用的图片和音视频。

## 素材与时间映射

`prepare.py` 以子序列的第零秒为原点，从对应的 60 fps 实机中间片提取预览与内嵌视频。交付中的 `public/` 已含原始 Xaero 地图 PNG、实机帧和内嵌实机短片。若需要从当前世界文件重算 `map.json`、重提实机或更换截图，可以运行以下命令。使用 Python 3 和 FFmpeg，可通过 `FFMPEG` 指定 FFmpeg 可执行文件。

```powershell
python prepare.py --media ../media --world 'L:\.minecraft\areas-hint\play.simpfun.cn_方块提瓦特\overworld.json' --screenshot 'C:\Users\郁小悟520\Pictures\Screenshots\屏幕截图 2026-11-21 121230.png'
```

| 合成 | 内容 | 时长 |
|---|---|---|
| g02、g04 | 璃月港脚印与片名、书页提问 | 14、5 秒 |
| g05、g06 | 主城与街区，内嵌真实边界与层级变化 | 12、10 秒 |
| g07–g09 | 村庄命名、绑定键记录、27 点村界 | 8、11、10 秒 |
| g10、g11 | 包围盒、高度与水平射线的两个／一个交点 | 9、9 秒 |
| g13–g15 | 主世界全图、父子链精查、边数 | 10、14、8 秒 |
| g16、g17 | 截图先对齐风起地，再移向璃月港 | 11、7 秒 |
| g18–g20 | 添加签名、默认审批流程、片尾树景 | 12、8、8 秒 |

## 数据复算

`prepare.py` 读取源文件里一级顶点与辅助包围盒，按域名等级、上级、包围盒、高度预筛，再按一级顶点的平均中心到示例点的距离、文件顺序精查。精查使用向正 X 方向的水平射线。在 (3100, 80, −3150) 处，一级预筛含蒙德与璃月，蒙德先命中。第二级命中风啸山坡，第三级命中风起地。三次射线判定分别遍历 217、102、37 条边。轮廓保留原始比例，X 向右、Z 向下。

当前文件共 82 个域名，一级、二级、三级分别为 6、20、56 个。9,041 个一级顶点用于绘制轮廓，328 个二级顶点用于预筛。镜头中的数值是这个坐标的文件复算结果。更新原始数据后应重渲 g13–g16，并同步旁白和字幕。
