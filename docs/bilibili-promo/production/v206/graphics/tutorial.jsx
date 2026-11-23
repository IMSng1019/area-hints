import React from 'react';
import {useCurrentFrame} from 'remotion';
import {Canvas,Text,Pic,Window,Side,Clip,Svg,ease,ramp,ink,green,paper,gold,pos} from './shared';

const blocks=[[.16,.13],[.31,.13],[.46,.13],[.61,.13],[.76,.13],[.16,.38],[.31,.38],[.46,.38],[.61,.38],[.76,.38],[.31,.64],[.46,.64],[.61,.64],[.76,.64]];
// 同一条街线驱动路面、走过的轨迹和玩家位置，经过拐角时三者始终重合。
const route=[[65,342],[437,342],[462,347],[479,361],[491,391],[512,425],[551,440],[708,440],[708,377]];
const street=route.map(p=>p.join(',')).join(' ');
const lengths=route.slice(1).map(([x,y],i)=>Math.hypot(x-route[i][0],y-route[i][1]));
const routeLength=lengths.reduce((a,b)=>a+b,0);
function routePosition(progress){
 let distance=progress*routeLength;
 for(let i=0;i<lengths.length;i++){
   if(distance<=lengths[i]){
     const k=distance/lengths[i];
     return route[i].map((v,axis)=>v+(route[i+1][axis]-v)*k);
   }
   distance-=lengths[i];
 }
 return route[route.length-1];
}
function CityPlan({t}){
 const a=ease(t,.15,2),b=ease(t,6.4,9.8);
 const progress=.49*a+.51*b;
 const [x,y]=routePosition(progress);
 return <Svg viewBox="0 0 1280 720"><rect width="1280" height="720" fill="#dfe2cc"/>
 <path d="M-30 680Q185 695 259 631T300 479Q319 414 425 411T643 433Q759 437 887 343T1305 312" fill="none" stroke="#91b9b1" strokeWidth="90"/>
 <path d="M116 93L261 69 482 89 590 47 1084 59 1166 190 1175 568 1052 644 384 642 356 585 113 571Z" fill="#c2cbb0" stroke={green} strokeWidth="10"/>
 {blocks.map(([x,y],i)=><g key={i} transform={'translate('+1280*x+' '+720*y+')'}><rect x="0" y="28" width="106" height="64" rx="5" fill="#97a88c"/><rect x="6" y="19" width="93" height="65" rx="5" fill="#eee5cf" stroke="#83957c" strokeWidth="3"/><path d="M6 53h94" stroke="#c3b79b" strokeWidth="4"/></g>)}
 <path d="M551 261L1022 261 1022 423 711 422 711 544 551 544Z" fill={gold} fillOpacity={.15+b*.28} stroke="#997637" strokeWidth="6" strokeDasharray="14 8"/>
 <polyline points={street} fill="none" stroke="#f8f2df" strokeWidth="37" strokeLinejoin="round" strokeLinecap="round"/>
 <polyline points={street} fill="none" stroke="#b27d32" strokeWidth="4" strokeLinejoin="round" strokeLinecap="round" pathLength={1} strokeDasharray="1" strokeDashoffset={1-progress}/>
 <text x="158" y="537" fontSize="41" fill={green}>主城</text><text x="823" y="397" fontSize="40" fill={ink}>街区</text>
 <circle cx={x} cy={y} r={23+5*b} fill={gold} opacity=".28"/>
 <circle cx={x} cy={y} r="15" fill={gold} stroke={ink} strokeWidth="4"/></Svg>
}
export function City(){
 const t=useCurrentFrame()/60;
 return <Canvas><Window><CityPlan t={t}/><div style={{position:'absolute',inset:0,opacity:ease(t,2,2.34)*(1-ease(t,5.6,6))}}>
 <Clip from={2} seconds={4} file="walls"/></div></Window><Side kicker="域名层级" lines={'进城以后，\n还能知道自己\n在哪条街吗？'}/></Canvas>
}
export function Levels(){
 return <Canvas><Window><Clip seconds={10} file="levels"/></Window><Side kicker="测试世界 · 域名层级" lines={'每走进一级，\n名字也跟着变。'}/></Canvas>
}
function Nav({active}){
 return <div style={{...pos(82,876,1280,50),fontSize:26,color:'#778979',display:'flex',gap:32}}>{['命名','划界','生效'].map((name,i)=><span key={name} style={{color:i===active?green:'#8d9886',fontWeight:i===active?750:400,borderBottom:i===active?'4px solid '+green:'none'}}>{name}</span>)}</div>
}
export function Naming(){
 const t=useCurrentFrame()/60;
 const zoom=ease(t,1.04,1.75), panels=1-ease(t,1,1.45);
 const width=555+(1280-555)*zoom;
 return <Canvas>
 {['corner','save'].map((file,i)=><div key={file} style={{...pos(682+i*604+zoom*75,292,555,370),opacity:panels*(.35+.65*ease(t,.12+i*.22,.35+i*.22))}}>
   <Pic file={file+'.jpg'} style={{height:312}}/><Text x={0} y={333} w={555} size={38}>{['记录拐角','确认保存'][i]}</Text>
 </div>)}
 <div style={{...pos(78-2*zoom,292-158*zoom,width,width*9/16),background:ink,border:'2px solid #34574b',overflow:'hidden'}}>
   <Pic file="name.jpg"/><div style={{position:'absolute',inset:0,opacity:ease(t,1.5,1.82)}}>
     <Clip from={1.5} seconds={2} file="command"/><Clip from={3.5} seconds={4.5} file="village"/>
   </div>
 </div>
 <Text x={78} y={625} w={555} size={38} style={{opacity:panels}}>起名</Text>
 <div style={{position:'absolute',inset:0,opacity:ease(t,1.38,1.92)}}><Side kicker="创建域名" lines={'就叫它\n“村庄”。'}/><Nav active={0}/></div>
 </Canvas>
}
export function Recording(){
 const t=useCurrentFrame()/60;
 return <Canvas><Window><Clip seconds={4} file="level"/><Clip from={4} seconds={7} file="points"/></Window>
 {t<4?<Side kicker="创建入口" lines={<span style={{fontFamily:'Consolas, monospace',fontSize:42}}>/areahint easyadd</span>} small="域名等级 · 1"/>:<Side kicker="记录顶点" lines={'走到拐角，\n按一下 X。'} small="绑定键 · 默认 X"/>}<Nav active={1}/></Canvas>
}
function RowDiagram({t}){
 const line='M96 523H368V435H605V255H867V431H1140';
 return <Svg viewBox="0 0 1280 720"><rect width="1280" height="720" fill="#dce2cc"/>
 {[[150,532],[306,538],[477,454],[646,288],[752,288],[911,457],[1034,457]].map(([x,y])=><g transform={'translate('+x+' '+y+')'} key={x}><rect width="92" height="91" fill="#c2b390"/><path d="M-10 12L47 -27 103 12V53H-10Z" fill="#718a77"/></g>)}
 <path d={line} fill="none" stroke="#bac5ae" strokeWidth="8"/><path d={line} fill="none" stroke={green} strokeWidth="8" pathLength="1" strokeDasharray="1" strokeDashoffset={1-ease(t,.1,2.65)}/>
 {[[96,523],[368,523],[368,435],[605,435],[605,255],[867,255],[867,431],[1140,431]].map(([x,y],i)=><circle key={i} cx={x} cy={y} r="10" opacity={ease(t,i*.33,i*.33+.2)} fill={gold} stroke={ink} strokeWidth="3"/>)}
 </Svg>
}
export function Corners(){
 const t=useCurrentFrame()/60;
 return <Canvas><Window><RowDiagram t={t}/><Clip from={3} seconds={3} file="row"/><Clip from={6} seconds={4} file="point27"/></Window><Side kicker="村庄边界" lines={t<6?'沿着屋檐，\n让边界转弯。':'第 27 个点'} small={t<6?'记录顶点':'村界已经闭合'}/><Nav active={1}/></Canvas>
}
