"""从入港录像中提取片名镜头前景的提灯、草坡与白花。"""

import argparse
import io
import subprocess
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

from prepare_clips import find_ffmpeg


ROOT = Path(__file__).resolve().parents[1]


def get_frame(movie, seconds, crop):
    command = [find_ffmpeg(), "-loglevel", "error", "-ss", str(seconds), "-i", movie,
               "-vf", crop, "-frames:v", "1", "-f", "image2pipe", "-c:v", "png", "pipe:1"]
    return Image.open(io.BytesIO(subprocess.check_output(command))).convert("RGB")


def polygon(size, coordinates):
    points = [tuple(map(int, pair.split(","))) for pair in coordinates.split()]
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).polygon(points, fill=255)
    return np.asarray(mask, dtype=float)


def dusk(source, alpha, lamplit=False):
    rgb = np.asarray(source, dtype=float)
    luma = np.einsum("ijk,k->ij", rgb, [0.213, 0.715, 0.072])
    # 将草叶的日光落入冷青阴影，保留叶面纹理及白花的暖高光。
    shaded = np.array([5, 13, 16]) + luma[:, :, None] * [0.52, 0.67, 0.71]
    shaded += (rgb - luma[:, :, None]) * [0.37, 0.38, 0.33]
    if lamplit:
        y, x = np.ogrid[:source.height, :source.width]
        distance = ((x-666)/58)**2 + ((y-75)/82)**2
        warmth = (np.exp(-distance) * .96)[:, :, None]
        shaded = shaded * (1 - warmth) + rgb * warmth
    layer = Image.fromarray(np.clip(shaded, 0, 255).astype("uint8")).convert("RGBA")
    layer.putalpha(Image.fromarray(alpha.astype("uint8")).filter(ImageFilter.GaussianBlur(.5)))
    return layer


def lantern_bank(source):
    # 绕原画面的提灯、灯台和草地方块外廓落点，远处的天空与海湾逐像素扣除。
    outline = polygon(source.size, """
      0,15 57,14 72,54 151,47 182,60 190,100 214,127 223,104
      256,86 307,81 350,74 407,78 459,82 493,77 521,87 527,131
      553,130 564,115 577,108 590,115 600,110 604,92 617,78
      643,75 650,88 655,81 654,63 659,59 659,55 663,54 665,49
      668,52 669,55 674,58 676,61 677,77 681,79 683,74
      694,74 700,86 703,98 701,113 696,129 690,136 690,147
      684,153 684,161 678,169 681,175 673,179 677,185 672,198
      683,204 684,215 694,215 699,226 701,239 715,245 722,258
      723,273 736,273 732,317 744,319 740,339 752,340 754,357
      777,358 786,352 790,355 787,395 807,397 809,432 829,434
      834,464 850,469 855,493 882,495 888,516 908,520 914,554 0,554
    """)
    rgb = np.asarray(source, dtype=float)
    r,g,b = rgb.transpose(2,0,1)
    y,x = np.ogrid[:source.height,:source.width]
    # 靠天际线的细草叶透出原画面的海水间隙，灯与白花单独保留。
    tips = ((y<129) & (x<650)) | ((y<93) & (x>681))
    green = np.clip((g-b-1)/9,0,1)
    natural = np.where(tips, green, 1)
    return dusk(source, outline * natural, lamplit=True)


def right_flowers(source):
    # 跟随原帧最靠近镜头的白花及两侧草叶，将右下草坡略转后压在画角。
    r,g,b = np.asarray(source, dtype=float).transpose(2,0,1)
    y,x = np.ogrid[:source.height,:source.width]
    green = np.clip((g-b-3)/11,0,1)
    flower = (x>110) & (x<235) & (y>86) & (y<270) & (r>95) & (g>90) & (b>61)
    alpha = 255 * np.maximum(green,flower)
    return dusk(source,alpha)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, nargs="?", default=Path("J:/素材"))
    folder = parser.parse_args().source
    lantern = get_frame(folder/"pv补.mp4", 7.20, "crop=940:554:0:526")
    flowers = get_frame(folder/"pv3.mp4", 5.52, "crop=430:304:280:776")
    bank = Image.new("RGBA", (1920,1080))
    lamp = lantern_bank(lantern).resize((1175,693), Image.Resampling.LANCZOS)
    bank.alpha_composite(lamp, (-696,731))
    right = right_flowers(flowers).rotate(29, Image.Resampling.BICUBIC, expand=True)
    bank.alpha_composite(right, (1564,814))
    bank.save(ROOT/"public/art/brand-bank.webp", lossless=True, method=5)


if __name__ == "__main__":
    main()
