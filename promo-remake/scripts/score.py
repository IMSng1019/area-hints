"""为 84 秒剪辑重写器乐、动作音和声像，运行本文件可生成 public/score.m4a。"""

import importlib.util
import subprocess
from pathlib import Path
from tempfile import TemporaryDirectory

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

from prepare_clips import find_ffmpeg


PROJECT = Path(__file__).resolve().parents[1]
INSTRUMENTS = PROJECT.parent / "docs/bilibili-promo/production/v206/audio/score.py"
spec = importlib.util.spec_from_file_location("instruments", INSTRUMENTS)
instruments = importlib.util.module_from_spec(spec)
spec.loader.exec_module(instruments)
DURATION = 84
RATE = instruments.RATE
mix = instruments.mix = np.zeros((DURATION * RATE, 2), dtype=np.float32)
rng = np.random.default_rng(472)
string, bow, tap = instruments.string, instruments.bow, instruments.tap


def noise(seconds, low, high):
    # 带通限制脚步与风声频段，避免与拨弦的旋律相互遮盖。
    source = rng.standard_normal(round(seconds * RATE))
    return sosfilt(butter(2, [low, high], fs=RATE, btype="bandpass", output="sos"), source)


def step(at, strength, pan):
    t = np.arange(round(.26 * RATE)) / RATE
    heel = np.sin(2 * np.pi * (62 * t + .8 * (1 - np.exp(-22*t)))) * np.exp(-25*t)
    grit = noise(.26, 540, 3500) * (np.exp(-32*t) + .28*np.exp(-44*np.abs(t-.065)))
    instruments.write_note(at, (heel + grit*.78) * (1 - np.exp(-480*t)) * strength, pan)


def swell(at, duration, strength):
    # 转场的低通噪声在切点前抬起，尾端略缓收。
    n = round(duration * RATE)
    progress = np.arange(n) / n
    env = np.minimum(progress/.83, (1-progress)/.17).clip(0, 1)**1.5
    for pan in (-.66, .66):
        instruments.write_note(at, noise(duration, 230, 3200) * env * strength, pan)


def chime(at, note, strength, pan=0):
    t = np.arange(round(2.5*RATE)) / RATE
    f = instruments.frequency(note)
    # 木片与金属的不同衰减速度使跨界反馈既短促又有可辨的音高。
    tone = np.zeros(len(t))
    for ratio, amplitude, decay in [(1, 1, 3.6), (2.01, .28, 5), (3.98, .18, 8.2), (5.46, .13, 11)]:
        tone += np.sin(2*np.pi*f*ratio*t) * amplitude * np.exp(-decay*t)
    instruments.write_note(at, tone * (1-np.exp(-225*t)) * strength, pan)


# 低声部依次取 E、G、C、A、D，旋律反复出现 B–D–E 三音。
HARMONY = [(40, 55, 59, 64), (43, 55, 62, 67), (36, 55, 60, 64),
           (45, 57, 60, 64), (38, 54, 57, 62)]


def bar(at, length, chord, level, motion):
    bass, low, middle, top = HARMONY[chord]
    beat = length/4
    bow(at, [bass, low, middle], length + .20, .093 * level)
    string(at, bass, .39*level, -.05, 1.45)
    for offset, note, pan in [(0.5, middle, -.3), (1.5, top, .24), (2.75, low, -.2), (3.5, middle, .33)]:
        string(at+beat*offset, note, .23*level, pan, .86)
    if motion:
        for pos, amp in [(0, .24), (2.5, .16)]:
            tap(at+pos*beat, amp*level, -.02)
        for pos in (1, 3):
            t = np.arange(round(.095*RATE)) / RATE
            rim = (.57*np.sin(2*np.pi*780*t)+.22*np.sin(2*np.pi*1370*t)) * np.exp(-61*t)
            instruments.write_note(at+pos*beat, rim*(1-np.exp(-420*t))*.115*level, .36)


def phrase(at, notes, gain):
    for delay, note, weight in notes:
        string(at+delay, note, weight*gain, -.08, 1.24)


def arrange():
    bow(.05, [40, 55, 59], 6.7, .072)
    phrase(.55, [(0, 71, .48), (1.01, 74, .40), (1.68, 76, .50)], .77)
    # 近景时让脚步从左向右擦过；第三人称进港的跳字落在下一次高音。
    for t, amp, pan in [(3.70, .21, -.4), (4.16, .30, -.22), (4.68, .36, .0), (5.18, .27, .29)]:
        step(t, amp, pan)
    swell(5.08, .6, .11)
    bar(5.68, 3.32, 2, .85, False)
    bar(9.0, 4.5, 4, .82, False)
    chime(10.25, 74, .18, .14)
    swell(12.88, .68, .15)
    # 逐镜决定小节长度，保留面板和地图段的律动，给机制段腾出听觉空间。
    sequence = [
        (13.5, 3.5, 1, 1.0, True), (17, 2.25, 2, 1.06, True), (19.25, 2.25, 4, 1, True),
        (21.5, 3.25, 0, .84, False), (24.75, 3.25, 2, .88, False),
        (28, 2.85, 1, 1, True), (30.85, 2.83, 3, 1, True), (33.68, 2.82, 4, .98, True),
        (36.5, 2.5, 2, 1.04, True), (39, 2.5, 4, .85, True),
        (41.5, 3, 0, .67, False), (44.5, 3, 2, .67, False), (47.5, 4, 3, .71, False),
        (51.5, 4, 4, .69, False), (55.5, 4, 1, .83, False),
        (59.5, 3.5, 2, .94, False), (63, 2.5, 1, 1, True), (65.5, 2.5, 3, 1, True),
        (68, 2, 4, .85, False), (70, 2.33, 0, .99, True), (72.33, 2.34, 2, 1.02, True),
        (74.67, 2.33, 4, 1.06, True), (77, 3.5, 2, .68, False),
    ]
    for row in sequence:
        bar(*row)
    phrase(14.64, [(0,71,.47),(.42,74,.36),(.93,76,.50),(1.52,79,.37)], 1)
    phrase(17, [(0,76,.45),(.85,74,.36),(1.72,71,.42),(2.25,69,.40),(3.34,74,.47)], 1)
    for i, n in enumerate([67, 71, 74, 79]):
        chime(21.62+i*.13, n, .155-i*.012, -.45+i*.3)
    phrase(29, [(0,71,.32),(.74,74,.31),(1.82,76,.36),(3.25,72,.35),(4.78,69,.35)], .9)
    swell(36.07, .48, .105)
    swell(40.80, .73, .10)
    # 左声道发包，右声道收到；随后打点对齐射线交点变化与下级域名跳出。
    for at, note, pan in [(43.03, 67, -.63), (43.55, 74, .58), (47.6, 71, .3),
                          (51.72, 69, -.12), (53.22, 74, .3), (57.15, 74, -.2), (57.38, 79, .28)]:
        chime(at, note, .17, pan)
    phrase(59.5, [(0,76,.44),(1.12,74,.34),(2.1,71,.48)], 1)
    phrase(63.12, [(0,67,.34),(.95,71,.43),(1.55,74,.4),(2.4,76,.41),(3.6,72,.39)], 1)
    swell(69.58, .50, .10)
    phrase(70.12, [(0,71,.44),(.57,74,.36),(1.16,76,.47),(2.22,79,.42),
                    (3.56,76,.45),(4.53,74,.37),(5.12,69,.42)], 1)
    swell(76.30, .73, .13)
    phrase(77.1, [(0,76,.37),(1.15,74,.30),(2.15,71,.35)], .86)
    bow(80.43, [43,55,59], 3.33, .091)
    for t, note, level, pan in [(80.5,43,.55,-.05),(80.56,62,.42,-.25),(80.66,71,.38,.3)]:
        string(t, note, level, pan, 1.9)
    chime(80.85, 79, .24, .12)


def render():
    arrange()
    # 短立体声反射与柔和磁带压缩将近景音效和配器放进同一声场。
    wet = sosfilt(butter(2, 3200, fs=RATE, output="sos"), mix, axis=0)
    for delay, gain in [(.031, .14), (.063, .09), (.139, .061), (.232, .047), (.341, .026)]:
        offset = int(delay*RATE)
        mix[offset:] += gain * wet[:-offset, ::-1]
    mix[:] = np.tanh(1.25*mix)
    mix[-RATE:] *= np.linspace(1, 0, RATE)[:, None]
    path = PROJECT / "public/score.m4a"
    path.parent.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory() as directory:
        wav = Path(directory) / "music.wav"
        wavfile.write(wav, RATE, mix)
        subprocess.run([find_ffmpeg(), "-hide_banner", "-v", "error", "-y", "-i", wav,
                        "-af", "loudnorm=I=-17:TP=-1.8:LRA=9", "-ar", "48000",
                        "-c:a", "aac", "-b:a", "256k", "-movflags", "+faststart", path], check=True)
    print(path)


if __name__ == "__main__":
    render()
