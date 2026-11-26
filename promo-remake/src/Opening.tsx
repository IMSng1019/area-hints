import React from 'react';
import {AbsoluteFill,Sequence,useCurrentFrame} from 'remotion';
import {Board,Clip,Label,Rise,box,gold,ink,paper,sans,serif,tween} from './visual';

function HarborBackdrop({offset}:{offset:number}) {
  return <AbsoluteFill>
    <Clip name="harbor-orbit" offset={offset}/>
    <AbsoluteFill style={{background:'linear-gradient(90deg,rgba(9,23,31,.96) 0%,rgba(10,24,31,.86) 28%,rgba(10,24,31,.56) 51%,rgba(10,24,31,.14) 76%,rgba(10,24,31,.10) 100%)'}}/>
  </AbsoluteFill>;
}

export function Arrival() {
  const f=useCurrentFrame();
  return <AbsoluteFill style={{background:'black',overflow:'hidden'}}>
    <Sequence durationInFrames={67} name="璃月港环拍"><Clip name="harbor-orbit"/></Sequence>
    <Sequence from={67} durationInFrames={39} name="沿石阶下山"><Clip name="pv-follow" offset={21}/></Sequence>
    <Sequence from={106} durationInFrames={24} name="石阶上的脚步"><Clip name="footsteps" offset={16}/></Sequence>
    <Sequence from={130} durationInFrames={144} name="第三人称入港"><Clip name="arrival" offset={24}/></Sequence>
    <Sequence from={274} durationInFrames={56} name="人物走进远景"><Clip name="harbor-push" offset={94}/></Sequence>
    <AbsoluteFill style={{background:'linear-gradient(to bottom,rgba(0,0,0,.14),transparent 23%,transparent 70%,rgba(0,0,0,.18))'}}/>
    {/* 整幅远景从纯黑渐显，让天空和港口在同一次曝光中浮现。 */}
    {f<43 && <AbsoluteFill style={{backgroundColor:'black',opacity:1-tween(f,1,43)}}/>}
    {/* 片名出现前先叠入港口航拍，用同一条摄影机轨迹接过片名的五秒。 */}
    <Sequence from={312}><AbsoluteFill style={{opacity:tween(f,312,329)}}><HarborBackdrop offset={100}/></AbsoluteFill></Sequence>
  </AbsoluteFill>;
}
export function Brand() {
  const f=useCurrentFrame();
  return <AbsoluteFill style={{background:ink,color:paper,fontFamily:sans}}>
    <HarborBackdrop offset={118}/>
    <div style={box(130,351,1620,410)}>
    <Rise duration={23} style={{fontSize:52,color:paper}}>想要一个像原神一样跳字的模组？</Rise>
    <Rise delay={37} duration={27} style={{fontFamily:serif,fontSize:190,lineHeight:1.1,marginTop:30,letterSpacing:-6}}>Area <i style={{color:gold}}>Hints</i></Rise>
    </div>
    <AbsoluteFill style={{background:ink,opacity:tween(f,137,149)}}/>
  </AbsoluteFill>;
}
export function Overview() {
  const f=useCurrentFrame(), p=tween(f,0,27);
  return <Board>
    <div style={box(95,220,580,620)}><Label>AREA HINTS</Label>
      {['功能众多','操作便捷','性能表现高'].map((line,i)=><Rise key={line} delay={4+i*30} style={{fontFamily:serif,fontSize:86,color:i===2?gold:paper,marginTop:25,lineHeight:1.2}}>{line}</Rise>)}
    </div>
    <div style={{...box(665,155,1170,770),display:'flex',alignItems:'center',overflow:'hidden',clipPath:`inset(0 ${(1-p)*100}% 0 0)`}}><Clip name="overview" style={{height:'auto',objectFit:'contain'}}/></div>
  </Board>;
}
