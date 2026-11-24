import React from 'react';
import {AbsoluteFill,Audio,Composition,Series,registerRoot,staticFile} from 'remotion';
import {Arrival,Brand,Overview} from './Opening';
import {Details,Feature,Maps,Titles} from './Features';
import {Mechanism,mechanismFrames} from './Mechanism';
import {Credits,Logo,Uses} from './Closing';
import {ink} from './visual';

const scenes=[
  ['璃月港',405,<Arrival/>],['模组揭晓',105,<Brand/>],['九格实拍',135,<Overview/>],
  ['域名跳字',195,<Titles/>],['可视化创建',255,<Feature kind="create"/>],['实地核对边界',150,<Feature kind="bounds"/>],
  ['服务器与客户端',mechanismFrames,<Mechanism/>],['风起地的故事',105,<Feature kind="story"/>],
  ['地图联动',150,<Maps/>],['指令',60,<Details/>],['应用场景',210,<Uses/>],['制作名单',105,<Credits/>],['标志',105,<Logo/>],
] as const;
function Film(){
  return <AbsoluteFill style={{backgroundColor:ink}}><Series>{scenes.map(([name,duration,content])=><Series.Sequence key={name} name={name} durationInFrames={duration}>{content}</Series.Sequence>)}</Series><Audio src={staticFile('score.m4a')}/></AbsoluteFill>;
}
const dimensions={width:1920,height:1080,fps:30};
function Root(){
  return <><Composition id="Area-Hints" {...dimensions} component={Film} durationInFrames={scenes.reduce((sum,s)=>sum+s[1],0)}/><Composition id="Mechanism" {...dimensions} component={Mechanism} durationInFrames={mechanismFrames}/></>;
}
registerRoot(Root);
