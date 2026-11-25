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
        <Picture file={item.file} style={{objectFit:'contain',width:'auto',maxWidth:'89%',height:i===0?77:135,marginTop:30,imageRendering:'pixelated',transform:`translateY(${(1-tween(f,4,34))*12}px)`}}/>
        <div style={{position:'absolute',bottom:-1,left:0,height:3,width:820*tween(f,24+i*47,60+i*47),background:i<2?gold:aqua,opacity:.78}}/>
      </div>;
    })}
    <div style={{...box(106,956,1650,55),fontSize:33,color:'#d0ded9'}}>同一片世界里，城、街区与地标可以各有自己的名字和副标题。</div>
  </Board>;
}

const features={
  create:{clip:'create',kicker:'域名编辑',head:['给区域','划一条边界。'],body:['打开可视化编辑器，','用画笔涂出范围，','再填写域名属性。']},
  bounds:{clip:'bounds',kicker:'边界可视化',head:['回到世界，','看清范围。'],body:['打开边界可视化，','在实景中核对顶点','和围出的范围。']},
  story:{clip:'story',kicker:'域名描述',head:['地名背后，','还有故事。'],body:['在风起地打开描述，','读一页地图作者','写下的故事。']},
};
export function Feature({kind}:{kind:keyof typeof features}) {
  const f=useCurrentFrame(),data=features[kind];
  // 按录像里属性页和提交预览真实出现的帧切换提示。
  const steps=['打开编辑器','涂出范围','填写名称','提交前核对'];
  const active=f<54?0:f<194?1:f<377?2:3;
  return <Board>
    <div style={{...box(0,105,1280,851),overflow:'hidden',background:'#081319',clipPath:`inset(0 ${(1-tween(f,0,14))*100}% 0 0)`}}>
      {kind==='bounds'?<>
        <Sequence durationInFrames={130} name="打开开关"><Clip name="bounds" style={{objectFit:'contain'}}/></Sequence>
        <Sequence from={114} name="围绕实地边界"><Clip name="aerial" offset={24} style={{objectFit:'contain',opacity:tween(f,114,130)}}/></Sequence>
      </>:<Clip name={data.clip} style={{objectFit:'contain'}}/>}
    </div>
    <div style={{...box(1290,105,2,851),background:'linear-gradient(transparent,#819d99 20%,#819d99 80%,transparent)'}}/>
    <div style={box(1360,192,515,795)}><Label>{data.kicker}</Label>
      <Rise delay={4} style={{fontFamily:serif,fontSize:69,lineHeight:1.23,marginTop:50,letterSpacing:-2}}>{data.head.map(t=><div key={t}>{t}</div>)}</Rise>
      <Rise delay={29} duration={24} style={{fontSize:34,lineHeight:1.65,marginTop:38,color:'#cedcd7'}}>{data.body.map(t=><div key={t}>{t}</div>)}</Rise>
      {kind==='create'&&<div style={{marginTop:48}}>{steps.map((s,i)=><div key={s} style={{display:'flex',alignItems:'center',gap:16,marginTop:10,fontSize:28,color:i===active?gold:'#839b9a'}}><div style={{width:i===active?55:17,height:2,background:'currentColor'}}/>{s}</div>)}</div>}
    </div>
  </Board>;
}

export function Maps(){
  const f=useCurrentFrame();
  return <Board><div style={box(98,77,1700,160)}><Label>地图联动</Label><Rise style={{fontFamily:serif,fontSize:76,marginTop:15}}>同一组域名，两张地图。</Rise></div>
    {['bluemap','xaero'].map((id,i)=><div key={id} style={{...box(98+873*i,295,849,640)}}>
      <div style={{width:'100%',height:538,overflow:'hidden',borderTop:'2px solid '+(i?aqua:gold)}}><Picture file={'art/'+id+'.jpg'} style={{objectPosition:i?'62% 50%':'64% 48%',scale:String(1.025+tween(f,0,239,0,.055))}}/></div>
      <div style={{fontFamily:latin,fontSize:42,marginTop:23}}>{i?'Xaero':'BlueMap'}</div>
    </div>)}
  </Board>;
}
export function Details() {
  const frame = useCurrentFrame();
  // 分类沿用游戏内指令面板，每组保留两个可辨认的例子。
  const groups = [
    ['常用与查询', 'help', 'check'], ['显示与外观', 'boundviz', 'titlestyle'],
    ['域名创建与编辑', 'easyadd', 'rename'], ['顶点与形状', 'expandarea', 'dividearea'],
    ['传送', 'settp', 'tcp'], ['文本、描述与签名', 'addsubtitle', 'addsignature'],
    ['维度域名', 'firstdimname', 'dimensionalityname'], ['系统与调试', 'reload', 'debug'],
  ];
  return <Board>
    <div style={{...box(104,86,1712,130),display:'flex',alignItems:'baseline',gap:52}}>
      <div style={{fontFamily:serif,fontSize:88,fontWeight:700}}>指令概览</div>
      <div style={{fontFamily:'Consolas,monospace',fontSize:45,color:aqua}}>/areahint</div>
    </div>
    {groups.map(([label,first,second],index)=>{
      const row = Math.floor(index/2);
      const reveal = tween(frame,row*3,17+row*3);
      return <div key={label} style={{...box(108+(index%2)*882,255+row*177,810,143),
        opacity:reveal,translate:`${(1-reveal)*24}px 0`,borderTop:'2px solid #617c79',paddingTop:16}}>
        <div style={{fontSize:42,fontWeight:700,color:paper}}>{label}</div>
        <div style={{display:'flex',gap:34,marginTop:6,fontSize:34,fontFamily:'Consolas,monospace',color:aqua}}>
          <span>{first}</span><span style={{color:'#87a6a8'}}>·</span><span>{second}</span>
        </div>
      </div>;
    })}
    <div style={{...box(108,972,1650,52),fontSize:30,color:'#becdca'}}>输入 <span style={{fontFamily:'Consolas,monospace',color:gold}}>/areahint help</span> 查看完整指令。</div>
  </Board>;
}
