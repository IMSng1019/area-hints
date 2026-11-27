import React from 'react';
import {AbsoluteFill,staticFile,useCurrentFrame} from 'remotion';
import {Board,Clip,Picture,box,gold,ink,paper,sans,serif,tween} from './visual';

export function Uses() {
  const frame=useCurrentFrame();
  const captions=['开一个生存 RPG 服','标记红石机器','为地图填色'];
  return <Board>
    <div style={{...box(94,70,1732,105),fontFamily:serif,fontSize:76,fontWeight:700}}>把域名用在自己的世界里</div>
    {captions.map((caption,i)=><div key={caption} style={{...box(94+i*588,215,554,572),opacity:tween(frame,3+i*9,30+i*9)}}>
      <div style={{height:434,overflow:'hidden'}}>
        {i===0?<Clip name="harbor-orbit" offset={188} style={{objectPosition:'48% 50%'}}/>:i===1?<Clip name="redstone"/>:<Picture file="art/xaero.jpg" style={{objectPosition:'72% 45%'}}/>}
      </div>
      <div style={{borderTop:'3px solid '+gold,paddingTop:23,fontSize:50,fontWeight:700,color:paper}}>{caption}</div>
    </div>)}
    <div style={{...box(94,848,1740,150),fontFamily:serif,fontWeight:700,fontSize:77,opacity:tween(frame,125,153)}}>你想要的，<span style={{color:gold}}>Area Hints</span> 都能实现。</div>
    <AbsoluteFill style={{background:'#fff',opacity:tween(frame,300,329)}}/>
  </Board>;
}
export function Credits() {
  const f=useCurrentFrame();
  return <AbsoluteFill style={{background:'#fff',fontFamily:sans,color:ink,alignItems:'center',justifyContent:'center'}}>
    <div style={{width:1060,textAlign:'center',opacity:tween(f,0,23)*(1-tween(f,128,149))}}>
      <div style={{fontFamily:serif,fontSize:94,fontWeight:700,marginBottom:28}}>制作名单</div>
      {[['模组作者','IMSng'],['分镜与实机录制','创作团队'],['实机场景','方块提瓦特']].map(([role,name])=>
        <div key={role} style={{display:'flex',justifyContent:'center',alignItems:'baseline',gap:28,padding:'12px 0'}}>
          <span style={{fontSize:50,fontWeight:700,color:'#42545b'}}>{role}</span>
          <span style={{fontSize:68,fontWeight:700,lineHeight:1.3}}>{name}</span>
        </div>)}
    </div>
  </AbsoluteFill>;
}
export function Logo(){
  const frame=useCurrentFrame();
  // 遮罩沿官方图标的五条边展开，落版时保留原图的顶点与线宽。
  const progress=tween(frame,0,56);
  return <AbsoluteFill style={{background:'#fff',color:'#101a1f',alignItems:'center',justifyContent:'center'}}>
    <svg viewBox="0 0 512 512" width="284" height="284" style={{opacity:tween(frame,0,9)}}>
      <defs><mask id="closing-outline"><path d="M242 29 L483 168 L375 482 L108 405 L32 202 Z"
        fill="none" stroke="white" strokeWidth="59" strokeLinejoin="round" strokeLinecap="round"
        pathLength="1" strokeDasharray="1 1" strokeDashoffset={1-progress}/></mask></defs>
      <image href={staticFile('art/icon.png')} width="512" height="512" mask="url(#closing-outline)"/>
    </svg>
    <div style={{fontFamily:serif,fontSize:144,fontWeight:700,letterSpacing:-5,marginTop:24,opacity:tween(frame,26,57)}}>Area Hints</div>
  </AbsoluteFill>;
}
