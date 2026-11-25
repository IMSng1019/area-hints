import React from 'react';
import {AbsoluteFill, useCurrentFrame} from 'remotion';
import {aqua, gold, ink, paper, sans, serif, tween} from './visual';

export const mechanismFrames = 19 * 30;
const muted = '#b5c7c2';
const steps = [
  {start: 0, label: '进服同步', title: '进服时，先同步域名', copy: '服务端按维度发送域名文件。客户端保存副本，加载当前维度的边界。'},
  {start: 136, label: '本地识别', title: '走进边界，地名浮现', copy: '每个客户端用自己的位置识别域名。A 走进璃月港触发跳字，B 仍在港外。'},
  {start: 284, label: '提交编辑', title: '扩界后，交给服务端', copy: 'A 提交扩界请求。服务端核对编辑权限与新边界，通过后写入世界存档。'},
  {start: 420, label: '同步更新', title: '边界一改，在线玩家同步更新', copy: '服务端向在线玩家分发新边界。B 原地重新识别，也进入了璃月港。'},
];

// 三张图共用港区地形，玩家走动与服务端修改边界是两种独立的变化。
function harborOutline(extension: number) {
  return `M66 112 L91 63 L148 49 L192 61 L218 54 L${248+extension*62} ${75-extension*16} L${258+extension*76} 116 L${231+extension*66} ${148+extension*11} L177 172 L119 157 L90 163 Z`;
}

function Atlas({extension=0, loaded=1, player, movement=0, proposed=0}: {
  extension?:number; loaded?:number; player?:'A'|'B'; movement?:number; proposed?:number;
}) {
  const x = player === 'A' ? 42 + 102 * movement : 298;
  return <g>
    <rect width="400" height="220" rx="5" fill="#153139"/>
    <svg width="400" height="220" viewBox="0 0 400 220" overflow="hidden">
      <path d="M-10 233 V-10 H360 C354 35 322 49 335 75 C349 102 325 126 291 140 L284 160 L236 177 L219 200 L190 224 Z" fill="#2c4645"/>
      {[0, 1, 2, 3].map(i=><path key={i} transform={`translate(${i*9},${i*5})`} d="M360 -10 C358 32 320 49 335 75 C349 102 325 126 291 140 L284 160 L236 177 L219 200 L190 224" fill="none" stroke="#78928c" strokeWidth={i ? .8 : 1.8} opacity={i ? .28 : .65}/>)}
      <g transform="rotate(-19 157 108)" fill="#68817a" opacity=".48">
        {[[27,64],[58,64],[94,64],[136,64],[170,64],[46,98],[79,98],[125,98],[163,98],[201,98],[62,133],[101,133],[140,133],[181,133]].map(([cx,cy])=><rect key={cx+','+cy} x={cx} y={cy} width="23" height="13"/>)}
        <path d="M8 88 H254 M28 123 H280 M116 40 V167" fill="none" stroke="#9aa99a" strokeWidth="4"/>
      </g>
      <g stroke="#9bb4ad" fill="none" opacity=".40"><path d="M251 158 l15 25 9 -5 -16 -24 M275 150 l18 26 8 -5 -18 -27 M304 136 l15 22 7 -4 -15 -23"/></g>
      <rect width="400" height="220" fill="url(#sync-grid)"/>
      <g opacity={loaded}>
        <path d={harborOutline(extension)} stroke={aqua} strokeWidth="3.2" fill={aqua} fillOpacity=".21" strokeLinejoin="round"/>
        {proposed > 0 && <path d={harborOutline(1)} stroke={gold} strokeWidth="3" strokeDasharray="7 5" fill={gold} fillOpacity=".06" opacity={proposed}/>}
      </g>
      {player && <g opacity={loaded}>
        {player==='A' && movement>0 && <line x1="42" y1="112" x2={x} y2="112" stroke={paper} strokeWidth="2" strokeDasharray="4 5"/>}
        <circle cx={x} cy="112" r="11" fill="#102028" stroke={gold} strokeWidth="2.4"/>
        <circle cx={x} cy="112" r="5" fill={gold}/>
      </g>}
    </svg>
    <rect width="400" height="220" rx="5" stroke="#74928d" strokeWidth="1.3" fill="none"/>
  </g>;
}

function Client({id,y,f}: {id:'A'|'B'; y:number; f:number}) {
  const received=tween(f,72,92);
  const movement=tween(f,153,215);
  const updated=tween(f,459,489);
  const proposed=id==='A' ? tween(f,292,316)*(1-tween(f,456,480)) : 0;
  const atHarbor=id==='A' ? movement > .25 : updated > .55;
  const titleProgress=id==='A' ? tween(f,172,192) : tween(f,473,494);
  const status= f<136 ? (f<86 ? '接收数据' : '已载入主世界') :
    f<284 ? (id==='A' ? '跨过港区边界' : '停留在港外') :
    f<420 ? (id==='A' ? '提交扩界请求' : '仍使用原边界') :
    updated<.55 ? '等待新边界' : (id==='A' ? '载入新边界' : '原地进入港区');
  return <g transform={`translate(1090,${y})`}>
    <Atlas player={id} loaded={received} movement={movement} extension={updated} proposed={proposed}/>
    <g transform="translate(436,0)">
      <text y="47" fill={paper} fontSize="37" fontWeight="700">客户端 {id}</text>
      <text y="100" fill={muted} fontSize="29">{status}</text>
      {f>=136 && atHarbor && <g opacity={titleProgress} transform={`translate(0,${(1-titleProgress)*9})`}>
        <path d="M0 133 h105" stroke={gold} strokeWidth="1.8"/>
        <text y="193" fill={gold} fontFamily={serif} fontSize="51" fontWeight="bold">璃月港</text>
      </g>}
    </g>
  </g>;
}

// 沿同一条曲线移动数据包，扩界请求走回服务端，存档完成后再向两个客户端分发。
function NetworkLine({targetY,progress,reverse,active}: {targetY:number;progress:number;reverse:boolean;active:boolean}) {
  const t=reverse ? 1-progress : progress;
  const v=1-t;
  const x=v*v*v*663+3*v*v*t*878+3*v*t*t*894+t*t*t*1070;
  const y=(v*v*v+3*v*v*t)*600+(3*v*t*t+t*t*t)*targetY;
  const tone=reverse?gold:aqua;
  return <g>
    <path d={`M663 600 C878 600 894 ${targetY} 1070 ${targetY}`} stroke={active?tone:'#4d6569'} strokeOpacity={active?.87:.65} strokeWidth={active?3:2} fill="none"/>
    {active && progress>0 && progress<1 && <g transform={`translate(${x},${y})`}>
      <circle r="20" fill={tone} opacity=".13"/><rect x="-6.5" y="-6.5" width="13" height="13" transform="rotate(45)" fill={tone}/>
    </g>}
  </g>;
}

export function Mechanism() {
  const f=useCurrentFrame();
  const stage=steps.filter(step=>f>=step.start).length-1;
  const {start,title,copy}=steps[stage];
  const captionOpacity=tween(f,start,start+9)*(stage===3?1:1-tween(f,steps[stage+1].start-6,steps[stage+1].start));
  const reverse=stage===2;
  const transfer=stage===0?tween(f,20,78):reverse?tween(f,319,359):tween(f,427,470);
  const serverState=f<359?'世界存档':f<382?'校验编辑权限与边界':'扩界已保存';
  return <AbsoluteFill style={{backgroundColor:ink}}>
    <svg viewBox="0 0 1920 1080" width="100%" height="100%" fontFamily={sans} aria-label="进服同步、本地识别、提交编辑与在线玩家更新">
      <defs>
        <radialGradient id="sync-bg" cx=".65" cy=".5" r=".78"><stop stopColor="#18343c"/><stop offset="1" stopColor="#0a1922"/></radialGradient>
        <pattern id="sync-grid" width="25" height="25" patternUnits="userSpaceOnUse"><path d="M25 0 H0 V25" fill="none" stroke="#9db4b1" strokeWidth=".7" opacity=".24"/></pattern>
      </defs>
      <rect width="1920" height="1080" fill="url(#sync-bg)"/>
      <text x="96" y="87" fill={aqua} fontSize="28" fontWeight="600" letterSpacing="3">服务端与客户端</text>
      <path d="M96 114 H1824" stroke="#50666b"/>
      <g opacity={captionOpacity}>
        <text x="90" y="218" fill={paper} fontFamily={serif} fontSize="77" fontWeight="bold">{title}</text>
        <text x="97" y="286" fill="#d1dcd6" fontSize="34">{copy}</text>
      </g>
      <g transform="translate(96 450)">
        <path d="M12 10 H547 V305 H12 Z M22 20 H557 V315 H22 Z" fill="#172b32" stroke="#537175" strokeWidth="1.6"/>
        <g transform="scale(1.34)"><Atlas extension={tween(f,368,389)}/></g>
        <text y="-33" fontSize="40" fontWeight="700" fill={paper}>服务端</text>
        <text x="530" y="-34" textAnchor="end" fill={muted} fontSize="27">主世界 · 港区示意</text>
        <text y="370" fill={f<359 ? muted : gold} fontSize="31">{serverState}</text>
      </g>
      <NetworkLine targetY={460} progress={transfer} reverse={reverse} active={stage!==1}/>
      <NetworkLine targetY={744} progress={transfer} reverse={false} active={stage===0||stage===3}/>
      <text x="847" y="850" textAnchor="middle" fill={reverse?gold:muted} fontSize="27">
        {['分发维度文件','各自识别位置','A 上传新边界','同步新边界'][stage]}
      </text>
      <Client id="A" y={350} f={f}/><Client id="B" y={634} f={f}/>
      {steps.map((step,index)=>{
        const x=96+index*436;
        const end=steps[index+1]?.start??mechanismFrames;
        const p=Math.max(0,Math.min(1,(f-step.start)/(end-step.start)));
        return <g key={step.label}>
          <path d={`M${x} 940 h411`} stroke="#415b60" strokeWidth="2.6"/>
          <path d={`M${x} 940 h${411*p}`} stroke={aqua} strokeWidth="4"/>
          <text x={x} y="996" fill={stage===index?paper:muted} fontSize="29" fontWeight={stage===index?'bold':'normal'}>{step.label}</text>
        </g>;
      })}
    </svg>
  </AbsoluteFill>;
}
