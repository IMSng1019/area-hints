import React from 'react';
import {AbsoluteFill, Sequence, useCurrentFrame} from 'remotion';
import {Board, Clip, Picture, box, serif, tween} from './visual';

export const photoShade = 'linear-gradient(to bottom,rgba(0,0,0,.14),transparent 23%,transparent 70%,rgba(0,0,0,.18))';
export const harborExposure = (openingFrame: number) => tween(openingFrame,320,339);

export function HarborPaper() {
  return <AbsoluteFill style={{background: [
    'radial-gradient(ellipse at 26% 35%,#fff7deb8,transparent 33%)',
    'radial-gradient(ellipse at 13% 73%,#93c3ae9e,transparent 53%)',
    'radial-gradient(ellipse at 98% 93%,#cddcc785,transparent 47%)',
    'linear-gradient(112deg,#dae8d5 2%,#f5ead4 48%,#f8f3e5 82%,#f1efdc)',
  ].join(',')}}/>;
}

const titleGreen = '#1b3933';

function HarborTitle({frame}: {frame: number}) {
  const emblem = tween(frame,10,34);
  const reveal = tween(frame,25,51);
  const question = tween(frame,9,29);
  // 遮罩从字标左缘扫过，在拨弦落点附近读出完整片名。
  const edge = reveal * 116 - 16;
  return <div style={{...box(970,351,875,380),display:'flex',alignItems:'center',flexDirection:'column',justifyContent:'center',color:titleGreen}}>
    <svg width="204" height="41" viewBox="0 0 204 41" aria-hidden="true" style={{opacity:emblem}}>
      <defs><linearGradient id="harbor-rule" gradientUnits="userSpaceOnUse" x1="0" x2="74" y1="21" y2="21"><stop stopColor="#a3894f" stopOpacity="0"/><stop offset="1" stopColor="#a3894f"/></linearGradient></defs>
      <path d="M0 21h74" stroke="url(#harbor-rule)" strokeWidth="1.5"/>
      <path d="M0 21h74" transform="translate(204 0) scale(-1 1)" stroke="url(#harbor-rule)" strokeWidth="1.5"/>
      <path d="M101 3l17 10-8 23-19-6-6-15Z" fill="none" stroke="#8f7646" strokeWidth="2.3"
        pathLength="1" strokeDasharray="1 1" strokeDashoffset={1-emblem} strokeLinejoin="round"/>
    </svg>
    <div style={{fontFamily:serif,fontSize:154,letterSpacing:-8,lineHeight:1.16,whiteSpace:'nowrap',padding:'0 18px 3px',
      maskImage:`linear-gradient(90deg,black ${edge}%,transparent ${edge+16}%)`,
      transform:`translateY(${(1-reveal)*13}px)`}}>
      Area <i style={{color:'#376455'}}>Hints</i>
    </div>
    <div style={{fontFamily:'"STZhongsong","SimSun",serif',fontSize:42,letterSpacing:.8,color:'#314c43',marginTop:9,
      opacity:question,translate:`0 ${(1-question)*9}px`}}>想要一个像原神一样跳字的模组？</div>
  </div>;
}

export function Brand() {
  const frame = useCurrentFrame();
  const pull = 1 - tween(frame,5,41);
  const drift = tween(frame,34,140);
  const land = tween(frame,4,18);
  // 所有实景层共用取景坐标；码头和草坡在拉开镜头时相对后山多走一点。
  const layerOrigin = '540px 580px';
  return <AbsoluteFill style={{overflow:'hidden'}}>
    <HarborPaper/>
    <AbsoluteFill style={{transformOrigin:layerOrigin,opacity:land,
      transform:`translate(${pull*89-drift*3}px,${pull*18-drift*1.5}px) scale(${1+.27*pull})`}}>
      <AbsoluteFill><Picture file="art/brand-water.webp"/></AbsoluteFill>
      <AbsoluteFill><Picture file="art/brand-harbor.webp"/></AbsoluteFill>
    </AbsoluteFill>
    <AbsoluteFill style={{transformOrigin:layerOrigin,opacity:land,
      transform:`translate(${pull*108+drift*2}px,${pull*23+drift}px) scale(${1+.29*pull})`}}>
      <Picture file="art/brand-wharf.webp"/>
    </AbsoluteFill>
    <HarborTitle frame={frame}/>
    <AbsoluteFill style={{transformOrigin:'50% 100%',opacity:tween(frame,9,29)*.87,
      transform:`translateY(${pull*77+drift*2}px) scale(${1.012+pull*.07})`}}>
      <Picture file="art/brand-grass.svg"/>
    </AbsoluteFill>
    {/* 实景沿用上一镜的下一帧，与前段共用同一条淡出曲线。 */}
    <Sequence durationInFrames={9} name="接璃月港远景">
      <AbsoluteFill style={{opacity:1-harborExposure(frame+330)}}>
        <Clip name="harbor-push" offset={150}/><AbsoluteFill style={{background:photoShade}}/>
      </AbsoluteFill>
    </Sequence>
    {frame>=137 && <AbsoluteFill style={{opacity:tween(frame,137,149)}}><Board/></AbsoluteFill>}
  </AbsoluteFill>;
}
