import React from 'react';
import {AbsoluteFill, useCurrentFrame} from 'remotion';
import {aqua, gold, ink, paper, sans, tween} from './visual';

type Point = [number, number];
type Kind = 'create' | 'expand' | 'shrink' | 'divide';
type Tool = {
  kind: Kind;
  title: string;
  command: string;
  points: Point[];
  setup: string[];
  trace: string[];
  save: string[];
  result: string[];
  firstOffset: Point;
  lastOffset: Point;
};

const tools: Tool[] = [
  {
    kind: 'create', title: '新建域名', command: 'easyadd',
    points: [[103,162],[143,65],[288,45],[368,137],[255,203]],
    setup: ['先取名、选等级', '沿外围依次录点'],
    trace: ['每个转角录一点', '至少记录三个点'],
    save: ['首尾自动相连', '选高度、颜色'],
    result: ['确认保存后', '生成一个新域名'],
    firstOffset: [-43,19], lastOffset: [26,20],
  },
  {
    kind: 'expand', title: '扩展域名', command: 'expandarea',
    points: [[235,83],[358,83],[405,121],[354,168],[235,168]],
    setup: ['选中已有域名', '从域内走向外围'],
    trace: ['在域外录下', '新的边界转角'],
    save: ['回到域内', '将外侧线接回原边'],
    result: ['金色部分并入', '域名向外延伸'],
    firstOffset: [-53,-12], lastOffset: [-53,24],
  },
  {
    kind: 'shrink', title: '收缩域名', command: 'shrinkarea',
    points: [[368,85],[263,85],[225,129],[260,175],[368,175]],
    setup: ['选中已有域名', '从域外走向内部'],
    trace: ['在域内录下', '退让后的新边界'],
    save: ['再走回域外', '将内侧线接回原边'],
    result: ['斜纹部分移出', '其余区域保留'],
    firstOffset: [23,-11], lastOffset: [24,22],
  },
  {
    kind: 'divide', title: '分割域名', command: 'dividearea',
    points: [[37,126],[143,126],[202,98],[264,146],[384,146]],
    setup: ['选中已有域名', '从边界一侧出发'],
    trace: ['沿分割线录点', '穿过整个域名'],
    save: ['在另一侧结束', '分别为两块命名'],
    result: ['两块共享分割线', '一次提交两个域名'],
    firstOffset: [-12,-28], lastOffset: [9,-25],
  },
];

// 录点、闭合及结果显现的节拍与成片音效共用，图中的路径都是本段的 XZ 俯视示例。
const hits = [75, 147, 224, 301, 378];
const original: Point[] = [[83,80],[125,48],[238,38],[304,66],[304,184],[257,210],[140,215],[83,181]];
const points = (p: Point[]) => p.map(([x,y]) => x + ',' + y).join(' ');
const svgPath = (p: Point[]) => 'M' + points(p);
const serif = '"Noto Serif SC","STZhongsong","SimSun",serif';
const muted = '#A7BBBE';
const paint = '#13313A';
const mintFill = '#285650';
const drawn = (f: number, from: number, to: number) => tween(f, from, to);

function Terrain() {
  return <defs>
    <linearGradient id="tools-back"><stop stopColor="#0A1821"/><stop offset=".72" stopColor="#122B33"/><stop offset="1" stopColor="#142C34"/></linearGradient>
    <pattern id="tools-grid" width="32" height="32" patternUnits="userSpaceOnUse">
      <path d="M32 0H0V32" stroke="#98C7BD" opacity=".13" strokeWidth="1" fill="none"/>
      <circle r="1.2" fill="#7DA5A3" opacity=".5"/>
    </pattern>
    <pattern id="tools-excise" patternUnits="userSpaceOnUse" width="10" height="10" patternTransform="rotate(34)">
      <rect width="10" height="10" fill="#4E3932"/><path d="M0 0V10" stroke="#DD9E78" strokeWidth="3.3"/>
    </pattern>
    <clipPath id="tools-plot"><rect width="448" height="249" rx="2"/></clipPath>
  </defs>;
}

function Shape({vertices, color, fill, width=2.6}: {vertices: Point[];color: string;fill:string;width?:number}) {
  return <polygon points={points(vertices)} fill={fill} stroke={color} strokeWidth={width} strokeLinejoin="round"/>;
}

function Endpoint({p, offset, label}: {p:Point;offset:Point;label:string}) {
  const x = p[0] + offset[0];
  const y = p[1] + offset[1];
  return <g>
    <line x1={p[0]} y1={p[1]} x2={x} y2={y} stroke={gold} strokeWidth="1.6"/>
    <rect x={x-13} y={y-13} width="26" height="26" rx="3" fill="#172B32" stroke="#AA945D"/>
    <text x={x} y={y+6.5} fill={paper} fontSize="18.5" fontWeight="600" textAnchor="middle">{label}</text>
  </g>;
}

function Results({tool,frame}: {tool:Tool;frame:number}) {
  const alpha = drawn(frame,465,490);
  const mid = tool.points.slice(1,4);
  const common = {strokeLinejoin:'round' as const,strokeWidth:3};
  if (tool.kind === 'create') {
    return <g opacity={alpha}>
      <Shape vertices={tool.points} color={aqua} fill={mintFill}/>
      <text x="229" y="136" textAnchor="middle" fontSize="28" fill={paper}>新域名</text>
    </g>;
  }
  if (tool.kind === 'divide') {
    // 分割线最外面的两个录点仅确定穿越方向，域外线段不会成为两个新域名的边。
    const seam:Point[] = [[83,126],...mid,[304,146]];
    const north:Point[] = [...seam, original[3],original[2],original[1],original[0]];
    const south:Point[] = [...seam, original[4],original[5],original[6],original[7]];
    return <g opacity={alpha}>
      <Shape vertices={north} color={aqua} fill={mintFill}/>
      <Shape vertices={south} color={gold} fill="#66523B"/>
      <text x="196" y="80" textAnchor="middle" fontSize="23" fill={paper}>北区</text>
      <text x="196" y="177" textAnchor="middle" fontSize="23" fill={paper}>南区</text>
    </g>;
  }
  const upper: Point = [304,tool.points[1][1]];
  const lower: Point = [304,tool.points[3][1]];
  const retained: Point[] = [...original.slice(0,4),upper,...mid,lower,...original.slice(4)];
  const delta: Point[] = [upper,...mid,lower];
  return <g opacity={alpha}>
    <Shape vertices={retained} color={aqua} fill={mintFill}/>
    <polygon points={points(delta)} fill={tool.kind === 'expand' ? '#9D7847' : 'url(#tools-excise)'}/>
    <path d={svgPath([upper,...mid,lower])} fill="none" stroke={tool.kind === 'expand' ? gold : aqua} {...common}/>
    <path d={svgPath([upper,lower])} stroke="#C6C1A6" strokeDasharray="5 5" strokeWidth="1.9"/>
  </g>;
}

function Track({tool,frame}: {tool: Tool;frame:number}) {
  const curve = tool.points;
  const combined = drawn(frame,430,454);
  const done = drawn(frame,465,490);
  const joins:Point[] = tool.kind==='create' ? [] : tool.kind==='divide'
    ? [[83,126],[304,146]] : [[304,curve[1][1]],[304,curve[3][1]]];
  let head = curve[0];
  const trail:Point[] = [head];
  for (let i=1; i<curve.length; i++) {
    const ratio = drawn(frame,hits[i-1]+18,hits[i]);
    const left = curve[i-1], right = curve[i];
    if (ratio === 0) break;
    head = [left[0]+(right[0]-left[0])*ratio,left[1]+(right[1]-left[1])*ratio];
    trail.push(head);
  }
  return <g>
    <Results tool={tool} frame={frame}/>
    <g opacity={1-done*.7}>
      <polyline points={points(trail)} stroke={gold} fill="none" strokeWidth="3" strokeDasharray="6 5" strokeLinejoin="round"/>
      {tool.kind === 'create' && <path d={svgPath([curve[4],curve[0]])} fill="none" stroke={gold} strokeWidth="2.8" pathLength="1" strokeDasharray={`${combined} 2`}/>}
    </g>
    <g opacity={combined*(1-done)}>
      {joins.map(([x,y])=><g key={x+','+y}>
        <circle cx={x} cy={y} r="7.5" fill="#19343B" stroke={aqua} strokeWidth="2.2"/>
        <circle cx={x} cy={y} r="2.3" fill={aqua}/>
      </g>)}
    </g>
    {curve.map(([x,y],i) => {
      if (frame < hits[i]) return null;
      const ring = drawn(frame,hits[i],hits[i]+22);
      return <g key={i}>
        <circle cx={x} cy={y} r={5+12*ring} stroke={gold} strokeWidth="2" opacity={1-ring} fill="none"/>
        <circle cx={x} cy={y} r={i===0||i===4?5.4:4.3} fill={gold} stroke="#203239" strokeWidth="1.8"/>
      </g>;
    })}
    {frame < hits[4] && <g opacity={drawn(frame,30,56)}>
      <circle cx={head[0]} cy={head[1]} r="8.8" stroke={paper} strokeWidth="2" fill="none"/>
      <circle cx={head[0]} cy={head[1]} r="3.1" fill={paper}/>
    </g>}
    {frame >= hits[0] && <Endpoint p={curve[0]} offset={tool.firstOffset} label="起"/>}
    {frame >= hits[4] && <Endpoint p={curve[4]} offset={tool.lastOffset} label="终"/>}
  </g>;
}

function Panel({tool,index,frame}: {tool:Tool;index:number;frame:number}) {
  const left = index % 2 === 0 ? 76 : 980;
  const top = index < 2 ? 167 : 579;
  const result = frame>=465;
  const lines = result ? tool.result : frame>=378 ? tool.save : frame>=142 ? tool.trace : tool.setup;
  const textIn = drawn(frame,0,16);
  const textBeat = result ? 465 : frame>=378 ? 378 : frame>=142 ? 142 : 0;
  return <g transform={`translate(${left},${top})`}>
    <rect width="865" height="382" fill="#10252E" stroke="#3B555E" strokeWidth="1.3"/>
    <path d="M0 42 V0 H53" fill="none" stroke="#8AA9A7" strokeWidth="2.2"/>
    <g opacity={textIn}>
      <text x="32" y="69" fill={paper} fontSize="48" fontWeight="600" fontFamily={serif}>{tool.title}</text>
      <text x="33" y="112" fontFamily="Consolas,monospace" fill={aqua} fontSize="23">/areahint {tool.command}</text>
      <g opacity={.45+.55*drawn(frame,textBeat,textBeat+10)}>
        <path d="M34 181 h36" stroke={result ? aqua : gold} strokeWidth="3.3"/>
        {lines.map((line,i)=><text x="32" y={231+i*47} key={line} fill={paper} fontSize="32">{line}</text>)}
      </g>
      <g transform="translate(347,48) scale(1.105)">
        <g clipPath="url(#tools-plot)">
          <rect width="448" height="249" fill={paint}/><rect width="448" height="249" fill="url(#tools-grid)"/>
          <g fill="none" stroke="#7F9E96" strokeWidth=".85" opacity=".16">
            {[0,1,2,3,4,5].map(i=><path key={i} d={`M -30 ${55+i*12} C 60 ${-40+i*16},125 ${70+i*18},170 ${15+i*13} S 350 ${-55+i*11},500 ${50+i*14}`}/>)}
          </g>
          {tool.kind !== 'create' && <g opacity={1-drawn(frame,465,490)*.75}>
            <Shape vertices={original} color={aqua} fill="#27494A"/>
            {original.map(([x,y])=><rect key={x+','+y} x={x-2.7} y={y-2.7} width="5.4" height="5.4" fill={aqua}/>)}
          </g>}
          <Track tool={tool} frame={frame}/>
        </g>
        <path d="M0 14 V0 H16 M432 0 H448 V14 M448 234 V249 H432 M16 249 H0 V234" stroke="#6F9598" strokeWidth="1.5" fill="none"/>
      </g>
    </g>
  </g>;
}

export function EditingTools() {
  const frame = useCurrentFrame();
  const instruction = frame < 75 ? '走到目标位置' : frame < 378 ? '短按绑定键，记录脚下的顶点' :
    frame < 465 ? '结束录点，处理边界' : '保存后的俯视范围';
  return <AbsoluteFill style={{backgroundColor:ink}}>
    <svg width="100%" height="100%" viewBox="0 0 1920 1080" fontFamily={sans} role="img" aria-label="创建、扩展、收缩和分割域名的同步录点对照">
      <Terrain/>
      <rect width="1920" height="1080" fill="url(#tools-back)"/>
      <text x="76" y="106" fontFamily={serif} fontSize="63" fontWeight="600" fill={paper}>用脚步编辑边界</text>
      <g transform="translate(1062,90)" fontSize="27">
        <path d="M0 0 h47" stroke={aqua} strokeWidth="3"/><text x="60" y="9" fill="#C5D3D1">原边界</text>
        <path d="M210 0 h47" stroke={gold} strokeDasharray="6 5" strokeWidth="3"/><text x="270" y="9" fill="#C5D3D1">录点路线</text>
        <circle cx="480" r="5.3" fill={gold}/><text x="501" y="9" fill="#C5D3D1">已录顶点</text>
      </g>
      {tools.map((tool,index)=><Panel key={tool.kind} tool={tool} index={index} frame={frame}/>)}
      <g transform="translate(76,1017)">
        <line x2="56" stroke={gold} strokeWidth="3"/><text x="75" y="10" fontSize="29" fill={paper}>{instruction}</text>
        <text x="1768" y="10" textAnchor="end" fontSize="24" fill={muted}>XZ 俯视示意</text>
      </g>
    </svg>
  </AbsoluteFill>;
}
