import {existsSync,mkdirSync,renameSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {bundle} from '@remotion/bundler';
import {renderMedia,renderStill,selectComposition} from '@remotion/renderer';

const root=fileURLToPath(new URL('..',import.meta.url));
process.chdir(root);
mkdirSync('out',{recursive:true});
const browserExecutable=[process.env.REMOTION_BROWSER_EXECUTABLE,
  'node_modules/.remotion/chrome-headless-shell/win64/chrome-headless-shell-win64/chrome-headless-shell.exe',
  'C:/Program Files/Google/Chrome/Application/chrome.exe'].find(p=>p&&existsSync(p));
const serveUrl=await bundle({entryPoint:path.resolve('src/index.tsx'),publicDir:path.resolve('public')});
const settings={serveUrl,browserExecutable:browserExecutable?path.resolve(browserExecutable):undefined};
const composition=await selectComposition({...settings,id:'Area-Hints'});
if(process.argv.includes('--stills')){
  for(const second of [1,3,5,7,10,14,19,28,35,41,47,51,55,60,65,72,77,81,86,92,100,106,115,122,128]){
    const frame=Math.round(second*composition.fps);
    await renderStill({...settings,composition,frame,scale:.5,output:path.resolve('out','frame-'+frame+'.png')});
  }
}else{
  // 单独导出四格时从整片取相同的帧区间，保留同步配乐和录点提示音。
  const toolsOnly=process.argv.includes('--tools');
  const output=toolsOnly?'编辑四格-v7':'Area-Hints-v7';
  let last=-1;
  await renderMedia({...settings,composition,codec:'h264',audioCodec:'aac',audioBitrate:'256k',crf:16,
    imageFormat:'jpeg',jpegQuality:96,colorSpace:'bt709',concurrency:4,
    ...(toolsOnly?{frameRange:[46*composition.fps,61*composition.fps-1]}:{}),
    outputLocation:path.resolve('out',output+'.tmp.mp4'),
    onProgress:({progress})=>{const step=Math.floor(progress*20);if(step!==last){console.log(step*5+'%');last=step;}},
  });
  renameSync(path.join('out',output+'.tmp.mp4'),path.join('out',output+'.mp4'));
}
