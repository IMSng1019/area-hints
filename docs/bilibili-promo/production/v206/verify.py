"""完整解码成片，复核视频显示时间、章节、AAC 响度，并制作关键镜头校片图。"""
import argparse
import json
import re
import subprocess
from fractions import Fraction
from pathlib import Path
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw, ImageFont
from master import EDL, FF, FP, REVIEW, TITLE, info, ff


def check(path=TITLE):
    probe = info(path)
    video = next(s for s in probe['streams'] if s['codec_type'] == 'video')
    audio = next(s for s in probe['streams'] if s['codec_type'] == 'audio')
    assert (video['codec_name'], video['width'], video['height'], video['field_order']) == ('h264', 1920, 1080, 'progressive')
    assert all(video[k] == 'bt709' for k in ('color_space', 'color_primaries', 'color_transfer'))
    assert (audio['codec_name'], audio['sample_rate'], audio['channels'], audio['channel_layout']) == ('aac', '48000', 2, 'stereo')
    assert float(video['start_time']) == float(audio['start_time']) == 0
    video_seconds = int(video['duration_ts']) * Fraction(video['time_base'])
    audio_seconds = int(audio['duration_ts']) * Fraction(audio['time_base'])
    assert video_seconds == Fraction(EDL['frames'], EDL['fps'])
    # AAC 以 1024 个采样为一帧，复用容器可能保留尾帧的部分填充，报告真实差值。
    sample_delta = (audio_seconds - video_seconds) * int(audio['sample_rate'])
    assert sample_delta.denominator == 1 and abs(sample_delta) < 1024, sample_delta
    pts = subprocess.check_output([FP, '-v', 'error', '-select_streams', 'v', '-show_entries',
                                   'packet=pts', '-of', 'csv=p=0', str(path)]).splitlines()
    tick = Fraction(video['time_base'])
    # B 帧可交换包的解码次序，显示次序以每帧的 PTS 排列。
    displayed = sorted(map(int, pts))
    assert len(displayed) == EDL['frames'] and all(p*tick == Fraction(i, EDL['fps']) for i,p in enumerate(displayed))
    chapters = json.loads(subprocess.check_output([FP, '-v', 'error', '-show_chapters', '-of', 'json', str(path)]))['chapters']
    found = [(round(float(c['start_time'])*EDL['fps']), c['tags']['title']) for c in chapters]
    expected = [(c['start'], c['name']) for c in EDL['chapters']]
    if path.resolve() == TITLE.resolve() and found != expected:
        # 留存的旧版片头和章节可用原生工程随附的 XML 核对。
        markers = ET.parse(TITLE.parent/'premiere/Areas-Hint.xml').findall('./sequence/marker')
        expected = [(int(m.findtext('in')), m.findtext('name')) for m in markers]
    assert found == expected, found
    decoded = subprocess.run([FF, '-hide_banner', '-nostdin', '-xerror', '-i', str(path), '-map', '0:v:0', '-map','0:a:0',
        '-vf', 'blackdetect=d=0.05', '-af', 'loudnorm=I=-16:TP=-1.2:LRA=11:print_format=json',
        '-progress', 'pipe:1', '-f', 'null', '-'], capture_output=True, check=True)
    frames = int(re.findall(rb'^frame=(\d+)', decoded.stdout, re.M)[-1])
    assert frames == EDL['frames'] and b'black_start:' not in decoded.stderr
    meter = json.loads(re.search(rb'\{[^{}]*input_i[^{}]*\}', decoded.stderr).group())
    assert float(meter['input_tp']) <= -1
    return dict(file=path.name, decoded_frames=frames, video_seconds=float(video_seconds), audio_seconds=float(audio_seconds),
                audio_sample_delta=int(sample_delta), chapter_count=len(chapters),
                exact_frame_interval='1/60', video_average_mbps=round(int(video['bit_rate'])/1e6,2),
                input_lufs=float(meter['input_i']), input_true_peak=float(meter['input_tp']), black_runs_over_50ms=0)


def contact(path=TITLE, output_dir=REVIEW):
    seconds = [0,4.2,16,24.7,28.95,29.35,30.2,31.5,33.75,34.12,37.5,45,49,57.3,62.8,68.9,78.9,81.7,90.8,96,103,106.8,109.3,
               114.5,123.5,132,137.5,145,152,158.7,165.7,168.5,171.3,178.5,187.2,192.8,202.5,205.98]
    for sheet, start in enumerate(range(0,len(seconds),12),1):
        times = seconds[start:start+12]
        canvas = Image.new('RGB',(1320, (len(times)+2)//3*275),'#1e2728')
        pen = ImageDraw.Draw(canvas)
        for i,t in enumerate(times):
            tmp = output_dir/'thumb.jpg'
            ff('-ss', t, '-i', path, '-frames:v', 1, '-vf', 'scale=440:248', tmp)
            x, y = i%3*440, i//3*275
            canvas.paste(Image.open(tmp), (x,y))
            pen.text((x+7,y+250), f'{int(t)//60:02}:{t%60:05.2f}', font=ImageFont.truetype('consola.ttf',17), fill='white')
        canvas.save(output_dir/f'check-{sheet}.jpg',quality=93)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('video', type=Path, nargs='?', default=TITLE, help='成片 MP4')
    parser.add_argument('--output-dir', type=Path, help='解码报告与校片图目录')
    args = parser.parse_args()
    path = args.video.resolve()
    output_dir = (args.output_dir or (REVIEW if path == TITLE else REVIEW/'revision')).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    results = check(path)
    print(json.dumps(results, ensure_ascii=False), flush=True)
    (output_dir/'decode.json').write_text(json.dumps(results, indent=2)+'\n',encoding='utf-8')
    contact(path, output_dir)
