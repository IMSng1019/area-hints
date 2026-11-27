import React from 'react';
import {AbsoluteFill} from 'remotion';
import {Clip,Contours,Picture,aqua,gold,ink,latin,paper,sans,serif} from './visual';

const vertices: [number, number][] = [[360,70],[655,255],[548,625],[170,625],[62,255]];
const points=(scale=1)=>vertices.map(([x,y])=>`${360+(x-360)*scale},${360+(y-360)*scale}`).join(' ');
const polygon=points();

function BoundaryHud(){
  return <div style={{position:'absolute',right:76,top:70,width:760,height:820}}>
    <div style={{position:'absolute',left:18,top:24,color:aqua,fontFamily:latin,fontSize:22,fontWeight:700,letterSpacing:5}}>
      MULTI-LEVEL DOMAIN
    </div>
    <div style={{position:'absolute',right:20,top:18,color:'#b9d3ce',fontFamily:latin,fontSize:18,letterSpacing:3}}>
      LIYUE / AREA 01
    </div>
    <svg viewBox="0 0 720 720" width="760" height="760" style={{position:'absolute',left:0,top:55,overflow:'visible'}}>
      <defs>
        <linearGradient id="cover-polygon-fill" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor={gold} stopOpacity=".32"/>
          <stop offset=".55" stopColor={aqua} stopOpacity=".13"/>
          <stop offset="1" stopColor={gold} stopOpacity=".24"/>
        </linearGradient>
        <linearGradient id="cover-polygon-sweep" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor={aqua} stopOpacity="0"/>
          <stop offset=".44" stopColor={aqua} stopOpacity=".22"/>
          <stop offset=".62" stopColor={gold} stopOpacity=".11"/>
          <stop offset="1" stopColor={gold} stopOpacity="0"/>
        </linearGradient>
        <filter id="cover-polygon-glow" x="-40%" y="-40%" width="180%" height="180%">
          <feGaussianBlur stdDeviation="9" result="blur"/>
          <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
        </filter>
      </defs>
      <polygon points={points(.9)} fill="none" stroke={paper} strokeOpacity=".12" strokeWidth="2" strokeDasharray="12 14"/>
      <polygon points={points(.78)} fill="none" stroke={aqua} strokeOpacity=".18" strokeWidth="2" strokeDasharray="4 12"/>
      <polygon points={polygon} fill="url(#cover-polygon-fill)" stroke={gold} strokeWidth="4" filter="url(#cover-polygon-glow)"/>
      <polygon points={polygon} fill="url(#cover-polygon-sweep)"/>
      {vertices.map(([x,y],i)=><g key={i}>
        <circle cx={x} cy={y} r="18" fill={ink} fillOpacity=".82" stroke={paper} strokeWidth="4"/>
        <circle cx={x} cy={y} r="7" fill={i%2?aqua:gold}/>
        <text x={x+27} y={y-20} fill={paper} fillOpacity=".72" fontFamily={latin} fontSize="18" letterSpacing="2">
          P{i+1}
        </text>
      </g>)}
    </svg>
    <div style={{position:'absolute',right:28,bottom:18,display:'flex',alignItems:'baseline',gap:18}}>
      <span style={{color:'#b5cfca',fontFamily:sans,fontSize:24,fontWeight:700}}>边界已建立</span>
      <span style={{color:gold,fontFamily:latin,fontSize:38,fontWeight:700,letterSpacing:1}}>LIVE</span>
    </div>
  </div>;
}

function Chip({children,accent=false}:{children:React.ReactNode;accent?:boolean}){
  return <div style={{display:'flex',alignItems:'center',gap:13,padding:'13px 20px',border:`1px solid ${accent?gold:'#78938d'}`,
    borderRadius:999,background:'rgba(5,20,25,.72)',color:accent?gold:'#e2eae5',fontSize:29,fontWeight:700}}>
    <span style={{width:9,height:9,borderRadius:'50%',background:accent?gold:aqua}}/>{children}
  </div>;
}

export function Cover(){
  return <AbsoluteFill style={{background:ink,color:paper,fontFamily:sans,overflow:'hidden'}}>
    <Clip name="harbor-orbit" offset={120} style={{filter:'brightness(.7) saturate(1.12) contrast(1.08)',scale:1.045}}/>
    <AbsoluteFill style={{background:'linear-gradient(90deg,rgba(5,19,24,.98) 0%,rgba(5,19,24,.9) 36%,rgba(5,19,24,.37) 69%,rgba(5,19,24,.52) 100%)'}}/>
    <AbsoluteFill style={{background:'linear-gradient(0deg,rgba(5,19,24,.92) 0%,transparent 42%)'}}/>
    <Contours tone="#bad7cd" opacity={.13}/>

    <div style={{position:'absolute',left:96,top:70,display:'flex',alignItems:'center',gap:22,zIndex:4}}>
      <div style={{width:86,height:86,borderRadius:22,background:paper,display:'grid',placeItems:'center',boxShadow:'0 18px 55px rgba(0,0,0,.38)'}}>
        <Picture file="art/icon.png" style={{width:76,height:76,objectFit:'contain'}}/>
      </div>
      <div>
        <div style={{color:gold,fontFamily:latin,fontSize:21,fontWeight:700,letterSpacing:5}}>MINECRAFT FABRIC · 1.20.4</div>
        <div style={{fontFamily:serif,fontSize:58,fontWeight:700,lineHeight:1.08,letterSpacing:-1}}>Area Hints</div>
      </div>
    </div>

    <div style={{position:'absolute',left:96,top:238,width:900,zIndex:4,textShadow:'0 12px 40px rgba(0,0,0,.35)'}}>
      <div style={{fontFamily:serif,fontSize:172,fontWeight:700,lineHeight:.98,letterSpacing:-8}}>把域名</div>
      <div style={{fontFamily:serif,fontSize:188,fontWeight:700,lineHeight:1.03,letterSpacing:-9,color:gold}}>画进世界</div>
      <div style={{marginTop:32,fontSize:39,fontWeight:700,lineHeight:1.5,color:'#dbe6e0',letterSpacing:1}}>
        看得见、画得出、管得住的多级区域系统
      </div>
    </div>

    <div style={{position:'absolute',left:96,bottom:84,display:'flex',gap:16,zIndex:4}}>
      <Chip accent>可视化编辑</Chip>
      <Chip>多级域名</Chip>
      <Chip>地图联动</Chip>
    </div>

    <BoundaryHud/>
    <div style={{position:'absolute',right:82,bottom:42,color:'#93aea7',fontFamily:latin,fontSize:17,letterSpacing:3,zIndex:4}}>
      BOUNDARY VISUALIZATION / CLIENT + SERVER
    </div>
  </AbsoluteFill>;
}
