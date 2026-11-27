"""从剪过的八条 PR 子序列制作 60 fps 中间片、剪辑时间线和审片文件。"""
import argparse
import gzip
import json
import os
from pathlib import Path
import shutil
import subprocess
import wave
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw, ImageFont

BASE = Path(__file__).resolve().parent
OUT = BASE / 'output'
MEDIA = OUT / 'Media'
CACHE = BASE / 'cache'
for directory in (MEDIA, CACHE):
    directory.mkdir(parents=True, exist_ok=True)
TOOLS = Path(os.environ['LOCALAPPDATA']) / 'Programs/ChatCut/resources/app.asar.unpacked/node_modules/ffmpeg-ffprobe-static'
FF = os.environ.get('FFMPEG') or shutil.which('ffmpeg') or str(TOOLS / 'ffmpeg.exe')
FP = os.environ.get('FFPROBE') or shutil.which('ffprobe') or str(TOOLS / 'ffprobe.exe')
FPS = 60
TICKS = 254016000000
RATE = 48000
ENC = ['-c:v','h264_nvenc','-preset','p5','-rc','vbr','-cq','17','-b:v','16M','-maxrate','32M','-bufsize','48M','-g','60','-bf','0','-pix_fmt','yuv420p','-colorspace','bt709','-color_primaries','bt709','-color_trc','bt709']

def ff(*args):
    subprocess.run([FF, '-hide_banner', '-loglevel', 'error', '-nostdin', '-y', *map(str,args)], check=True)

def source_sequences(project):
    root = ET.fromstring(gzip.open(project, 'rb').read())
    objects = {o.get('ObjectID', o.get('ObjectUID')): o for o in root}
    def ref(node):
        return objects[node.get('ObjectRef',node.get('ObjectURef'))]
    result = {}
    for sequence in root.findall('Sequence'):
        name = sequence.findtext('Name').strip()
        for link in sequence.iter('Second'):
            group = objects.get(link.get('ObjectRef'))
            if group is None or group.tag != 'VideoTrackGroup':
                continue
            track = ref(group.find('.//Tracks/Track'))
            item = ref(track.find('.//TrackItems/TrackItem')).find('ClipTrackItem')
            timing = item.find('TrackItem')
            head = int(timing.findtext('Start','0')) / TICKS
            end = int(timing.findtext('End')) / TICKS
            clip = ref(ref(item.find('SubClip')).find('Clip')).find('Clip')
            media = ref(ref(clip.find('Source')).find('.//Media'))
            offset = int(clip.findtext('InPoint','0')) / TICKS
            result[name] = (Path(media.findtext('FilePath')), offset - head, end)
    return result

PORT='进入璃月港'
TREE='进入风起地'
BOOK='风起地的域名描述展示'
FLY='可视化俯瞰环视展示'
LEVEL='多级跳字展示'
BUILD='创建域名展示'
PANEL='可视化指令面板展示'
CMD='指令展示'
CARDS={'Brand':5, 'Boundary':9, 'Detection':17, 'Approval':10, 'End':7}
# 目标时长、来源、子序列内入点；定格插片单独保存以便继续修剪。
CUTS = [
    (6,PORT,3.6),(7,TREE,.7),(5.7,BOOK,.2),(1.3,'书页停留',0),(5,'Brand',0),
    (11,FLY,1),(10,LEVEL,.2),(5,PANEL,2),(2,BUILD,39),
    (2,BUILD,8.5),(2.5,'创建命令',0),(2,BUILD,18.7),(2.5,'输入村庄',0),
    (10,BUILD,21),(8,BUILD,32.5),(3,BUILD,42),(4,BUILD,58),(3,BUILD,72),
    (5,BUILD,75),(9,'Boundary',0),(2,BUILD,80),(3,'高度选项',0),(7,BUILD,82),
    (7,PANEL,3),(3,CMD,14),(17,'Detection',0),(10,PANEL,27),(10,'Approval',0),
    (3,BUILD,86),(3,PORT,6),(5.5,BOOK,.3),(.5,'书页停留',0),(3,TREE,4.8),(7,'End',0)
]
STILLS={'书页停留':(BOOK,5.75,1.3),'创建命令':(BUILD,9.9,2.5),'输入村庄':(BUILD,19.55,2.5),'高度选项':(BUILD,80.25,3)}
CHAPTERS=[(0,'走进提瓦特'),(25,'域名的范围'),(46,'给村庄命名'),(95,'顶点与高度'),(116,'作者工具'),(126,'玩家位置如何判定'),(143,'多人协作'),(163,'回到提瓦特')]

def path_for(name):
    return (OUT if name in CARDS else MEDIA) / (name+'.mp4')

def normalize(project):
    for name,(path,offset,end) in source_sequences(project).items():
        target=path_for(name)
        if not target.exists():
            print('制作实机中间片',name,flush=True)
            ff('-hwaccel','cuda','-ss',max(0,offset),'-i',path,'-t',end,'-map','0:v:0','-map','0:a:3','-vf','fps=60',*ENC,'-c:a','aac','-b:a','256k','-ar',RATE,'-movflags','+faststart',target)
    for name,(source,time,length) in STILLS.items():
        img=MEDIA/(name+'.jpg')
        ff('-ss',time,'-i',path_for(source),'-frames:v',1,'-q:v',2,img)
        if name in ('创建命令','输入村庄','高度选项'):
            screenshot=Image.open(img).convert('RGB')
            # 源画面作背景，用局部原生 UI 和文字解释提高聊天区的可读性。
            result=screenshot.point(lambda x:int(x*.39))
            draw=ImageDraw.Draw(result)
            font=ImageFont.truetype('C:/Windows/Fonts/msyhbd.ttc',48)
            small=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',31)
            text={'创建命令':('开始创建域名','/areahint easyadd'),'输入村庄':('给区域命名','村庄'),'高度选项':('选择高度范围','示例选择  ·  不限制高度')}[name]
            draw.rounded_rectangle((914,280,1790,566),radius=22,fill=(246,244,232))
            draw.rectangle((959,337,974,391),fill=(40,110,100))
            draw.text((998,328),text[0],font=font,fill=(29,53,48))
            draw.text((998,437),text[1],font=small,fill=(55,75,68))
            region=(0,615,900,1080)
            result.paste(screenshot.crop(region),region[:2])
            result.save(img,quality=96)
        ff('-loop',1,'-framerate',60,'-i',img,'-t',length,*ENC,'-an',path_for(name))

def frames(seconds):
    return round(seconds*FPS)

def node(parent,tag,value=None,**attrs):
    el=ET.SubElement(parent,tag,attrs)
    if value is not None: el.text=str(value)
    return el

def rate(el):
    r=node(el,'rate');node(r,'timebase',FPS);node(r,'ntsc','FALSE')

def sample(el):
    s=node(el,'samplecharacteristics');rate(s)
    for k,v in [('width',1920),('height',1080),('anamorphic','FALSE'),('pixelaspectratio','square'),('fielddominance','none')]: node(s,k,v)

def timeline(project):
    durations={n:frames(v[2]) for n,v in source_sequences(project).items()}
    durations.update({n:frames(s) for n,s in CARDS.items()})
    durations.update({n:frames(v[2]) for n,v in STILLS.items()})
    root=ET.Element('xmeml',version='4')
    seq=node(root,'sequence',id='areas-promo')
    node(seq,'name','Areas Hint · 走进提瓦特');node(seq,'duration',11100);rate(seq)
    tc=node(seq,'timecode');rate(tc);node(tc,'string','00:00:00:00');node(tc,'frame',0);node(tc,'displayformat','NDF')
    media=node(seq,'media');video=node(media,'video');sample(node(video,'format'));vt=node(video,'track')
    audio=node(media,'audio');node(audio,'numOutputChannels',2);fmt=node(node(audio,'format'),'samplecharacteristics');node(fmt,'depth',16);node(fmt,'samplerate',RATE)
    fileids={}
    def clip(track,i,name,start,length,source,path,maxframes,kind):
        c=node(track,'clipitem',id='clip-'+str(i))
        node(c,'name',name);node(c,'duration',maxframes);rate(c)
        for k,v in [('start',start),('end',start+length),('in',source),('out',source+length)]: node(c,k,v)
        if path not in fileids:
            fileids[path]='media-'+str(len(fileids)+1)
            f=node(c,'file',id=fileids[path]);node(f,'name',path.name)
            node(f,'pathurl',path.resolve().as_uri().replace('file:///','file://localhost/'));rate(f);node(f,'duration',maxframes)
            m=node(f,'media')
            if kind=='video': sample(node(m,'video'))
            else:
                a=node(m,'audio');s=node(a,'samplecharacteristics');node(s,'depth',16);node(s,'samplerate',RATE);node(a,'channelcount',2)
        else: node(c,'file',id=fileids[path])
        if kind=='audio':
            st=node(c,'sourcetrack');node(st,'mediatype','audio');node(st,'trackindex',1)
    t=0
    for i,(length,name,source) in enumerate(CUTS):
        n=frames(length);clip(vt,i,name,t,n,frames(source),path_for(name),durations[name],'video');t+=n
    assert t==11100, t
    for i,(file,label) in enumerate([('Voice','普通话旁白'),('Score','原创配乐'),('Game','游戏现场')]):
        track=node(audio,'track')
        clip(track,200+i,label,0,t,0,OUT/(file+'.wav'),t,'audio')
    for seconds,label in CHAPTERS:
        mark=node(seq,'marker');node(mark,'name',label);node(mark,'in',frames(seconds));node(mark,'out',-1)
    ET.indent(root);(OUT/'Areas-Hint.xml').write_bytes(b'<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE xmeml>\n'+ET.tostring(root,encoding='utf-8'))

def effects():
    # 游戏声单独成轨；逐切开短淡入淡出，防止跳剪位置爆音。
    with wave.open(str(OUT/'Game.wav'),'wb') as out:
        out.setparams((2,2,RATE,0,'NONE',''))
        for length,name,source in CUTS:
            size=round(length*RATE)*4
            if name in CARDS or name in STILLS:
                out.writeframesraw(bytes(size));continue
            af=f'volume=6dB,afade=in:d=0.015,afade=out:st={length-.055}:d=0.055'
            data=subprocess.check_output([FF,'-v','error','-ss',str(source),'-i',str(path_for(name)),'-t',str(length),'-vn','-map','0:a:0','-af',af,'-ar',str(RATE),'-ac','2','-f','s16le','-'])
            out.writeframesraw(data[:size].ljust(size,b'\0'))

def picture_edits():
    parts=[]
    for i,(length,name,source) in enumerate(CUTS):
        part=CACHE/f'shot-{i:02}.mp4'
        if name in CARDS: part=path_for(name)
        elif not part.exists():
            ff('-ss',source,'-i',path_for(name),'-an','-frames:v',frames(length),*ENC,part)
        parts.append(part)
    return parts

def compile_master():
    parts=picture_edits()
    # 用各段独立解码导出同一组编码参数，再无损拼接以兼容 Remotion 的 H.264 设置。
    for i,(_,name,_) in enumerate(CUTS):
        if name in CARDS:
            part=CACHE/f'graphic-{name}.mp4'
            ff('-i',parts[i],'-an','-vf','fps=60',*ENC,part)
            parts[i]=part
    (CACHE/'playlist.txt').write_text(''.join("file '"+p.resolve().as_posix()+"'\n" for p in parts),encoding='utf-8')
    ff('-f','concat','-safe',0,'-i',CACHE/'playlist.txt','-c','copy',CACHE/'picture.mp4')
    # 软字幕便于单独修订，同时给投稿版烧录字幕，底部预留游戏 HUD 的空间。
    srt=OUT/'narration.srt'
    subtitle_filter="subtitles=filename=narration.srt:force_style='FontName=Microsoft YaHei,FontSize=16,Outline=1.1,Shadow=0,MarginV=16,Alignment=2'"
    args=[FF,'-v','error','-nostdin','-y','-i',str(CACHE/'picture.mp4')]
    for track in ('Voice','Score','Game'): args.extend(['-i',track+'.wav'])
    args.extend(['-filter_complex','[1:a][2:a][3:a]amix=inputs=3:normalize=0,alimiter=limit=0.88:level=0:latency=1[m]','-map','0:v','-map','[m]','-vf',subtitle_filter,*ENC,'-c:a','aac','-b:a','320k','-ar','48000','-t','185','-movflags','+faststart','Areas-Hint.mp4'])
    assert srt.is_file()
    subprocess.run(args,cwd=OUT,check=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('stage',choices=['prepare','xml','shots','master'])
    parser.add_argument('--project',type=Path,default=Path('Q:/PR/tmp/素材.prproj'))
    args=parser.parse_args()
    if args.stage=='prepare': normalize(args.project);effects();timeline(args.project)
    elif args.stage=='xml': timeline(args.project)
    elif args.stage=='shots': picture_edits()
    else: compile_master()
