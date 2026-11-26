import React from 'react';
import {AbsoluteFill, Easing, interpolate, useCurrentFrame} from 'remotion';
import {HarborPopup} from './HarborPopup';
import {Contours, Picture, backgroundInk, box, gold, paper, serif, tween} from './visual';

export const harborPreroll = 15;

function glide(frame: number, from: number, to: number) {
  return interpolate(frame,[from,to],[0,1],{
    easing:Easing.bezier(.42,0,.25,1), extrapolateLeft:'clamp', extrapolateRight:'clamp',
  });
}

function HarborTitle({frame}: {frame: number}) {
  const emblem = tween(frame,13,36);
  const reveal = tween(frame,28,55);
  const question = tween(frame,18,38);
  const edge = reveal * 116 - 16;
  return <div style={{...box(972,350,875,380),display:'flex',alignItems:'center',flexDirection:'column',
    justifyContent:'center',color:paper,opacity:1-glide(frame,132,155)}}>
    <svg width="204" height="41" viewBox="0 0 204 41" aria-hidden="true" style={{opacity:emblem}}>
      <defs><linearGradient id="harbor-rule" gradientUnits="userSpaceOnUse" x1="0" x2="74" y1="21" y2="21"><stop stopColor={gold} stopOpacity="0"/><stop offset="1" stopColor={gold}/></linearGradient></defs>
      <path d="M0 21h74" stroke="url(#harbor-rule)" strokeWidth="1.5"/>
      <path d="M0 21h74" transform="translate(204 0) scale(-1 1)" stroke="url(#harbor-rule)" strokeWidth="1.5"/>
      <path d="M101 3l17 10-8 23-19-6-6-15Z" fill="none" stroke={gold} strokeWidth="2.3"
        pathLength="1" strokeDasharray="1 1" strokeDashoffset={1-emblem} strokeLinejoin="round"/>
    </svg>
    <div style={{fontFamily:serif,fontSize:154,letterSpacing:-8,lineHeight:1.16,whiteSpace:'nowrap',padding:'0 18px 3px',
      maskImage:`linear-gradient(90deg,black ${edge}%,transparent ${edge+16}%)`,
      translate:`0 ${(1-reveal)*10}px`}}>
      Area <i style={{color:gold}}>Hints</i>
    </div>
    <div style={{fontFamily:'"STZhongsong","SimSun",serif',fontSize:42,letterSpacing:.8,color:'#e1e5df',marginTop:9,
      opacity:question,translate:`0 ${(1-question)*7}px`}}>想要一个像原神一样跳字的模组？</div>
  </div>;
}

export function Brand() {
  // 把片名前 15 帧纳入同一次运镜，局部第 75 帧仍对应全片 13.50 秒。
  const frame = useCurrentFrame() - harborPreroll;
  const turn = 1-glide(frame,-2,53);
  const drift = glide(frame,47,151);
  const transition = glide(frame,-harborPreroll,18);
  const edge = 111-183*transition;
  // 以两个机位中同一座山峰配准，随后将山城缓慢移到左边给片名让位。
  const town = `translate(${379*turn-5*drift}px,${-6*turn-2*drift}px) scale(${1+.24*turn})`;
  const wharf = `translate(${386*turn-3*drift}px,${4*turn+drift}px) scale(${1+.236*turn})`;
  return <AbsoluteFill style={{overflow:'hidden',background:backgroundInk,
    maskImage:transition<1?`linear-gradient(102deg,transparent ${edge}%,black ${edge+65}%)`:undefined}}>
    <AbsoluteFill style={{background:[
      'radial-gradient(ellipse at 27% 32%,#487b7b 0%,transparent 51%)',
      'radial-gradient(ellipse at 91% 20%,#a75a32 0%,transparent 54%)',
      `linear-gradient(105deg,#164a50 0%,${backgroundInk} 49%,#43332b 100%)`,
    ].join(',')}}/>
    <AbsoluteFill style={{transformOrigin:'100% 50%',transform:`translate(${40*turn-2*drift}px,${4*turn}px) scale(${1+.04*turn})`}}>
      <Picture file="art/brand-far-ridge.webp"/>
    </AbsoluteFill>
    <AbsoluteFill style={{transformOrigin:'100% 50%',transform:`translate(${100*turn-3*drift}px,${12*turn}px) scale(${1+.08*turn})`}}>
      <Picture file="art/brand-mid-ridge.webp"/>
    </AbsoluteFill>
    <AbsoluteFill style={{transform:town,transformOrigin:'0 0'}}>
      <AbsoluteFill><Picture file="art/brand-harbor-plate.webp" style={{position:'absolute',left:-226,width:2146}}/></AbsoluteFill>
      <AbsoluteFill><Picture file="art/brand-harbor.webp" style={{filter:'saturate(.88) brightness(.99)'}}/></AbsoluteFill>
    </AbsoluteFill>
    <AbsoluteFill style={{transform:wharf,transformOrigin:'0 0'}}>
      <Picture file="art/brand-wharf.webp" style={{filter:'saturate(.94)'}}/>
    </AbsoluteFill>
    <AbsoluteFill style={{background:'linear-gradient(0deg,#08182170,transparent 29%)'}}/>
    <AbsoluteFill style={{maskImage:'linear-gradient(90deg,transparent 54%,black 83%)'}}>
      <Contours tone="#dfab80" opacity={.027+.08*glide(frame,126,165)}/>
    </AbsoluteFill>
    <AbsoluteFill style={{transformOrigin:'50% 100%',
      transform:`translate(${-12*drift}px,${36*turn+2*drift}px) scale(${1+.29*turn})`}}>
      <Picture file="art/brand-bank.webp"/>
    </AbsoluteFill>
    <HarborTitle frame={frame}/>
    <div style={box(300,242,260,96)}><HarborPopup frame={frame}/></div>
  </AbsoluteFill>;
}
