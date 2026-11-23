import React from 'react';
import {AbsoluteFill, OffthreadVideo, staticFile} from 'remotion';

export function Cover() {
  return <AbsoluteFill style={{background:'#182b27',fontFamily:'"Microsoft YaHei", sans-serif',color:'#fff9e9'}}>
    <OffthreadVideo muted trimBefore={480} src={staticFile('master/进入璃月港.mp4')}
      style={{width:'100%',height:'100%',objectFit:'cover'}}/>
    <AbsoluteFill style={{background:'linear-gradient(0deg,rgba(8,25,22,.92),rgba(8,25,22,.55) 25%,transparent 55%)'}}/>
    <div style={{position:'absolute',left:98,bottom:118}}>
      <div style={{display:'flex',alignItems:'center',gap:20,fontSize:44,fontWeight:600,color:'#ffe4a3'}}>
        <span style={{width:72,height:6,background:'#efbd60'}}/>Area Hints <span style={{fontSize:37}}>· 自制 MC 域名模组</span>
      </div>
      <div style={{marginTop:12,fontSize:137,fontWeight:800,letterSpacing:-3,lineHeight:1.2}}>MC 也能报地名</div>
    </div>
  </AbsoluteFill>;
}
