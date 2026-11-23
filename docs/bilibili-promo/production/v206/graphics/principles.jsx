import React from 'react';
import {useCurrentFrame} from 'remotion';
import {Canvas,Pic,Text,Window,Side,Svg,ramp,ease,pos,gold,green,ink,paper,soft} from './shared';

export function Intro(){
 const t=useCurrentFrame()/60, dissolve=ease(t,.55,2.3);
 return <Canvas><Svg><defs><pattern id="stone" width="110" height="62" patternUnits="userSpaceOnUse" patternTransform="rotate(-16)"><path d="M0 0h110v62H0z M55 0v30 M0 31h110" fill="none" stroke="#ccc8b5" strokeWidth="2"/></pattern></defs>
 <rect width="1920" height="1080" fill="url(#stone)"/>
 <path d="M1230 -40 L1130 240 1000 305 1050 440 870 660 975 850 880 1120 1990 1120 1990 -40Z" fill="#bfceb4" stroke={green} strokeWidth="9"/>
 {[[460,714,62],[693,615,75],[1128,530,64]].map(([x,y,angle],i)=><g key={i} opacity={ease(t,2+i*1.4,2.35+i*1.4)} transform={'translate('+x+' '+y+') rotate('+angle+')'} fill={i===2?'#ac6718':ink}><path d="M-26 2Q-34 -27 -20 -66Q-6 -94 18 -77Q42 -59 28 -21L15 8Z"/><path d="M-24 14L15 20 12 42Q-2 56 -19 43Z"/></g>)}</Svg>
 <Text x={1270} y={323} w={550} size={40} color={green} style={{opacity:ease(t,5.05,5.6)}}>璃月港</Text>
 <div style={{...pos(125,144,1200),opacity:ease(t,7.0,8.5)}}><div style={{fontSize:126,fontWeight:800,letterSpacing:-5}}>Areas Hint</div><div style={{fontSize:52,fontWeight:550}}>域名模组</div></div>
 {dissolve<1&&<Pic file="port.jpg" style={{position:'absolute',opacity:1-dissolve}}/>}</Canvas>
}
export function Book(){
 const t=useCurrentFrame()/60;
 return <Canvas><Pic file="tree.jpg" style={{...pos(1000,0,920,1080),objectPosition:'49% center'}}/><div style={{...pos(0,0,1250,1080),background:'linear-gradient(90deg,#f0ebd9 74%,transparent)'}}/><div style={{...pos(117,306,100,8),background:green}}/><Text x={115} y={363} w={920} size={81} style={{opacity:ease(t,0,.6),fontFamily:'SimSun,serif'}}>你会在这里{ '\n' }留下一句什么话？</Text></Canvas>
}
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
 return <Canvas><Svg><defs><pattern id="grid" width="82" height="66" x="188" y="150" patternUnits="userSpaceOnUse"><path d="M82 0H0V66" fill="none" stroke="#d6d5c5" strokeWidth="2"/></pattern></defs>
 <rect x="78" y="79" width="1125" height="746" rx="18" fill="url(#grid)" stroke="#cecdb9" strokeWidth="2"/>
 <rect x={px(0)-7} y={pz(0)-7} width={834} height={542} fill="none" stroke="#718377" strokeWidth="4" strokeDasharray="15 11"/>
 <polygon points={cornerPoints.map(([x,z])=>[px(x),pz(z)].join(',')).join(' ')} fill="#bfd0ae" stroke={green} strokeWidth="7" strokeLinejoin="round"/>
 <text x={px(5)} y={pz(1.7)} fontSize="37" fill={green}>村庄</text>
 {ray&&<g opacity={opacity}><line x1={px(x)} x2={Math.max(px(x),end)} y1={pz(5)} y2={pz(5)} stroke="#ad7626" strokeWidth="6"/>{hitXs.filter(ix=>px(ix)<=end).map(ix=><circle key={ix} cx={px(ix)} cy={pz(5)} r="17" fill={paper} stroke="#98671c" strokeWidth="7"/>)}</g>}
 <circle cx={px(x)} cy={pz(5)} r="16" fill={gold} stroke={ink} strokeWidth="5"/>
 <text x="949" y="778" fontSize="27" fill={green}>+X →</text><text x="112" y="775" fontSize="27" fill={green}>+Z ↓</text></Svg>
 <Text x={79} y={855} w={700} size={29} color={green}>边界示意</Text>
 {ray?<><Text x={1304} y={239} w={540} size={39} color={green}>向右数交点</Text><Text x={1288} y={313} w={520} size={166}>{t<2.35?'·':hitXs.length}</Text><Text x={1304} y={578} w={520} size={60}>{x<4?'还在村外':'走进村庄'}</Text></>:<><Text x={1284} y={210} w={550} size={63}>{'已经进框，\n为什么还在村外？'}</Text><Text x={1286} y={559} w={490} size={32} color={green}>高度范围 · 侧视</Text><svg style={pos(1286,626,390,143)}><rect x="39" y="20" width="320" height="86" fill="#d4ddc6"/><path d="M20 20H360M20 106H360" stroke={green} strokeWidth="3" strokeDasharray="9 6"/><circle cx="184" cy="61" r="13" fill={gold} stroke={ink} strokeWidth="4"/></svg></>}</Canvas>
}
export function Box(){return <Diagram t={useCurrentFrame()/60} ray={false}/>}
export function Ray(){return <Diagram t={useCurrentFrame()/60} ray/>}
