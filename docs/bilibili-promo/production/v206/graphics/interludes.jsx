import React from 'react';
import {AbsoluteFill, Img, useCurrentFrame} from 'remotion';
import harbor from './art/harbor.jpg';
import windrise from './art/windrise.jpg';
import {Canvas, Pic, ease, gold, green, ink, paper, pos} from './shared';

const fill = {position:'absolute',inset:0,width:'100%',height:'100%',objectFit:'cover'};
const serif = '"Noto Serif SC","Songti SC","SimSun",serif';
const border = 'M1220 -50C1180 80 1129 175 1153 260S1008 372 1022 465S902 619 930 723S957 894 863 1110';
const ground = border + 'L1940 1110V-50Z';

function Footprint({x, y, angle, start, t, crossed}) {
  const down = ease(t, start, start + .33);
  return <g opacity={down} transform={`translate(${x} ${y + 8*(1-down)}) rotate(${angle}) scale(${1.05-.05*down})`}>
    {crossed && <ellipse cy="-22" rx={33+38*down} ry={56+40*down} fill="none"
      stroke={gold} strokeWidth="2" opacity={1-ease(t,start+.2,start+1.1)}/>}
    <g fill={crossed?'#b57422':ink} stroke={paper} strokeWidth="2">
      <path d="M-24 0Q-32 -24 -19 -58Q-14 -79 6 -82Q26 -85 30 -64Q34 -36 17 6Z"/>
      <path d="M-23 11L15 18 12 36Q6 55 -14 45Q-24 41 -23 28Z"/>
    </g>
  </g>;
}

export function Intro() {
  const t = useCurrentFrame()/60;
  const reveal = ease(t,.35,2.1);
  const title = ease(t,6.3,7.65);
  return <Canvas>
    <svg viewBox="0 0 1920 1080" style={fill}>
      <defs>
        <linearGradient id="paper-light" x2="1" y2=".9" gradientUnits="objectBoundingBox">
          <stop stopColor="#fff9e7"/><stop offset="1" stopColor="#dcd4b9"/>
        </linearGradient>
        <pattern id="pavers" width="156" height="78" patternUnits="userSpaceOnUse" patternTransform="rotate(-18)">
          <path d="M0 1h156M0 40h156M2 1v39M80 40v38" fill="none" stroke="#a3997a" strokeWidth="1.5"/>
        </pattern>
        <clipPath id="coast-land"><path d={ground}/></clipPath>
        <linearGradient id="harbor-wash" x1="0%" y1="50%" x2="100%" y2="45%">
          <stop stopColor="#d3d7b9" stopOpacity=".8"/><stop offset=".57" stopColor="#d8dfa9" stopOpacity=".18"/><stop offset="1" stopColor="#e7dcc1" stopOpacity=".08"/>
        </linearGradient>
      </defs>
      <rect width="1920" height="1080" fill="url(#paper-light)"/>
      <path d="M-145 886C420 772 515 663 788 603S1290 412 1565 304" fill="none" stroke="#c2b498" strokeWidth="246" opacity=".24"/>
      <path d="M-145 886C420 772 515 663 788 603S1290 412 1565 304" fill="none" stroke="url(#pavers)" strokeWidth="235" opacity=".49"/>
      <g clipPath="url(#coast-land)">
        <image href={harbor} x="770" y="0" width="1220" height="1080" preserveAspectRatio="xMidYMid slice" opacity=".93"/>
        <rect x="840" width="1090" height="1080" fill="url(#harbor-wash)"/>
      </g>
      <path d={border} fill="none" stroke="#fcf6dc" strokeWidth="10"/>
      <path d={border} pathLength="1" fill="none" stroke={green} strokeWidth="3.5" strokeDasharray="1" strokeDashoffset={1-ease(t,.55,2.35)}/>
      <path d="M289 803Q562 684 645 650T1050 513" pathLength="1" fill="none" stroke="#988268"
        strokeWidth="3" strokeDasharray=".006 .015" opacity=".65" style={{clipPath:`inset(0 ${100-100*ease(t,1.8,5.1)}% 0 0)`}}/>
      <Footprint x={445} y={737} angle={62} t={t} start={1.84}/>
      <Footprint x={692} y={648} angle={76} t={t} start={3.16}/>
      <Footprint x={1100} y={520} angle={62} t={t} start={4.64} crossed/>
      <g opacity={ease(t,5.0,5.72)} transform="translate(1127 422)">
        <rect width="177" height="69" rx="3" fill="#173d37" opacity=".93"/>
        <text x="88" y="48" fontFamily={serif} fontSize="41" textAnchor="middle" fill="#fff6d9">璃月港</text>
      </g>
    </svg>
    <div style={{...pos(110,150,850),opacity:title,transform:`translateY(${17*(1-title)}px)`}}>
      <div style={{font:'italic 128px/1.1 Georgia,serif',letterSpacing:-6,color:ink}}>Area Hints</div>
      <div style={{marginTop:24,display:'flex',alignItems:'center',gap:24,fontSize:46,letterSpacing:5,color:green}}>
        <span style={{width:90,height:2,background:green}}/>域名模组
      </div>
    </div>
    {reveal<1 && <Pic file="port-from.png" style={{...fill,opacity:1-reveal}}/>}
  </Canvas>;
}

export function Book() {
  const t = useCurrentFrame()/60;
  const intoArt = ease(t,.07,1.55);
  const outOfArt = ease(t,3.63,4.93);
  const question = ease(t,.70,1.43) * (1-ease(t,3.41,4.18));
  // 两侧端点直接衔接源片，用树干和光柱在相同画面位置完成由实景到插画的匹配。
  const artScale = 1.12-.12*ease(t,0,4.86);
  return <Canvas theme="#18332f">
    <Pic file="tree-from.png" style={{...fill,transformOrigin:'51% 38%',transform:`scale(${1+.32*intoArt})`}}/>
    <AbsoluteFill style={{opacity:intoArt,transformOrigin:'52% 51%',transform:`scale(${artScale})`}}>
      <Img src={windrise} style={fill}/>
    </AbsoluteFill>
    <AbsoluteFill style={{opacity:question,background:'linear-gradient(90deg,rgba(13,30,25,.62),rgba(24,35,28,.10) 62%,transparent 90%)'}}/>
    <svg viewBox="0 0 1920 1080" style={{...fill,opacity:question}} aria-hidden="true">
      {Array.from({length:13},(_,i)=>{
        const phase = (i*.137+t*.10)%1;
        const x = 150+i*137+Math.sin(phase*Math.PI)*43;
        const y = 520+Math.cos(i*3.4)*157-phase*153;
        return <ellipse key={i} cx={x} cy={y} rx={2+i%3} ry={1+i%2}
          transform={`rotate(-28 ${x} ${y})`} fill="#f7d779" opacity={Math.sin(phase*Math.PI)*.68}/>;
      })}
    </svg>
    <div style={{...pos(124,504,920),opacity:question,transform:`translateY(${16*(1-question)}px)`,
      color:'#fff4d7',textShadow:'0 3px 16px #162819a8'}}>
      <div style={{display:'flex',gap:21,alignItems:'center',fontSize:33,letterSpacing:5,color:'#ffe2a1'}}>
        <span style={{width:70,height:2,background:'#f1cf89'}}/>风起地
      </div>
      <div style={{fontFamily:serif,fontSize:88,fontWeight:650,lineHeight:1.5,marginTop:10}}>这棵树有什么来头？</div>
    </div>
    {outOfArt>0 && <Pic file="book-to.png" style={{...fill,opacity:outOfArt}}/>}
  </Canvas>;
}
