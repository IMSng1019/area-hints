import React from 'react';
import {AbsoluteFill, useCurrentFrame} from 'remotion';
import {Canvas, Clip, Pic, Side, Text, Window, ease, gold, green, ink, pos} from './shared';

export function Team() {
  return <Canvas>
    <Window>
      <Clip seconds={3} file="panel"/>
      <Clip from={3} seconds={9} file="sign"/>
    </Window>
    <Side kicker="域名签名" lines={'谁能一起维护\n这片地方？'}/>
  </Canvas>;
}

export function Approval() {
  const frame = useCurrentFrame();
  return <Canvas>
    <Window><Pic file="sign-list.jpg"/></Window>
    <div style={{...pos(1436,175,75,7),background:green}}/>
    <Text x={1436} y={215} w={435} size={34} color={green}>多人服 · 默认设置</Text>
    <Text x={1436} y={296} w={440} size={41}>玩家提交 → OP 审批</Text>
    <Text x={1436} y={393} w={440} size={41} style={{opacity:ease(frame,55,80)}}>OP 修改 → 直接生效</Text>
    <div style={{opacity:ease(frame,94,119)}}>
      <div style={{...pos(1436,548,390,2),background:'#b2b8a3'}}/>
      <Text x={1436} y={592} w={430} size={30} color={green}>{'BlueMap · 网页地图标记\nLuckPerms · 权限节点'}</Text>
    </div>
  </Canvas>;
}

export function End() {
  const frame = useCurrentFrame();
  const fade = ease(frame,15,85);
  return <Canvas theme={ink}>
    <Pic file="tree.jpg"/>
    <AbsoluteFill style={{background:'linear-gradient(90deg,rgba(8,25,24,.92),rgba(8,25,24,.68) 27%,transparent 63%)',opacity:fade}}/>
    <div style={{position:'absolute',inset:0,opacity:fade,translate:`0 ${18*(1-fade)}px`}}>
      <div style={{...pos(115,486,83,6),background:gold}}/>
      <Text x={110} y={530} w={760} size={99} weight={750} color="#fff8e6">Areas Hint</Text>
      <Text x={116} y={668} w={680} size={57} color="#fff8e6">域名模组</Text>
      <Text x={118} y={795} w={680} size={34} color="#ffdd9d">IMSng　·　下载见简介</Text>
    </div>
  </Canvas>;
}
