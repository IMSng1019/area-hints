import React from 'react';
import {AbsoluteFill, Img, OffthreadVideo, Sequence, staticFile} from 'remotion';

export const paper='#f0ebd9',ink='#142f2f',green='#226658',gold='#edb352',soft='#c8d6bb';
export const font='"Source Han Sans SC","Microsoft YaHei",sans-serif';
export const ramp=(t,a,b)=>Math.max(0,Math.min(1,(t-a)/(b-a)));
export const ease=(t,a,b)=>{const u=ramp(t,a,b);return u*u*(3-2*u)};
export const pos=(left,top,width,height)=>({position:'absolute',left,top,width,height});
export const Canvas=({children,theme=paper})=><AbsoluteFill style={{background:theme,color:ink,fontFamily:font,overflow:'hidden'}}>{children}</AbsoluteFill>;
export const Pic=({file,style={}})=><Img src={staticFile(file)} style={{width:'100%',height:'100%',objectFit:'cover',...style}}/>;
export const Text=({x,y,w,size=48,weight=650,color=ink,children,style={}})=><div style={{...pos(x,y,w),fontSize:size,fontWeight:weight,lineHeight:1.38,color,whiteSpace:'pre-line',...style}}>{children}</div>;
export const Window=({children})=><div style={{...pos(76,134,1280,720),border:'2px solid #34574b',background:'#193a36',boxShadow:'0 13px 0 #d3ceba',overflow:'hidden'}}>{children}</div>;
export const Clip=({from=0,seconds,file})=><Sequence from={Math.round(from*60)} durationInFrames={Math.round(seconds*60)}><OffthreadVideo muted src={staticFile(file+'.mp4')} style={{width:'100%',height:'100%',objectFit:'contain'}}/></Sequence>;
export const Side=({kicker,lines,small})=><><div style={{...pos(1436,232,77,7),background:green}}/><Text x={1436} y={266} w={415} size={28} color={green}>{kicker}</Text><Text x={1436} y={339} w={435} size={54}>{lines}</Text>{small&&<Text x={1436} y={666} w={410} size={29} weight={450} color={green}>{small}</Text>}</>;
export const Svg=({children,viewBox='0 0 1920 1080'})=><svg style={{width:'100%',height:'100%',display:'block'}} viewBox={viewBox}>{children}</svg>;
