"""从璃月港环拍的两个机位提取片名镜头的雾山与林坡。"""

import argparse
import io
import subprocess
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

from prepare_clips import find_ffmpeg


PROJECT = Path(__file__).resolve().parents[1]
SIZE = (1920, 1080)


def exposure(start, end, value):
    t = np.clip((value - start) / (end - start), 0, 1)
    return t * t * (3 - 2 * t)


def frame(source, second):
    raw = subprocess.check_output([
        find_ffmpeg(), "-v", "error", "-ss", str(second), "-i", source,
        "-frames:v", "1", "-f", "image2pipe", "-c:v", "png", "-",
    ])
    im = Image.open(io.BytesIO(raw)).convert("RGB")
    if im.size != SIZE:
        raise ValueError("请使用 1920×1080 的 pv1.mp4 原录屏")
    return im


def ridge(source, skyline, palette, levels, foot, spread, blur):
    # 轮廓以裁后原帧的像素为坐标，雾气较厚的位置保留柔边。
    h, w = source.height, source.width
    x, y = np.arange(w)[None, :], np.arange(h)[:, None]
    rim = np.interp(x[0], *zip(*skyline))[None, :]
    matte = exposure(-blur, blur, y - rim) * (1 - exposure(*foot, y))
    matte *= (1 - exposure(*spread, x))
    gray = np.array(source.convert("L"), dtype=np.float32)
    shade = exposure(*levels, gray)
    dark, light = (np.array(color) for color in palette)
    rgb = dark + (light - dark) * shade[:, :, None]
    layer = Image.fromarray(rgb.astype("uint8")).convert("RGBA")
    layer.putalpha(Image.fromarray((matte * 255).astype("uint8")))
    return layer


def place_and_save(layer, label, scale, position, sky=None):
    bounds = tuple(round(v * scale) for v in layer.size)
    im = layer.resize(bounds, Image.Resampling.LANCZOS)
    canvas = sky if sky is not None else Image.new("RGBA", SIZE)
    canvas.alpha_composite(im, dest=position)
    canvas.save(PROJECT / "public/art" / f"brand-{label}-ridge.webp", lossless=True, method=5)


def cloud_light(source):
    # 用湖面机位上方的云层还原天光，左右和上下都渐隐，单帧里保留原有云量变化。
    sky = frame(source, 1.12).crop((0, 0, 1080, 300))
    sky = sky.resize((1470, 435), Image.Resampling.LANCZOS)
    gray = np.asarray(sky.convert("L").filter(ImageFilter.GaussianBlur(4)), dtype=np.float32)
    x, y = np.arange(sky.width)[None, :], np.arange(sky.height)[:, None]
    cloud = exposure(112, 201, gray)
    haze = np.exp(-((y - 260) / 147) ** 2)
    alpha = (.33 + cloud * .32) * haze * (1 - exposure(790, 1400, x))
    alpha *= exposure(0, 80, y) * (1 - exposure(345, 435, y))
    colors = np.array([45,62,70]) + cloud[:, :, None] * np.array([38,40,33])
    patch = Image.fromarray(colors.astype("uint8")).convert("RGBA")
    patch.putalpha(Image.fromarray((255 * alpha).astype("uint8")))
    canvas = Image.new("RGBA", SIZE)
    canvas.alpha_composite(patch, dest=(-50,40))
    return canvas


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, nargs="?", default=Path("J:/素材/pv1.mp4"))
    video = parser.parse_args().source

    # 后排留下山顶独树、第二道树脊和雾里的谷口，画幅右侧逐渐透出墨蓝底。
    far = frame(video, 19.5).crop((850, 0, 1920, 470))
    far_line = [(0,164),(15,170),(33,200),(42,213),(50,159),(54,146),(58,140),
                (66,121),(75,111),(101,100),(112,95),(116,81),(124,74),(150,67),
                (166,66),(178,62),(182,42),(190,40),(194,25),(202,20),(213,26),
                (220,46),(216,60),(246,81),(249,110),(253,183),(265,224),
                (301,246),(318,257),(340,272),(362,276),(401,270),(427,263),
                (452,225),(471,194),(507,186),(553,178),(563,167),(572,146),
                (583,133),(590,131),(601,145),(614,137),(628,124),(647,114),
                (667,103),(692,92),(697,82),(704,74),(714,70),(725,73),
                (724,84),(749,85),(753,100),(753,167),(769,209),(788,222),
                (806,207),(821,181),(845,153),(857,139),(896,129),(914,113),
                (941,96),(964,88),(979,81),(1006,78),(1031,65),(1049,50),(1069,29)]
    far = ridge(far, far_line, [(34,51,59),(83,101,105)], (110,190),
                (254,447), (770,1048), 3.8)
    place_and_save(far.filter(ImageFilter.GaussianBlur(1.4)), "far", 1.30, (-60,158),
                   cloud_light(video.with_name("pv3.mp4")))

    # 中景取港城背后的林坡，保留树冠与山脚石壁，底部在前一排屋檐后自然隐去。
    mid = frame(video, 9).crop((0, 220, 875, 590))
    mid_line = [(0,48),(21,55),(65,57),(92,52),(119,57),(140,65),(174,60),
                (215,67),(245,70),(268,81),(293,79),(315,83),(347,84),
                (390,86),(405,87),(411,70),(420,73),(440,88),(459,97),
                (477,97),(482,73),(491,74),(502,80),(509,92),(508,100),
                (538,106),(563,101),(587,112),(597,124),(631,118),(663,108),
                (676,93),(693,91),(700,98),(708,87),(724,87),(746,97),
                (763,111),(778,124),(781,135),(801,138),(818,155),(832,142),
                (848,145),(856,159),(874,147)]
    mid = ridge(mid, mid_line, [(17,36,41),(52,75,75)], (50,183),
                (228,351), (725,868), 2.6)
    place_and_save(mid.filter(ImageFilter.GaussianBlur(.65)), "mid", 2.0, (-566,251))


if __name__ == "__main__":
    main()
