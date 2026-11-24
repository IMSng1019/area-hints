import React from 'react';
import {AbsoluteFill, Easing, interpolate, useCurrentFrame} from 'remotion';

export const mechanismFrames = 840;

const ink = '#F5F0DF';
const cyan = '#8CE6DD';
const amber = '#F4BD70';
const muted = '#A1B8BA';
const sans = '"Microsoft YaHei", "Noto Sans SC", sans-serif';
const serif = '"Noto Serif SC", "STZhongsong", "SimSun", serif';
const mono = 'Consolas, monospace';
const smooth = (f: number, start: number, end: number) => interpolate(f, [start, end], [0, 1], {
  extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.bezier(0.42, 0, 0.24, 1),
});

type Point = [number, number];
const boundary: Point[] = [[935, 536], [1003, 424], [1170, 365], [1265, 400], [1365, 361],
  [1510, 412], [1560, 503], [1524, 645], [1410, 688], [1302, 768], [1164, 719], [1022, 736], [957, 650]];
const district: Point[] = [[1214, 532], [1294, 467], [1381, 485], [1464, 539], [1420, 627], [1315, 675], [1247, 635]];
const points = (list: Point[]) => list.map(p => p.join(',')).join(' ');
const terrain = Array.from({length: 19}, (_, index) => {
  // 轮廓线只提供底图纹理；明亮的直线多边形表示参与判定的实际示例边界。
  const contour = Array.from({length: 120}, (_, step) => {
    const t = step * Math.PI / 60;
    const r = 40 + index * 20 + Math.sin(t * 3 + index * .09) * 18 + Math.cos(t * 7) * 7;
    return [Math.round(1280 + Math.cos(t) * r * 1.65), Math.round(535 + Math.sin(t) * r * .91)];
  });
  return 'M' + contour.map(p => p.join(',')).join(' L') + ' Z';
});

function Definitions() {
  return <defs>
    <radialGradient id="mechanism-bg" cx=".7" cy=".45" r=".85">
      <stop stopColor="#17333A"/><stop offset=".63" stopColor="#0B1B24"/><stop offset="1" stopColor="#071319"/>
    </radialGradient>
    <linearGradient id="mechanism-map" x1="0" x2="1" y1="0" y2="1">
      <stop stopColor="#173D43"/><stop offset="1" stopColor="#102A32"/>
    </linearGradient>
    <pattern id="mechanism-grid" width="52" height="52" patternUnits="userSpaceOnUse">
      <path d="M52 0 H0 V52" stroke="#9AD8D4" strokeOpacity=".18" fill="none"/>
      <circle cx="0" cy="0" r="1.5" fill="#8EABA9"/>
    </pattern>
    <clipPath id="mechanism-chart"><rect x="822" y="278" width="1008" height="548"/></clipPath>
    <marker id="mechanism-arrow" viewBox="0 0 10 8" refX="8" refY="4" markerWidth="5" markerHeight="5" orient="auto">
      <path d="M0 0 L9 4 L0 8" fill="none" stroke={amber} strokeWidth="1.5"/>
    </marker>
  </defs>;
}

const chapters = [
  {start: 0, label: '多人同步', title: ['服务器存档', '客户端识别'], body: ['加入服务器或域名更新时，', '边界数据会同步给玩家。']},
  {start: 180, label: '客户端 · 空间索引', title: ['先缩小', '搜索范围'], body: ['包围盒圈出水平范围，', '高度区间筛掉上下层。']},
  {start: 300, label: '客户端 · 多边形判定', title: ['一条射线', '判定内外'], body: ['从玩家向正 X 方向发射射线。', '穿过边界奇数次，即在域内。']},
  {start: 420, label: '客户端 · 域名层级', title: ['沿父子层级', '找到所在域名'], body: ['逐级查找，取命中的最深层。', '显示名称改变时，跳字出现。']},
];

function Caption({f}: {f: number}) {
  const i = chapters.filter(c => f >= c.start).length - 1;
  const chapter = chapters[i];
  const enter = smooth(f, chapter.start, chapter.start + 14);
  const out = i < 3 ? 1 - smooth(f, chapters[i + 1].start - 8, chapters[i + 1].start) : 1;
  return <g fontFamily={sans} opacity={enter * out} transform={`translate(0,${15 * (1 - enter)})`}>
    <line x1="96" y1="296" x2="146" y2="296" stroke={amber} strokeWidth="3"/>
    <text x="162" y="306" fontSize="27" letterSpacing="3" fill={amber}>{chapter.label}</text>
    {chapter.title.map((line, j) => <text key={line} x="90" y={464 + j * 113}
      fontFamily={serif} fontSize={98} fontWeight={600} letterSpacing={1} fill={ink}>{line}</text>)}
    {chapter.body.map((line, j) => <text key={line} x="99" y={688 + 49 * j} fontSize={32} fill="#CBD6D3">{line}</text>)}
  </g>;
}

// 剖开的三张地图与终端中的平面轮廓对应同一份由服务器同步的域名数据。
function ServerAtlas({f}: {f: number}) {
  const arrive = smooth(f, 8, 42);
  return <g transform={`translate(1030,${535 + (1 - arrive) * 30})`}>
    {[2, 1, 0].map(i => <g key={i} transform={`translate(0,${i * 29 * arrive})`}>
      <path d="M-194 -22 L-24 -127 L193 -34 L25 79 Z" fill={['#244A50','#17343F','#102630'][i]} stroke={i ? '#4D777E' : cyan} strokeWidth="2.5"/>
      {i === 0 && <>
        <path d="M-153 -21 L-23 -101 L148 -29 L24 52 Z M-104 -53 L66 23 M-54 -84 L116 -10 M-89 5 L40 -71 M-33 30 L95 -48"
          stroke="#87AEB0" strokeOpacity=".46" strokeWidth="1.5" fill="none"/>
        <path d="M-104 -12 L-64 -43 L-27 -43 L4 -55 L66 -30 L91 -12 L62 1 L35 -2 L5 21 L-45 9 L-64 11 Z"
          fill="#3D766F" stroke={cyan} strokeWidth="3"/>
        <circle cx="3" cy="-16" r="6" fill={amber}/>
      </>}
    </g>)}
    <text y="225" textAnchor="middle" fontSize="32" fontWeight="600" fill={ink}>服务器</text>
    <text y="270" textAnchor="middle" fontSize="25" fill={muted}>按维度保存域名</text>
  </g>;
}

function Monitor({x, y, id, f}: {x: number; y: number; id: string; f: number}) {
  const received = smooth(f, 60, 90);
  return <g transform={`translate(${x} ${y})`}>
    <rect x="-109" y="-66" width="218" height="134" rx="7" fill="#152B36" stroke="#7E9EAC" strokeWidth="3"/>
    <rect x="-98" y="-55" width="196" height="106" rx="2" fill="#0B1D25"/>
    <g opacity={received}>
      <path d="M-68 -3 L-44 -34 L-7 -42 L13 -31 L44 -34 L65 -8 L55 17 L27 25 L10 40 L-14 27 L-43 31 L-61 19 Z"
        fill="#244D4C" stroke={cyan} strokeWidth="2.5"/>
      <circle cx={id === 'A' ? -23 : 28} cy="5" r="6" fill={amber}/>
    </g>
    <path d="M0 69 V81 M-34 82 H34" stroke="#9AADB2" strokeWidth="4"/>
    <text y="129" textAnchor="middle" fontSize="28" fill={ink}>客户端 {id}</text>
  </g>;
}

function Network({f}: {f: number}) {
  const receive = smooth(f, 32, 74);
  return <g opacity={1 - smooth(f, 163, 183)} fontFamily={sans}>
    <g opacity={smooth(f, 10, 35)}>
      {[398, 688].map(y => {
        const d = `M1207 535 H1302 C1345 535 1347 ${y} 1418 ${y} H1502`;
        return <g key={y}>
          <path d={d} fill="none" stroke="#42616A" strokeWidth="2.6"/>
          <path d={d} fill="none" stroke={cyan} strokeWidth="3" pathLength="1" strokeDasharray={`${receive} 2`}/>
          <path d={d} fill="none" stroke={ink} strokeWidth="6" pathLength="1"
            strokeDasharray=".035 .965" strokeDashoffset={-(f % 84) / 84} opacity={smooth(f, 22, 46)}/>
        </g>;
      })}
      <text x="1298" y="826" textAnchor="middle" fill={cyan} fontSize="27">同步域名数据</text>
    </g>
    <g opacity={smooth(f, 0, 23)}><ServerAtlas f={f}/></g>
    <g opacity={smooth(f, 12, 38)}>
      <Monitor x={1616} y={398} f={f} id="A"/><Monitor x={1616} y={688} f={f} id="B"/>
    </g>
  </g>;
}

function HeightGauge({f}: {f: number}) {
  const lift = smooth(f, 193, 248);
  const y = 722 - lift * 170;
  return <g>
    <text x="1745" y="392" textAnchor="middle" fill={ink} fontSize="23">高度 Y</text>
    <rect x="1717" y="425" width="10" height="305" fill="#34535C"/>
    <rect x="1717" y="473" width="10" height="182" fill={cyan}/>
    {[473, 655].map((v, i) => <g key={v}><path d={`M1709 ${v} h27`} stroke={cyan} strokeWidth="2"/>
      <text x="1742" y={v + 7} fontFamily={mono} fontSize="22" fill={muted}>{i ? '48' : '96'}</text></g>)}
    <path d={`M1697 ${y - 8} L1710 ${y} L1697 ${y + 8} Z`} fill={amber}/>
    <text x="1743" y={y + 8} fill={amber} fontFamily={mono} fontSize="26">{Math.round(48 + (655 - y) * 48 / 182)}</text>
  </g>;
}

function Survey({f}: {f: number}) {
  const bounds = smooth(f, 193, 236) * (1 - smooth(f, 291, 320));
  const crossed = smooth(f, 345, 374);
  const layer = smooth(f, 415, 442);
  // 示例点以恒定 Z 向右走，交点与轮廓精确相交，观众可以直接核对奇偶数。
  const playerX = 944 + 160 * crossed + 256 * smooth(f, 433, 483);
  const playerZ = 466;
  const leftX = 935 + (536 - playerZ) * (1003 - 935) / (536 - 424);
  const rightX = 1510 + (playerZ - 412) * (1560 - 1510) / (503 - 412);
  const rayEnd = playerX + (1660 - playerX) * smooth(f, 302, 322);
  const ray = smooth(f, 300, 311) * (1 - smooth(f, 410, 426));
  // 观察点先跨入一级域名，再向东南走进下属的璃月港。
  const playerY = playerZ + 103 * smooth(f, 435, 482);
  const inDistrict = playerX >= 1214 + (playerY - 532) * (1247 - 1214) / 103;
  const titleIn = smooth(f, 467, 484);
  return <g fontFamily={sans} opacity={smooth(f, 167, 191)}>
    <rect x="822" y="278" width="1008" height="548" fill="url(#mechanism-map)" opacity=".72"/>
    <g clipPath="url(#mechanism-chart)">
      {terrain.map((d, i) => <path key={i} d={d} stroke="#8DA7A2" strokeWidth={i % 4 ? 1 : 1.8}
        opacity={i % 4 ? .14 : .23} fill="none"/>)}
      <rect x="822" y="278" width="1008" height="548" fill="url(#mechanism-grid)"/>
      <polygon points={points(boundary)} fill={cyan} fillOpacity=".105" stroke={cyan} strokeWidth="3.5"/>
      {boundary.map(([x,y]) => <rect key={x} x={x - 3} y={y - 3} width="6" height="6" fill={cyan}/>)}
      <g opacity={bounds}>
        <rect x="935" y="361" width="625" height="407" stroke={amber} strokeWidth="2.4" strokeDasharray="10 8" fill={amber} fillOpacity=".035"/>
        <text x="940" y="341" fill={amber} fontSize="24">水平包围盒</text><HeightGauge f={f}/>
      </g>
      <g opacity={ray}>
        <line x1={playerX} x2={rayEnd} y1={playerZ} y2={playerZ} stroke={amber} strokeWidth="3.5" markerEnd="url(#mechanism-arrow)"/>
        {[leftX, rightX].filter(x => x > playerX && x < rayEnd).map(x => <g key={x}>
          <circle cx={x} cy={playerZ} r="11" stroke={amber} strokeWidth="3" fill="#1D353C"/>
          <circle cx={x} cy={playerZ} r="3" fill={amber}/>
        </g>)}
        <text x="1665" y={playerZ - 16} fill={amber} fontSize="23" fontFamily={mono}>+X</text>
        <g opacity={smooth(f, 318, 327)}>
          <rect x="1165" y="613" width="336" height="62" fill="#112B32" stroke="#527977"/>
          <text x="1333" y="654" textAnchor="middle" fill={ink} fontSize="29">
            {playerX > leftX ? '1 个交点  ·  域内' : '2 个交点  ·  域外'}
          </text>
        </g>
      </g>
      <g opacity={layer}>
        <polygon points={points(district)} fill={amber} fillOpacity=".17" stroke={amber} strokeWidth="3"/>
        <text x="1021" y="686" fontSize="28" fill={cyan}>二级 · 云来海</text>
        <text x="1328" y="619" fontSize="25" textAnchor="middle" fill={amber}>三级 · 璃月港</text>
        <path d={`M1104 466 L${playerX} ${playerY}`} fill="none" stroke={amber} strokeWidth="2" strokeDasharray="5 7"/>
      </g>
      <circle cx={playerX} cy={playerY} r={18 + ((f % 44) / 44) * 9} fill="none" stroke={amber} strokeWidth="2" opacity={.6 * (1 - f % 44 / 44)}/>
      <circle cx={playerX} cy={playerY} r="9" stroke="#1C3339" strokeWidth="3" fill="#FFCD83"/>
      <text x={playerX - 15} y={playerY - 27} fontSize="24" fill={ink} textAnchor="middle" paintOrder="stroke"
        stroke="#18323B" strokeWidth="4">玩家</text>
    </g>
    <g stroke="#7FACA9" strokeWidth="2" fill="none">
      <path d="M822 305 V278 H853 M1799 278 H1830 V305 M1830 798 V826 H1799 M853 826 H822 V798"/>
    </g>
    <text x="822" y="246" fill={muted} fontSize="23" letterSpacing="2">主世界  /  XZ 俯视示意</text>
    <g opacity={layer} fontSize="30">
      <text x="824" y="885" fill={muted}>当前显示</text>
      <text x="985" y="886" fill={inDistrict ? muted : ink}>云来海</text>
      <g opacity={inDistrict ? titleIn : 0} transform={`translate(0 ${(1 - titleIn) * 15})`}>
        <path d="M1110 873 h55 m-12 -11 l12 11 -12 11" stroke={amber} strokeWidth="2.8" fill="none"/>
        <text x="1188" y="889" fontFamily={serif} fontWeight="600" fontSize="47" fill={amber}>璃月港</text>
      </g>
    </g>
  </g>;
}

export function Mechanism() {
  // 各阶段分别配时，玩家轨迹、交点与章节文案仍共享同一判定时刻。
  const f = interpolate(useCurrentFrame(), [0,270,465,660,mechanismFrames], [0,180,300,420,540]);
  return <AbsoluteFill style={{backgroundColor: '#0B1B24'}}>
    <svg width="100%" height="100%" viewBox="0 0 1920 1080" fontFamily={sans} role="img" aria-label="域名同步与客户端空间判定动画">
      <Definitions/>
      <rect width="1920" height="1080" fill="url(#mechanism-bg)"/>
      <path d="M96 138 H1828 M96 944 H1828" stroke="#456069" strokeWidth="1.3"/>
      <text x="96" y="112" fill={ink} fontSize="28" letterSpacing="6">域名如何触发</text>
      <text x="1827" y="113" fill={muted} fontFamily={mono} textAnchor="end" fontSize="22" letterSpacing="4">AREA HINTS</text>
      {f < 183 && <Network f={f}/>}
      {f >= 167 && <Survey f={f}/>}
      <Caption f={f}/>
      {chapters.map(({start}, i) => {
        const end = i < 3 ? chapters[i + 1].start : 540;
        const x = 96 + i * 437;
        return <g key={start}>
          <path d={`M${x} 944 h420`} stroke="#4B6065" strokeWidth="2.5"/>
          <path d={`M${x} 944 h${420 * Math.max(0, Math.min(1, (f - start) / (end - start)))}`} stroke={cyan} strokeWidth="3.5"/>
        </g>;
      })}
    </svg>
  </AbsoluteFill>;
}
