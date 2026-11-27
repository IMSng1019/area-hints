import React from 'react';
import {AbsoluteFill} from 'remotion';
import {Clip,Picture,aqua,gold,ink,latin,paper,sans,serif} from './visual';

const titleShadow='0 10px 0 rgba(4,18,23,.55), 0 22px 50px rgba(0,0,0,.42)';

function Brand(){
  return <div style={{display:'flex',alignItems:'center',gap:18}}>
    <div style={{width:70,height:70,borderRadius:18,background:paper,display:'grid',placeItems:'center',boxShadow:'0 14px 34px rgba(0,0,0,.3)'}}>
      <Picture file="art/icon.png" style={{width:62,height:62,objectFit:'contain'}}/>
    </div>
    <div>
      <div style={{color:gold,fontFamily:latin,fontSize:18,fontWeight:700,letterSpacing:4}}>MINECRAFT · FABRIC</div>
      <div style={{fontFamily:serif,fontSize:48,fontWeight:700,lineHeight:1.08,color:paper}}>Area Hints</div>
    </div>
  </div>;
}

function Pill({children,color=paper,background='rgba(4,18,23,.78)'}:{children:React.ReactNode;color?:string;background?:string}){
  return <div style={{padding:'13px 21px',borderRadius:999,background,color,fontSize:29,fontWeight:800,whiteSpace:'nowrap'}}>{children}</div>;
}

function ActionBoundary(){
  const points='212,192 780,104 878,476 554,642 104,518';
  const nodes=[[212,192],[780,104],[878,476],[554,642],[104,518]];
  return <div style={{position:'absolute',right:34,top:166,width:950,height:720}}>
    <svg viewBox="0 0 960 720" width="950" height="720" style={{overflow:'visible',filter:'drop-shadow(0 0 26px rgba(235,188,120,.42))'}}>
      <polygon points={points} fill={gold} fillOpacity=".09" stroke={gold} strokeWidth="6"/>
      <polygon points="253,215 741,141 827,455 553,605 149,498" fill="none" stroke={aqua} strokeOpacity=".42" strokeWidth="3" strokeDasharray="13 14"/>
      {nodes.map(([x,y],i)=><g key={i}>
        <circle cx={x} cy={y} r="20" fill={ink} stroke={paper} strokeWidth="5"/>
        <circle cx={x} cy={y} r="8" fill={i%2?aqua:gold}/>
      </g>)}
      <g transform="translate(640 40)">
        <rect width="236" height="58" rx="10" fill={ink} fillOpacity=".88" stroke={gold} strokeWidth="2"/>
        <text x="118" y="39" textAnchor="middle" fill={paper} fontFamily={sans} fontSize="25" fontWeight="700">当前区域 · LV.2</text>
      </g>
    </svg>
  </div>;
}

export function CoverAction(){
  return <AbsoluteFill style={{background:ink,color:paper,fontFamily:sans,overflow:'hidden'}}>
    <Clip name="pv-follow" offset={60} style={{filter:'saturate(1.2) contrast(1.07) brightness(.9)',objectPosition:'50% 58%'}}/>
    <AbsoluteFill style={{background:'linear-gradient(90deg,rgba(4,17,22,.97) 0%,rgba(4,17,22,.82) 38%,rgba(4,17,22,.12) 72%,rgba(4,17,22,.24) 100%)'}}/>
    <AbsoluteFill style={{background:'linear-gradient(0deg,rgba(4,17,22,.75) 0%,transparent 38%)'}}/>
    <div style={{position:'absolute',left:86,top:66,zIndex:5}}><Brand/></div>
    <div style={{position:'absolute',right:84,top:72,padding:'17px 28px',borderRadius:16,background:'#e75936',color:'#fff6df',fontFamily:latin,fontSize:28,fontWeight:800,letterSpacing:1.5,rotate:'3deg',boxShadow:'0 14px 34px rgba(0,0,0,.3)'}}>
      FABRIC 1.20.4
    </div>
    <div style={{position:'absolute',left:92,top:232,width:900,zIndex:5,textShadow:titleShadow}}>
      <div style={{fontFamily:serif,fontSize:164,fontWeight:700,lineHeight:.96,letterSpacing:-7}}>服务器区域</div>
      <div style={{fontFamily:serif,fontSize:178,fontWeight:700,lineHeight:1.02,letterSpacing:-8,color:gold}}>终于好用了</div>
      <div style={{marginTop:26,paddingLeft:22,borderLeft:`9px solid ${gold}`,fontSize:42,fontWeight:800,lineHeight:1.45}}>
        可视化画边界 · 全服实时同步
      </div>
    </div>
    <div style={{position:'absolute',left:92,bottom:76,display:'flex',gap:14,zIndex:5}}>
      <Pill background="#e75936" color="#fff8e8">服务器必装</Pill>
      <Pill>多级域名</Pill>
      <Pill>地图联动</Pill>
    </div>
    <ActionBoundary/>
  </AbsoluteFill>;
}

export function CoverMap(){
  return <AbsoluteFill style={{background:ink,color:paper,fontFamily:sans,overflow:'hidden'}}>
    <Picture file="art/xaero.jpg" style={{filter:'saturate(1.16) contrast(1.08) brightness(.82)',scale:1.035}}/>
    <AbsoluteFill style={{background:'linear-gradient(90deg,rgba(4,16,21,.98) 0%,rgba(4,16,21,.9) 34%,rgba(4,16,21,.25) 75%,rgba(4,16,21,.46) 100%)'}}/>
    <AbsoluteFill style={{background:'linear-gradient(180deg,rgba(4,16,21,.48),transparent 30%,rgba(4,16,21,.78))'}}/>
    <div style={{position:'absolute',left:88,top:68,zIndex:5}}><Brand/></div>
    <div style={{position:'absolute',left:94,top:236,width:940,zIndex:5,textShadow:titleShadow}}>
      <div style={{fontFamily:serif,fontSize:180,fontWeight:700,lineHeight:.96,letterSpacing:-8}}>多级域名</div>
      <div style={{fontFamily:serif,fontSize:184,fontWeight:700,lineHeight:1.02,letterSpacing:-8,color:gold}}>一图全看清</div>
      <div style={{marginTop:30,fontSize:40,fontWeight:800,lineHeight:1.5,color:'#e7eee9'}}>
        一级、二级、三级区域，全部叠在地图上
      </div>
    </div>
    <div style={{position:'absolute',right:76,top:164,width:430,zIndex:5,display:'grid',gap:16}}>
      {[
        ['01','提瓦特大陆','#ed6254'],
        ['02','璃月','#efb85d'],
        ['03','璃月港','#89d47c'],
      ].map(([level,name,color])=><div key={level} style={{display:'flex',alignItems:'center',gap:18,padding:'19px 22px',borderRadius:18,background:'rgba(3,15,19,.82)',border:`2px solid ${color}`}}>
        <span style={{color,fontFamily:latin,fontSize:34,fontWeight:800}}>{level}</span>
        <span style={{fontFamily:serif,fontSize:38,fontWeight:700}}>{name}</span>
      </div>)}
    </div>
    <div style={{position:'absolute',left:94,bottom:76,display:'flex',gap:14,zIndex:5}}>
      <Pill background={gold} color={ink}>层级关系一眼看懂</Pill>
      <Pill>BlueMap + Xaero</Pill>
    </div>
    <div style={{position:'absolute',right:82,bottom:52,color:'#d2e1da',fontFamily:latin,fontSize:20,letterSpacing:4,zIndex:5}}>
      MAP SYNC / MULTI-LEVEL DOMAIN
    </div>
  </AbsoluteFill>;
}

export function CoverLiyue(){
  return <AbsoluteFill style={{background:'#0b3035',color:paper,fontFamily:sans,overflow:'hidden'}}>
    <Picture file={'art/cover-liyue-base.png'} style={{filter:'saturate(1.12) contrast(1.06) brightness(.94)'}}/>
    <AbsoluteFill style={{background:'linear-gradient(90deg,rgba(11,48,53,.96) 0%,rgba(11,48,53,.84) 36%,rgba(11,48,53,.18) 70%,rgba(11,48,53,.04) 100%)'}}/>
    <AbsoluteFill style={{background:'radial-gradient(ellipse at 96% 8%,rgba(205,112,52,.55) 0%,transparent 46%)'}}/>
    <AbsoluteFill style={{background:'linear-gradient(0deg,rgba(8,34,38,.82) 0%,transparent 34%)'}}/>
    <AbsoluteFill style={{background:'linear-gradient(90deg,rgba(6,28,32,.5) 0%,rgba(6,28,32,.34) 55%,transparent 80%)'}}/>
    <div style={{position:'absolute',left:120,top:58,zIndex:5,display:'flex',alignItems:'center',gap:24}}>
      <div style={{width:104,height:104,borderRadius:26,background:paper,display:'grid',placeItems:'center',boxShadow:'0 14px 34px rgba(0,0,0,.35)'}}>
        <Picture file={'art/icon.png'} style={{width:92,height:92,objectFit:'contain'}}/>
      </div>
      <div style={{fontFamily:serif,fontSize:84,fontWeight:700,color:paper,textShadow:'0 6px 24px rgba(0,0,0,.5)'}}>Area Hints</div>
    </div>
    <div style={{position:'absolute',right:120,top:60,zIndex:5,padding:'12px 38px',borderRadius:26,background:'rgba(4,18,23,.88)',border:`6px solid ${gold}`,color:gold,fontSize:78,fontWeight:900,rotate:'3deg',boxShadow:'0 14px 34px rgba(0,0,0,.4)'}}>MC 模组</div>
    <div style={{position:'absolute',left:114,top:222,width:1560,zIndex:5,textShadow:titleShadow}}>
      <div style={{fontFamily:serif,fontSize:214,fontWeight:700,lineHeight:.98,letterSpacing:-8}}>像原神一样</div>
      <div style={{fontFamily:serif,fontSize:214,fontWeight:700,lineHeight:1.04,letterSpacing:-8,color:gold}}>给 MC 跳域名</div>
      <div style={{display:'inline-block',marginTop:34,padding:'10px 36px',borderRadius:26,background:'rgba(4,18,23,.74)',fontSize:92,fontWeight:800,lineHeight:1.3,textShadow:'none'}}>可视化画边界 · 全服同步</div>
    </div>
  </AbsoluteFill>;
}

export function CoverEditor(){
  return <AbsoluteFill style={{background:ink,color:paper,fontFamily:sans,overflow:'hidden'}}>
    <Clip name="create" offset={150} style={{filter:'saturate(1.05) contrast(1.06) brightness(.88)'}}/>
    <AbsoluteFill style={{background:'linear-gradient(90deg,rgba(3,16,21,.98) 0%,rgba(3,16,21,.9) 39%,rgba(3,16,21,.25) 76%,rgba(3,16,21,.34) 100%)'}}/>
    <div style={{position:'absolute',left:86,top:66,zIndex:5}}><Brand/></div>
    <div style={{position:'absolute',left:92,top:238,width:900,zIndex:5,textShadow:titleShadow}}>
      <div style={{fontFamily:serif,fontSize:178,fontWeight:700,lineHeight:.96,letterSpacing:-8}}>边界编辑</div>
      <div style={{fontFamily:serif,fontSize:172,fontWeight:700,lineHeight:1.04,letterSpacing:-8,color:gold}}>像画画一样简单</div>
      <div style={{marginTop:30,fontSize:40,fontWeight:800,lineHeight:1.5,color:'#e7eee9'}}>
        拖一笔改范围，保存后客户端立即更新
      </div>
    </div>
    <div style={{position:'absolute',left:600,top:300,width:760,height:510,border:'9px dashed '+gold,borderRadius:'48% 52% 46% 54%',rotate:'-7deg',filter:'drop-shadow(0 0 24px rgba(235,188,120,.58))',zIndex:4}}/>
    <div style={{position:'absolute',left:92,bottom:72,display:'flex',alignItems:'center',gap:12,zIndex:5}}>
      <Pill background="#e75936" color="#fff8e8">1 画边界</Pill>
      <span style={{color:gold,fontSize:32,fontWeight:900}}>→</span>
      <Pill>2 填名称</Pill>
      <span style={{color:gold,fontSize:32,fontWeight:900}}>→</span>
      <Pill>3 提交同步</Pill>
    </div>
    <div style={{position:'absolute',right:82,bottom:50,color:'#d2e1da',fontFamily:latin,fontSize:20,letterSpacing:4,zIndex:5}}>
      VISUAL DOMAIN EDITOR
    </div>
  </AbsoluteFill>;
}
