import React from 'react';
import {AbsoluteFill, Easing, Img, OffthreadVideo, interpolate, staticFile, useCurrentFrame} from 'remotion';

export const ink = '#0b1821', paper = '#f4f0e7', gold = '#ebbc78', aqua = '#8adcd0';
export const sans = '"Microsoft YaHei", "Noto Sans CJK SC", sans-serif';
export const serif = 'Georgia,"SimSun","Noto Serif CJK SC",serif';
export const latin = 'Bahnschrift,"Arial Narrow",Arial,sans-serif';
export const full: React.CSSProperties = {width:'100%',height:'100%',objectFit:'cover',display:'block'};
export const box = (left:number,top:number,width:number,height:number):React.CSSProperties => ({position:'absolute',left,top,width,height});
export function tween(frame:number, start:number, end:number, a=0, b=1) {
  return interpolate(frame,[start,end],[a,b],{extrapolateLeft:'clamp',extrapolateRight:'clamp',easing:Easing.bezier(.28,.04,.22,1)});
}
export function Clip({name,offset=0,style={}}:{name:string;offset?:number;style?:React.CSSProperties}) {
  return <OffthreadVideo muted src={staticFile('clips/'+name+'.mp4')} startFrom={offset} style={{...full,...style}}/>;
}
export function Picture({file,style={}}:{file:string;style?:React.CSSProperties}) {
  return <Img src={staticFile(file)} style={{...full,...style}}/>;
}
export function Rise({children,delay=0,style={}}:React.PropsWithChildren<{delay?:number;style?:React.CSSProperties}>) {
  const f=useCurrentFrame(),p=tween(f,delay,delay+17);
  return <div style={{opacity:p,transform:'translateY('+((1-p)*22)+'px)',...style}}>{children}</div>;
}
export function Label({children,color=gold}:React.PropsWithChildren<{color?:string}>) {
  return <div style={{color,fontFamily:latin,fontSize:24,fontWeight:600,letterSpacing:4.5}}>{children}</div>;
}
// 用同一组等高线连接实拍与机制图，保持地形线在文案区之外。
export function Contours({tone='#96bfbe',opacity=.18}:{tone?:string;opacity?:number}) {
  return <svg width="1920" height="1080" style={{position:'absolute',inset:0,opacity}} viewBox="0 0 1920 1080">
    {Array.from({length:23},(_,i)=><path key={i} d={`M ${590+i*26} -90 C ${780+i*20} 100,${1310+i*14} -15,${1200+i*19} 315 S ${890+i*21} 663,${1260+i*19} 804 S ${1630+i*18} 1000,${1450+i*19} 1210`} fill="none" stroke={tone} strokeWidth={i%5===0?2:1}/>)}
  </svg>;
}
export function Board({children}:React.PropsWithChildren) {
  return <AbsoluteFill style={{backgroundColor:ink,color:paper,fontFamily:sans,overflow:'hidden'}}><Contours/>{children}</AbsoluteFill>;
}
