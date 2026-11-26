import React from 'react';
import {Img, interpolate, staticFile} from 'remotion';

// 对齐模组的 500ms 原地渐显和 300ms 上浮退场，中间停留缩短为 47 帧。
export function HarborPopup({frame}: {frame: number}) {
  if (frame < 75 || frame >= 146) return null;
  const clamp = {extrapolateLeft:'clamp', extrapolateRight:'clamp'} as const;
  const enter = interpolate(frame + .5,[75,90],[0,1],clamp);
  const leave = interpolate(frame + .5,[137,146],[0,1],clamp) ** 3;
  const opacity = (1 - (1 - enter) ** 3) * (1 - leave);
  return <Img src={staticFile('titles/harbor-popup.png')} alt="璃月港" style={{
    display:'block', width:'100%', height:'auto', imageRendering:'pixelated',
    opacity, transform:`translateY(${-66 * leave}%)`,
  }}/>;
}
