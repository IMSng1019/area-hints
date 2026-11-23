"""从整数帧剪辑表准备素材、制作封面，并导出带定位字幕的投稿文件。"""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

from PIL import Image, ImageDraw, ImageFont
from source import sequence_ranges

ROOT = Path(__file__).resolve().parent
MEDIA = ROOT / 'media'
REVIEW = ROOT / 'review'
BIN = Path(os.environ.get('LOCALAPPDATA', '')) / 'Programs/ChatCut/resources/app.asar.unpacked/node_modules/ffmpeg-ffprobe-static'
FF = os.getenv('FFMPEG') or shutil.which('ffmpeg') or str(BIN / 'ffmpeg.exe')
FP = os.getenv('FFPROBE') or shutil.which('ffprobe') or str(BIN / 'ffprobe.exe')
EDL = json.loads((ROOT / 'edit.json').read_text(encoding='utf-8'))
FPS = EDL['fps']
TITLE = ROOT / 'Areas-Hint.mp4'
COLOR = ['-pix_fmt', 'yuv420p', '-colorspace', 'bt709', '-color_trc', 'bt709', '-color_primaries', 'bt709']
GAME = ['进入璃月港', '进入风起地', '风起地的域名描述展示', '可视化俯瞰环视展示',
        '多级跳字展示', '创建域名展示', '指令展示', '可视化指令面板展示']


def ff(*args):
    subprocess.run([FF, '-hide_banner', '-loglevel', 'error', '-nostdin', '-y', *map(str, args)], cwd=ROOT, check=True)


def info(path):
    return json.loads(subprocess.check_output([FP, '-v', 'error', '-show_streams', '-show_format',
                                               '-of', 'json', str(path)], encoding='utf-8'))


def snapshot(source, frame, destination):
    ff('-ss', f'{frame / FPS:.8f}', '-i', source, '-map', '0:v:0', '-frames:v', 1, destination)


def prepare(project):
    MEDIA.mkdir(exist_ok=True)
    REVIEW.mkdir(exist_ok=True)
    # 每条输入的零点沿用原 PR 命名序列，复制完整素材以保留修剪余量。
    ranges = None
    for name in GAME:
        destination = MEDIA / (name + '.mp4')
        if destination.exists():
            continue
        cached = ROOT.parent / 'output/Media' / destination.name
        if cached.is_file():
            shutil.copy2(cached, destination)
            continue
        if ranges is None:
            ranges = sequence_ranges(project)
        source, offset, length = ranges[name]
        tmp = REVIEW / destination.name
        ff('-ss', offset, '-i', source, '-t', length, '-map', '0:v:0', '-map', '0:a:3',
           '-vf', 'fps=60', '-c:v', 'libx264', '-crf', 16, '-preset', 'fast', *COLOR,
           '-c:a', 'aac', '-b:a', '256k', '-ar', 48000, tmp)
        tmp.replace(destination)
    for name, source, last_frame, duration in [('tree-hold', GAME[1], 467, 48), ('book-hold', GAME[2], 353, 18)]:
        still = REVIEW / (name + '.png')
        snapshot(MEDIA / (source + '.mp4'), last_frame, still)
        ff('-loop', 1, '-framerate', FPS, '-i', still, '-frames:v', duration, '-an', '-c:v', 'libx264',
           '-crf', 15, *COLOR, MEDIA / (name + '.mp4'))
    snapshot(MEDIA / '进入璃月港.mp4', 480, REVIEW / 'cover-source.png')
    cover = Image.open(REVIEW / 'cover-source.png').convert('RGBA')
    shade = Image.new('RGBA', cover.size)
    pen = ImageDraw.Draw(shade)
    for y in range(540, 1080):
        pen.line((0, y, 1919, y), fill=(8, 22, 20, int(205 * ((y - 540) / 539) ** .8)))
    cover = Image.alpha_composite(cover, shade)
    pen = ImageDraw.Draw(cover)
    title = ImageFont.truetype('C:/Windows/Fonts/msyhbd.ttc', 132)
    subtitle = ImageFont.truetype('C:/Windows/Fonts/msyhbd.ttc', 48)
    pen.rectangle((92, 790, 179, 800), fill='#f3cf73')
    pen.text((200, 789), '自制 MC 域名模组', font=subtitle, anchor='lm', fill='#fff0cb', stroke_width=1, stroke_fill='#443d2b')
    pen.text((83, 908), 'MC 也能报地名', font=title, anchor='lm', fill='#fffdf2', stroke_width=2, stroke_fill='#28332a')
    cover.convert('RGB').save(ROOT / '封面.jpg', quality=96, subsampling=0)
    cover.thumbnail((384, 216))
    cover.convert('RGB').save(REVIEW / 'cover-mobile.jpg')


def stamp(value):
    hours, minutes, seconds, millis = map(int, re.split('[:,]', value))
    return (hours * 3600 + minutes * 60 + seconds) * 1000 + millis


def ass_time(millis):
    seconds, fraction = divmod(round(millis / 10), 100)
    minutes, seconds = divmod(seconds, 60)
    hours, minutes = divmod(minutes, 60)
    return f'{hours}:{minutes:02}:{seconds:02}.{fraction:02}'


def subtitles(clip, target):
    start = clip['start'] * 1000 / FPS
    end = start + clip['duration'] * 1000 / FPS
    # 实机字幕让开快捷栏；保存镜头使用上沿，给聊天记录与中央跳字留空间。
    shot = clip['shot']
    paper = shot in (5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 18, 19)
    x, y = 960, 1020
    if shot == 12:
        y = 104
    elif shot == 20 or shot in (1, 3) or (shot == 17 and '/g' not in clip['path']):
        y = 918
    elif shot == 4:
        y = 1046
    header = """[Script Info]
ScriptType: v4.00+
PlayResX: 1920
PlayResY: 1080
WrapStyle: 2
ScaledBorderAndShadow: yes
[V4+ Styles]
Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding
Style: Main,Microsoft YaHei,59,&H00FFFFFF,&H00FFFFFF,&H00232923,&H80000000,0,0,0,0,100,100,0,0,1,2.4,0.8,2,50,50,30,1
Style: Paper,Microsoft YaHei,59,&H002F2F14,&H002F2F14,&H00D9EBF0,&H00000000,0,0,0,0,100,100,0,0,1,1.2,0,2,50,50,30,1
[Events]
Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text
"""
    chunks = (ROOT / 'audio/narration.srt').read_text(encoding='utf-8-sig').strip().split('\n\n')
    lines = []
    for chunk in chunks:
        _, times, *words = chunk.splitlines()
        a, b = map(stamp, times.split(' --> '))
        a, b = max(a, start), min(b, end)
        if a < b:
            phrase = r'\N'.join(words)
            pos = r'{\pos(' + f'{x},{y}' + ')}'
            style = 'Paper' if paper else 'Main'
            lines.append(f'Dialogue: 0,{ass_time(a-start)},{ass_time(b-start)},{style},,0,0,0,,{pos}{phrase}')
    target.write_text(header + '\n'.join(lines) + '\n', encoding='utf-8-sig')


def render_segment(index, clip):
    source = ROOT / clip['path']
    out = REVIEW / f'part-{index:02}.mp4'
    # 发布前检查源长度，避免容器被截短却成功拼接。
    frames = int(info(source)['streams'][0]['nb_frames'])
    if clip['in'] + clip['duration'] > frames:
        raise ValueError(f'{source} has {frames} frames: {clip}')
    captions = REVIEW / f'part-{index:02}.ass'
    subtitles(clip, captions)
    encode = ['-c:v', 'h264_nvenc', '-preset', 'p5', '-rc', 'constqp', '-qp', '10']
    if os.getenv('AREAS_SOFTWARE_ENCODE'):
        encode = ['-c:v', 'libx264', '-preset', 'fast', '-crf', '10']
    ff('-ss', f"{clip['in'] / FPS:.8f}", '-i', source, '-map', '0:v:0', '-an', '-frames:v', clip['duration'],
       '-vf', f'fps=60,ass=review/{captions.name}', *encode, '-g', 60, '-bf', 0, *COLOR, out)
    if int(info(out)['streams'][0]['nb_frames']) != clip['duration']:
        raise ValueError(f'Incomplete encoded clip {out}')
    return out


def export():
    cursor = 0
    parts = []
    for index, clip in enumerate(EDL['clips']):
        if clip['start'] != cursor:
            raise ValueError(f'Unexpected timeline gap at frame {cursor}')
        print(f"Rendering {index + 1}/{len(EDL['clips'])}: {clip['path']}", flush=True)
        parts.append(render_segment(index, clip))
        cursor += clip['duration']
    assert cursor == EDL['frames'] == 12360
    # 显式指定每段时长，避免逐段累积毫秒舍入误差。
    playlist = ''.join(f"file '{part.name}'\nduration {clip['duration']/FPS:.9f}\n"
                       for part, clip in zip(parts, EDL['clips']))
    (REVIEW / 'parts.ffconcat').write_text(playlist, encoding='utf-8')
    chapters = [';FFMETADATA1']
    starts = [c['start'] for c in EDL['chapters']] + [cursor]
    for chapter, next_start in zip(EDL['chapters'], starts[1:]):
        chapters.extend(['[CHAPTER]', 'TIMEBASE=1/60', f"START={chapter['start']}", f'END={next_start}', f"title={chapter['name']}"])
    (REVIEW / 'chapters.txt').write_text('\n'.join(chapters), encoding='utf-8')
    tmp = REVIEW / 'master.mp4'
    # 全片两遍 VBR 把更多码率留给运动中的实机画面与地图细节。
    picture = ['-f', 'concat', '-safe', 0, '-i', REVIEW / 'parts.ffconcat']
    codec = ['-c:v', 'libx264', '-threads', 12, '-preset', 'veryfast', '-b:v', '24M', '-maxrate', '42M',
             '-bufsize', '48M', '-g', 120, '-bf', 0, *COLOR, '-passlogfile', REVIEW / 'vbr']
    ff(*picture, '-map', '0:v:0', '-an', *codec, '-pass', 1, '-f', 'null', '-')
    ff(*picture, '-i', ROOT / 'audio/Mix.wav',
       '-f', 'ffmetadata', '-i', REVIEW / 'chapters.txt', '-map', '0:v:0', '-map', '1:a:0', '-map_chapters', 2,
       *codec, '-pass', 2, '-c:a', 'aac', '-ar', 48000, '-b:a', '320k', '-movflags', '+faststart', tmp)
    tmp.replace(TITLE)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('stage', choices=['prepare', 'export'])
    parser.add_argument('--project', type=Path, default=Path('Q:/PR/tmp/素材.prproj'))
    args = parser.parse_args()
    if args.stage == 'prepare':
        prepare(args.project)
    else:
        export()
