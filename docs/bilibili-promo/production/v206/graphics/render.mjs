import {existsSync, mkdirSync, renameSync} from 'node:fs';
import {publishFilm} from './publish.mjs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {bundle} from '@remotion/bundler';
import {selectComposition, renderMedia, renderStill} from '@remotion/renderer';

const root = path.dirname(fileURLToPath(import.meta.url));
const base = path.dirname(root);
const cache = path.join(root, '.cache');
const ids = process.argv.slice(2);
if (!ids.length) ids.push('Cover', 'Area-Hints');
if (ids.some(id => ['Area-Hints','Cover','Soundtrack'].includes(id))) await import('./sync-master.mjs');
mkdirSync(cache, {recursive:true});
mkdirSync(path.join(base,'media'), {recursive:true});
const serveUrl = await bundle({entryPoint:path.join(root,'index.jsx'), publicDir:path.join(root,'public'),
  outDir:path.join(cache,'bundle-'+process.pid)});
const browserExecutable = [process.env.REMOTION_BROWSER_EXECUTABLE,'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].find(p => p && existsSync(p));
const opts = {serveUrl, browserExecutable, port:Number(process.env.REMOTION_RENDER_PORT || 42680)};

for (const id of ids) {
  const master = id === 'Area-Hints';
  const composition = await selectComposition({...opts, id});
  if (id === 'Soundtrack') {
    await renderMedia({...opts, composition, codec:'aac', sampleRate:48000, audioBitrate:'320k',
      outputLocation:path.join(base,'audio','revision','Remotion-mix.m4a')});
    continue;
  }
  if (id === 'Cover') {
    await renderStill({...opts, composition, frame:0, imageFormat:'jpeg', jpegQuality:96,
      output:path.join(base, 'Area-Hints-cover.jpg')});
    continue;
  }
  const staging = path.join(cache, id+'-'+process.pid+'.mp4');
  let reported = -1;
  await renderMedia({...opts, composition, inputProps:composition.props, codec:'h264', colorSpace:'bt709',
    outputLocation:staging, concurrency:Number(process.env.REMOTION_CONCURRENCY || 6),
    jpegQuality:95, muted:!master, timeoutInMilliseconds:90000, x264Preset:'veryfast',
    ...(master ? {videoBitrate:'24M', encodingMaxRate:'42M', encodingBufferSize:'48M',
      sampleRate:48000, audioBitrate:'320k', audioCodec:'aac', metadata:{title:'Area Hints'}} : {crf:16}),
    onProgress:({progress}) => {
      const pct = Math.floor(progress * 20) * 5;
      if (pct !== reported) console.log(id, pct+'%');
      reported = pct;
    }});
  if (master) publishFilm(staging);
  else renameSync(staging, path.join(base, 'media', id+'.mp4'));
}
