import React from 'react';
import {Sequence,useCurrentFrame} from 'remotion';
import titles from './titles.json';
import {Board,Clip,Label,Picture,Rise,aqua,box,gold,latin,paper,serif,tween} from './visual';

export function Titles() {
  const f=useCurrentFrame();
  return <Board>
    <div style={box(104,89,1700,190)}><Label>跨过边界</Label><Rise style={{fontFamily:serif,fontSize:83,marginTop:12}}>让地名跳出来。</Rise></div>
    {titles.map((item,i)=>{
      const x=104+(i%2)*865,y=320+Math.floor(i/2)*307;
      return <div key={item.file} style={{...box(x,y,820,271),borderTop:'2px solid #687775',borderBottom:'1px solid #35474c',background:'linear-gradient(120deg,#122833,#0d2028)',opacity:tween(f,0,12),display:'flex',alignItems:'center',justifyContent:'center'}}>
        <div style={{position:'absolute',top:17,left:24,color:'#b4c9c6',fontSize:25}}>{['入城实拍','一级','父域名与二级','三级'][i]}</div>
        <Picture file={item.file} style={{objectFit:'contain',width:'auto',maxWidth:'89%',height:i===0?77:135,marginTop:30,imageRendering:'pixelated',transform:`translateY(${(1-tween(f,4,21))*12}px)`}}/>
      </div>;
    })}
    <div style={{...box(106,956,1650,55),fontSize:33,color:'#d0ded9'}}>同一片世界里，城、街区与地标可以各有自己的名字和副标题。</div>
  </Board>;
}

const features={
  create:{clip:'create',kicker:'域名编辑',head:['给区域','划一条边界。'],body:['在面板中发起创建，','沿地图点出轮廓，','再填写域名属性。']},
  bounds:{clip:'bounds',kicker:'边界可视化',head:['回到世界，','看清范围。'],body:['打开边界可视化，','在实景中核对顶点','和围出的范围。']},
  story:{clip:'story',kicker:'域名描述',head:['地名背后，','还有故事。'],body:['在风起地打开描述，','读一页地图作者','写下的故事。']},
};
export function Feature({kind}:{kind:keyof typeof features}) {
  const f=useCurrentFrame(),data=features[kind];
  // 缓慢移动取景中心，跟随录屏中从地图绘制区到属性栏的操作。
  const focus=kind==='create'?tween(f,62,156):0;
  const steps=['选择创建','点出边界','编辑属性','核对预览'];
  const active=f<50?0:f<164?1:f<211?2:3;
  return <Board>
    <div style={{...box(0,105,1280,851),overflow:'hidden',background:'#081319',clipPath:`inset(0 ${(1-tween(f,0,14))*100}% 0 0)`}}>
      {kind==='bounds'?<>
        <Sequence durationInFrames={38} name="打开开关"><Clip name="bounds" offset={8} style={{objectFit:'contain'}}/></Sequence>
        <Sequence from={34} name="围绕实地边界"><Clip name="aerial" offset={42} style={{objectFit:'contain',opacity:tween(f,34,38)}}/></Sequence>
      </>:<Clip name={data.clip} offset={kind==='story'?37:0} style={{objectFit:'contain',transform:`scale(${kind==='create'?1.045:1}) translateX(${(0.48-focus)*2.7}%)`}}/>}
      {kind==='create'&&f>=55&&f<158&&<svg width="1280" height="851" style={{position:'absolute',inset:0,opacity:tween(f,55,68)*(1-tween(f,143,158))}}><path d="M 692 235 H 812 L 847 270" fill="none" stroke="#ffe3a5" strokeWidth="3"/><circle cx="847" cy="270" r="6" fill="#ffe3a5"/><rect x="507" y="200" width="186" height="60" rx="4" fill="#0a1c25" opacity=".94"/><text x="530" y="240" fill="#ffe3a5" fontSize="28">绘制范围</text></svg>}
    </div>
    <div style={{...box(1290,105,2,851),background:'linear-gradient(transparent,#819d99 20%,#819d99 80%,transparent)'}}/>
    <div style={box(1360,192,515,795)}><Label>{data.kicker}</Label>
      <Rise delay={4} style={{fontFamily:serif,fontSize:69,lineHeight:1.23,marginTop:50,letterSpacing:-2}}>{data.head.map(t=><div key={t}>{t}</div>)}</Rise>
      <Rise delay={11} style={{fontSize:34,lineHeight:1.65,marginTop:38,color:'#cedcd7'}}>{data.body.map(t=><div key={t}>{t}</div>)}</Rise>
      {kind==='create'&&<div style={{marginTop:48}}>{steps.map((s,i)=><div key={s} style={{display:'flex',alignItems:'center',gap:16,marginTop:10,fontSize:28,color:i===active?gold:'#839b9a'}}><div style={{width:i===active?55:17,height:2,background:'currentColor'}}/>{s}</div>)}</div>}
    </div>
  </Board>;
}

export function Maps(){
  const f=useCurrentFrame();
  return <Board><div style={box(98,77,1700,160)}><Label>地图联动</Label><Rise style={{fontFamily:serif,fontSize:76,marginTop:15}}>同一组域名，两张地图。</Rise></div>
    {['bluemap','xaero'].map((id,i)=><div key={id} style={{...box(98+873*i,295,849,640)}}>
      <div style={{width:'100%',height:538,overflow:'hidden',borderTop:'2px solid '+(i?aqua:gold)}}><Picture file={'art/'+id+'.jpg'} style={{objectPosition:i?'62% 50%':'64% 48%',scale:String(1.03+tween(f,0,150,0,.05))}}/></div>
      <div style={{fontFamily:latin,fontSize:42,marginTop:23}}>{i?'Xaero':'BlueMap'}</div>
    </div>)}
  </Board>;
}
export function Details() {
  return <Board><div style={box(106,185,1630,740)}><Label>补充信息</Label><div style={{fontFamily:serif,fontSize:80,marginTop:36}}>从指令，也能进入工作流。</div>
  {[['/areahint boundviz','切换边界可视化'],['/areahint reload','重新载入域名数据']].map(([cmd,desc])=><div key={cmd} style={{display:'flex',alignItems:'baseline',borderBottom:'1px solid #4c6666',marginTop:46,paddingBottom:20}}><div style={{width:790,fontFamily:'Consolas,monospace',fontSize:45,color:aqua}}>{cmd}</div><div style={{fontSize:36}}>{desc}</div></div>)}
  <div style={{fontSize:27,color:'#adc3bf',marginTop:34}}>Area Hints 模组代码采用 MIT 许可。</div></div></Board>;
}
