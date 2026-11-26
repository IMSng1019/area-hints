import React from 'react';
import {AbsoluteFill,Sequence,useCurrentFrame} from 'remotion';
import titles from './titles.json';
import {Board,Clip,Label,Picture,Rise,aqua,box,gold,ink,latin,paper,sans,serif,tween} from './visual';

export function Titles() {
  const f=useCurrentFrame();
  const parentColor = '#efb74e';
  const childColor = '#a6e998';
  const names = ['入城实拍', '一级域名', <><span style={{color:parentColor}}>父域名</span> · <span style={{color:childColor}}>二级域名</span></>, '三级域名'];
  return <Board>
    <div style={box(104,89,1700,190)}><Label>跨过边界</Label><Rise style={{fontFamily:serif,fontSize:83,marginTop:12}}>让地名跳出来。</Rise></div>
    {titles.map((item,i)=>{
      const x=104+(i%2)*865,y=320+Math.floor(i/2)*307;
      return <div key={item.file} style={{...box(x,y,820,271),background:'#122630',opacity:tween(f,0,12)}}>
        <div style={{...box(0,0,820,2),background:'#687775'}}/>
        <div style={{...box(20,7,790,40),color:i===1?parentColor:'#d3dfda',fontSize:25}}>{names[i]}</div>
        <Picture file={item.file} style={box(0,48,820,220)}/>
        {/* 在主副标题间的空隙标出相同的父名，沿用原图里的金色。 */}
        {(i===1||i===2)&&<div style={{
          ...box(i===1?247:76,166,304,3),background:parentColor,boxShadow:'0 1px 2px #1e211e',
          transformOrigin:'left',scale:`${tween(f,i===1?30:62,i===1?54:86)} 1`,
        }}/>}
      </div>;
    })}
    <div style={{...box(106,956,1700,55),fontSize:33,color:'#d0ded9'}}>走进二级区域，<span style={{color:parentColor}}>父域名</span>与<span style={{color:childColor}}>当前地名</span>并排出现，下方显示这个地点的副标题。</div>
  </Board>;
}

const features={
  create:{clip:'create',kicker:'域名编辑',head:['给区域','划一条边界。'],body:['打开可视化编辑器，','用画笔涂出范围，','再填写域名属性。']},
  bounds:{clip:'bounds',kicker:'边界可视化',head:['回到世界，','看清范围。'],body:['打开边界可视化，','在实景中核对顶点','和围出的范围。']},
};
export function Feature({kind}:{kind:keyof typeof features}) {
  const f=useCurrentFrame(),data=features[kind];
  // 输入框从第 220 帧出现，镜头回撤时仍可看到指针移向下一步。
  const nameFocus = tween(f,220,245)*(1-tween(f,292,313));
  // 按录像里属性页和提交预览真实出现的帧切换提示。
  const steps=['打开编辑器','涂出范围','填写名称','提交前核对'];
  const active=f<54?0:f<194?1:f<377?2:3;
  return <Board>
    <div style={{...box(0,105,1280,851),overflow:'hidden',background:'#081319',clipPath:`inset(0 ${(1-tween(f,0,14))*100}% 0 0)`}}>
      {kind==='bounds'?<>
        <Sequence durationInFrames={130} name="打开开关"><Clip name="bounds" style={{objectFit:'contain'}}/></Sequence>
        <Sequence from={114} name="围绕实地边界"><Clip name="aerial" offset={24} style={{objectFit:'contain',opacity:tween(f,114,130)}}/></Sequence>
      </>:<div style={{...box(0,66,1280,720),overflow:'hidden'}}>
        <Clip name={data.clip} style={{transformOrigin:'30% 16%',scale:1+0.34*nameFocus}}/>
      </div>}
    </div>
    <div style={{...box(1290,105,2,851),background:'linear-gradient(transparent,#819d99 20%,#819d99 80%,transparent)'}}/>
    <div style={box(1360,192,515,795)}><Label>{data.kicker}</Label>
      <Rise delay={4} style={{fontFamily:serif,fontSize:69,lineHeight:1.23,marginTop:50,letterSpacing:-2}}>{data.head.map(t=><div key={t}>{t}</div>)}</Rise>
      <Rise delay={29} duration={24} style={{fontSize:34,lineHeight:1.65,marginTop:38,color:'#cedcd7'}}>{data.body.map(t=><div key={t}>{t}</div>)}</Rise>
      {kind==='create'&&<div style={{marginTop:48}}>{steps.map((s,i)=><div key={s} style={{display:'flex',alignItems:'center',gap:16,marginTop:10,fontSize:28,color:i===active?gold:'#839b9a'}}><div style={{width:i===active?55:17,height:2,background:'currentColor'}}/>{s}</div>)}</div>}
    </div>
  </Board>;
}

// 以雪山和海岸线上的对应点配准两张原图，共用一段从风起地移向龙脊雪山的运镜。
const mapViews = [
  {name:'BlueMap',image:'bluemap',height:1103,ratio:1,offsetX:0,offsetY:0},
  {name:'Xaero',image:'xaero',height:1484,ratio:1.263,offsetX:-67,offsetY:-64},
];
export function Maps(){
  const frame=useCurrentFrame();
  const pan=tween(frame,32,178);
  const centerX=1390-135*pan, centerY=444+210*pan;
  const windowWidth=852, windowHeight=580;
  return <AbsoluteFill style={{backgroundColor:'#e7e2d6',color:ink,fontFamily:sans}}>
    <div style={box(96,75,1650,155)}>
      <Label color="#4b675f">地图联动</Label>
      <div style={{fontFamily:serif,fontSize:76,marginTop:10,fontWeight:700}}>同一组域名，两张地图。</div>
    </div>
    <div style={{...box(1320,153,504,64),textAlign:'right',fontSize:31,color:'#405751'}}>风起地　→　龙脊雪山</div>
    {mapViews.map((map,i)=>{
      const scale=windowWidth/((930-65*pan)*map.ratio);
      const imageX=windowWidth/2-(centerX*map.ratio+map.offsetX)*scale;
      const imageY=windowHeight/2-(centerY*map.ratio+map.offsetY)*scale;
      return <div key={map.name} style={box(96+876*i,275,windowWidth,723)}>
        <div style={{position:'relative',height:windowHeight,overflow:'hidden',backgroundColor:'#24424a'}}>
          <Picture file={'art/'+map.image+'.jpg'} style={box(imageX,imageY,2304*scale,map.height*scale)}/>
        </div>
        <div style={{marginTop:19,paddingTop:9,borderTop:'1px solid #909f93',fontSize:44,fontFamily:latin,fontWeight:600}}>{map.name}</div>
      </div>;
    })}
  </AbsoluteFill>;
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
