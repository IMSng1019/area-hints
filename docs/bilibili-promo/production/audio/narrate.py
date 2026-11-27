"""以独立的普通话句子生成可修改的剪辑旁白和画面时间字幕。"""

import argparse
import csv
import math
import os
from pathlib import Path

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, resample_poly, sosfilt
import sherpa_onnx


HERE = Path(__file__).resolve().parent
RATE = 48000
DURATION = 185


def timecode(seconds):
    milliseconds = round(seconds * 1000)
    hours, remainder = divmod(milliseconds, 3600000)
    minutes, remainder = divmod(remainder, 60000)
    seconds, remainder = divmod(remainder, 1000)
    return f"{hours:02}:{minutes:02}:{seconds:02},{remainder:03}"


def trim_quiet(samples, rate):
    # 按包络剪去模型在前后生成的长静音，保留轻辅音的起落。
    active = np.flatnonzero(np.abs(samples) > max(0.001, np.max(np.abs(samples)) * 0.008))
    if not len(active):
        raise ValueError("语音模型返回了无声片段")
    margin = int(rate * 0.045)
    return samples[max(0, active[0] - margin):active[-1] + margin + 1]


def synthesize(tts, text, max_duration):
    speed = 1.0
    for _ in range(5):
        generated = tts.generate(text, sid=0, speed=speed)
        samples = trim_quiet(np.asarray(generated.samples), generated.sample_rate)
        if len(samples) / generated.sample_rate <= max_duration:
            break
        speed *= (len(samples) / generated.sample_rate) / max_duration * 1.05
    else:
        raise ValueError(f"旁白超出镜头长度: {text}")
    divisor = math.gcd(RATE, generated.sample_rate)
    result = resample_poly(samples, RATE // divisor, generated.sample_rate // divisor)
    fade = min(len(result) // 2, round(RATE * 0.008))
    result[:fade] *= np.linspace(0, 1, fade)
    result[-fade:] *= np.linspace(1, 0, fade)
    return result, speed


def make_voice(model, output):
    config = sherpa_onnx.OfflineTtsConfig()
    config.model.vits.model = str(model / "model.int8.onnx")
    config.model.vits.tokens = str(model / "tokens.txt")
    config.model.vits.lexicon = str(model / "lexicon.txt")
    config.model.vits.dict_dir = str(model / "dict")
    config.model.num_threads = 3
    config.model.vits.noise_scale = 0.52
    config.model.vits.noise_scale_w = 0.57
    tts = sherpa_onnx.OfflineTts(config)
    stem = np.zeros(DURATION * RATE, dtype=np.float32)
    cues = []
    with (HERE / "cues.tsv").open(encoding="utf-8") as source:
        for index, cue in enumerate(csv.DictReader(source, delimiter="\t"), 1):
            start, stop = float(cue["start"]), float(cue["end"])
            line = cue["line"]
            sample, speed = synthesize(tts, line, stop - start)
            frame = round(start * RATE)
            stem[frame:frame + len(sample)] = sample
            # 字幕和音频共用同一入点，句尾额外留出一瞬阅读时间。
            end = min(stop, start + len(sample) / RATE + 0.14)
            cues.append(f"{index}\n{timecode(start)} --> {timecode(end)}\n{line}\n")
            print(f"{index:02d} {start:6.2f}–{end:6.2f}  {speed:.2f}x {line}", flush=True)
    stem = sosfilt(butter(2, 72, fs=RATE, btype="highpass", output="sos"), stem)
    # 轻压峰值，在导出混音时保留余量。
    stem = np.tanh(stem * 1.8)
    stem *= 0.84 / np.max(np.abs(stem))
    output.mkdir(exist_ok=True, parents=True)
    wavfile.write(output / "Voice.wav", RATE, np.round(stem * 32767).astype(np.int16))
    (output / "narration.srt").write_text("\n".join(cues), encoding="utf-8-sig")


if __name__ == "__main__":
    cli = argparse.ArgumentParser()
    cli.add_argument("--model", type=Path, required=True)
    cli.add_argument("--output", type=Path, default=HERE.parent / "output")
    args = cli.parse_args()
    make_voice(args.model.resolve(), args.output.resolve())
