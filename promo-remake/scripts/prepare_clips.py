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

# 每段分别记录源文件、起点秒数、输出帧数、播放倍率与重新构图方式。
SHOTS = {
    "harbor-orbit": [("pv1", 2.30, 525, 1, FILM)],
    "footsteps": [("pv2", 0, 45, 1, FILM)],
    "harbor-push": [("pv3", 1.12, 174, 1, FILM)],
    "pv-follow": [("pv补", 2.54, 120, 1, FILM)],
    "arrival": [("第三人称进入璃月港", 8.03, 246, 1, ARRIVAL)],
    "hierarchy": [("多级跳字展示", 0.45, 279, 1, FILM)],
    "story": [
        ("可视化指令面板展示", 0.10, 48, 1, FILM),
        ("风起地的域名描述展示", 0.13, 177, 1, FILM),
    ],
    "aerial": [("可视化俯瞰环视展示", 0.64, 276, 1, FILM)],
    "redstone": [("红石机器", 0.10, 330, 0.65, FILM)],
    "bounds": [("boundviz展示", 2.17, 279, 1, WINDOW)],
    "panel": [
        ("可视化指令面板展示", 7.47, 90, 1, FILM),
        ("可视化指令面板展示", 17.45, 72, 1, FILM),
        ("可视化指令面板展示", 24.10, 66, 1, FILM),
    ],
    "create": [
        ("可视化面板创建", 6.20, 54, 1, WINDOW),
        ("可视化面板创建", 17.10, 102, 1.20, WINDOW),
        # 从画布打开属性直到提交前核对，按原片时间顺序连续，压紧输入框里等候的几秒。
        ("可视化面板创建", 41.30, 65, 1.38, WINDOW),
        ("可视化面板创建", 44.29, 36, 3.60, WINDOW),
        ("可视化面板创建", 48.61, 120, 1.60, WINDOW),
        ("可视化面板创建", 55.01, 43, 0.73, WINDOW),
    ],
}


def find_ffmpeg():
    configured = os.environ.get("FFMPEG")
    if configured and shutil.which(configured):
        return configured
    if shutil.which("ffmpeg"):
        return "ffmpeg"
    app_data = Path(os.environ.get("LOCALAPPDATA", Path.home() / "AppData/Local"))
    return app_data / "Programs/ChatCut/resources/app.asar.unpacked/node_modules/ffmpeg-ffprobe-static/ffmpeg.exe"


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
        filters.append(f"[{i}:v]scale=464:260:force_original_aspect_ratio=increase,crop=464:260,setsar=1,fps=30,pad=480:270:8:5:0x101f27[c{i}]")
    layout = "|".join(f"{(i%3)*480}_{(i//3)*270}" for i in range(9))
    filters.append("".join(f"[c{i}]" for i in range(9)) + f"xstack=inputs=9:layout={layout},format=yuv420p[out]")
    execute(ffmpeg, [*inputs, "-filter_complex_threads", "2", "-filter_complex", ";".join(filters),
                     "-map", "[out]", "-frames:v", "225", "-an", "-c:v", "libx264", "-threads", "4",
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
