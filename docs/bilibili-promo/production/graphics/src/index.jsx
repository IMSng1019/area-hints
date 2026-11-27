import React from "react";
import {
  AbsoluteFill, Composition, Easing, Img, interpolate,
  OffthreadVideo, Sequence, registerRoot, staticFile, useCurrentFrame,
} from "remotion";

const paper = "#f3efe2";
const ink = "#1c3d35";
const green = "#416c57";
const ochre = "#ad552f";
const serif = 'Georgia, "Noto Serif SC", SimSun, serif';
const sans = '"Microsoft YaHei", "Noto Sans SC", sans-serif';
const latin = 'Bahnschrift, Arial, sans-serif';
const frameStyle = {backgroundColor: paper, color: ink, fontFamily: sans, overflow: "hidden"};

function progress(frame, start, end) {
  return interpolate(frame, [start, end], [0, 1], {
    extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.inOut(Easing.cubic),
  });
}

function Enter({start = 0, duration = 38, children, style}) {
  const amount = progress(useCurrentFrame(), start, start + duration);
  return <div style={{...style, opacity: amount, transform: "translateY(" + (1 - amount) * 27 + "px)"}}>{children}</div>;
}

function Line({x, y, width, color = ink, delay = 0, height = 2}) {
  const amount = progress(useCurrentFrame(), delay, delay + 52);
  return <div style={{position: "absolute", left: x, top: y, width: width * amount, height, background: color}}/>;
}

function Kicker({children, start = 0, light = false}) {
  return <Enter start={start} style={{fontFamily: latin, color: light ? "#e6d8ad" : ochre, fontSize: 23, fontWeight: 650, letterSpacing: 5}}>{children}</Enter>;
}

// 共享同一组手绘感等高线，让书页、边界图和片尾保有一致的地形语言。
function contour(ring) {
  const points = [];
  for (let index = 0; index < 145; index++) {
    const angle = index / 144 * Math.PI * 2;
    const warp = 1 + .11 * Math.sin(3 * angle + ring * .032)
        + .08 * Math.cos(5 * angle - .8) + .04 * Math.sin(8 * angle + .2);
    const distance = (25 + ring * 15) * warp;
    const x = Math.cos(angle) * distance * 1.28 + 2 * ring;
    const y = Math.sin(angle) * distance * .86 - ring * 1.05;
    points.push((index ? "L" : "M") + x.toFixed(1) + "," + y.toFixed(1));
  }
  return points.join(" ") + " Z";
}

const rings = Array.from({length: 34}, (_, i) => contour(i));
function Relief({x = 1350, y = 540, opacity = .2, scale = 1}) {
  return <g transform={`translate(${x} ${y}) scale(${scale}) rotate(-16)`} fill="none" stroke={green} opacity={opacity}>
    {rings.map((d, i) => <path key={i} d={d} strokeWidth={i % 5 === 0 ? 2.8 : 1.2}/>)}
  </g>;
}

function Cross({x, y, size = 10}) {
  return <path d={`M ${x-size} ${y} h ${2*size} M ${x} ${y-size} v ${2*size}`} fill="none" stroke={green} strokeWidth="2"/>;
}

function Plate({children}) {
  return <svg viewBox="0 0 1920 1080" width="1920" height="1080" style={{position:"absolute", inset:0}}>
    <defs>
      <pattern id="survey-dots" width="44" height="44" patternUnits="userSpaceOnUse">
        <circle cx="2" cy="2" r="1.25" fill={green} opacity=".25"/>
      </pattern>
    </defs>
    {children}
  </svg>;
}

function Brand() {
  const frame = useCurrentFrame();
  const wipe = progress(frame, 4, 45);
  const trace = progress(frame, 45, 146);
  return <AbsoluteFill style={frameStyle}>
    <Img src={staticFile("book.jpg")} style={{width:"100%", height:"100%", transform:`scale(${1 + wipe * .12})`}}/>
    <AbsoluteFill style={{backgroundColor:paper, clipPath:`inset(${(1-wipe)*30}% ${(1-wipe)*35}% round ${(1-wipe)*8}px)`, opacity: Math.min(1, wipe * 4)}}>
      <Plate>
        <rect x="950" width="970" height="1080" fill="url(#survey-dots)"/>
        <Relief x={1420} y={555} opacity={.43}/>
        <path d="M1090 880 L1160 741 L1335 711 L1388 574 L1489 492 L1575 326 L1684 267"
          stroke={ochre} strokeLinejoin="round" strokeWidth="5" pathLength="1" strokeDasharray="1" strokeDashoffset={1-trace} fill="none"/>
        <g opacity={trace}><circle cx="1684" cy="267" r="13" fill={paper} stroke={ochre} strokeWidth="4"/><circle cx="1684" cy="267" r="4" fill={ochre}/></g>
        <Cross x={990} y={130}/><Cross x={1790} y={930}/>
      </Plate>
      <div style={{position:"absolute",left:128,top:229}}>
        <Kicker start={36}>MINECRAFT  /  FABRIC</Kicker>
        <Enter start={35} style={{fontFamily:serif,fontSize:170,letterSpacing:-10,lineHeight:1.22,marginTop:43}}>Areas Hint</Enter>
        <Enter start={46} style={{fontFamily:serif,fontSize:73,fontWeight:650,letterSpacing:5,marginTop:9}}>域名模组</Enter>
      </div>
      <Line x={134} y={695} width={97} delay={67} color={ochre} height={5}/>
      <Enter start={65} style={{position:"absolute",left:132,top:736,fontSize:36,letterSpacing:1}}>给 Minecraft 地图里的地方起名字</Enter>
    </AbsoluteFill>
  </AbsoluteFill>;
}

// X、Z 平面上的同一组角点同时用作平面边界与立体高度范围的基底。
const area = [[-295,-77],[-118,-211],[174,-159],[318,47],[102,201],[-206,153]];
function Boundary() {
  const frame = useCurrentFrame();
  const trace = progress(frame, 17, 169);
  const turn = progress(frame, 242, 341);
  const height = progress(frame, 310, 435) * 190;
  const base = area.map(([x,z]) => [1252+x+z*turn*.24, 540+z*(1-turn*.45)+x*turn*.10+turn*100]);
  const roof = base.map(([x,y]) => [x,y-height]);
  const coords = (points) => points.map(p => p.join(",")).join(" ");
  const sideLabel = progress(frame,351,408);
  const titleStyle = {fontFamily:serif,fontSize:88,lineHeight:1.25,fontWeight:650,letterSpacing:1};
  return <AbsoluteFill style={frameStyle}>
    <Plate>
      <rect x="772" y="95" width="1020" height="801" fill="url(#survey-dots)"/>
      <Relief x={1263} y={549} opacity={.10} scale={.83}/>
      <Cross x={772} y={95}/><Cross x={1792} y={95}/><Cross x={772} y={896}/><Cross x={1792} y={896}/>
      <polygon points={coords(base)} fill={green} fillOpacity={progress(frame,124,171)*.15}/>
      {base.map((p,i) => {
        const next = (i + 1) % base.length;
        return <polygon key={i} points={coords([p,base[next],roof[next],roof[i]])}
          fill={green} fillOpacity={turn*(i%2?.13:.21)} stroke={green} strokeOpacity={turn*.65} strokeWidth="2"/>;
      })}
      <polygon points={coords(base)} fill="none" stroke={ink} strokeWidth="6" strokeLinejoin="round"
        pathLength="1" strokeDasharray="1" strokeDashoffset={1-trace}/>
      <polygon points={coords(roof)} fill={green} fillOpacity={turn*.17} stroke={green} strokeOpacity={turn} strokeWidth="4"/>
      {base.map(([x,y],i) => <circle key={i} cx={x} cy={y} r="8" fill={paper} stroke={ochre} strokeWidth="4" opacity={progress(frame,8+i*20,20+i*20)}/>)}
      <g opacity={sideLabel} stroke={ochre} strokeWidth="3">
        <path d={`M1645 ${base[3][1]-height} v${height} m-10 0 h20 M1635 ${base[3][1]-height} h20`} fill="none"/>
      </g>
    </Plate>
    <div style={{position:"absolute",left:120,top:248}}>
      <Kicker>REGION GEOMETRY</Kicker>
      <Enter start={12} style={{...titleStyle,marginTop:60}}>边界示意</Enter>
      <div style={{position:"relative",marginTop:45,width:580,height:105}}>
        <div style={{position:"absolute",fontSize:40,opacity:progress(frame,30,65)*(1-progress(frame,233,279))}}>连接顶点，围成域名范围。</div>
        <div style={{position:"absolute",fontSize:40,opacity:progress(frame,282,337)}}>限定高度，区分上下空间。</div>
      </div>
    </div>
    <Line x={122} y={667} width={75} delay={22} color={ochre} height={4}/>
    <div style={{position:"absolute",left:122,top:704,fontFamily:latin,color:green,fontSize:25,letterSpacing:2}}>
      <span style={{opacity:1-turn}}>X  /  Z</span><span style={{position:"absolute",left:0,whiteSpace:"nowrap",opacity:turn}}>X  /  Y  /  Z</span>
    </div>
  </AbsoluteFill>;
}

function Approval() {
  const frame = useCurrentFrame();
  const imageSize = 1 + progress(frame,0,500)*.055;
  return <AbsoluteFill style={frameStyle}>
    <div style={{position:"absolute",inset:"0 47% 0 0",background:"#17342b",overflow:"hidden"}}>
      <Img src={staticFile("panel.jpg")} style={{position:"absolute",width:"100%",height:"100%",objectFit:"cover",opacity:.34}}/>
      <div style={{position:"absolute",inset:0,background:"linear-gradient(0deg,#122b23 3%,transparent 70%)"}}/>
      <div style={{position:"absolute",left:60,right:60,top:230,height:501,overflow:"hidden",border:"1px solid #a19c80"}}>
        <Img src={staticFile("panel.jpg")} style={{width:"100%",height:"100%",objectFit:"contain",background:"#102219",transform:`scale(${imageSize})`}}/>
      </div>
      <div style={{position:"absolute",left:62,top:772,color:paper,fontSize:34,letterSpacing:2}}>域名选择 <span style={{fontSize:25,color:"#d6d1ae",marginLeft:20}}>游戏实机</span></div>
    </div>
    <div style={{position:"absolute",left:1104,top:191}}>
      <Kicker>SERVER SETTINGS</Kicker>
      <Enter start={6} style={{fontFamily:serif,fontSize:66,fontWeight:650,marginTop:39}}>默认修改审批</Enter>
      <Enter start={37} style={{fontSize:29,color:green,marginTop:75}}>普通玩家</Enter>
      <Enter start={48} style={{fontSize:46,fontWeight:650,marginTop:13}}>提交修改 <span style={{color:ochre}}>→</span> OP 审批生效</Enter>
      <Enter start={186} style={{fontSize:29,color:green,marginTop:99}}>服务器 OP</Enter>
      <Enter start={198} style={{fontSize:46,fontWeight:650,marginTop:13}}>修改域名 <span style={{color:ochre}}>→</span> 直接生效</Enter>
    </div>
    <Line x={1105} y={580} width={632} delay={85} color="#b3b9a5" height={1}/>
  </AbsoluteFill>;
}

function End() {
  const frame = useCurrentFrame();
  return <AbsoluteFill style={{...frameStyle,backgroundColor:"#162a23",color:paper}}>
    <Img src={staticFile("end.jpg")} style={{width:"100%",height:"100%",objectFit:"cover",transform:`scale(${1 + progress(frame,0,330)*.034})`}}/>
    <AbsoluteFill style={{background:"linear-gradient(90deg,rgba(8,28,24,.94),rgba(8,28,24,.83) 28%,rgba(8,28,24,.30) 60%,transparent)"}}/>
    <div style={{position:"absolute",left:114,top:193}}>
      <Kicker light start={10}>MINECRAFT  /  FABRIC</Kicker>
      <Enter start={16} style={{fontFamily:serif,fontSize:142,letterSpacing:-6,lineHeight:1.24,marginTop:26}}>Areas Hint</Enter>
      <Enter start={30} style={{fontFamily:serif,fontWeight:600,fontSize:60,letterSpacing:5}}>域名模组</Enter>
      <Enter start={76} style={{fontWeight:600,fontSize:43,marginTop:107}}>模组下载见简介 <span style={{color:"#e5c990"}}>↗</span></Enter>
      <Enter start={94} style={{fontSize:27,marginTop:25,color:"#e1e0d1"}}>模组作者　IMSng</Enter>
    </div>
    <Line x={117} y={573} width={110} delay={51} color="#e5c990" height={3}/>
  </AbsoluteFill>;
}

const parcel = [[780,480],[1070,252],[1430,286],[1610,528],[1400,770],[1000,720]];
const rayZ = 400;
// 横穿每条边时求真实交点；图形、交点计数与玩家位置共用这组数值。
const crossings = parcel.flatMap(([x1,z1], i) => {
  const [x2,z2] = parcel[(i+1)%parcel.length];
  if ((z1 > rayZ) === (z2 > rayZ)) return [];
  return [x1+(rayZ-z1)*(x2-x1)/(z2-z1)];
}).sort((a,b)=>a-b);

function Detection() {
  const frame = useCurrentFrame();
  const playerX = 809 + 350 * progress(frame,541,632);
  const rayExtent = (1710 - playerX) * progress(frame,245,325);
  const intersections = crossings.filter(x => x > playerX);
  const hit = intersections.length % 2 === 1;
  const finalShot = progress(frame,770,802);
  const isRayVisible = frame >= 240 && frame < 780;
  const title = frame < 240 ? "筛选候选域名" : frame < 548 ? "向右发出射线" : frame < 778 ? "跨过边界" : "找到最深一级";
  const summary = frame < 240 ? "检查水平外框与玩家高度" : frame < 778
      ? (hit ? "交点为奇数，在域名内" : "交点为偶数，在域名外") : "沿父子关系逐级检测";
  return <AbsoluteFill style={frameStyle}>
    <Plate>
      <rect x="722" y="135" width="1050" height="691" fill="url(#survey-dots)"/>
      <g opacity={1-finalShot}>
        <polygon points={parcel.map(pair=>pair.join(",")).join(" ")} fill="#b9c9af" fillOpacity=".70" stroke={ink} strokeWidth="4.5"/>
        <rect x="780" y="252" width="830" height="518" fill="none" stroke={ochre} strokeDasharray="11 9" strokeWidth="3" opacity={.88-progress(frame,236,270)*.43}/>
        <text x="781" y="224" fontFamily={sans} fontSize="28" fill={ochre}>XZ 外接矩形</text>
        <g opacity={isRayVisible?1:0}>
          <path d={`M${playerX} ${rayZ} h${rayExtent}`} stroke={ochre} strokeWidth="5"/>
          <path d={`M${playerX+rayExtent-16} ${rayZ-9} l16 9 -16 9`} stroke={ochre} strokeWidth="4" fill="none"/>
          {intersections.map((x,i) => <g key={x} opacity={playerX+rayExtent>=x?1:0}>
            <rect x={x-10} y={rayZ-10} width="20" height="20" transform={`rotate(45 ${x} ${rayZ})`} fill={paper} stroke={ochre} strokeWidth="4"/>
            <text x={x+10} y={rayZ+48} fontFamily={sans} fontSize="26" fill={ink}>交点 {i+1}</text>
          </g>)}
        </g>
        <circle cx={playerX} cy={rayZ} r="15" fill={ink} stroke={paper} strokeWidth="4"/>
        <text x={playerX-31} y={rayZ-34} fontFamily={sans} fontSize="28" fontWeight="bold" fill={ink}>玩家</text>
      </g>
    </Plate>
    <div style={{position:"absolute",left:104,top:234}}>
      <Kicker>REGION DETECTION</Kicker>
      <Enter start={7} style={{marginTop:49,fontFamily:serif,fontSize:66,fontWeight:650}}>{title}</Enter>
      <div style={{marginTop:33,fontSize:31,color:green}}>{summary}</div>
      <div style={{marginTop:84,width:552,minHeight:146,fontSize:38,fontWeight:600,lineHeight:1.55}}>
        {frame < 240 ? <>高度 60–90 格<br/><span style={{color:ochre}}>玩家 Y = 75</span></>
          : frame < 778 ? <><span style={{fontFamily:serif,fontSize:81}}>{intersections.length}</span> 个交点<br/>{hit?"域名内":"域名外"}</>
          : <>顶级 <span style={{color:ochre}}>→</span> 二级 <span style={{color:ochre}}>→</span> 三级</>}
      </div>
    </div>
    <Sequence from={770} durationInFrames={250}>
      <div style={{position:"absolute",left:712,top:178,width:1086,opacity:finalShot}}>
        <div style={{height:611,overflow:"hidden",border:"2px solid #657767"}}>
          <OffthreadVideo src={staticFile("levels.mp4")} trimBefore={254} muted
            style={{width:"100%",height:"100%",objectFit:"cover"}}/>
        </div>
        <div style={{fontSize:27,marginTop:14,color:green}}>游戏实机 · 多级域名提示</div>
      </div>
    </Sequence>
  </AbsoluteFill>;
}

const cards = [["Brand",Brand,5],["Boundary",Boundary,9],["Approval",Approval,10],["End",End,7],["Detection",Detection,17]];
function Film() {
  return <>{cards.map(([id,component,seconds]) => <Composition key={id} id={id} component={component}
    width={1920} height={1080} fps={60} durationInFrames={seconds*60}/>)}</>;
}
registerRoot(Film);
