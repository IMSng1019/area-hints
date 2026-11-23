"""以定稿的意群生成 206 秒旁白，并根据每句实际长度写出字幕。"""
import argparse
import csv
import math
import os
import sys
import tempfile
from pathlib import Path

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, resample_poly, sosfilt

HERE = Path(__file__).resolve().parent
RATE = 48000


def clock(second):
    total = round(second * 1000)
    return f"00:{total // 60000:02}:{total // 1000 % 60:02},{total % 1000:03}"


def main():
    install = Path(os.environ.get("TEMP", tempfile.gettempdir())) / "areahint-promo-melo-v1"
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=Path, default=install / "model")
    parser.add_argument("--library", type=Path, default=install / "py")
    parser.add_argument("--audition", action="store_true")
    args = parser.parse_args()
    sys.path.insert(0, str(args.library))
    import sherpa_onnx

    cfg = sherpa_onnx.OfflineTtsConfig()
    cfg.model.vits.model = str(args.model / "model.int8.onnx")
    cfg.model.vits.tokens = str(args.model / "tokens.txt")
    cfg.model.vits.lexicon = str(args.model / "lexicon.txt")
    cfg.model.vits.dict_dir = str(args.model / "dict")
    cfg.model.vits.noise_scale = 0.54
    cfg.model.vits.noise_scale_w = 0.58
    cfg.model.num_threads = 4
    tts = sherpa_onnx.OfflineTts(cfg)
    stem = np.zeros(206 * RATE, dtype=np.float32)
    cues = []
    rows = list(csv.DictReader((HERE / "cues.tsv").read_text(encoding="utf8").splitlines(), delimiter="\t"))
    if args.audition:
        rows = [dict(start=0, end=15, line="璃月港。提瓦特。风啸山坡。Areas Hint。Xaero。")]
    for index, row in enumerate(rows, 1):
        start, end = float(row["start"]), float(row["end"])
        line = row["line"]
        # 专名按词典支持的拼写试读；字幕保留品牌原文。
        spoken = line.replace("Xaero", "zero").replace("“", "").replace("”", "").replace("；", "。")
        speed = 1.02
        for _ in range(5):
            voice = tts.generate(spoken, sid=0, speed=speed)
            sr = voice.sample_rate
            x = np.asarray(voice.samples)
            active = np.flatnonzero(np.abs(x) > max(0.001, np.abs(x).max() * 0.008))
            if not len(active):
                raise ValueError(line)
            x = x[max(0, active[0] - sr // 30):active[-1] + sr // 25 + 1]
            divisor = math.gcd(RATE, sr)
            x = resample_poly(x, RATE // divisor, sr // divisor)
            window = round((end - start) * RATE)
            if len(x) <= window:
                break
            speed *= len(x) / window * 1.03
        else:
            raise ValueError(f"旁白超出时窗 {line}")
        fade = min(len(x) // 2, RATE // 125)
        x[:fade] *= np.linspace(0, 1, fade)
        x[-fade:] *= np.linspace(1, 0, fade)
        sample = round(start * RATE)
        stem[sample:sample + len(x)] = x
        out = min(start + len(x) / RATE + 0.12, end)
        cues.append(f"{index}\n{clock(start)} --> {clock(out)}\n{line}\n")
        print(f"{index:02}  {start:.2f}–{out:.2f}  {speed:.2f}x  {line}", flush=True)
    stem = sosfilt(butter(2, 70, fs=RATE, btype="highpass", output="sos"), stem)
    stem = np.tanh(stem * 1.7)
    stem *= 0.82 / np.abs(stem).max()
    if args.audition:
        wavfile.write(HERE / "audition.wav", RATE, (stem[:sample + len(x)] * 32767).astype(np.int16))
    else:
        # 单独输出双单声道，导入 PR 时与游戏声、音乐共享立体声轨道格式。
        pcm = np.round(stem * 32767).astype(np.int16)
        review = HERE.parent / "review"
        review.mkdir(exist_ok=True)
        wavfile.write(review / "voice-dry.wav", RATE, np.stack([pcm, pcm], axis=-1))
        (HERE / "narration.srt").write_text("\n".join(cues), encoding="utf-8")


if __name__ == "__main__":
    main()
