import React from 'react';
import {AbsoluteFill, Freeze, Sequence, useCurrentFrame} from 'remotion';
import {Clip, box, gold, ink, paper, sans, serif, tween} from './visual';

export function Story() {
  const frame = useCurrentFrame();
  const bookOpens = 94;
  const location = tween(frame, 4, 23) * (1 - tween(frame, 77, bookOpens));
  const excerpt = tween(frame, bookOpens + 5, bookOpens + 24);

  return <AbsoluteFill style={{backgroundColor: ink, color: paper, fontFamily: sans, overflow: 'hidden'}}>
    {/* 原实录第 48 帧为黑场，延续前一帧的树冠，再接回同一地点的实景。 */}
    <Sequence durationInFrames={bookOpens}>
      <Freeze frame={47} active={frame === 48}><Clip name="story"/></Freeze>
    </Sequence>
    {frame < bookOpens && <>
      <AbsoluteFill style={{background: 'linear-gradient(18deg, rgba(7,20,24,.82), transparent 43%)'}}/>
      <div style={{...box(106,755,650,210), opacity: location}}>
        <div style={{fontSize: 29, letterSpacing: 4, color: '#f2d6a7'}}>域名描述</div>
        <div style={{fontFamily: serif, fontWeight: 700, fontSize: 112, marginTop: 7}}>风起地</div>
      </div>
    </>}

    <Sequence from={bookOpens}>
      <Clip name="story" offset={bookOpens} style={{
        ...box(238,0,1920,1080),
        maskImage: 'linear-gradient(to right, transparent, black 28%)',
      }}/>
      {/* 引文逐字摘自这一帧的书页，整页和完成按钮都保留在原始像素尺寸。 */}
      <div style={{...box(108,355,735,340), opacity: excerpt}}>
        <div style={{color: gold, fontSize: 28, letterSpacing: 2}}>风起地 · 手记节选</div>
        <div style={{fontFamily: serif, fontSize: 63, lineHeight: 1.55, marginTop: 32}}>
          <div>“据说这里是温妮莎</div>
          <div>登天的地方”</div>
        </div>
      </div>
    </Sequence>
  </AbsoluteFill>;
}
