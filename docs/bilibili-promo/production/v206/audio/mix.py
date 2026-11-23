"""制作游戏与手制音效轨，按共同峰值包络导出 PR 可直接相加的三个分轨。"""
import json
import os
import re
import shutil
import subprocess
from pathlib import Path

import numpy as np
from scipy.io import wavfile
from scipy.ndimage import maximum_filter1d

HERE = Path(__file__).resolve().parent
MEDIA = HERE.parent / "media"
REVIEW = HERE.parent / "review"
RATE = 48000
FRAMES = 206 * RATE
BIN = Path(os.environ.get("LOCALAPPDATA", "")) / "Programs/ChatCut/resources/app.asar.unpacked/node_modules/ffmpeg-ffprobe-static"
FF = os.getenv("FFMPEG") or shutil.which("ffmpeg") or str(BIN / "ffmpeg.exe")

# 内嵌实机也从对应子序列的原速音频入点起剪，直接实机段从主剪辑表读取。
WINDOWS = [
    (42, 46, "可视化俯瞰环视展示", 6), (52, 62, "多级跳字展示", 0.2),
    (63.5, 65.5, "指令展示", 16), (65.5, 70, "创建域名展示", 17),
    (70, 74, "创建域名展示", 23.5), (74, 81, "创建域名展示", 33),
    (84, 87, "创建域名展示", 58), (87, 91, "创建域名展示", 76),
    (170, 173, "可视化指令面板展示", 3), (173, 182, "可视化指令面板展示", 27),
]


def game_stem():
    track = np.zeros((FRAMES, 2), dtype=np.float32)
    windows = WINDOWS.copy()
    edl = json.loads((HERE.parent / "edit.json").read_text(encoding="utf8"))
    for cut in edl["clips"]:
        name = Path(cut["path"]).stem
        if name.isascii():
            continue
        start = cut["start"] / edl["fps"]
        windows.append((start, start + cut["duration"] / edl["fps"], name, cut["in"] / edl["fps"]))
    for start, end, name, offset in windows:
        count = round((end - start) * RATE)
        packet = subprocess.check_output([FF, "-v", "error", "-ss", str(offset), "-i", str(MEDIA / f"{name}.mp4"),
            "-t", str(end - start), "-map", "0:a:0", "-ac", "2", "-ar", str(RATE), "-f", "f32le", "-"])
        pcm = np.frombuffer(packet, dtype=np.float32).reshape(-1, 2)[:count].copy()
        if count - len(pcm) > RATE // 20:
            raise ValueError(f"{name} 游戏声缺失 {count - len(pcm)} 个采样")
        # 游戏独立录音轨电平偏低；放大后用软限幅留住脚步和界面点击的瞬态。
        pcm = 0.44 * np.tanh(pcm * (23 if start == 0 else 18))
        fade = RATE // 40
        pcm[:fade] *= np.linspace(0, 1, fade)[:, None]
        pcm[-fade:] *= np.linspace(1, 0, fade)[:, None]
        first = round(start * RATE)
        track[first:first + len(pcm)] += pcm
    for start, pitch, amplitude in [(75, 510, 0.065), (101.2, 784, 0.10), (102.2, 784, 0.10)]:
        t = np.arange(round(0.36 * RATE)) / RATE
        tone = (np.sin(2 * np.pi * t * pitch) + 0.22 * np.sin(2 * np.pi * t * pitch * 2.72))
        ping = amplitude * tone * (1 - np.exp(-800 * t)) * np.exp(-17 * t)
        first = round(start * RATE)
        track[first:first + len(t)] += ping[:, None]
    return track


def measure(path):
    out = subprocess.run([FF, "-nostdin", "-hide_banner", "-i", str(path), "-af",
        "loudnorm=I=-16:TP=-1.2:LRA=11:print_format=json", "-f", "null", "-"],
        stderr=subprocess.PIPE, check=True)
    stats = json.loads(re.search(rb"\{[^{}]*input_i[^{}]*\}", out.stderr).group())
    return float(stats["input_i"]), float(stats["input_tp"])


def mix():
    REVIEW.mkdir(exist_ok=True)
    tracks = {}
    for name, target in (("Voice", -16.5), ("Score", -29)):
        dry = REVIEW / f"{name.lower()}-dry.wav"
        level, peak = measure(dry)
        rate, data = wavfile.read(dry)
        if rate != RATE or data.shape != (FRAMES, 2):
            raise ValueError(f"{dry.name} 的声道、采样率或采样数有误")
        tracks[name] = data.astype(np.float32) * (10 ** ((target - level) / 20) / 32768)
    tracks["Game"] = game_stem()
    combined = sum(tracks.values())
    temp = REVIEW / "pre-master.wav"
    wavfile.write(temp, RATE, combined)
    level, peak = measure(temp)
    gain = 10 ** ((-16 - level) / 20)
    # 两侧联动的短时间峰值包络共同作用于分轨，保持 PR 内三个推子置零时的混音。
    peaks = (np.max(np.abs(combined), axis=1) * gain).reshape(-1, 240).max(axis=1)
    envelope = np.minimum(1.0, 0.82 / np.maximum(maximum_filter1d(peaks, 7), 1e-7))
    env = np.interp(np.arange(FRAMES), np.arange(len(envelope)) * 240 + 120, envelope).astype(np.float32)
    pcm_sum = np.zeros((FRAMES, 2), dtype=np.int32)
    for name, data in tracks.items():
        pcm = np.rint(data * (gain * 32767) * env[:, None]).astype(np.int16)
        pcm_sum += pcm
        wavfile.write(HERE / f"{name}.wav", RATE, pcm)
    assert np.max(np.abs(pcm_sum)) < 32767
    wavfile.write(HERE / "Mix.wav", RATE, pcm_sum.astype(np.int16))
    for name in [*tracks, "Mix"]:
        level, peak = measure(HERE / f"{name}.wav")
        print(f"{name:6} {level:5.2f} LUFS-I   {peak:5.2f} dBTP")
        if name == "Mix" and (abs(level + 16) > 0.6 or peak > -1.0):
            raise ValueError("终混响度或真峰值超出交付范围")


if __name__ == "__main__":
    mix()
