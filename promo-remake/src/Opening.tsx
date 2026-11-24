import React from 'react';
import {AbsoluteFill,Sequence,useCurrentFrame} from 'remotion';
import {Board,Clip,Label,Rise,box,gold,ink,paper,serif,tween} from './visual';

export function Arrival() {
  const f=useCurrentFrame();
  return <AbsoluteFill style={{background:'black',overflow:'hidden'}}>
    <Sequence durationInFrames={188} name="璃月港环拍"><Clip name="harbor-orbit"/></Sequence>
    <Sequence from={188} durationInFrames={45} name="石阶上的脚步"><Clip name="footsteps"/></Sequence>
    <Sequence from={233} durationInFrames={148} name="人物走进远景"><Clip name="harbor-push" style={{scale:String(1+tween(f,233,381,0,.045))}}/></Sequence>
    <Sequence from={381} durationInFrames={159} name="第三人称入港"><Clip name="arrival"/></Sequence>
    {f>=228&&f<239&&<AbsoluteFill style={{background:'linear-gradient(107deg,transparent 1%,#26333b 18%,#17242e 66%,transparent 92%)',width:'215%',transform:`translateX(${-106+158*tween(f,228,239)}%)`}}/>}
    {f<48 && <AbsoluteFill style={{background:'linear-gradient(103deg,#000 14%,rgba(0,0,0,.82) 54%,transparent 100%)',width:'205%',left:'-1%',transform:'translateX('+(-100*tween(f,0,48))+'%)'}}/>}
    <AbsoluteFill style={{background:'linear-gradient(to bottom,rgba(0,0,0,.14),transparent 23%,transparent 70%,rgba(0,0,0,.18))'}}/>
    <AbsoluteFill style={{background:ink,opacity:tween(f,515,539)}}/>
  </AbsoluteFill>;
}
export function Brand() {
  const f=useCurrentFrame();
  return <Board><div style={{...box(135,220,1700,710)}}>
    <Rise><Label>想要一个能做到上述功能的模组？</Label></Rise>
    <Rise delay={38} style={{fontFamily:serif,fontSize:76,marginTop:55}}>隆重介绍</Rise>
    <Rise delay={64} duration={26} style={{fontFamily:serif,fontSize:220,lineHeight:1.08,letterSpacing:-10}}>Area <i style={{color:gold}}>Hints</i></Rise>
    <div style={{marginTop:40,height:2,background:gold,width:1510*tween(f,66,110)}}/>
  </div></Board>;
}
export function Overview() {
  const f=useCurrentFrame(), p=tween(f,0,27);
  return <Board>
    <div style={box(95,220,580,620)}><Label>AREA HINTS</Label>
      {['功能众多','操作便捷','大区域也能','快速定位'].map((line,i)=><Rise key={line} delay={[4,34,64,64][i]} style={{fontFamily:serif,fontSize:i>1?68:86,color:i>1?gold:paper,marginTop:i===3?0:25,lineHeight:1.2}}>{line}</Rise>)}
    </div>
    <div style={{...box(665,155,1170,770),display:'flex',alignItems:'center',overflow:'hidden',clipPath:`inset(0 ${(1-p)*100}% 0 0)`}}><Clip name="overview" style={{height:'auto',objectFit:'contain'}}/></div>
  </Board>;
}
