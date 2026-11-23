"""为提瓦特实机和作者操作段录制原创拨弦、木音与弦乐动机。"""

from pathlib import Path

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, resample_poly, sosfilt


RATE = 24000
DURATION = 206
PI2 = 2 * np.pi
OUTPUT = Path(__file__).resolve().parents[1] / "review" / "score-dry.wav"
rng = np.random.default_rng(1019)
mix = np.zeros((DURATION * RATE, 2), dtype=np.float32)


def write_note(start, signal, pan):
    frame = round(start * RATE)
    count = min(len(signal), len(mix) - frame)
    if count > 0:
        mix[frame:frame + count, 0] += signal[:count] * np.sqrt((1 - pan) / 2)
        mix[frame:frame + count, 1] += signal[:count] * np.sqrt((1 + pan) / 2)


def frequency(note):
    return 440 * 2 ** ((note - 69) / 12)


def string(start, note, strength, pan=0, decay=1.0):
    # 各泛音随拨弦位置、力度与时间独立改变，避免重复固定采样。
    t = np.arange(round(RATE * 3.6 * decay)) / RATE
    hz = frequency(note)
    signal = np.zeros_like(t)
    point = rng.uniform(0.15, 0.23)
    for n in range(1, 10):
        partial = np.sin(n * np.pi * point) / n ** 1.23
        vibrato = 0.018 * n * np.sin(PI2 * 3.75 * t) * (1 - np.exp(-3 * t))
        phase = PI2 * hz * n * (1 + 0.000035 * n * n) * t
        signal += partial * np.sin(phase + vibrato) * np.exp(-t * (1.6 + n * 0.6) / decay)
    pick = rng.normal(0, 0.027, len(t)) * np.exp(-110 * t)
    signal = (signal + pick) * (1 - np.exp(-480 * t)) * strength
    write_note(start, signal, pan)


def bow(start, notes, seconds, strength):
    # 两侧弦乐缓起缓落，和弦共用低频根音，把人声的中频让开。
    t = np.arange(round(RATE * seconds)) / RATE
    envelope = np.sin(np.pi * np.arange(len(t)) / len(t)) ** 0.7
    for index, note in enumerate(notes):
        hz = frequency(note)
        pan = (-0.5, 0.17, 0.56)[index]
        for cents in (-3.5, 3.25):
            f = hz * 2 ** (cents / 1200)
            phase = PI2 * f * t + rng.uniform(0, PI2)
            phase += 0.08 * np.sin(PI2 * 4.2 * t + index)
            tone = np.sin(phase) + 0.20 * np.sin(2 * phase) + 0.07 * np.sin(3 * phase)
            write_note(start, tone * envelope * strength / (1.2 + index * 0.45), pan)


def tap(start, strength, pan):
    t = np.arange(round(RATE * 0.24)) / RATE
    thump = np.sin(PI2 * (68 * t + 0.7 * (1 - np.exp(-28 * t)))) * np.exp(-23 * t)
    shell = 0.26 * np.sin(PI2 * 423 * t) * np.exp(-48 * t)
    write_note(start, (thump + shell) * strength * (1 - np.exp(-650 * t)), pan)


# G(add9) 与 Cmaj7 的开放音程，折回低音 E 后在结尾落到 G。
chords = [
    (43, 50, 59, 62), (40, 47, 55, 62),
    (48, 55, 59, 64), (38, 45, 57, 64),
    (43, 50, 59, 64), (45, 52, 60, 67),
    (48, 55, 62, 64), (38, 45, 54, 62),
]
motifs = [
    [(0, 74), (1.55, 71), (3.0, 67)],
    [(0.5, 67), (2, 69), (3, 74)],
    [(0, 76), (1.5, 74), (3, 71)],
    [(0, 69), (2, 67)],
]


def section(start, end, beat, density, drums=False):
    index = 0
    bar = start
    while bar < end - 0.5:
        bottom, middle, upper, extra = chords[index % len(chords)]
        length = min(4 * beat, end - bar)
        bow(bar, [bottom, middle, upper], min(length + 0.6, end - bar), 0.12 * density)
        arp = [(0.05, upper, -0.26), (1.55, extra, 0.30), (2.65, middle + 12, -0.12)]
        for offset, note, pan in arp:
            at = bar + offset * beat
            if at < end - 0.28:
                string(at + rng.uniform(0, 0.018), note, density * rng.uniform(0.38, 0.55), pan)
        if index % 4 in (0, 2):
            for offset, note in motifs[(index // 2) % 4]:
                at = bar + offset * beat
                if at < end - 0.35:
                    string(at, note, 0.52 * density, 0.12, 1.24)
        if drums:
            for offset, volume in [(0, 0.41), (2.5, 0.24)]:
                at = bar + offset * beat
                if at < end - 0.2:
                    tap(at, volume * density, -0.08)
        index += 1
        bar = start + index * 4 * beat


def render():
    string(3.35, 74, 0.75, -0.08, 1.55)
    string(4.34, 67, 0.48, 0.22, 1.25)
    section(7.10, 20.95, 0.88, 0.61)
    section(21.2, 40, 0.95, 0.54)
    section(40.1, 61.85, 0.77, 0.68)
    section(62.12, 90.38, 0.70, 0.79, drums=True)
    section(91.16, 106.85, 0.82, 0.48)
    # 保存之后留下真实的 UI 反馈，再用亮和弦接住出现的地名。
    for offset, note, pan in [(0, 55, -0.42), (0.10, 62, 0.26), (0.24, 71, 0.12)]:
        string(115.72 + offset, note, 0.72, pan, 1.5)
    section(116.48, 146.9, 0.89, 0.50)
    section(148.06, 170.0, 0.78, 0.73, drums=True)
    section(170.2, 189.5, 0.82, 0.58)
    section(190.08, 197.45, 0.96, 0.53)
    bow(197.7, [43, 50, 59], 8.24, 0.08)
    for start, note, level in [(197.8, 74, 0.59), (199.5, 71, 0.54),
                               (200.84, 67, 0.49), (202.09, 62, 0.48), (202.92, 67, 0.65)]:
        string(start, note, level, 0.08, 1.55)
    # 短的左右早反射与三次低通延迟让拨弦的尾音置于同一空间。
    wet = sosfilt(butter(2, 2500, fs=RATE, output="sos"), mix, axis=0)
    for delay, level, swap in [(0.044, 0.13, True), (0.101, 0.08, False),
                               (0.177, 0.06, True), (0.298, 0.036, False)]:
        offset = int(RATE * delay)
        response = wet[:-offset, ::-1] if swap else wet[:-offset]
        mix[offset:] += response * level
    integrated = np.sqrt(np.mean(np.square(mix)))
    mix[:] *= 10 ** (-31 / 20) / integrated
    mix[-RATE:] *= np.linspace(1, 0, RATE)[:, None]
    stereo = resample_poly(mix, 2, 1, axis=0)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    wavfile.write(OUTPUT, 48000, np.round(np.clip(stereo, -1, 1) * 32767).astype(np.int16))


if __name__ == "__main__":
    render()
