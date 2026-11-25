"""为 129 秒剪辑编排器乐、动作音和声像，运行本文件可生成 public/score.m4a。"""

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
DURATION = 129
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


def bar(at, length, chord, level, pattern):
    # 长小节用三音分散和弦，阅读段的空拍让紧接着的操作音清晰可辨。
    bass, low, middle, top = HARMONY[chord]
    bow(at, [bass, low, middle], length + .4, .10 * level)
    string(at, bass, .33 * level, -.06, 1.65)
    voicings = {
        "open": [(.23, middle, -.34), (.66, top, .27)],
        "pulse": [(.13, low, -.3), (.38, top, .30), (.63, middle, -.18), (.86, top, .12)],
        "answer": [(.23, top, .30), (.57, middle, -.29), (.84, low, -.11)],
        "pedal": [],
    }
    for offset, note, pan in voicings[pattern]:
        string(at + length * offset, note, .245 * level, pan, 1.16)


def phrase(at, notes, gain):
    for delay, note, weight in notes:
        string(at + delay, note, weight * gain, -.08, 1.48)


def arrange():
    # 开篇先留出宽阔空间，随着近景脚步接近，再让拨弦旋律进入前景。
    bow(.08, [40, 55, 59], 4.65, .065)
    phrase(.56, [(0, 71, .44), (1.66, 74, .35), (2.50, 76, .40)], .82)
    for frame, strength, pan in [(110, .23, -.19), (119, .18, .15)]:
        step(frame / 30, strength, pan)
    swell(123 / 30, .28, .043)
    # 字首浮现时单敲一次，高音延音过渡到最后一个远景。
    chime(144 / 30, 74, .17, .12)

    # 起点、终点、和弦与密度随对应片段变化，长句跨过小节线。
    bars = [
        (130 / 30, 274 / 30, 2, .70, "pedal"), (274 / 30, 11, 4, .68, "open"),
        (11, 16, 1, .93, "answer"),
        (16, 19.5, 2, .96, "pulse"), (19.5, 23, 4, .93, "answer"),
        (23, 27.5, 0, .73, "open"), (27.5, 32, 2, .77, "answer"),
        (32, 36.7, 1, .86, "pulse"), (36.7, 41.4, 3, .84, "answer"),
        (41.4, 46, 4, .80, "open"),
        (46, 49.75, 0, .74, "pedal"), (49.75, 53.50, 2, .76, "open"),
        (53.50, 57.25, 3, .78, "pedal"), (57.25, 61, 1, .80, "answer"),
        (61, 65, 2, .89, "pulse"), (65, 69, 4, .77, "open"),
        (69, 71.55, 0, .71, "pedal"), (71.55, 74.10, 2, .64, "open"),
        (74.10, 78.30, 3, .68, "answer"), (78.30, 83.50, 4, .72, "open"),
        (83.50, 88, 1, .74, "open"),
        (88, 91.50, 2, .86, "open"), (91.50, 95, 0, .79, "pedal"),
        (95, 99, 1, .92, "pulse"), (99, 103, 3, .87, "answer"),
        (103, 108, 4, .73, "open"),
        (108, 111.67, 0, .92, "pulse"), (111.67, 115.33, 2, .99, "answer"),
        (115.33, 119, 4, .91, "pulse"), (119, 124, 2, .66, "open"),
    ]
    for start, end, chord, level, pattern in bars:
        bar(start, end - start, chord, level, pattern)

    # 鼓只点出九宫格和应用蒙太奇，讲解和工具段由弦乐承担拍感。
    for at, strength in [(16, .19), (18.62, .11), (19.5, .15), (22.1, .09),
                         (108, .17), (111.67, .14), (114.42, .08), (115.33, .16)]:
        tap(at, strength, -.07)

    phrase(12.42, [(0, 71, .44), (.85, 74, .34), (1.75, 76, .49)], .91)
    phrase(16.36, [(0, 76, .39), (1.35, 74, .30), (2.46, 71, .36),
                   (3.98, 69, .30), (5.64, 74, .44)], .91)
    chime(23.47, 71, .13, -.25)
    chime(23.77, 76, .12, .24)
    phrase(34.30, [(0, 71, .32), (1.42, 74, .30), (3.07, 76, .37),
                   (5.60, 72, .31), (7.73, 69, .32)], .87)

    # 四工具并列时，选取首点、第三点、末点作低音木拨，闭合只响一次。
    for frame, note, pan in [(35, 59, -.30), (143, 62, .04), (251, 64, .26)]:
        string(46 + frame / 30, note, .22, pan, .75)
    chime(46 + 289 / 30, 67, .13, .04)
    phrase(61.54, [(0, 67, .34), (1.47, 71, .33), (2.87, 74, .38)], .81)
    swell(68.1, .94, .067)

    # 首次收包、A 跨界、服务端保存、B 收到更新，各自落在画面对应的动作上。
    chime(69 + 78 / 30, 67, .125, .14)
    chime(69 + 172 / 30, 74, .14, -.16)
    string(69 + 382 / 30, 64, .22, -.42, .75)
    chime(69 + 473 / 30, 79, .165, .39)

    phrase(88.50, [(0, 76, .43), (1.65, 74, .33), (3.42, 71, .45)], .91)
    phrase(95.45, [(0, 67, .33), (1.49, 71, .40), (2.52, 74, .33),
                    (4.27, 76, .41), (5.86, 72, .34)], .92)
    phrase(108.35, [(0, 71, .43), (.99, 74, .34), (2.08, 76, .48),
                    (3.88, 79, .38), (5.86, 76, .43), (7.70, 74, .37)], .98)
    phrase(119.40, [(0, 76, .35), (1.43, 74, .29), (2.96, 71, .34)], .81)
    bow(123.99, [43, 55, 59], 4.65, .087)
    for t, note, level, pan in [(124.03, 43, .51, -.05), (124.15, 62, .38, -.25),
                                (124.28, 71, .34, .30)]:
        string(t, note, level, pan, 2.03)
    chime(124.61, 79, .203, .11)


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
