import React from 'react';
import {AbsoluteFill, Audio, OffthreadVideo, Sequence, staticFile, useCurrentFrame} from 'remotion';
import edit from '../edit.json';
import {graphics} from './scenes';

const paperShots = new Set([5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 18, 19]);
const trackNames = {Voice: '普通话旁白', Score: '原创配乐', Game: '游戏声与音效'};
const fileName = (cut) => cut.path.split('/').pop();
const sceneFor = (cut) => graphics[fileName(cut).replace(/\.mp4$/, '')];

function atFrame(timestamp) {
  const [h, m, s, ms] = timestamp.split(/[:,]/).map(Number);
  return Math.round((((h * 60 + m) * 60 + s) + ms / 1000) * edit.fps);
}

export async function loadFilm({props}) {
  const response = await fetch(staticFile('master/narration.srt'));
  if (!response.ok) throw new Error('缺少总剪字幕，请先运行 npm run prepare:media。');
  const content = (await response.text()).trim().replace(/\r/g, '');
  const captions = content.split(/\n\s*\n/).map((block) => {
    const [, times, ...lines] = block.split('\n');
    const [start, end] = times.split(' --> ').map(atFrame);
    return {start, end, text: lines.join('\n')};
  });
  return {props: {...props, captions}};
}

function Subtitles({captions}) {
  const frame = useCurrentFrame();
  const cue = captions.find((c) => frame >= c.start && frame < c.end);
  if (!cue) return null;
  const cut = edit.clips.find((c) => frame >= c.start && frame < c.start + c.duration);
  const onPaper = paperShots.has(cut.shot);
  const clearHotbar = [1, 3, 20].includes(cut.shot) || (cut.shot === 17 && !sceneFor(cut));
  const vertical = cut.shot === 12 ? {top: 54} :
    {bottom: clearHotbar ? 158 : cut.shot === 4 ? 30 : 53};
  return <div style={{position:'absolute', left:64, right:64, ...vertical, textAlign:'center',
    fontFamily:'"Microsoft YaHei", "Noto Sans CJK SC", sans-serif', fontSize:48, lineHeight:1.26,
    whiteSpace:'pre-line', fontWeight:550, color:onPaper ? '#183b34' : '#fffdf6',
    WebkitTextStroke:onPaper ? '1px #f0ebd9' : '3.4px #172326', paintOrder:'stroke fill',
    textShadow:onPaper ? 'none' : '0 2px 6px #162127'}}>{cue.text}</div>;
}

export function Soundtrack({voiceGain = 1, scoreGain = 1, gameGain = 1}) {
  const gains = {Voice:voiceGain, Score:scoreGain, Game:gameGain};
  return Object.entries(trackNames).map(([id, label]) =>
    <Sequence key={id} name={label}>
      <Audio src={staticFile('master/' + id + '.wav')} volume={gains[id]}/>
    </Sequence>);
}

export function Film({captions = [], showCaptions = true, voiceGain, scoreGain, gameGain}) {
  return <AbsoluteFill style={{backgroundColor:'#122423'}}>
    {edit.clips.map((cut) => {
      const Graphic = sceneFor(cut);
      const file = fileName(cut);
      return <Sequence key={cut.start} from={cut.start} durationInFrames={cut.duration}
        name={`镜 ${cut.shot} · ${file.replace(/\.mp4$/, '')}`}>
        {Graphic ? <Graphic/> : <OffthreadVideo muted src={staticFile('master/' + file)}
          trimBefore={cut.in} style={{width:'100%', height:'100%', objectFit:'contain'}}/>}
      </Sequence>;
    })}
    <Soundtrack voiceGain={voiceGain} scoreGain={scoreGain} gameGain={gameGain}/>
    {showCaptions && <Sequence durationInFrames={edit.frames} name="中文字幕"><Subtitles captions={captions}/></Sequence>}
  </AbsoluteFill>;
}
