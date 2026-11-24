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
  for(const frame of [88,146,230,367,485,605,759,884,1015,1170,1376,1500,1640,1760,1844,2000,2065,2250,2350,2490]){
    await renderStill({...settings,composition,frame,scale:.5,output:path.resolve('out','frame-'+frame+'.png')});
  }
}else{
  let last=-1;
  await renderMedia({...settings,composition,codec:'h264',audioCodec:'aac',audioBitrate:'256k',crf:16,
    imageFormat:'jpeg',jpegQuality:96,colorSpace:'bt709',concurrency:4,
    outputLocation:path.resolve('out/Area-Hints.tmp.mp4'),
    onProgress:({progress})=>{const step=Math.floor(progress*20);if(step!==last){console.log(step*5+'%');last=step;}},
  });
  renameSync('out/Area-Hints.tmp.mp4','out/Area-Hints.mp4');
}
