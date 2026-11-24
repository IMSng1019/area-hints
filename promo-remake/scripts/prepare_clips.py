"""从原始实录制作无声、逐帧可定位的 Remotion 剪辑素材。"""

import argparse
import os
import shutil
import subprocess
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from tempfile import TemporaryDirectory


PROJECT = Path(__file__).resolve().parents[1]
OUTPUT = PROJECT / "public/clips"
FPS = 30

# 窗口化录制的上下边缘分别是标题栏和任务栏，交互界面保留完整左右布局。
WINDOW = "crop=1920:1004:0:24,pad=1920:1080:0:38:0x0b151b"
FILM = "setsar=1"
ARRIVAL = "crop=1440:810:240:136,scale=1920:1080:flags=lanczos"
# 运笔时镜头略向画笔移动再收回，保持色块、光标和中央工具栏的关系可见。
BRUSH = "crop=1776:1000:'90+42*sin(3.14159*t/3.8)':26,scale=1920:1080"

# 每段分别记录源文件、起点秒数、输出帧数、播放倍率与重新构图方式。
SHOTS = {
    "harbor-orbit": [("pv1", 2.30, 192, 2.25, FILM)],
    "footsteps": [("pv2", 0, 72, 0.62, FILM)],
    "harbor-push": [("pv3", 1.87, 132, 1, FILM)],
    "arrival": [("第三人称进入璃月港", 8.27, 189, 1, ARRIVAL)],
    "hierarchy": [("多级跳字展示", 0.45, 279, 1, FILM)],
    "story": [("风起地的域名描述展示", 0.38, 165, 1, FILM)],
    "aerial": [("可视化俯瞰环视展示", 0.64, 240, 1, FILM)],
    "bounds": [("boundviz展示", 2.20, 264, 1, WINDOW)],
    "panel": [
        ("可视化指令面板展示", 7.50, 78, 1.12, FILM),
        ("可视化指令面板展示", 17.65, 54, 1, FILM),
        ("可视化指令面板展示", 24.15, 48, 1.22, FILM),
    ],
    "create": [
        ("可视化面板创建", 7.24, 50, 1.17, WINDOW),
        ("可视化面板创建", 17.33, 114, 1.88, BRUSH),
        ("可视化面板创建", 42.90, 47, 1, WINDOW),
        ("可视化面板创建", 54.55, 59, 1, WINDOW),
    ],
}


def find_ffmpeg():
    if "FFMPEG" in os.environ:
        return os.environ["FFMPEG"]
    if shutil.which("ffmpeg"):
        return "ffmpeg"
    return Path.home() / "AppData/Local/Programs/ChatCut/resources/app.asar.unpacked/node_modules/ffmpeg-ffprobe-static/ffmpeg.exe"


def execute(ffmpeg, arguments):
    subprocess.run([ffmpeg, "-hide_banner", "-v", "error", "-y", *arguments], check=True)


def encode_clip(ffmpeg, source, work, name):
    parts = []
    for index, (stem, start, frames, speed, composition) in enumerate(SHOTS[name]):
        part = work / f"{name}-{index}.mp4"
        # 先按原始时间戳变速，再采样到 30 fps；慢镜头保留 60 fps 的原始动作采样。
        filters = f"setpts=(PTS-STARTPTS)/{speed},fps={FPS},{composition},setsar=1,format=yuv420p"
        execute(ffmpeg, [
            "-threads", "3", "-ss", str(start), "-i", source / f"{stem}.mp4",
            "-an", "-filter_threads", "2", "-vf", filters, "-frames:v", str(frames),
            "-c:v", "libx264", "-threads", "4", "-preset", "veryfast", "-crf", "17",
            "-g", "30", "-keyint_min", "30", "-sc_threshold", "0", "-movflags", "+faststart", part,
        ])
        parts.append(part)
    result = OUTPUT / f"{name}.mp4"
    if len(parts) == 1:
        os.replace(parts[0], result)
    else:
        playlist = work / f"{name}.txt"
        playlist.write_text("\n".join(f"file '{p.name}'" for p in parts), encoding="utf-8")
        execute(ffmpeg, ["-f", "concat", "-safe", "0", "-i", playlist, "-c", "copy", "-movflags", "+faststart", result])
    print(f"{result.name}: {sum(shot[2] for shot in SHOTS[name])} frames", flush=True)


def encode_overview(ffmpeg, source):
    # 预合成九宫格，使最终的 Remotion 时间线只需解码一路视频。
    cells = ["arrival", "hierarchy", "story", "panel", "create", "bounds", "aerial", "xaero小地图联动", "bluemap地图联动"]
    inputs = []
    filters = []
    for i, name in enumerate(cells):
        if name in SHOTS:
            inputs += ["-threads", "2", "-i", OUTPUT / f"{name}.mp4"]
        else:
            inputs += ["-loop", "1", "-framerate", "30", "-i", source / f"{name}.png"]
        filters.append(f"[{i}:v]scale=464:260:force_original_aspect_ratio=increase,crop=464:260,setsar=1,fps=30,tpad=stop_duration=1:stop_mode=clone,pad=480:270:8:5:0x101f27[c{i}]")
    layout = "|".join(f"{(i%3)*480}_{(i//3)*270}" for i in range(9))
    filters.append("".join(f"[c{i}]" for i in range(9)) + f"xstack=inputs=9:layout={layout},format=yuv420p[out]")
    execute(ffmpeg, [*inputs, "-filter_complex_threads", "2", "-filter_complex", ";".join(filters),
                     "-map", "[out]", "-frames:v", "180", "-an", "-c:v", "libx264", "-threads", "4",
                     "-preset", "veryfast", "-crf", "17", "-g", "30", "-movflags", "+faststart", OUTPUT / "overview.mp4"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, nargs="?", default=Path("J:/素材"))
    arguments = parser.parse_args()
    ffmpeg = find_ffmpeg()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory(dir=OUTPUT.parent.parent) as directory, ThreadPoolExecutor(2) as pool:
        tasks = [pool.submit(encode_clip, ffmpeg, arguments.source, Path(directory), name) for name in SHOTS]
        for task in tasks:
            task.result()
    encode_overview(ffmpeg, arguments.source)


if __name__ == "__main__":
    main()
