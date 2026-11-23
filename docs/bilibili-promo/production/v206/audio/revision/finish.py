"""清理逐句配音和环境声，输出可在 Remotion 以单位增益直接混合的分轨。"""
import argparse
import csv
import math
import os
import re
import shutil
import subprocess
from pathlib import Path

import numpy as np
from scipy.io import wavfile
from scipy.ndimage import maximum_filter1d
from scipy.signal import butter, resample_poly, sosfilt
from pronunciation import cache_key, segments

HERE = Path(__file__).resolve().parent
WORK = HERE.parents[1] / "review/voice-revision"
RATE = 48000
LENGTH = 206 * RATE
BIN = Path(os.environ.get("LOCALAPPDATA", "")) / "Programs/ChatCut/resources/app.asar.unpacked/node_modules/ffmpeg-ffprobe-static"
FF = os.getenv("FFMPEG") or shutil.which("ffmpeg") or str(BIN / "ffmpeg.exe")


def filter_audio(pcm, filters, channels=1):
    fmt = ["-ar", str(RATE), "-ac", str(channels), "-f", "f32le"]
    proc = subprocess.run([FF, "-loglevel", "error", *fmt, "-i", "-", "-af", filters, *fmt, "-"],
                          input=pcm.astype(np.float32).tobytes(), stdout=subprocess.PIPE, check=True)
    return np.frombuffer(proc.stdout, dtype=np.float32).copy().reshape(-1, channels).squeeze()


def clock(second):
    ms = round(second * 1000)
    return f"00:{ms//60000:02d}:{ms//1000%60:02d},{ms%1000:03d}"


def fit_voice(locked):
    narration = np.zeros(LENGTH, dtype=np.float32)
    captions = []
    cues = list(csv.DictReader((HERE / "cues.tsv").read_text(encoding="utf8").splitlines(), delimiter="\t"))
    fixed = {}
    if locked:
        for block in locked.read_text(encoding="utf-8-sig").strip().split("\n\n"):
            num, timing, *words = block.splitlines()
            finish = timing.split(" --> ")[1]
            h,m,s,ms = map(int,re.split("[:,]",finish))
            fixed[int(num)] = (h*3600+m*60+s+ms/1000, "".join(words))
    for idx, cue in enumerate(cues, 1):
        start, end = map(float, (cue["start"], cue["end"]))
        phrase = cue["line"]
        if idx in (6,21,41) and fixed:
            end, approved = fixed[idx]
            if approved != phrase:
                raise ValueError(f"已锁定的第 {idx} 条字幕与配音稿不同")
        digest = cache_key(segments(phrase))
        sr, raw = wavfile.read(WORK / f"{idx:02d}-{digest}.wav")
        raw = raw.astype(np.float32) / 32768
        div = math.gcd(sr, RATE)
        pcm = resample_poly(raw, RATE//div, sr//div)
        # 对语音首尾留出辅音余量，只裁掉边缘空白。
        hits = np.flatnonzero(np.abs(pcm) > max(0.002, np.abs(pcm).max()*0.006))
        pcm = pcm[max(0,hits[0]-RATE//30):min(len(pcm),hits[-1]+RATE//24)]
        window = round((end-start)*RATE)
        speed = max(1, (len(pcm)+RATE//40)/window)
        if speed > 1.01:
            pcm = filter_audio(pcm, f"atempo={speed:.6f}")
        if len(pcm) > window:
            raise ValueError(f"第 {idx} 句越界，请重生成该句")
        pcm = filter_audio(pcm, "highpass=f=85,lowpass=f=11500")
        ramp = min(RATE//140, len(pcm)//2)
        pcm[:ramp] *= np.linspace(0,1,ramp)
        pcm[-ramp:] *= np.linspace(1,0,ramp)
        # 按活动语音部分的小幅度增益调整句间音量，保留句内语气变化。
        active = pcm[np.abs(pcm)>0.015]
        level = np.sqrt(np.mean(active*active))
        pcm *= np.clip(0.155/level, 0.67, 1.45)
        pos = round(start*RATE)
        narration[pos:pos+len(pcm)] = pcm
        stop = min(end, start+len(pcm)/RATE+.08)
        captions.append(f"{idx}\n{clock(start)} --> {clock(stop)}\n{phrase}\n")
        print(f"{idx:02d} {len(pcm)/RATE:.2f}s ×{speed:.2f} {phrase}", flush=True)
    if locked:
        (HERE/"narration.srt").write_bytes(locked.read_bytes())
    else:
        (HERE/"narration.srt").write_text("\n".join(captions),encoding="utf8")
    return np.stack([narration,narration],axis=1)


def stereo(name):
    sr, pcm = wavfile.read(HERE.parent/(name+".wav"))
    if sr != RATE or pcm.shape != (LENGTH,2):
        raise ValueError(name)
    return pcm.astype(np.float32)/32768


def meter(name, pcm):
    path = WORK/(name+"-meter.wav")
    wavfile.write(path, RATE, pcm)
    proc = subprocess.run([FF, "-hide_banner", "-nostdin", "-i", str(path), "-af",
        "loudnorm=I=-16:TP=-1.2:print_format=json", "-f", "null", "-"], stderr=subprocess.PIPE, check=True)
    payload = proc.stderr.decode(errors="replace")
    return tuple(float(re.search(f'"{key}" : "([^"]+)"', payload).group(1)) for key in ("input_i","input_tp"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--lock-srt", type=Path, help="沿用已经渲染的字幕时码并将发音修订限于对应时间窗")
    voice = fit_voice(parser.parse_args().lock_srt)
    score = stereo("Score")
    # 游戏轨在无旁白的间隙也含风声，温和去底噪，再留住脚步和界面反馈。
    game = filter_audio(stereo("Game"), "highpass=f=65,afftdn=nr=9:nf=-47,lowpass=f=9200,volume=-4dB", 2)
    stems = {"Voice": voice, "Score": score, "Game": game}
    for name, target in (("Voice",-16.2),("Score",-30.5)):
        level, _ = meter(name,stems[name])
        stems[name] *= 10**((target-level)/20)
    combined = sum(stems.values())
    level, _ = meter("sum",combined)
    gain = 10**((-16-level)/20)
    peak = (np.max(abs(combined),axis=1)*gain).reshape(-1,240).max(axis=1)
    envelope = np.minimum(1, 0.83/np.maximum(maximum_filter1d(peak,7),1e-6))
    envelope = np.interp(np.arange(LENGTH), np.arange(len(envelope))*240+120,envelope).astype(np.float32)
    total = np.zeros((LENGTH,2),dtype=np.int32)
    for name, data in stems.items():
        pcm = np.rint(data*gain*envelope[:,None]*32767).astype(np.int16)
        total += pcm
        wavfile.write(HERE/(name+".wav"),RATE,pcm)
    if np.max(abs(total))>=32767:
        raise ValueError("混音溢出")
    wavfile.write(HERE/"Mix.wav",RATE,total.astype(np.int16))
    print("终混响度及真峰值", meter("final",total.astype(np.float32)/32768))


if __name__ == "__main__":
    main()
