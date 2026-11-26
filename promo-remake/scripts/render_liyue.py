"""用指定画面母版和《璃月》的后半段导出卡点试剪。"""

import argparse
from pathlib import Path
from tempfile import NamedTemporaryFile, TemporaryDirectory

import numpy as np

from liyue_music import DURATION, render_music
from prepare_clips import FPS, PROJECT, execute, find_ffmpeg
from score import render_sfx


# 左列为原画面帧，右列为音乐时间（秒）；切镜、跳字和段落起点都吸附到最近的视频帧。
CUES = [
    (0, 0),
    (67, 2.20),       # 石阶下行
    (106, 3.30),      # 脚步特写
    (130, 4.35),      # 第三人称进港
    (274, 8.94),      # 拉回远景
    (405, 13.40),     # 港楼上方浮出“璃月港”
    (480, 15.95),     # 九格实拍
    (690, 22.18),     # 域名跳字
    (960, 31.82),     # 可视化编辑器
    (1380, 45.75),    # 四种边界操作
    (1830, 60.75),    # 实地核对边界
    (2070, 67.18),    # 进服同步
    (2206, 71.47),    # 本地识别
    (2354, 76.82),    # 提交编辑
    (2490, 81.11),    # 同步更新
    (2640, 85.40),    # 性能实测
    (3000, 95.04),    # 风起地手记
    (3210, 101.47),   # 地图联动
    (3450, 110.04),   # 指令概览
    (3600, 114.33),   # 三联画
    (3930, 122.90),   # 制作名单
    (4080, 127.01),   # 标志开始描边
    (4137, 129.42),   # 字标完整显现
    (4230, DURATION),
]
SOURCE = [frame for frame, _ in CUES]
TARGET = [round(seconds * FPS) for _, seconds in CUES]


def picture_filter():
    # 各区间按实际跨度换算帧时长，求和得到连续时间戳；只在最后一次转换到输出帧率。
    spans = []
    for index in range(1, len(CUES)):
        start = SOURCE[index - 1]
        length = SOURCE[index] - start
        target_length = TARGET[index] - TARGET[index - 1]
        spans.append(f"clip(N-{start},0,{length})*{target_length}/{length}")
    clock = "+".join(spans)
    return (f"settb=1/90000,setpts='round(({clock})*90000/{FPS})',fps={FPS},"
            f"tpad=stop=2:stop_mode=clone,trim=end_frame={TARGET[-1]}")


def render(source, picture, output):
    if not picture.is_file():
        raise SystemExit(f"找不到画面母版 {picture}。可用 --picture 指定已有母版。")
    if not source.is_file():
        raise SystemExit(f"找不到原曲 {source}")
    output.parent.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory(dir=output.parent) as temp:
        folder = Path(temp)
        music, effects = folder / "music.wav", folder / "effects.wav"
        render_music(source, music)
        duration = TARGET[-1] / FPS
        render_sfx(effects, map_time=lambda t: np.interp(t * FPS, SOURCE, TARGET) / FPS,
                   duration=duration)
        # 成片从输出目录继承读取权限，避免沿用私有临时目录的 Windows 权限。
        with NamedTemporaryFile(dir=output.parent, suffix=".mp4", delete=False) as file:
            result = Path(file.name)
        try:
            execute(find_ffmpeg(), [
                "-i", picture, "-i", music, "-i", effects,
                "-filter_complex", "[1:a][2:a]amix=inputs=2:weights='0.9 0.6':normalize=0,"
                                   "alimiter=limit=0.88:level=0:latency=1[mix]",
                "-map", "0:v:0", "-map", "[mix]", "-vf", picture_filter(), "-t", f"{duration:.6f}",
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "16", "-pix_fmt", "yuv420p",
                "-colorspace", "bt709", "-color_primaries", "bt709", "-color_trc", "bt709",
                "-c:a", "aac", "-ar", "48000", "-b:a", "320k", "-movflags", "+faststart", result,
            ])
            result.replace(output)
        finally:
            result.unlink(missing_ok=True)
    print(output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("music", type=Path, help="陈致逸、HOYO-MiX《璃月》的 FLAC 文件")
    parser.add_argument("--picture", type=Path, help="指定画面母版路径")
    parser.add_argument("--output", type=Path)
    variants = parser.add_mutually_exclusive_group()
    variants.add_argument("--warm", action="store_true", help="使用 v15 暖色背景的画面母版")
    variants.add_argument("--teal-orange", action="store_true", help="使用 v16 青橙背景的画面母版")
    args = parser.parse_args()
    if args.teal_orange:
        master, filename = "Area-Hints-v16-picture.mp4", "Area-Hints-v16-Liyue-Teal-Orange.mp4"
    elif args.warm:
        master, filename = "Area-Hints-v15-picture.mp4", "Area-Hints-v15-Liyue-Background.mp4"
    else:
        master, filename = "Area-Hints-v12.mp4", "Area-Hints-v13-Liyue.mp4"
    render(args.music, args.picture or PROJECT / "out" / master, args.output or PROJECT / "out" / filename)
