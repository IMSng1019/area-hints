"""完整解码成片，复核视频显示时间、章节、AAC 响度，并制作关键镜头校片图。"""
import json
import re
import subprocess
from fractions import Fraction

from PIL import Image, ImageDraw, ImageFont
from master import EDL, FF, FP, REVIEW, TITLE, info, ff


def check():
    probe = info(TITLE)
    video = next(s for s in probe['streams'] if s['codec_type'] == 'video')
    audio = next(s for s in probe['streams'] if s['codec_type'] == 'audio')
    assert (video['codec_name'], video['width'], video['height'], video['field_order']) == ('h264', 1920, 1080, 'progressive')
    assert all(video[k] == 'bt709' for k in ('color_space', 'color_primaries', 'color_transfer'))
    assert (audio['codec_name'], audio['sample_rate'], audio['channels']) == ('aac', '48000', 2)
    assert float(video['duration']) == float(audio['duration']) == 206
    pts = subprocess.check_output([FP, '-v', 'error', '-select_streams', 'v', '-show_entries',
                                   'packet=pts', '-of', 'csv=p=0', str(TITLE)]).splitlines()
    tick = Fraction(video['time_base'])
    assert len(pts) == EDL['frames'] and all(int(p)*tick == Fraction(i, 60) for i,p in enumerate(pts))
    chapters = json.loads(subprocess.check_output([FP, '-v', 'error', '-show_chapters', '-of', 'json', str(TITLE)]))['chapters']
    assert [(round(float(c['start_time'])*60), c['tags']['title']) for c in chapters] == [(c['start'],c['name']) for c in EDL['chapters']]
    decoded = subprocess.run([FF, '-hide_banner', '-nostdin', '-xerror', '-i', str(TITLE), '-map', '0:v:0', '-map','0:a:0',
        '-vf', 'blackdetect=d=0.05', '-af', 'loudnorm=I=-16:TP=-1.2:LRA=11:print_format=json',
        '-progress', 'pipe:1', '-f', 'null', '-'], capture_output=True, check=True)
    frames = int(re.findall(rb'^frame=(\d+)', decoded.stdout, re.M)[-1])
    assert frames == EDL['frames'] and b'black_start:' not in decoded.stderr
    meter = json.loads(re.search(rb'\{[^{}]*input_i[^{}]*\}', decoded.stderr).group())
    assert float(meter['input_tp']) <= -1
    return dict(decoded_frames=frames, video_seconds=206, audio_seconds=206, chapter_count=len(chapters),
                exact_frame_interval='1/60', video_average_mbps=round(int(video['bit_rate'])/1e6,2),
                input_lufs=float(meter['input_i']), input_true_peak=float(meter['input_tp']), black_runs_over_50ms=0)


def contact():
    seconds = [0,4.2,16,24.7,31.5,37.5,45,49,57.3,62.8,68.9,78.9,81.7,90.8,96,103,106.8,109.3,
               114.5,123.5,132,137.5,145,152,158.7,165.7,168.5,171.3,178.5,187.2,192.8,202.5,205.98]
    for sheet, start in enumerate(range(0,len(seconds),12),1):
        times = seconds[start:start+12]
        canvas = Image.new('RGB',(1320, (len(times)+2)//3*275),'#1e2728')
        pen = ImageDraw.Draw(canvas)
        for i,t in enumerate(times):
            tmp = REVIEW/'thumb.jpg'
            ff('-ss', t, '-i', TITLE, '-frames:v', 1, '-vf', 'scale=440:248', tmp)
            x, y = i%3*440, i//3*275
            canvas.paste(Image.open(tmp), (x,y))
            pen.text((x+7,y+250), f'{int(t)//60:02}:{t%60:05.2f}', font=ImageFont.truetype('consola.ttf',17), fill='white')
        canvas.save(REVIEW/f'check-{sheet}.jpg',quality=93)


if __name__ == '__main__':
    results = check()
    print(json.dumps(results, ensure_ascii=False), flush=True)
    (REVIEW/'decode.json').write_text(json.dumps(results, indent=2)+'\n',encoding='utf-8')
    contact()
