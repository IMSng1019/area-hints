import {existsSync,mkdirSync,renameSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {bundle} from '@remotion/bundler';
import {getCompositions,renderMedia} from '@remotion/renderer';

const root=path.dirname(fileURLToPath(import.meta.url));
const out=path.resolve(root,'../media');
const cache=path.join(root,'.cache');
mkdirSync(out,{recursive:true}); mkdirSync(cache,{recursive:true});
const serveUrl=await bundle({entryPoint:path.join(root,'index.jsx'), publicDir:path.join(root,'public'),outDir:path.join(cache,'bundle-'+process.pid)});
const browserExecutable=[process.env.REMOTION_BROWSER_EXECUTABLE,'C:/Program Files/Google/Chrome/Application/chrome.exe','C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].find(p=>p&&existsSync(p));
const opts={serveUrl,browserExecutable,port:Number(process.env.REMOTION_RENDER_PORT||42680)};
const comps=await getCompositions(opts);
const ids=process.argv.slice(2);
for(const id of ids){if(!comps.some(c=>c.id===id))throw new Error('Unknown composition '+id)}
for(const c of comps.filter(c=>!ids.length||ids.includes(c.id))){
 const staging=path.join(cache,c.id+'-'+process.pid+'.mp4');
 let last=-1;
 await renderMedia({...opts,composition:c,codec:'h264',colorSpace:'bt709',crf:16,concurrency:Number(process.env.REMOTION_CONCURRENCY||4),muted:true,
  jpegQuality:94,outputLocation:staging,onProgress:({progress})=>{const step=Math.floor(progress*4);if(step!==last)console.log(c.id,step*25+'%');last=step;}});
 renameSync(staging,path.join(out,c.id+'.mp4'));
}
