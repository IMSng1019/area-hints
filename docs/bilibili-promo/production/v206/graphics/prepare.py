"""从代理实机和当前世界文件提取可移交的渲染素材。"""
import argparse
import collections
import json
import os
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PUBLIC = ROOT / 'public'
DEFAULT_MEDIA = ROOT.parent / 'media'
SCREENSHOT = Path.home() / 'Pictures/Screenshots/屏幕截图 2026-11-21 121230.png'
WORLD = Path('L:/.minecraft/areas-hint/play.simpfun.cn_方块提瓦特/overworld.json')
FF = os.environ.get('FFMPEG') or shutil.which('ffmpeg') or str(Path(os.environ['LOCALAPPDATA']) /
    'Programs/ChatCut/resources/app.asar.unpacked/node_modules/ffmpeg-ffprobe-static/ffmpeg.exe')
X, Y, Z = 3100, 80, -3150


def altitude_ok(area):
    altitude = area.get('altitude') or {}
    return (altitude.get('min') is None or altitude['min'] <= Y) and (altitude.get('max') is None or altitude['max'] >= Y)


def in_bounds(area):
    box = area['second-vertices']
    xs = [p['x'] for p in box]
    zs = [p['z'] for p in box]
    return min(xs) <= X <= max(xs) and min(zs) <= Z <= max(zs)


def distance(area):
    points = area['vertices']
    cx = sum(p['x'] for p in points) / len(points)
    cz = sum(p['z'] for p in points) / len(points)
    return (cx-X)**2 + (cz-Z)**2


def contains(area):
    points = area['vertices']
    count = 0
    for a, b in zip(points, points[1:] + points[:1]):
        if (a['z'] <= Z < b['z'] or b['z'] <= Z < a['z']):
            hit = a['x'] + (Z-a['z']) * (b['x']-a['x']) / (b['z']-a['z'])
            count += hit > X
    return count % 2 == 1


def export_map(world):
    areas = json.loads(world.read_text(encoding='utf-8-sig'))
    levels = collections.Counter(a['level'] for a in areas)
    checked = []
    parent = None
    queries = []
    for level in sorted(levels):
        group = [a for a in areas if a['level'] == level and altitude_ok(a)]
        if not group and level > 1:
            continue
        # Python 稳定排序保留距离相同的源文件次序，与客户端空间索引一致。
        candidates = sorted((a for a in group if (level == 1 or a.get('base-name') == parent)
                             and in_bounds(a)), key=distance)
        queries.append([a['name'] for a in candidates])
        for a in candidates:
            checked.append(dict(name=a['name'], edges=len(a['vertices']), hit=contains(a)))
            if checked[-1]['hit']:
                parent = a['name']
                break
        else:
            break
    data = dict(point=[X,Y,Z], levels=levels, checked=checked, candidates=queries, total=len(areas),
                edges=sum(len(a['vertices']) for a in areas), bounds=sum(len(a['second-vertices']) for a in areas))
    # 交付只包含绘图所需的地名、层级和几何。
    data['areas'] = [dict(name=a['name'], level=a['level'], parent=a.get('base-name'),
                          v=[[p['x'],p['z']] for p in a['vertices']]) for a in areas]
    (ROOT/'map.json').write_text(json.dumps(data, ensure_ascii=False,separators=(',',':')),encoding='utf-8')
    print({k:v for k,v in data.items() if k!='areas'}, flush=True)


STILLS = {'port':('进入璃月港',10.0), 'tree':('进入风起地',7.75),
          'book':('风起地的域名描述展示',3.75), 'name':('创建域名展示',19.5),
          'corner':('创建域名展示',39), 'save':('创建域名展示',83.2),
          'sign-list':('可视化指令面板展示',2159/60)}
CLIPS = {'walls':('可视化俯瞰环视展示',6,4), 'levels':('多级跳字展示',.2,10),
         'command':('指令展示',16,2), 'village':('创建域名展示',17,4.5),
         'level':('创建域名展示',23.5,4), 'points':('创建域名展示',33,7),
         'row':('创建域名展示',58,3), 'point27':('创建域名展示',76,4),
         'panel':('可视化指令面板展示',3,3), 'sign':('可视化指令面板展示',27,9)}


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--world', type=Path, default=WORLD)
    parser.add_argument('--media', type=Path, default=DEFAULT_MEDIA)
    parser.add_argument('--screenshot', type=Path, default=SCREENSHOT)
    args = parser.parse_args()
    PUBLIC.mkdir(exist_ok=True)
    export_map(args.world)
    shutil.copyfile(args.screenshot, PUBLIC/'xaero.png')
    # 过场以两侧真正的剪点帧为底图，确保进出插画时色调和构图对得上。
    for name, source, frame in [('tree-from','tree-hold',0), ('book-to','风起地的域名描述展示',12),
                                ('port-from','进入璃月港',605)]:
        subprocess.run([FF,'-loglevel','error','-y','-ss',str(frame/60),'-i',str(args.media/(source+'.mp4')),
                        '-frames:v','1',str(PUBLIC/(name+'.png'))],check=True)
    for target, (name, t) in STILLS.items():
        subprocess.run([FF,'-loglevel','error','-y','-ss',str(t),'-i',str(args.media/(name+'.mp4')),
            '-frames:v','1','-q:v','2',str(PUBLIC/(target+'.jpg'))], check=True)
    for target, (name, t, length) in CLIPS.items():
        print(target, flush=True)
        subprocess.run([FF,'-loglevel','error','-y','-ss',str(t),'-i',str(args.media/(name+'.mp4')),
            '-frames:v',str(round(length*60)),'-an','-vf','scale=1360:766,fps=60','-c:v','libx264',
            '-threads','4','-preset','veryfast','-crf','17','-pix_fmt','yuv420p','-g','30',
            str(PUBLIC/(target+'.mp4'))], check=True)
