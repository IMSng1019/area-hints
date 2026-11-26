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

const spanX = atlas.bounds.maxX - atlas.bounds.minX;
const spanZ = atlas.bounds.maxZ - atlas.bounds.minZ;
const formatSpan = new Intl.NumberFormat('zh-CN', {maximumFractionDigits: 2});
const mapScale = 850 / atlas.width;

function MapDimensions({frame}: {frame: number}) {
  // 尺寸线对齐原始边界极值，投影时预留的空白不计入跨度。
  const left = atlas.margin * mapScale;
  const right = (atlas.width - atlas.margin) * mapScale;
  const top = left;
  const bottom = top + (right - left) * spanZ / spanX;
  const middleX = (left + right) / 2;
  const middleZ = (top + bottom) / 2;
  const railX = 914;
  const railY = bottom + 37;

  return <g opacity={tween(frame,34,60)} textAnchor="middle" fontFamily={sans}>
    <g stroke="#85aaa9" strokeWidth={2} fill="none">
      <path d={`M${left},${railY-7}v14m0,-7H${middleX-151} M${middleX+151},${railY}H${right}m0,-7v14`}/>
      <path d={`M${railX-7},${top}h14m-7,0V${middleZ-44} M${railX},${middleZ+45}V${bottom}m-7,0h14`}/>
    </g>
    <text x={middleX} y={railY+10} fill={paper} fontSize={30}>
      <tspan fill={muted} fontSize={26}>横向 X</tspan>
      <tspan dx={12} fontFamily={latin} fontWeight={700}>{formatSpan.format(spanX)}</tspan>
      <tspan dx={7} fontSize={24}>格</tspan>
    </text>
    <text x={railX} y={middleZ-7} fill={muted} fontSize={26}>纵向 Z</text>
    <text x={railX} y={middleZ+28} fill={paper} fontSize={30}>
      <tspan fontFamily={latin} fontWeight={700}>{formatSpan.format(spanZ)}</tspan>
      <tspan dx={5} fontSize={24}>格</tspan>
    </text>
  </g>;
}

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

    <svg style={box(857,237,985,635)} viewBox="0 0 985 635" aria-label="方块提瓦特主世界的三级域名边界及横纵跨度">
      <g transform={`scale(${mapScale})`}>
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
      </g>
      <MapDimensions frame={frame}/>
    </svg>
    <div style={{...box(857,872,850,48),textAlign:'center',color:muted,fontSize:28}}>方块提瓦特 · 主世界域名边界</div>
    <div style={{...box(96,935,1728,91),borderTop:'1px solid #486067',paddingTop:23,fontSize:29,color:muted}}>
      客户端离线测试　·　32,768 个采样坐标　·　Ryzen 9 9950X3D　·　JDK 17
    </div>
  </AbsoluteFill>;
}
