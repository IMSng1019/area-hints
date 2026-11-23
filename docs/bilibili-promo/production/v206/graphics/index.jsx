import React from 'react';
import {registerRoot,Composition} from 'remotion';
import {Intro,Book,Box,Ray} from './principles';
import {Atlas,Search,Workload,MapIn,MapPort} from './atlas';
import {Team,Approval,End} from './ending';
import {City,Levels,Naming,Recording,Corners} from './tutorial';
const shots=[[2,14,Intro],[4,5,Book],[5,12,City],[6,10,Levels],[7,8,Naming],[8,11,Recording],[9,10,Corners],[10,9,Box],[11,9,Ray],[13,10,Atlas],[14,14,Search],[15,8,Workload],[16,11,MapIn],[17,7,MapPort],[18,12,Team],[19,8,Approval],[20,8,End]];
registerRoot(()=>shots.map(([n,secs,component])=><Composition key={n} id={'g'+String(n).padStart(2,'0')} component={component} width={1920} height={1080} fps={60} durationInFrames={secs*60}/>));
