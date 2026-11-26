"""从指定的《璃月》FLAC 制作约 132 秒的试剪配乐。"""

import argparse
from pathlib import Path
from tempfile import TemporaryDirectory

from prepare_clips import PROJECT, execute, find_ffmpeg


START = 191.250
END = 274.000
# 两处源时间相隔 49.286 秒，位于相同拍位，附近音色、和声与响度接近。
OUTGOING = 260.154
INCOMING = 210.868
CROSSFADE = 0.160
DURATION = END - START + OUTGOING - INCOMING


def render_music(source, output):
    output.parent.mkdir(parents=True, exist_ok=True)
    # 以所选拍位为交叠中心，使前后镜头各自沿用原曲的节奏。
    first_duration = OUTGOING + CROSSFADE / 2 - START
    second_start = INCOMING - CROSSFADE / 2
    filters = (
        f"[0:a]aresample=48000,atrim=duration={first_duration:.3f},asetpts=PTS-STARTPTS[a];"
        f"[1:a]aresample=48000,atrim=duration={END - second_start:.3f},asetpts=PTS-STARTPTS[b];"
        f"[a][b]acrossfade=d={CROSSFADE}:c1=qsin:c2=qsin,"
        f"afade=t=in:d=0.22,afade=t=out:st={DURATION - 0.36:.3f}:d=0.36,volume=-2.5dB[score]"
    )
    with TemporaryDirectory(dir=output.parent) as temp:
        result = Path(temp) / "score.wav"
        execute(find_ffmpeg(), [
            "-ss", str(START), "-i", source, "-ss", f"{second_start:.3f}", "-i", source,
            "-filter_complex", filters, "-map", "[score]", "-map_metadata", "-1",
            "-ac", "2", "-ar", "48000", "-c:a", "pcm_s24le", result,
        ])
        result.replace(output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="《璃月》原曲 FLAC 文件")
    parser.add_argument("--output", type=Path, default=PROJECT / ".cache/liyue-music.wav")
    args = parser.parse_args()
    render_music(args.source, args.output)
    print(f"{args.output} ({DURATION:.3f}s)")
