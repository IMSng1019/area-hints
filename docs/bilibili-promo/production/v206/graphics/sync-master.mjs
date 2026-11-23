import {copyFileSync, existsSync, mkdirSync, statSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {readFileSync} from 'node:fs';

const root = path.dirname(fileURLToPath(import.meta.url));
const production = path.dirname(root);
const dest = path.join(root, 'public', 'master');
mkdirSync(dest, {recursive:true});
const edit = JSON.parse(readFileSync(path.join(production, 'edit.json'), 'utf8'));
const sourceFiles = edit.clips.map((clip) => clip.path).filter((p) => !/\/g\d+\.mp4$/.test(p));
const sound = ['Voice.wav','Score.wav','Game.wav','narration.srt'].map((n) => 'audio/revision/' + n);
for (const relative of new Set([...sourceFiles, ...sound])) {
  const source = path.join(production, relative);
  const target = path.join(dest, path.basename(source));
  if (!existsSync(source)) throw new Error('总剪缺少素材 ' + source);
  if (!existsSync(target) || statSync(target).size !== statSync(source).size ||
      statSync(target).mtimeMs < statSync(source).mtimeMs) copyFileSync(source, target);
}
