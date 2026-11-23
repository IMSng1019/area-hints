import React from 'react';
import {useCurrentFrame} from 'remotion';
import data from './map.json';
import {Canvas,Text,Svg,Pic,green,ink,paper,gold,ease,ramp,pos} from './shared';

const contours = data.areas.map(area=>({...area,path:'M'+area.v.map(p=>p.join(' ')).join('L')+'Z'}));
const xs=contours.flatMap(area=>area.v.map(p=>p[0]));
const zs=contours.flatMap(area=>area.v.map(p=>p[1]));
const minX=Math.min(...xs),maxX=Math.max(...xs),minZ=Math.min(...zs),maxZ=Math.max(...zs);
const full={x:(maxX+minX)/2,z:(minZ+maxZ)/2,s:Math.min(1195/(maxX-minX),722/(maxZ-minZ))};
const [px,,pz]=data.point;
const target={x:px,z:pz,s:1.45};
const wind=data.checked.at(-1).name;
const swatches=['#65816d','#a4b398','#a7a98a','#899a80','#b69e72','#7c9793'];
const top=contours.filter(a=>a.level===1);
const names=data.checked.map(a=>a.name);
const sum=data.checked.reduce((n,a)=>n+a.edges,0);
const fmt=n=>n.toLocaleString('en-US');
function mix(a,b,t){return a+(b-a)*t}
function camera(progress){
  // 以比率控制缩放，并让示例点平稳移到截图转场的锚点。
  const s=full.s*Math.pow(target.s/full.s,progress);
  const startX=(px-full.x)*full.s, startZ=(pz-full.z)*full.s;
  return {x:px-startX*(1-progress)/s,z:pz-startZ*(1-progress)/s,s};
}
function MapField({progress=0,level=3,active=-1,fade=0}){
 const {x,z,s}=camera(progress);
 return <svg style={pos(72,83,1320,810)} viewBox="0 0 1320 810">
 <defs><pattern id="carto-grid" width="62" height="62" patternUnits="userSpaceOnUse"><path d="M62 0H0V62" fill="none" stroke="#e0dcca" strokeWidth="2"/></pattern></defs>
 <rect width="1320" height="810" fill="url(#carto-grid)"/>
 <g transform={'translate(660 405) scale('+s+') translate('+(-x)+' '+(-z)+')'}>
 {[...contours].sort((a,b)=>a.level-b.level).map((area,i)=>{
   const idx=names.indexOf(area.name);
   const highlighted=active>=0&&idx===active;
   const isTop=area.level===1;
   const opacity=ramp(level-area.level+1,0,1)*(highlighted?1:1-fade*.84);
   const fill=isTop?swatches[(top.findIndex(a=>a.name===area.name)+swatches.length)%swatches.length]:'#d1dac3';
   return <path key={i} d={area.path} fill={highlighted?'#a5c6a7':fill} fillOpacity={highlighted?.85:isTop?.82:.12} stroke={highlighted?green:isTop?'#495d50':'#677a68'} strokeWidth={highlighted?5:isTop?2.7:1.2} vectorEffect="non-scaling-stroke" opacity={opacity}/>;
 })}
 </g>
 {active>=0&&<><circle cx={660+(px-x)*s} cy={405+(pz-z)*s} r="11" fill={gold} stroke={ink} strokeWidth="4"/><text x={680+(px-x)*s} y={390+(pz-z)*s} fontSize="33" fill={ink} fontWeight="650">{active===names.length-1?wind:'示例位置'}</text></>}
 </svg>
}
export function Atlas(){
 const t=useCurrentFrame()/60;
 return <Canvas><MapField level={1+ease(t,1.8,3.6)+ease(t,3.95,5.9)}/><Text x={1425} y={151} w={450} size={35} color={green}>方块提瓦特 · 主世界</Text>
 <Text x={1415} y={255} w={465} size={119}>{fmt(data.total)}</Text><Text x={1425} y={417} w={435} size={36}>个域名</Text>
 <Text x={1415} y={508} w={490} size={108} style={{opacity:ease(t,2.6,3.4)}}>{fmt(data.edges)}</Text><Text x={1425} y={658} w={438} size={36} style={{opacity:ease(t,2.6,3.4)}}>个一级顶点</Text>
 <Text x={88} y={872} w={1200} color={green} size={31}>{Object.entries(data.levels).map(([l,n])=>l+' 级 · '+n).join('     /     ')}</Text></Canvas>
}
export function Search(){
 const t=useCurrentFrame()/60;
 const i=t<4.6?0:t<9.1?1:2;
 return <Canvas><MapField progress={ease(t,.2,12.4)} active={i} fade={ease(t,0,1.5)}/>
 <Text x={1420} y={152} w={435} size={37} color={green}>从上级向下查找</Text>
 {data.checked.map((a,j)=><div key={a.name} style={{...pos(1420,271+136*j,460),opacity:i>=j?1:.24}}><div style={{fontSize:28,color:green}}>{j+1} 级域名</div><div style={{fontSize:58,fontWeight:680,color:i===j?green:ink}}>{a.name}</div></div>)}
 <Text x={90} y={866} w={1120} size={31} color={green}>示例坐标　({data.point.join(', ')})</Text></Canvas>
}
export function Workload(){
 const t=useCurrentFrame()/60;
 return <Canvas><MapField progress={1} active={2} fade={1}/><Text x={1418} y={154} w={460} size={37} color={green}>参与精查的域名</Text>
 {data.checked.map((a,i)=><div key={a.name} style={{...pos(1420,245+i*76,400),display:'flex',justifyContent:'space-between',fontSize:41,opacity:ease(t,i*.25,i*.25+.36)}}><span>{a.name}</span><b>{a.edges}</b></div>)}
 <div style={{...pos(1418,499,406,4),background:green}}/><Text x={1413} y={526} w={440} size={100}>{sum}<span style={{fontSize:37}}> 条边</span></Text>
 <Text x={1418} y={698} w={450} size={28} color={green}>{data.checked.map(a=>a.edges).join(' + ')} = {sum}</Text>
 <Text x={88} y={868} w={1130} size={31} color={green}>示例坐标 · 数据文件复算</Text></Canvas>
}
// 截图像素来自 Xaero 原图，X/Z 轮廓与截图均保持北上东右。
function Screenshot({cx,cy,s,anchorX,anchorY,t}){
 return <><Pic file="xaero.png" style={{...pos(anchorX-cx*s,anchorY-cy*s,2972*s,1914*s),maxWidth:'none',objectFit:'fill'}}/>
 <div style={{...pos(76,63,520,80),background:'#173632ee',color:paper,display:'grid',placeContent:'center',fontSize:40,fontWeight:660}}>Xaero 世界地图联动</div></>
}
export function MapIn(){
 const t=useCurrentFrame()/60, u=ease(t,1.4,8.4);
 return <Canvas><MapField progress={1} active={2} fade={1}/><div style={{...pos(0,0,1920,1080),opacity:ramp(t,0,.25)}}>
 <Screenshot cx={mix(2207,1486,u)} cy={mix(549,957,u)} s={mix(1.8,.655,u)} anchorX={mix(735,960,u)} anchorY={mix(480,540,u)}/></div></Canvas>
}
export function MapPort(){
 const t=useCurrentFrame()/60,u=ease(t,.15,5.65);
 return <Canvas><Screenshot cx={mix(1486,1500,u)} cy={mix(957,1635,u)} s={mix(.655,2.1,u)} anchorX={960} anchorY={540}/></Canvas>
}
