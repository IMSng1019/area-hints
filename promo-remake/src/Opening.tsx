import React from 'react';
import {AbsoluteFill,Sequence,useCurrentFrame} from 'remotion';
import {Board,Clip,Label,Rise,box,gold,paper,serif,tween} from './visual';
import {HarborPaper,harborExposure,photoShade} from './Brand';

export function Arrival() {
  const f=useCurrentFrame();
  return <AbsoluteFill style={{background:'black',overflow:'hidden'}}>
    <Sequence durationInFrames={67} name="璃月港环拍"><Clip name="harbor-orbit"/></Sequence>
    <Sequence from={67} durationInFrames={39} name="沿石阶下山"><Clip name="pv-follow" offset={21}/></Sequence>
    <Sequence from={106} durationInFrames={24} name="石阶上的脚步"><Clip name="footsteps" offset={16}/></Sequence>
    <Sequence from={130} durationInFrames={144} name="第三人称入港"><Clip name="arrival" offset={24}/></Sequence>
    <Sequence from={274} durationInFrames={56} name="人物走进远景"><Clip name="harbor-push" offset={94}/></Sequence>
    <AbsoluteFill style={{background:photoShade}}/>
    {/* 整幅远景从纯黑渐显，让天空和港口在同一次曝光中浮现。 */}
    {f<43 && <AbsoluteFill style={{backgroundColor:'black',opacity:1-tween(f,1,43)}}/>}
    {f>=320 && <AbsoluteFill style={{opacity:harborExposure(f)}}><HarborPaper/></AbsoluteFill>}
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
