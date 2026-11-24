import React from 'react';
import {AbsoluteFill,Sequence,useCurrentFrame} from 'remotion';
import {Board,Clip,Contours,Label,Rise,box,gold,ink,latin,paper,serif,tween} from './visual';

export function Arrival() {
  const f=useCurrentFrame();
  return <AbsoluteFill style={{background:'black',overflow:'hidden'}}>
    <Sequence durationInFrames={105} name="璃月港环拍"><Clip name="harbor-orbit"/></Sequence>
    <Sequence from={105} durationInFrames={65} name="人物擦过镜头"><Clip name="footsteps"/></Sequence>
    <Sequence from={170} durationInFrames={100} name="远景推进"><Clip name="harbor-push" offset={24} style={{scale:String(1+tween(f,170,270,0,.07))}}/></Sequence>
    <Sequence from={270} durationInFrames={135} name="第三人称入港"><Clip name="arrival"/></Sequence>
    {f>=165&&f<175&&<AbsoluteFill style={{background:'linear-gradient(107deg,transparent 1%,#26333b 18%,#17242e 66%,transparent 92%)',width:'215%',transform:`translateX(${-106+158*tween(f,165,175)}%)`}}/>}
    {f<35 && <AbsoluteFill style={{background:'linear-gradient(103deg,#000 14%,rgba(0,0,0,.82) 54%,transparent 100%)',width:'205%',left:'-1%',transform:'translateX('+(-100*tween(f,0,35))+'%)'}}/>}
    <AbsoluteFill style={{background:'linear-gradient(to bottom,rgba(0,0,0,.14),transparent 23%,transparent 70%,rgba(0,0,0,.18))'}}/>
    <AbsoluteFill style={{background:ink,opacity:tween(f,384,404)}}/>
  </AbsoluteFill>;
}
export function Brand() {
  const f=useCurrentFrame();
  return <Board><div style={{...box(135,220,1700,710)}}>
    <Rise><Label>想要一个能做到上述功能的模组？</Label></Rise>
    <Rise delay={24} style={{fontFamily:serif,fontSize:76,marginTop:55}}>隆重介绍</Rise>
    <Rise delay={34} style={{fontFamily:serif,fontSize:220,lineHeight:1.08,letterSpacing:-10}}>Area <i style={{color:gold}}>Hints</i></Rise>
    <div style={{marginTop:40,height:2,background:gold,width:1510*tween(f,32,77)}}/>
  </div></Board>;
}
export function Overview() {
  const f=useCurrentFrame(), p=tween(f,0,18);
  return <Board>
    <div style={box(95,220,580,620)}><Label>AREA HINTS</Label>
      {['功能众多','操作便捷','大区域也能','快速定位'].map((line,i)=><Rise key={line} delay={[3,17,31,31][i]} style={{fontFamily:serif,fontSize:i>1?68:86,color:i>1?gold:paper,marginTop:i===3?0:25,lineHeight:1.2}}>{line}</Rise>)}
    </div>
    <div style={{...box(665,155,1170,770),display:'flex',alignItems:'center',overflow:'hidden',clipPath:`inset(0 ${(1-p)*100}% 0 0)`}}><Clip name="overview" style={{height:'auto',objectFit:'contain'}}/></div>
  </Board>;
}
