import React from 'react';
import {AbsoluteFill,Audio,Composition,Series,registerRoot,staticFile} from 'remotion';
import {Arrival,Overview} from './Opening';
import {Brand} from './Brand';
import {Details,Feature,Maps,Titles} from './Features';
import {Mechanism,mechanismFrames} from './Mechanism';
import {EditingTools} from './EditingTools';
import {Performance,performanceFrames} from './Performance';
import {Credits,Logo,Uses} from './Closing';
import {ink} from './visual';

const fps=30;
const editingToolsFrames=15*fps;
const scenes=[
  ['璃月港',11*fps,<Arrival/>],
  ['模组揭晓',5*fps,<Brand/>],
  ['九格实拍',7*fps,<Overview/>],
  ['域名跳字',9*fps,<Titles/>],
  ['可视化创建',14*fps,<Feature kind="create"/>],
  ['四种边界编辑',editingToolsFrames,<EditingTools/>],
  ['实地核对边界',8*fps,<Feature kind="bounds"/>],
  ['服务器与客户端',mechanismFrames,<Mechanism/>],
  ['性能实测',performanceFrames,<Performance/>],
  ['风起地的故事',7*fps,<Feature kind="story"/>],
  ['地图联动',8*fps,<Maps/>],
  ['指令概览',5*fps,<Details/>],
  ['应用场景',11*fps,<Uses/>],
  ['制作名单',5*fps,<Credits/>],
  ['标志',5*fps,<Logo/>],
] as const;
function Film(){
  return <AbsoluteFill style={{backgroundColor:ink}}><Series>{scenes.map(([name,duration,content])=><Series.Sequence key={name} name={name} durationInFrames={duration}>{content}</Series.Sequence>)}</Series><Audio src={staticFile('score.m4a')}/></AbsoluteFill>;
}
const dimensions={width:1920,height:1080,fps};
function Root(){
  return <>
    <Composition id="Area-Hints" {...dimensions} component={Film} durationInFrames={scenes.reduce((sum,s)=>sum+s[1],0)}/>
    <Composition id="Editing-Tools" {...dimensions} component={EditingTools} durationInFrames={editingToolsFrames}/>
    <Composition id="Mechanism" {...dimensions} component={Mechanism} durationInFrames={mechanismFrames}/>
    <Composition id="Performance" {...dimensions} component={Performance} durationInFrames={performanceFrames}/>
  </>;
}
registerRoot(Root);
