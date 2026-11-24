import React from 'react';
import {AbsoluteFill,useCurrentFrame} from 'remotion';
import {Board,Clip,Label,Picture,Rise,box,gold,ink,latin,paper,serif,tween} from './visual';

// 红石用途使用等距电路示意，信号从拉杆经红石线传入灯块。
function Circuit() {
  const f=useCurrentFrame(),lit=f%138>56;
  const project=(x:number,z:number,y=0)=>[261+(x-z)*42,177+(x+z)*22-y*44];
  const poly=(pts:number[][])=>pts.map(([x,y])=>x+','+y).join(' ');
  const block=(x:number,z:number,h:number,colors:string[])=>{
    const a=project(x,z,h),b=project(x+1,z,h),c=project(x+1,z+1,h),d=project(x,z+1,h);
    return <g key={x+','+z} stroke="#14202a" strokeWidth="1.5"><polygon points={poly([b,c,project(x+1,z+1),project(x+1,z)])} fill={colors[1]}/><polygon points={poly([c,d,project(x,z+1),project(x+1,z+1)])} fill={colors[2]}/><polygon points={poly([a,b,c,d])} fill={colors[0]}/></g>;
  };
  return <svg viewBox="0 0 550 540" style={{width:'100%',height:'100%',background:'#20343d'}}>
    {Array.from({length:5},(_,z)=>Array.from({length:5},(_,x)=>block(x,z,.5,['#687c7b','#354b50','#2c4249'])))}
    <polyline fill="none" stroke={lit?'#fe6d54':'#743b38'} strokeWidth="11" points={poly([[.5,3.5],[2.5,3.5],[2.5,1.5],[3.5,1.5]].map(([x,z])=>project(x,z,.52)))}/>
    {block(3,1,1.45,lit?['#ffe4a3','#ebaa58','#c27c35']:['#946a4e','#6a4637','#493834'])}
    <line x1={project(.5,3.5,.55)[0]} y1={project(.5,3.5,.55)[1]} x2={project(.5,3.5,1.28)[0]+(lit?12:-12)} y2={project(.5,3.5,1.28)[1]} stroke="#e6c194" strokeWidth="10"/>
  </svg>;
}
export function Uses() {
  const f=useCurrentFrame();
  return <Board>
    <div style={box(94,86,1715,145)}><Label>把域名用在自己的世界里</Label></div>
    {['开一个生存 RPG 服','给朋友标出红石机器','为地图填上颜色'].map((caption,i)=><div key={caption} style={{...box(94+i*584,188,558,622),opacity:tween(f,i*44,i*44+26),transform:'translateY('+((1-tween(f,i*44,i*44+26))*23)+'px)'}}>
      <div style={{height:495,overflow:'hidden'}}>{i===0?<Clip name="harbor-orbit" offset={188} style={{objectPosition:'48% 50%'}}/>:i===1?<Circuit/>:<Picture file="art/xaero.jpg" style={{objectPosition:'72% 45%'}}/>}</div>
      <div style={{fontSize:39,fontFamily:serif,marginTop:20,borderTop:'2px solid #819b99',paddingTop:16}}>{caption}</div>
    </div>)}
    <Rise delay={151} style={{...box(94,875,1740,130),fontFamily:serif,fontSize:57}}>你想要的，<span style={{color:gold,fontFamily:serif}}>Area Hints</span> 都能实现。</Rise>
    <AbsoluteFill style={{background:'#fff',opacity:tween(f,300,329)}}/>
  </Board>;
}
export function Credits() {
  const f=useCurrentFrame();
  return <AbsoluteFill style={{background:'#fff',fontFamily:serif,color:ink,alignItems:'center',justifyContent:'center'}}>
    <div style={{textAlign:'center',opacity:tween(f,5,30)*(1-tween(f,121,149))}}>
      <div style={{fontSize:29,letterSpacing:8,marginBottom:44}}>制作名单</div>
      <div style={{fontSize:63,marginBottom:22}}>模组作者　<span style={{fontFamily:latin}}>IMSng</span></div>
      <div style={{fontSize:38,lineHeight:1.9}}>分镜与实机录制　创作团队<br/>实机场景　方块提瓦特</div>
    </div>
  </AbsoluteFill>;
}
export function Logo(){
  const f=useCurrentFrame(),p=tween(f,6,70);
  const nodes=[[48,5],[96,32],[74,94],[20,79],[5,40]];
  return <AbsoluteFill style={{background:'#fff',color:'#101a1f',alignItems:'center',justifyContent:'center'}}>
    <div style={{position:'relative',width:245,height:245}}><svg viewBox="-6 -6 114 114" width="245" height="245" style={{opacity:1-tween(f,69,87)}}>
      <path d="M48 5 L96 32 L74 94 L20 79 L5 40 Z" stroke="#111" strokeWidth="4.4" fill="none" pathLength="1" strokeDasharray="1" strokeDashoffset={1-p}/>
      {nodes.map(([cx,cy],i)=><circle key={cx} cx={cx} cy={cy} r={4.9*tween(f,5+i*8,17+i*8)} fill="#383838"/>)}
    </svg><Picture file="art/icon.png" style={{position:'absolute',inset:0,objectFit:'contain',opacity:tween(f,69,87)}}/></div>
    <Rise delay={43} style={{fontFamily:serif,fontSize:103,letterSpacing:-4,marginTop:18}}>Area Hints</Rise>
  </AbsoluteFill>;
}
