"""从第三人称入港的原始录屏提取稳定的游戏字模，并保留游戏阴影。"""

import argparse
import subprocess
from pathlib import Path

import numpy as np
from PIL import Image

from prepare_clips import PROJECT, find_ffmpeg


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", nargs="?", type=Path, default=Path("J:/素材/第三人称进入璃月港.mp4"))
    args = parser.parse_args()
    # 1080p 原片三个字的白色笔画分别占 51×16 字模网格中的若干格，每格为 4×4 像素。
    command = [find_ffmpeg(), "-v", "error", "-ss", "10.25", "-i", args.source, "-an",
               "-vf", "fps=8,crop=204:64:856:274,format=gray", "-frames:v", "20",
               "-f", "rawvideo", "pipe:1"]
    frames = np.frombuffer(subprocess.check_output(command), np.uint8).reshape(20, 64, 204)
    # 在停留段取二十帧，按字模格内的中心像素取中位数，去掉移动的背景和编码边缘。
    cells = frames.reshape(20, 16, 4, 51, 4)[:, :, 1:3, :, 1:3]
    strokes = np.median(cells, axis=(0, 2, 4)) > 220
    surface = np.zeros((17, 52, 4), dtype=np.uint8)
    surface[1:, 1:][strokes] = (63, 63, 63, 255)
    surface[:-1, :-1][strokes] = (255, 255, 255, 255)
    image = Image.fromarray(surface).resize((208, 68), Image.Resampling.NEAREST)
    image.save(PROJECT / "public/titles/harbor-popup.png", optimize=True)


if __name__ == "__main__":
    main()
