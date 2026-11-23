import {existsSync, readFileSync, renameSync, writeFileSync} from 'node:fs';
import {spawnSync} from 'node:child_process';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const root = path.dirname(fileURLToPath(import.meta.url));
const base = path.dirname(root);
const cache = path.join(root, '.cache');
const output = path.join(base, 'Area-Hints.mp4');

export function publishFilm(picture, sound) {
  const edit = JSON.parse(readFileSync(path.join(base,'edit.json'), 'utf8'));
  const chapterFile = path.join(cache, 'chapters-'+process.pid+'.txt');
  const data = edit.chapters.map(({start,name}, i) => ['[CHAPTER]', 'TIMEBASE=1/'+edit.fps,
    'START='+start, 'END='+(edit.chapters[i+1]?.start ?? edit.frames), 'title='+name].join('\n'));
  writeFileSync(chapterFile, ';FFMETADATA1\n'+data.join('\n')+'\n');

  const bundled = path.join(process.env.LOCALAPPDATA || '', 'Programs/ChatCut/resources/app.asar.unpacked/node_modules/ffmpeg-ffprobe-static/ffmpeg.exe');
  const ffmpeg = process.env.FFMPEG || (existsSync(bundled) ? bundled : 'ffmpeg');
  const temp = path.join(cache, 'chaptered-'+process.pid+'.mp4');
  const seconds = edit.frames/edit.fps;
  // 保留 Remotion 的音画码流；AAC 最后一个包的有效采样与片尾对齐。
  const result = spawnSync(ffmpeg, ['-v','error','-nostdin','-y','-i',picture, ...(sound ? ['-i',sound] : []),
    '-f','ffmetadata','-i',chapterFile, '-map','0:v:0','-map',sound ? '1:a:0' : '0:a:0',
    '-map_chapters',sound ? '2' : '1','-c','copy','-t',String(seconds),
    '-bsf:a','setts=duration=min(DURATION\\,'+(seconds*48000)+'-PTS)', '-movflags','+faststart',temp], {stdio:'inherit'});
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error('封装投稿文件失败');
  renameSync(temp, output);
}

if (process.argv[2] === '--refresh-sound') {
  publishFilm(output, path.join(base,'audio','revision','Remotion-mix.m4a'));
}
