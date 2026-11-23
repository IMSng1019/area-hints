import React from 'react';
import {registerRoot, Composition} from 'remotion';
import {Film, Soundtrack, loadFilm} from './Film';
import {Cover} from './Cover';
import {graphics} from './scenes';
import edit from '../edit.json';

const format = {width:1920, height:1080, fps:edit.fps};
registerRoot(() => <>
  <Composition id="Area-Hints" component={Film} {...format} durationInFrames={edit.frames}
    defaultProps={{captions:[], showCaptions:true, voiceGain:1, scoreGain:1, gameGain:1}} calculateMetadata={loadFilm}/>
  <Composition id="Cover" component={Cover} {...format} durationInFrames={1}/>
  <Composition id="Soundtrack" component={Soundtrack} width={1920} height={1080} fps={1}
    durationInFrames={edit.frames/edit.fps} defaultProps={{voiceGain:1,scoreGain:1,gameGain:1}}/>
  {Object.entries(graphics).map(([id, component]) =>
    <Composition key={id} id={id} component={component} {...format}
      durationInFrames={edit.clips.find((clip) => clip.path === 'media/' + id + '.mp4').duration}/>)}
</>);
