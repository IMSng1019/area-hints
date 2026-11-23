import React from 'react';
import {useCurrentFrame} from 'remotion';
import {Canvas,Text,Svg,ramp,ease,pos,gold,green,ink,paper} from './shared';

export {Intro, Book} from './interludes';

export const cornerPoints=[[0,0],[10,0],[10,8],[4,8],[4,3],[0,3]];
function Diagram({ray,t}){
 const x=ray?2+4*ease(t,3.3,5.3):2;
 const hitXs=cornerPoints.flatMap(([ax,az],i)=>{
   const [bx,bz]=cornerPoints[(i+1)%cornerPoints.length];
   if ((az<=5&&5<bz)||(bz<=5&&5<az)){
     const ix=ax+(5-az)*(bx-ax)/(bz-az);
     if(ix>x)return [ix];
   }
   return [];
 }).sort((a,b)=>a-b);
 const px=x=>188+82*x,pz=z=>150+66*z;
 const end=ray?px(2)+((px(12)-px(2))*ramp(t,.9,2.55)):px(x);
 const opacity=ray?1-ease(t,8.05,8.75):1;
 const crossings=hitXs.filter(ix=>px(ix)<=end);
 return <Canvas><Svg><defs>
 <pattern id="grid" width="82" height="66" x="188" y="150" patternUnits="userSpaceOnUse"><path d="M82 0H0V66" fill="none" stroke="#d6d5c5" strokeWidth="2"/></pattern>
 <linearGradient id="village-wash" x1="0%" y1="0%" x2="90%" y2="90%"><stop stopColor="#dbe4c6"/><stop offset="1" stopColor="#a5bea3"/></linearGradient>
 <linearGradient id="ray-warm"><stop stopColor="#e6ab41"/><stop offset="1" stopColor="#98651e"/></linearGradient></defs>
 <rect x="78" y="79" width="1125" height="746" rx="18" fill="url(#grid)" stroke="#cecdb9" strokeWidth="2"/>
 <rect x={px(0)-7} y={pz(0)-7} width={834} height={542} fill="none" stroke="#718377" strokeWidth="4" strokeDasharray="15 11"/>
 <polygon points={cornerPoints.map(([x,z])=>[px(x),pz(z)].join(',')).join(' ')} fill="url(#village-wash)" stroke={green} strokeWidth="7" strokeLinejoin="round"/>
 {cornerPoints.map(([vx,vz])=><rect key={vx+','+vz} x={px(vx)-4} y={pz(vz)-4} width="8" height="8" fill="#f5efd9" stroke={green} strokeWidth="2"/>)}
 <text x={px(5)} y={pz(1.7)} fontSize="37" fill={green}>村庄</text>
 {ray&&<g opacity={opacity}>
   <line x1={px(x)} x2={Math.max(px(x),end)} y1={pz(5)} y2={pz(5)} stroke="url(#ray-warm)" strokeWidth="6"/>
   {end>px(x)+20&&<path d={`M${end-14} ${pz(5)-8}l15 8-15 8`} fill="none" stroke="#98651e" strokeWidth="4"/>}
   {crossings.map(ix=>{
     // 波纹从射线真正到达边线的那一帧开始。
     const impact=.9+1.65*(ix-2)/10;
     const pulse=ease(t,impact,impact+.68);
     return <g key={ix} transform={`translate(${px(ix)} ${pz(5)})`}>
       <circle r={15+35*pulse} fill="none" stroke="#bf852e" opacity={1-pulse} strokeWidth="3"/>
       <circle r={13+4*(1-pulse)} fill={paper} stroke="#98671c" strokeWidth="5"/>
       <circle r="3.5" fill="#98671c"/>
     </g>;
   })}
 </g>}
 <circle cx={px(x)} cy={pz(5)} r="16" fill={gold} stroke={ink} strokeWidth="5"/>
 <text x="949" y="778" fontSize="27" fill={green}>+X →</text><text x="112" y="775" fontSize="27" fill={green}>+Z ↓</text></Svg>
 <Text x={79} y={855} w={700} size={29} color={green}>边界示意</Text>
 {ray?<><Text x={1304} y={239} w={540} size={39} color={green}>向右数交点</Text><Text x={1288} y={300} w={520} size={172} style={{fontFamily:'Georgia,serif',fontVariantNumeric:'tabular-nums'}}>{t<.9?'·':crossings.length}</Text><Text x={1304} y={578} w={520} size={60}>{x<4?'还在村外':'走进村庄'}</Text></>:<><Text x={1284} y={210} w={550} size={63}>{'已经进框，\n为什么还在村外？'}</Text><Text x={1286} y={559} w={490} size={32} color={green}>高度范围 · 侧视</Text><svg style={pos(1286,626,390,143)}><rect x="39" y="20" width="320" height="86" fill="#d4ddc6"/><path d="M20 20H360M20 106H360" stroke={green} strokeWidth="3" strokeDasharray="9 6"/><circle cx="184" cy="61" r="13" fill={gold} stroke={ink} strokeWidth="4"/></svg></>}</Canvas>
}
export function Box(){return <Diagram t={useCurrentFrame()/60} ray={false}/>}
export function Ray(){return <Diagram t={useCurrentFrame()/60} ray/>}
