import React from 'react';
import {AbsoluteFill,useCurrentFrame} from 'remotion';
import atlas from './performance-map.json';
import {aqua,box,ink,latin,paper,sans,serif,tween} from './visual';

export const performanceFrames = 12 * 30;
const muted = '#b4c5c5';
const accent = '#ffae78';
// 轻调原图的六种色相，细轮廓在深底视频中仍能区分。
const lineColors: Record<string,string> = {
  '#55FF55':'#94e599', '#AA00AA':'#c694e7', '#00AA00':'#5cbd84',
  '#FF0000':'#ff887e', '#55FFFF':'#85ddd9', '#FFAA00':'#f9c479',
};

export function Performance() {
  const frame = useCurrentFrame();
  const result = tween(frame,70,98);
  return <AbsoluteFill style={{background:ink,color:paper,fontFamily:sans}}>
    <div style={{...box(96,74,1728,62),borderBottom:'1px solid #486067',color:aqua,fontSize:29,fontWeight:700}}>性能实测</div>
    <div style={{...box(92,173,1760,120),fontFamily:serif,fontWeight:700,fontSize:80,opacity:tween(frame,0,15)}}>识别脚下的域名，要多久？</div>

    <div style={{...box(98,345,750,260),opacity:result,translate:`0 ${(1-result)*12}px`}}>
      <div style={{color:muted,fontSize:32}}>客户端单次查询平均耗时</div>
      <div style={{display:'flex',gap:27,alignItems:'baseline',marginTop:8}}>
        <span style={{fontFamily:latin,fontWeight:700,fontSize:185,color:accent,lineHeight:1.04,letterSpacing:-7}}>0.92</span>
        <span style={{fontWeight:700,fontSize:51}}>微秒</span>
      </div>
    </div>
    <div style={{...box(98,643,720,165),display:'flex',gap:74,borderTop:'2px solid #496169',paddingTop:19}}>
      {[['83','个域名'],['9,095','个边界顶点']].map(([number,caption])=><div key={caption}>
        <div style={{fontFamily:latin,fontSize:65,lineHeight:1.05,fontWeight:700}}>{number}</div>
        <div style={{color:muted,fontSize:30,marginTop:8}}>{caption}</div>
      </div>)}
    </div>

    <svg style={box(855,210,975,650)} viewBox={`0 0 ${atlas.width} ${atlas.height}`} aria-label="方块提瓦特主世界的三级域名边界">
      {atlas.areas.map((area,index)=>{
        const start = 4+(area.level-1)*17+index%7;
        const progress = tween(frame,start,start+65);
        return <path key={area.name+'-'+index} d={area.d} fill="none"
          stroke={lineColors[area.color.toUpperCase()] ?? '#e3e9df'}
          strokeWidth={area.level===1?3.5:area.level===2?2.4:1.9}
          strokeLinejoin="round" strokeLinecap="round" vectorEffect="non-scaling-stroke"
          opacity={progress===0?0:1} pathLength={1} strokeDasharray={progress<1?'1 1':undefined}
          strokeDashoffset={1-progress}/>;
      })}
    </svg>
    <div style={{...box(860,856,965,48),textAlign:'center',color:muted,fontSize:28}}>方块提瓦特 · 主世界域名边界</div>
    <div style={{...box(96,935,1728,91),borderTop:'1px solid #486067',paddingTop:23,fontSize:29,color:muted}}>
      客户端离线测试　·　32,768 个采样坐标　·　Ryzen 9 9950X3D　·　JDK 17
    </div>
  </AbsoluteFill>;
}
